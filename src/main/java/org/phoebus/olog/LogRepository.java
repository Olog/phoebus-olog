/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog;

import static org.phoebus.olog.ElasticConfig.ES_LOG_ARCHIVE_INDEX;
import static org.phoebus.olog.ElasticConfig.ES_LOG_INDEX;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.CombinedFieldsOperator;
import co.elastic.clients.elasticsearch._types.query_dsl.CombinedFieldsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.DisMaxQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import co.elastic.clients.elasticsearch.core.ExistsRequest;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.MgetRequest;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.mget.MultiGetResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.phoebus.olog.entity.Attachment;
import org.phoebus.olog.entity.Log;
import org.phoebus.olog.entity.Log.LogBuilder;
import org.phoebus.olog.entity.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class LogRepository implements CrudRepository<Log, String> {

  private static final Logger logger = Logger.getLogger(LogRepository.class.getName());

  @SuppressWarnings("unused")
  @Autowired
  @Qualifier("client")
  ElasticsearchClient client;

  @Autowired AttachmentRepository attachmentRepository;

  @Autowired SequenceGenerator generator;

  @Override
  public <S extends Log> S save(S log) {
    try {
      Long id = generator.getID();
      LogBuilder validatedLog = LogBuilder.createLog(log).id(id).createDate(Instant.now());
      if (log.getAttachments() != null && !log.getAttachments().isEmpty()) {
        Set<Attachment> createdAttachments = new HashSet<>();
        log.getAttachments().stream()
            .filter(attachment -> attachment.getAttachment() != null)
            .forEach(attachment -> createdAttachments.add(attachmentRepository.save(attachment)));
        validatedLog = validatedLog.setAttachments(createdAttachments);
      }

      Log document = validatedLog.build();

      IndexRequest<Object> indexRequest =
          IndexRequest.of(
              i ->
                  i.index(ES_LOG_INDEX)
                      .id(String.valueOf(id))
                      .document(document)
                      .refresh(Refresh.True));
      IndexResponse response = client.index(indexRequest);

      if (response.result().equals(Result.Created)) {
        GetRequest getRequest = GetRequest.of(g -> g.index(ES_LOG_INDEX).id(response.id()));
        GetResponse<Log> resp = client.get(getRequest, Log.class);
        return (S) resp.source();
      }
    } catch (Exception e) {
      String message = MessageFormat.format(TextUtil.LOG_NOT_SAVED, log);
      logger.log(Level.SEVERE, message, e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
    return null;
  }

  @Override
  public <S extends Log> Iterable<S> saveAll(Iterable<S> logs) {
    List<S> createdLogs = new ArrayList<>();
    logs.forEach(log -> createdLogs.add(save(log)));
    return createdLogs;
  }

  public Log update(Log log) {
    try {
      Log document = LogBuilder.createLog(log).build();
      IndexRequest<Log> indexRequest =
          IndexRequest.of(
              i ->
                  i.index(ES_LOG_INDEX)
                      .id(String.valueOf(document.getId()))
                      .refresh(Refresh.True)
                      .document(document));

      IndexResponse response = client.index(indexRequest);

      if (response.result().equals(Result.Updated)) {
        GetRequest getRequest = GetRequest.of(g -> g.index(ES_LOG_INDEX).id(response.id()));
        GetResponse<Log> resp = client.get(getRequest, Log.class);
        return resp.source();
      }
    } catch (Exception e) {
      String message = MessageFormat.format(TextUtil.LOG_NOT_UPDATED, log);
      logger.log(Level.SEVERE, message, e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
    return null;
  }

  public Log archive(Log log) {
    try {
      // retrieve the log version from elastic
      GetResponse<Log> resp =
          client.get(
              GetRequest.of(g -> g.index(ES_LOG_INDEX).id(String.valueOf(log.getId()))), Log.class);
      if (!resp.found()) {
        logger.log(
            Level.SEVERE, () -> MessageFormat.format(TextUtil.LOG_NOT_ARCHIVED, log.getId()));
      } else {
        Log originalDocument = resp.source();
        String updatedVersion = originalDocument.getId() + "_v" + resp.version();
        IndexRequest<Log> indexRequest =
            IndexRequest.of(
                i ->
                    i.index(ES_LOG_ARCHIVE_INDEX)
                        .id(updatedVersion)
                        .document(originalDocument)
                        .refresh(Refresh.True));
        IndexResponse response = client.index(indexRequest);
        if (response.result().equals(Result.Created)) {
          GetRequest getRequest =
              GetRequest.of(g -> g.index(ES_LOG_ARCHIVE_INDEX).id(response.id()));
          return client.get(getRequest, Log.class).source();
        } else {
          logger.log(
              Level.SEVERE, () -> MessageFormat.format(TextUtil.LOG_NOT_ARCHIVED, updatedVersion));
        }
      }
    } catch (IOException e) {
      logger.log(Level.SEVERE, MessageFormat.format(TextUtil.LOG_NOT_ARCHIVED, log.getId()), e);
    }
    return null;
  }

  public SearchResult findArchivedById(String id) {
    FieldSort.Builder fb = new FieldSort.Builder();
    fb.field("modifyDate");
    fb.order(SortOrder.Desc);

    SearchRequest searchRequest =
        SearchRequest.of(
            s ->
                s.index(ES_LOG_ARCHIVE_INDEX)
                    .query(
                        WildcardQuery.of(q -> q.field("id").caseInsensitive(true).value(id + "*"))
                            ._toQuery())
                    .timeout("60s")
                    .sort(SortOptions.of(so -> so.field(fb.build()))));
    try {
      final SearchResponse<Log> searchResponse = client.search(searchRequest, Log.class);
      List<Log> result =
          searchResponse.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
      SearchResult searchResult = new SearchResult();
      searchResult.setHitCount(searchResponse.hits().total().value());
      searchResult.setLogs(result);
      return searchResult;
    } catch (IOException | IllegalArgumentException e) {
      logger.log(Level.SEVERE, TextUtil.LOGS_SEARCH_NOT_COMPLETED, e);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, TextUtil.LOGS_SEARCH_NOT_COMPLETED);
    }
  }

  @Override
  public Optional<Log> findById(String id) {
    try {
      GetRequest getRequest = GetRequest.of(g -> g.index(ES_LOG_INDEX).id(id));
      GetResponse<Log> resp = client.get(getRequest, Log.class);

      if (!resp.found()) {
        throw new ResponseStatusException(
            HttpStatus.NOT_FOUND, MessageFormat.format(TextUtil.LOG_NOT_FOUND, id));
      }
      return Optional.of(resp.source());
    } catch (Exception e) {
      // https://www.baeldung.com/exception-handling-for-rest-with-spring#controlleradvice
      String message = MessageFormat.format(TextUtil.LOG_NOT_RETRIEVED, id);
      logger.log(Level.SEVERE, message, e);
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
  }

  @Override
  public boolean existsById(String logId) {
    try {
      ExistsRequest existsRequest = ExistsRequest.of(e -> e.index(ES_LOG_INDEX).id(logId));
      return client.exists(existsRequest).value();
    } catch (IOException e) {
      String message = MessageFormat.format(TextUtil.LOG_EXISTS_FAILED, logId);
      logger.log(Level.SEVERE, message, e);
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
  }

  @Override
  public Iterable<Log> findAll() {
    throw new ResponseStatusException(
        HttpStatus.NOT_FOUND, TextUtil.LOGS_RETRIEVE_ALL_NOT_SUPPORTED);
  }

  @Override
  public Iterable<Log> findAllById(Iterable<String> logIds) {
    List<String> ids = new ArrayList<>();
    logIds.forEach(ids::add);
    MgetRequest mgetRequest = MgetRequest.of(r -> r.index(ES_LOG_INDEX).ids(ids));
    try {
      List<Log> foundLogs = new ArrayList<>();
      MgetResponse<Log> resp = client.mget(mgetRequest, Log.class);
      for (MultiGetResponseItem<Log> multiGetResponseItem : resp.docs()) {
        if (!multiGetResponseItem.isFailure()) {
          foundLogs.add(multiGetResponseItem.result().source());
        }
      }
      return foundLogs;
    } catch (Exception e) {
      String message = MessageFormat.format(TextUtil.LOGS_NOT_FOUND, logIds);
      logger.log(Level.SEVERE, message, e);
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
  }

  @Override
  public long count() {
    return 0;
  }

  @Override
  public void deleteById(String id) {
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, TextUtil.LOGS_DELETE_NOT_SUPPORTED);
  }

  @Override
  public void delete(Log entity) {
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, TextUtil.LOGS_DELETE_NOT_SUPPORTED);
  }

  @Override
  public void deleteAll(Iterable<? extends Log> entities) {
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, TextUtil.LOGS_DELETE_NOT_SUPPORTED);
  }

  @Override
  public void deleteAll() {
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, TextUtil.LOGS_DELETE_NOT_SUPPORTED);
  }

  public void debugFieldMappingJson() {
    try {
      // Get a few sample documents to see the actual data structure
      SearchRequest sampleRequest =
          SearchRequest.of(
              s ->
                  s.index(ES_LOG_INDEX)
                      .query(q -> q.matchAll(m -> m))
                      .size(3) // Get 3 sample documents
              );

      SearchResponse<Log> response = client.search(sampleRequest, Log.class);

      logger.log(Level.INFO, "=== ACTUAL DOCUMENT CONTENT ===");
      logger.log(Level.INFO, "Total documents in index: " + response.hits().total().value());

      if (response.hits().total().value() > 0) {
        List<Hit<Log>> hitsList = response.hits().hits();
        for (int index = 0; index < hitsList.size(); index++) {
          Hit<Log> hit = hitsList.get(index);
          Log log = hit.source();
          logger.log(Level.INFO, "\n--- Document " + (index + 1) + " ---");
          logger.log(Level.INFO, "ID: " + hit.id());
          logger.log(Level.INFO, "Title: '" + log.getTitle() + "'");
          logger.log(
              Level.INFO,
              "Description: '"
                  + (log.getDescription() != null
                      ? log.getDescription()
                              .substring(0, Math.min(100, log.getDescription().length()))
                          + "..."
                      : "null")
                  + "'");
          logger.log(Level.INFO, "Owner: '" + log.getOwner() + "'");

          // Inspect Tags structure
          if (log.getTags() != null && !log.getTags().isEmpty()) {
            logger.log(Level.INFO, "Tags found: " + log.getTags().size());
            log.getTags()
                .forEach(
                    tag -> {
                      logger.log(
                          Level.INFO,
                          "  Tag: name='" + tag.getName() + "', state='" + tag.getState() + "'");

                      // Check if there are any other properties we might have missed
                      logger.log(Level.INFO, "  Tag class: " + tag.getClass().getName());

                      // Try to see all fields of the tag object
                      try {
                        java.lang.reflect.Field[] fields = tag.getClass().getDeclaredFields();
                        for (java.lang.reflect.Field field : fields) {
                          field.setAccessible(true);
                          Object value = field.get(tag);
                          logger.log(Level.INFO, "  Tag field '" + field.getName() + "': " + value);
                        }
                      } catch (Exception e) {
                        logger.log(Level.INFO, "  Could not inspect tag fields: " + e.getMessage());
                      }
                    });
          } else {
            logger.log(Level.INFO, "No tags found in this document");
          }

          // Inspect Logbooks structure
          if (log.getLogbooks() != null && !log.getLogbooks().isEmpty()) {
            logger.log(Level.INFO, "Logbooks found: " + log.getLogbooks().size());
            log.getLogbooks()
                .forEach(
                    logbook -> {
                      logger.log(
                          Level.INFO,
                          "  Logbook: name='"
                              + logbook.getName()
                              + "', owner='"
                              + logbook.getOwner()
                              + "', state='"
                              + logbook.getState()
                              + "'");
                    });
          } else {
            logger.log(Level.INFO, "No logbooks found in this document");
          }
        }
      } else {
        logger.log(Level.INFO, "No documents found in index!");
      }

    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error debugging document content: " + e.getMessage(), e);
    }
  }

  @Autowired LogSearchUtil logSearchUtil;

  public SearchResult search(MultiValueMap<String, String> searchParameters) {

    SearchRequest searchRequest = logSearchUtil.buildSearchRequest(searchParameters);
    try {
      debugFieldMappingJson();
      String query = searchParameters.getFirst("query");

      if (query == null || query.isBlank()) {
        try {
          final SearchResponse<Log> searchResponse = client.search(searchRequest, Log.class);
          List<Log> result =
              searchResponse.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
          SearchResult searchResult = new SearchResult();
          searchResult.setHitCount(searchResponse.hits().total().value());
          searchResult.setLogs(result);
          return searchResult;
        } catch (IOException | IllegalArgumentException e) {
          logger.log(Level.SEVERE, TextUtil.SEARCH_NOT_COMPLETED, e);
          throw new ResponseStatusException(
              HttpStatus.INTERNAL_SERVER_ERROR, TextUtil.SEARCH_NOT_COMPLETED);
        }
      }

      BoolQuery hybridQuery =
          BoolQuery.of(
              b ->
                  b/*.should(
                          MultiMatchQuery.of(
                                  m ->
                                      m.query(query)
                                          .fields("title^5", "description^4","owner^2")
                                          //.type(TextQueryType.CrossFields)
                                          //.fuzziness("AUTO")
                                          .operator(Operator.Or) // Fixed: changed from And
                                          .boost(3.0f))
                              ._toQuery())*/
                      .should(getTagsQuery(query))
                          /*NestedQuery.of(
                                  n ->
                                      n.path("tags")
                                          .query(
                                              q ->
                                                  q.wildcard( // Use wildcard for keyword fields
                                                      w ->
                                                          w.field("name") // CHANGE: Just "name"
                                                              .caseInsensitive(true)
                                                              .value("*" + query + "*"))))
                              ._toQuery())*/
                          .should(QueryStringQuery.of(
                                          q ->
                                                  q.query("*" + query + "*")
                                                          .fields("title^5", "description^4", "owner^3", "level^1")
                                                          .defaultOperator(Operator.And)
                                                          .analyzeWildcard(true)
                                                          .allowLeadingWildcard(true)
                                                          .boost(2.0f))
                                  ._toQuery())

                      .minimumShouldMatch("1"));

      SearchRequest request =
          SearchRequest.of(
              s ->
                  s.index(ES_LOG_INDEX)
                      .query(hybridQuery._toQuery())
                      .timeout("60s")
                      .size(10000)
                      .from(0)
                      .sort(SortOptions.of(so -> so.score(score -> score.order(SortOrder.Desc)))));

      SearchResponse<Log> searchResponse = client.search(request, Log.class);

      // Log the scores and explanations
      searchResponse
          .hits()
          .hits()
          .forEach(
              hit -> {
                logger.log(
                    Level.INFO,
                    () ->
                        String.format(
                            "Doc ID: %s, Score: %f, Title: %s, Description: %s, Explanation: %s",
                            hit.id(),
                            hit.score(),
                            hit.source().getTitle(),
                            hit.source().getDescription(),
                            hit.explanation() != null ? hit.explanation().description() : "N/A"));
              });

      List<Log> result =
          searchResponse.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
      SearchResult searchResult = new SearchResult();
      searchResult.setHitCount(searchResponse.hits().total().value());
      searchResult.setLogs(result);
      return searchResult;
    } catch (IOException | IllegalArgumentException e) {
      logger.log(Level.SEVERE, TextUtil.SEARCH_NOT_COMPLETED, e);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, TextUtil.SEARCH_NOT_COMPLETED);
    }
  }

  @Override
  public void deleteAllById(Iterable ids) {
    Iterator<String> iterator = ids.iterator();
    while (iterator.hasNext()) {
      deleteById(iterator.next());
    }
  }

  private Query getTagsQuery(String query){
      String[] items = query.split(" ");
      DisMaxQuery.Builder tagQuery = new DisMaxQuery.Builder();
      List<Query> tagsQueries = new ArrayList<>();
      for (String pattern : items) {
          tagsQueries.add(WildcardQuery.of(w -> w.field("tags.name")
                  .caseInsensitive(true)
                  .value(pattern.trim()))._toQuery());
      }

      Query tagsQuery = tagQuery.queries(tagsQueries).build()._toQuery();
      NestedQuery nestedTagsQuery = NestedQuery.of(n -> n.path("tags").query(tagsQuery));
      return nestedTagsQuery._toQuery();
  }
}
