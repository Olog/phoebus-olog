package org.phoebus.olog;

import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.DisMaxQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ExistsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.FuzzyQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A utility class for creating a search query for log entries based on time,
 * logbooks, tags, properties, description, etc.
 *
 * @author Kunal Shroff
 */
@Service
public class LogSearchUtil {

    private static final String MILLI_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final DateTimeFormatter MILLI_FORMAT = DateTimeFormatter.ofPattern(MILLI_PATTERN).withZone(ZoneId.systemDefault());

    @SuppressWarnings("unused")
    @Value("${elasticsearch.log.index:olog_logs}")
    private String ES_LOG_INDEX;
    @Value("${elasticsearch.log.type:olog_log}")
    @SuppressWarnings("unused")
    private String ES_LOG_TYPE;
    @Value("${elasticsearch.result.size.search.default:100}")
    private int defaultSearchSize;
    @SuppressWarnings("unused")
    @Value("${elasticsearch.result.size.search.max:1000}")
    private int maxSearchSize;

    private static final Logger LOGGER = Logger.getLogger(LogSearchUtil.class.getName());

    /**
     * @param searchParameters - the various search parameters
     * @return A {@link SearchRequest} based on the provided search parameters
     */
    public SearchRequest buildSearchRequest(MultiValueMap<String, String> searchParameters) {
        BoolQuery.Builder boolQueryBuilder = new Builder();
        boolean fuzzySearch = false;
        List<String> searchTerms = new ArrayList<>();
        List<String> descriptionPhraseSearchTerms = new ArrayList<>();
        List<String> titleSearchTerms = new ArrayList<>();
        List<String> titlePhraseSearchTerms = new ArrayList<>();
        boolean temporalSearch = false;
        boolean includeEvents = false;
        ZonedDateTime start = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
        ZonedDateTime end = ZonedDateTime.now();
        List<String> levelSearchTerms = new ArrayList<>();
        int searchResultSize = defaultSearchSize;
        int from = 0;

        // Default sort order
        SortOrder sortOrder = null;

        for (Entry<String, List<String>> parameter : searchParameters.entrySet()) {
            switch (parameter.getKey().strip().toLowerCase()) {
                case "desc":
                case "description":
                case "text":
                    for (String value : parameter.getValue()) {
                        for (String pattern : getSearchTerms(value)) {
                            String term = pattern.trim().toLowerCase();
                            // Quoted strings will be mapped to a phrase query
                            if (term.startsWith("\"") && term.endsWith("\"")) {
                                descriptionPhraseSearchTerms.add(term.substring(1, term.length() - 1));
                            } else {
                                searchTerms.add(term);
                            }
                        }
                    }
                    break;
                case "title":
                    for (String value : parameter.getValue()) {
                        for (String pattern : getSearchTerms(value)) {
                            String term = pattern.trim().toLowerCase();
                            // Quoted strings will be mapped to a phrase query
                            if (term.startsWith("\"") && term.endsWith("\"")) {
                                titlePhraseSearchTerms.add(term.substring(1, term.length() - 1));
                            } else {
                                titleSearchTerms.add(pattern.trim().toLowerCase());
                            }
                        }
                    }
                    break;
                case "fuzzy":
                    fuzzySearch = true;
                    break;
                case "phrase":
                    DisMaxQuery.Builder phraseQuery = new DisMaxQuery.Builder();
                    List<Query> phraseQueries = new ArrayList<>();
                    for (String value : parameter.getValue()) {
                        phraseQueries.add(MatchPhraseQuery.of(m -> m.field("description").query(value.trim().toLowerCase()))._toQuery());
                    }
                    phraseQuery.queries(phraseQueries);
                    boolQueryBuilder.must(phraseQuery.build()._toQuery());
                    break;
                case "owner":
                    DisMaxQuery.Builder ownerQuery = new DisMaxQuery.Builder();
                    List<Query> ownerQueries = new ArrayList<>();
                    for (String value : parameter.getValue()) {
                        for (String pattern : value.split("[\\|,;\\s+]")) {
                            ownerQueries.add(WildcardQuery.of(w -> w.field("owner")
                                    .caseInsensitive(true)
                                    .value(pattern.trim()))._toQuery());
                        }
                    }
                    ownerQuery.queries(ownerQueries);
                    boolQueryBuilder.must(ownerQuery.build()._toQuery());
                    break;
                case "tags":
                    DisMaxQuery.Builder tagQuery = new DisMaxQuery.Builder();
                    List<Query> tagsQueries = new ArrayList<>();
                    for (String value : parameter.getValue()) {
                        for (String pattern : value.split("[\\|,;]")) {
                            tagsQueries.add(WildcardQuery.of(w -> w.field("tags.name")
                                    .caseInsensitive(true)
                                    .value(pattern.trim()))._toQuery());
                        }
                    }
                    Query tagsQuery = tagQuery.queries(tagsQueries).build()._toQuery();
                    NestedQuery nestedTagsQuery = NestedQuery.of(n -> n.path("tags").query(tagsQuery));
                    boolQueryBuilder.must(nestedTagsQuery._toQuery());
                    break;
                case "logbooks":
                    DisMaxQuery.Builder logbookQuery = new DisMaxQuery.Builder();
                    List<Query> logbooksQueries = new ArrayList<>();
                    for (String value : parameter.getValue()) {
                        for (String pattern : value.split("[\\|,;]")) {
                            logbooksQueries.add(WildcardQuery.of(w -> w.field("logbooks.name")
                                    .caseInsensitive(true)
                                    .value(pattern.trim()))._toQuery());
                        }
                    }
                    Query logbooksQuery = logbookQuery.queries(logbooksQueries).build()._toQuery();
                    NestedQuery nestedLogbooksQuery = NestedQuery.of(n -> n.path("logbooks").query(logbooksQuery).scoreMode(ChildScoreMode.None));
                    boolQueryBuilder.must(nestedLogbooksQuery._toQuery());
                    break;
                case "start":
                    // If there are multiple start times submitted select the earliest
                    ZonedDateTime earliestStartTime = ZonedDateTime.now();
                    for (String value : parameter.getValue()) {
                        ZonedDateTime time = ZonedDateTime.from(MILLI_FORMAT.parse(value));
                        earliestStartTime = earliestStartTime.isBefore(time) ? earliestStartTime : time;
                    }
                    temporalSearch = true;
                    start = earliestStartTime;
                    break;
                case "end":
                    // If there are multiple end times submitted select the latest
                    ZonedDateTime latestEndTime = Instant.ofEpochMilli(Long.MIN_VALUE).atZone(ZoneId.systemDefault());
                    for (String value : parameter.getValue()) {
                        ZonedDateTime time = ZonedDateTime.from(MILLI_FORMAT.parse(value));
                        latestEndTime = latestEndTime.isBefore(time) ? time : latestEndTime;
                    }
                    temporalSearch = true;
                    end = latestEndTime;
                    break;
                case "includeevents":
                case "includeevent":
                    includeEvents = true;
                    break;
                case "properties":
                    DisMaxQuery.Builder propertyQuery = new DisMaxQuery.Builder();
                    for (String value : parameter.getValue()) {
                        for (String pattern : value.split("[\\|,;]")) {
                            String[] propertySearchFields;
                            propertySearchFields = Arrays.copyOf(pattern.split("\\."), 3);
                            BoolQuery.Builder bqb = new BoolQuery.Builder();
                            if (propertySearchFields[0] != null && !propertySearchFields[0].isEmpty()) {
                                bqb.must(WildcardQuery.of(w -> w.field("properties.name")
                                        .caseInsensitive(true)
                                        .value(propertySearchFields[0].trim()))._toQuery());
                            }
                            if (propertySearchFields[1] != null && !propertySearchFields[1].isEmpty()) {
                                BoolQuery.Builder bqb2 = new BoolQuery.Builder();
                                bqb2.must(WildcardQuery.of(w -> w.field("properties.attributes.name")
                                        .caseInsensitive(true)
                                        .value(propertySearchFields[1].trim()))._toQuery());
                                if (propertySearchFields[2] != null && !propertySearchFields[2].isEmpty()) {
                                    bqb2.must(WildcardQuery.of(w -> w.field("properties.attributes.value")
                                            .caseInsensitive(true)
                                            .value(propertySearchFields[2].trim()))._toQuery());
                                }
                                bqb.must(NestedQuery.of(n -> n.path("properties.attributes").query(bqb2.build()._toQuery()).scoreMode(ChildScoreMode.None))._toQuery());
                            }
                            propertyQuery.queries(q -> q.nested(NestedQuery.of(n -> n.path("properties").query(bqb.build()._toQuery()).scoreMode(ChildScoreMode.None))));
                        }
                    }
                    boolQueryBuilder.must(propertyQuery.build()._toQuery());
                    break;
                case "level":
                    for (String value : parameter.getValue()) {
                        for (String pattern : value.split("[\\|,;\\s+]")) {
                            levelSearchTerms.add(pattern.trim().toLowerCase());
                        }
                    }
                    break;
                case "size":
                case "limit":
                    Optional<String> maxSize = parameter.getValue().stream().max(Comparator.comparing(Integer::valueOf));
                    if (maxSize.isPresent()) {
                        try {
                            searchResultSize = Integer.valueOf(maxSize.get());
                        } catch (NumberFormatException e) {
                            LOGGER.log(Level.WARNING, () -> "Cannot parse size value\"" + maxSize.get() + "\" as number");
                        }
                    }
                    break;
                case "from":
                    Optional<String> maxFrom = parameter.getValue().stream().max(Comparator.comparing(Integer::valueOf));
                    if (maxFrom.isPresent()) {
                        try {
                            from = Integer.valueOf(maxFrom.get());
                        } catch (NumberFormatException e) {
                            LOGGER.log(Level.WARNING, () -> "Cannot parse from value\"" + maxFrom.get() + "\" as number");
                        }
                    }
                    break;
                case "sort": // Honor sort order if client specifies it
                    List<String> sortList = parameter.getValue();
                    if (sortList != null && sortList.size() > 0) {
                        String sort = sortList.get(0);
                        if (sort.toUpperCase().startsWith("ASC") || sort.toUpperCase().startsWith("UP")) {
                            sortOrder = SortOrder.Asc;
                        } else if (sort.toUpperCase().startsWith("DESC") || sort.toUpperCase().startsWith("DOWN")) {
                            sortOrder = SortOrder.Desc;
                        }
                    }
                    break;
                case "attachments":
                    DisMaxQuery.Builder attachmentsQuery = new DisMaxQuery.Builder();
                    attachmentsQuery.queries(Collections.emptyList());
                    List<String> parameterValues = parameter.getValue();
                    boolean searchAll = false;
                    for (String value : parameterValues) {
                        for (String pattern : value.split("[\\|,;]")) {
                            // No value for attachments -> search for existence of attachment, regardless of name and type
                            if (pattern == null || "null".equals(pattern) || pattern.isEmpty()) {
                                attachmentsQuery.queries(ExistsQuery.of(e -> e.field("attachments"))._toQuery());
                                searchAll = true;
                                break;
                            } else {
                                attachmentsQuery.queries(WildcardQuery.of(m -> m.field("attachments.filename").caseInsensitive(true).value(pattern.trim()))._toQuery());
                            }
                        }
                        if (searchAll) { // search all -> ignore other parameter values
                            break;
                        }
                    }
                    DisMaxQuery disMaxQuery = attachmentsQuery.build();
                    if (!disMaxQuery.queries().isEmpty()) {
                        boolQueryBuilder.must(disMaxQuery._toQuery());
                    }
                    //boolQueryBuilder.must(attachmentsQuery.build()._toQuery());
                    break;
                default:
                    // Unsupported search parameters are ignored
                    break;
            }
        }

        // Add the temporal queries
        if (temporalSearch) {
            // check that the start is before the end
            if (start.isBefore(end) || start.equals(end)) {
                DisMaxQuery.Builder temporalQuery = new DisMaxQuery.Builder();
                RangeQuery.Builder rangeQuery = new RangeQuery.Builder();
                // Add a query based on the create time
                rangeQuery.field("createdDate").from(Long.toString(1000 * start.toEpochSecond()))
                        .to(Long.toString(1000 * end.toEpochSecond()));
                if (includeEvents) {
                    RangeQuery.Builder eventsRangeQuery = new RangeQuery.Builder();
                    // Add a query based on the time of the associated events
                    eventsRangeQuery.field("events.instant").from(Long.toString(1000 * start.toEpochSecond()))
                            .to(Long.toString(1000 * end.toEpochSecond()));
                    NestedQuery.Builder nestedQuery = new NestedQuery.Builder();
                    nestedQuery.path("events").query(eventsRangeQuery.build()._toQuery());

                    temporalQuery.queries(rangeQuery.build()._toQuery(), nestedQuery.build()._toQuery());
                    boolQueryBuilder.must(temporalQuery.build()._toQuery());
                } else {
                    boolQueryBuilder.must(rangeQuery.build()._toQuery());
                }
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Failed to parse search parameters: " + searchParameters + ", CAUSE: Invalid start and end times");
            }
        }

        // Add the description query. Multiple search terms will be AND:ed.
        if (!searchTerms.isEmpty()) {
            if (fuzzySearch) {
                searchTerms.stream().forEach(searchTerm ->
                    boolQueryBuilder.must(FuzzyQuery.of(f -> f.field("description").value(searchTerm))._toQuery())
                );
            } else {
                searchTerms.stream().forEach(searchTerm ->
                    boolQueryBuilder.must(WildcardQuery.of(w -> w.field("description").value(searchTerm))._toQuery())
                );
            }
        }

        // Add phrase queries for description key. Multiple search terms will be AND:ed.
        if (!descriptionPhraseSearchTerms.isEmpty()) {
            descriptionPhraseSearchTerms.stream().forEach(phraseSearchTerm ->
                boolQueryBuilder.must(MatchPhraseQuery.of(m -> m.field("description").query(phraseSearchTerm))._toQuery())
            );
        }

        // Add the title query. Multiple search terms will be AND:ed.
        if (!titleSearchTerms.isEmpty()) {
            if (fuzzySearch) {
                titleSearchTerms.stream().forEach(searchTerm ->
                    boolQueryBuilder.must(FuzzyQuery.of(f -> f.field("title").value(searchTerm))._toQuery())
                );
            } else {
                titleSearchTerms.stream().forEach(searchTerm ->
                    boolQueryBuilder.must(WildcardQuery.of(w -> w.field("title").value(searchTerm))._toQuery())
                );
            }
        }

        // Add phrase queries for title key. Multiple search terms will be AND:ed.
        if (!titlePhraseSearchTerms.isEmpty()) {
            titlePhraseSearchTerms.stream().forEach(phraseSearchTerm ->
                boolQueryBuilder.must(MatchPhraseQuery.of(m -> m.field("title").query(phraseSearchTerm))._toQuery())
            );
        }

        // Add the level query
        if (!levelSearchTerms.isEmpty()) {
            DisMaxQuery.Builder levelQuery = new DisMaxQuery.Builder();
            List<Query> levelQueries = new ArrayList<>();
            if (fuzzySearch) {
                levelSearchTerms.stream().forEach(searchTerm ->
                    levelQueries.add(FuzzyQuery.of(f -> f.field("level").value(searchTerm))._toQuery())
                );
            } else {
                levelSearchTerms.stream().forEach(searchTerm ->
                    levelQueries.add(WildcardQuery.of(w -> w.field("level").value(searchTerm))._toQuery())
                );
            }
            levelQuery.queries(levelQueries);
            boolQueryBuilder.must(levelQuery.build()._toQuery());
        }

        int _searchResultSize = searchResultSize;
        int _from = from;
        FieldSort.Builder fb = new FieldSort.Builder();
        fb.field("createdDate");
        fb.order(sortOrder);

        return SearchRequest.of(s -> s.index(ES_LOG_INDEX)
                .query(boolQueryBuilder.build()._toQuery())
                .timeout("60s")
                .sort(SortOptions.of(so -> so.field(fb.build())))
                .size(Math.min(_searchResultSize, maxSearchSize))
                .from(_from));
    }

    /**
     * Parses a search query terms string into a string array. In particular,
     * quoted search terms must be maintained even if they contain the
     * separator chars used to tokenize the terms.
     *
     * @param searchQueryTerms String as specified by client
     * @return A {@link List} of search terms, some of which may be
     * quoted. Is void of any zero-length strings.
     */
    public List<String> getSearchTerms(String searchQueryTerms) {
        // Count double quote chars. Odd number of quote chars
        // is not supported -> throw exception
        long quoteCount = searchQueryTerms.chars().filter(c -> c == '\"').count();
        if (quoteCount == 0) {
            return Arrays.stream(searchQueryTerms.split("[\\|,;\\s+]")).filter(t -> t.length() > 0).collect(Collectors.toList());
        }
        if (quoteCount % 2 == 1) {
            throw new IllegalArgumentException("Unbalanced quotes in search query");
        }
        // If we come this far then at least one quoted term is
        // contained in user input
        List<String> terms = new ArrayList<>();
        int nextStartIndex = searchQueryTerms.indexOf('\"');
        while (nextStartIndex >= 0) {
            int endIndex = searchQueryTerms.indexOf('\"', nextStartIndex + 1);
            String quotedTerm = searchQueryTerms.substring(nextStartIndex, endIndex + 1);
            terms.add(quotedTerm);
            // Remove the quoted term from user input
            searchQueryTerms = searchQueryTerms.replace(quotedTerm, "");
            // Check next occurrence
            nextStartIndex = searchQueryTerms.indexOf('\"');
        }
        // Add remaining terms...
        List<String> remaining = Arrays.asList(searchQueryTerms.split("[\\|,;\\s+]"));
        //...but remove empty strings, which are "leftovers" when quoted terms are removed
        terms.addAll(remaining.stream().filter(t -> t.length() > 0).collect(Collectors.toList()));
        return terms;
    }
}
