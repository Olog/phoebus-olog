package org.phoebus.olog.ai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;

import org.phoebus.olog.entity.Log;
import org.phoebus.olog.entity.State;
import org.phoebus.olog.entity.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class IngestService {
    //azure only allowed up to 96 per bach
    private static final int PAGE_SIZE = 50;
    private static final int BATCH_SIZE = 50;
    private static final Logger logger = LoggerFactory.getLogger(IngestService.class);
    @Value("${elasticsearch.log.index}")
    private String logIndex;

    private final ElasticsearchClient esClient;
    private final ElasticsearchVectorStore vectorStore;

    public IngestService(ElasticsearchClient esClient,
                         ElasticsearchVectorStore vectorStore) {
        this.esClient = esClient;
        this.vectorStore = vectorStore;
    }
    // AUTO-INGEST
    // automatic ingest of created or updated log entry 
    @Async
    @EventListener
    public void onLogEntryCreated(LogEntryCreatedEvent event) {
        try {
            ingestSingle(event.getLog());
        } catch (Exception e) {
            logger.error("Failed to embed log entry {}: {}", event.getLog().getId(), e.getMessage());
        }
    }

    // SINGLE-INGEST
    private void ingestSingle(Log log) {
        OperationLogDocument flat = flattenLog(log);
        String embeddings = buildEmbeddings(flat.getTitle(), flat.getDescription());
        if (embeddings == null || embeddings.isBlank()) {
            logger.debug("Skipping log {} — no embeddable content", flat.getId());
            return;
        }
        Map<String, Object> metadata = buildMetadata(flat);
        vectorStore.add(List.of(new Document(flat.getId(), embeddings, metadata)));
        logger.info("Embedded log entry {}", flat.getId());
    }

    // BULK-INGEST
    // Called manually via POST /ai/ingest to backfill existing entries.

    public void ingestAll() throws IOException {
    int totalIngested = 0;
    String lastId = null;

    while (true) {
        final String currentLastId = lastId;

        SearchResponse<Map> response = esClient.search(
            s -> {
                s.index(logIndex)
                 .size(PAGE_SIZE)
                 .sort(sort -> sort.field(f -> f.field("id")
                     .order(SortOrder.Asc)));
                if (currentLastId != null) {
                    s.searchAfter(FieldValue.of(currentLastId));
                }
                return s;
            },
            Map.class
        );

        List<Hit<Map>> hits = response.hits().hits();
        if (hits.isEmpty()) break;

        processHits(hits);
        totalIngested += hits.size();
        logger.info("Current ingest total: {}", totalIngested);

        //lastId = (String) hits.get(hits.size() - 1).source().get("id");
        lastId = toString(hits.get(hits.size() - 1).source().get("id"));
        if (hits.size() < PAGE_SIZE) break;
    }

    logger.info("Total ingested: {}", totalIngested);
}
    // Processes a page of raw ES hits and sends them to the vector store in batches.
    // Uses flatten() because the input here is a raw ES Map, not a Java Log object.
    private void processHits(List<Hit<Map>> hits) {

    List<Document> docsToInsert = new ArrayList<>();

    for (Hit<Map> hit : hits) {

        Map sourceRaw = hit.source();
        if (sourceRaw == null) {
            continue;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> src = (Map<String, Object>) sourceRaw;

        OperationLogDocument flat = flatten(src);

        String embeddings = buildEmbeddings(flat.getTitle(), flat.getDescription());
        if (embeddings == null || embeddings.isBlank()) {
            continue;  // nothing to embed but there should always be something to embed 
        }
        docsToInsert.add(new Document(flat.getId(), embeddings, buildMetadata(flat)));
    }
    logger.info("Docs to embed (title & description): {}", docsToInsert.size());

        for (int i = 0; i < docsToInsert.size(); i += BATCH_SIZE) {
            List<Document> batch = docsToInsert.subList(i, 
                Math.min(i + BATCH_SIZE, docsToInsert.size()));
            vectorStore.add(batch);
        }
    }
    // Titles and descriptions are the only fields embedded
private String buildEmbeddings(String title, String description) {
        boolean hasTitle       = title != null && !title.isBlank();
        boolean hasDescription = description != null && !description.isBlank();

        if (hasTitle && hasDescription) return title.strip() + "\n" + description.strip();
        if (hasTitle)                   return title.strip();
        if (hasDescription)             return description.strip();
        return null;
    }

// Live Java object for auto-ingest
private Map<String, Object> buildMetadata(OperationLogDocument flat) {
    Map<String, Object> metadata = new HashMap<>();
    if (flat.getId()          != null) metadata.put("id",          flat.getId());
    if (flat.getOwner()       != null) metadata.put("owner",       flat.getOwner());
    if (flat.getTitle()       != null) metadata.put("title",       flat.getTitle());
    if (flat.getSource()      != null) metadata.put("source",      flat.getSource());
    if (flat.getLevel()       != null) metadata.put("level",       flat.getLevel());
    if (flat.getState()       != null) metadata.put("state",       flat.getState());
    if (flat.getCreatedDate() != null) metadata.put("createdDate", flat.getCreatedDate());
    if (flat.getModifyDate()  != null) metadata.put("modifyDate",  flat.getModifyDate());
    metadata.put("logbooks_name", flat.getLogbooksName() != null ? flat.getLogbooksName() : Collections.emptyList());
    metadata.put("tags_name",     flat.getTagsName()     != null ? flat.getTagsName()     : Collections.emptyList());
    metadata.put("events_name",   flat.getEventsName()   != null ? flat.getEventsName()   : Collections.emptyList());
    return metadata;
}
// Live Java object for auto-ingest
private OperationLogDocument flattenLog(Log log) {
    OperationLogDocument doc = new OperationLogDocument();
    doc.setId(toString(log.getId()));
    doc.setOwner(log.getOwner());
    doc.setTitle(log.getTitle());
    doc.setDescription(log.getDescription());
    doc.setSource(log.getSource());
    doc.setLevel(log.getLevel());
    doc.setState(log.getState() != null ? log.getState().toString() : null);
    doc.setCreatedDate(log.getCreatedDate() != null ? log.getCreatedDate().toEpochMilli() : null);
    doc.setModifyDate(log.getModifyDate() != null ? log.getModifyDate().toEpochMilli() : null); 
    doc.setLogbooksName(log.getLogbooks() != null
        ? log.getLogbooks().stream().map(lb -> lb.getName()).filter(n -> n != null).collect(Collectors.toList())
        : Collections.emptyList());
    doc.setTagsName(log.getTags() != null
        ? log.getTags().stream().map(t -> t.getName()).filter(n -> n != null).collect(Collectors.toList())
        : Collections.emptyList());
    doc.setEventsName(log.getEvents() != null
        ? log.getEvents().stream().map(e -> e.getName()).filter(n -> n != null).collect(Collectors.toList())
        : Collections.emptyList());
    return doc;
}
 // Flattens a raw Elasticsearch Map (bulk-ingest).
    @SuppressWarnings("unchecked")
    private OperationLogDocument flatten(Map<String, Object> src) {
        OperationLogDocument doc = new OperationLogDocument();

        doc.setId(toString(src.get("id")));
        doc.setOwner(toString(src.get("owner")));
        doc.setTitle(toString(src.get("title")));
        doc.setDescription(toString(src.get("description")));
        doc.setSource(toString(src.get("source")));
        doc.setLevel(toString(src.get("level")));
        doc.setState(toString(src.get("state")));
        doc.setCreatedDate(toLong(src.get("createdDate")));
        doc.setModifyDate(toLong(src.get("modifyDate")));

        doc.setLogbooksName(extractNestedNames(src, "logbooks", "name"));
        doc.setTagsName(extractNestedNames(src, "tags", "name"));
        doc.setEventsName(extractNestedNames(src, "events", "name"));

        return doc;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractNestedNames(Map<String, Object> src,
                                        String nestedField,
                                        String nameKey) {
        try {
            Object raw = src.get(nestedField);
            if (raw == null) return Collections.emptyList();

            List<String> names = new ArrayList<>();

            if (raw instanceof List) {
                for (Object item : (List<?>) raw) {
                    if (item instanceof Map) {
                        String name = (String) ((Map<String, Object>) item).get(nameKey);
                        if (name != null) names.add(name);
                    }
                }
            } else if (raw instanceof Map) {
                String name = (String) ((Map<String, Object>) raw).get(nameKey);
                if (name != null) names.add(name);
            }

            return names;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String toString(Object value) {
        return value != null ? value.toString() : null;
    }
    
    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

