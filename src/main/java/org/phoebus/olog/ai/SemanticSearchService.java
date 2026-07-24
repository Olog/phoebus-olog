package org.phoebus.olog.ai;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SemanticSearchService {

    private final QueryPlannerService plannerService;
    private final ElasticsearchVectorStore vectorStore;
    private final ChatClient chatClient;
    private static final Logger logger = LoggerFactory.getLogger(SemanticSearchService.class);

    public SemanticSearchService(QueryPlannerService plannerService,
                                ElasticsearchVectorStore vectorStore,
                                ChatClient.Builder chatClientBuilder) {
        this.plannerService = plannerService;
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
    }

    public SimpleSearchResponse search(SearchQueryRequest request) {

        if (!StringUtils.hasText(request.getQuery())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "query must not be empty");
        }

        QueryPlan plan = plannerService.plan(request.getQuery());
        String semanticQuery = StringUtils.hasText(plan.getSemanticQuery())
                ? plan.getSemanticQuery()
                : request.getQuery();

        Filter.Expression llmFilter = parseLlmFilter(plan.getFilterExpression());
        Filter.Expression dateFilter = buildDateFilter(
                request.getCreatedDateFrom(),
                request.getCreatedDateTo());
        Filter.Expression uiFilter = buildUiFilter(request);

        Filter.Expression finalFilter = combineFilters(llmFilter, dateFilter, uiFilter);

        // title/desc drawer inputs aren't exact-filterable in the vector store;
        // fold them into the semantic query as extra relevance signal
        StringBuilder sq = new StringBuilder(semanticQuery);
        if (StringUtils.hasText(request.getTitle())) {
            sq.append(" ").append(request.getTitle().trim());
        }
        if (StringUtils.hasText(request.getDesc())) {
            sq.append(" ").append(request.getDesc().trim());
        }
        semanticQuery = sq.toString();

        SearchRequest searchRequest = SearchRequest.builder()
                .query(semanticQuery)
                .topK(20)
                .filterExpression(finalFilter)   // may be null
                .build();

        List<Document> docs = vectorStore.similaritySearch(searchRequest);

        List<SearchHitDto> hits = docs.stream()
                .map(d -> new SearchHitDto(d.getText(), d.getMetadata()))
                .collect(Collectors.toList());

        return new SimpleSearchResponse(hits);
    }
    
    public AnalysisResponse analyze(AnalysisRequest request) {
        List<SearchHitDto> hits = request.getHits();
        if (hits == null || hits.isEmpty()) {
            return new AnalysisResponse(
                "No matching log entries were found for this query.",
                Collections.emptyList());
        }
        String analysis = analyzeWithLlm(request.getQuery(), hits);
        return new AnalysisResponse(analysis, hits);
    }
    
    /**
     * Build a filter expression that checks either createdDate OR eventStart
     * falls inside the given date range (yyyy-MM-dd).
     */
    private static final DateTimeFormatter UI_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Long toEpochMillis(String uiDate) {
        if (!StringUtils.hasText(uiDate)) {
            return null;
        }
        try {
            return LocalDateTime.parse(uiDate.trim(), UI_DATE_FORMAT)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        } catch (DateTimeParseException e) {
            logger.warn("Unparseable date filter value '{}', ignoring", uiDate);
            return null;
        }
    }

    private Filter.Expression parseLlmFilter(String llmFilter) {
        if (!StringUtils.hasText(llmFilter)) {
            return null;
        }
                 // Date constraints must come only from UI filters, never from the LLM.
         String normalized = llmFilter.toLowerCase();
         if (normalized.contains("createddate") || normalized.contains("modifydate")) {
             logger.warn("Ignoring LLM filter that attempts to constrain dates: {}", llmFilter);
             return null;
         }
        try {
            return new FilterExpressionTextParser().parse(llmFilter);
        } catch (Exception e) {
            logger.warn("Ignoring unparseable LLM filter '{}': {}", llmFilter, e.getMessage());
            return null;
        }
    }

    private Filter.Expression buildDateFilter(String fromDate, String toDate) {
        Long fromMs = toEpochMillis(fromDate);
        Long toMs = toEpochMillis(toDate);

        if (fromMs == null && toMs == null) {
            return null;
        }

        FilterExpressionBuilder b = new FilterExpressionBuilder();
        // entry matches if createdDate OR modifyDate falls in range
        return b.or(
                rangeOp(b, "createdDate", fromMs, toMs),
                rangeOp(b, "modifyDate", fromMs, toMs)
        ).build();
    }

    private FilterExpressionBuilder.Op rangeOp(FilterExpressionBuilder b,
                                               String field, Long fromMs, Long toMs) {
        if (fromMs != null && toMs != null) {
            return b.and(b.gte(field, fromMs), b.lt(field, toMs));
        }
        if (fromMs != null) {
            return b.gte(field, fromMs);
        }
        return b.lt(field, toMs);
    }

    private Filter.Expression buildUiFilter(SearchQueryRequest request) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        List<FilterExpressionBuilder.Op> ops = new ArrayList<>();

        if (request.getLogbooks() != null && !request.getLogbooks().isEmpty()) {
            ops.add(b.in("logbooks_name", request.getLogbooks().toArray()));
        }
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            ops.add(b.in("tags_name", request.getTags().toArray()));
        }
        if (request.getLevels() != null && !request.getLevels().isEmpty()) {
            ops.add(b.in("level", request.getLevels().toArray()));
        }
        if (StringUtils.hasText(request.getOwner())) {
            ops.add(b.eq("owner", request.getOwner().trim()));
        }

        if (ops.isEmpty()) {
            return null;
        }

        FilterExpressionBuilder.Op combined = ops.get(0);
        for (int i = 1; i < ops.size(); i++) {
            combined = b.and(combined, ops.get(i));
        }
        return combined.build();
    }

    private Filter.Expression combineFilters(Filter.Expression... expressions) {
        Filter.Expression result = null;
        for (Filter.Expression e : expressions) {
            if (e == null) {
                continue;
            }
            result = (result == null)
                    ? e
                    : new Filter.Expression(Filter.ExpressionType.AND, result, e);
        }
        return result;
    }

    private String analyzeWithLlm(String originalQuestion, List<SearchHitDto> hits) {

        if (hits.isEmpty()) {
            return "No matching log entries were found for this query.";
        }

        // LLM context
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            SearchHitDto h = hits.get(i);
            sb.append("Entry #").append(i + 1).append(":\n");
            sb.append("title and description: ").append(h.getContent()).append("\n");

            if (h.getMetadata() != null) {
                Object owner = h.getMetadata().get("owner");
                Object title = h.getMetadata().get("title");
                Object createdDate = h.getMetadata().get("createdDate");
                Object modifyDate = h.getMetadata().get("modifyDate");
                Object level = h.getMetadata().get("level");
                Object state = h.getMetadata().get("state");
                Object logbook = h.getMetadata().get("logbooks_name");
                Object tags = h.getMetadata().get("tags_name");
                Object events = h.getMetadata().get("events_name");


                if (owner != null) {
                    sb.append("owner: ").append(owner).append("\n");
                }
                if (title != null) {
                    sb.append("title: ").append(title).append("\n");
                }
                if (createdDate != null) {
                    sb.append("createdDate: ").append(createdDate).append("\n");
                }
                if (modifyDate != null) {
                    sb.append("modifyDate: ").append(modifyDate).append("\n");
                }
                if (level != null) {
                    sb.append("level: ").append(level).append("\n");
                }
                if (state != null) {
                    sb.append("state: ").append(state).append("\n");
                }
                if (logbook != null) {
                    sb.append("logbook: ").append(logbook).append("\n");
                }
                if (tags != null) {
                    sb.append("tags: ").append(tags).append("\n");
                }
                if (events != null) {
                    sb.append("events: ").append(events).append("\n");
                }
            }
            sb.append("\n");
        }

        String context = sb.toString();

        String systemPrompt = """
            You are an expert system administrator analyzing log entries.
                - Your task is to determine what the log entries show in relation to the user's question.
                - Focus on extracting insights from the log content and metadata.
                - Pay special attention to any patterns, anomalies, or important details that relate to the question.
            
            CRITICAL FORMATTING RULE:
            - Every time you reference a specific log entry, you MUST cite it using the format #N (e.g., #1, #5, #12).
            - If multiple entries support the same point, list them all: (#3, #7, #11).
            
            
            RESPONSE STRUCTURE:
            **Summary**
            - 2-3 bulletpoint overview of what the logs show in relation to the question. Cite entries inline as #N.
            
            **Key Findings**
            - List 3-5 specific observations. 
            - Point out anything important (e.g. major alarms, repeated issues, who owns the entries).
            - Always cite the relevant log entries for each finding using #N format.
            
            Be concise but specific. Do NOT invent entries that are not in the list.
            """;

        String userMessage = """
            User question:
            %s

            Retrieved log entries:
            %s

            Based on these entries, explain what they show and how they relate to the user's question.
            """.formatted(originalQuestion, context);

        return this.chatClient
                .prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();
    }
}

