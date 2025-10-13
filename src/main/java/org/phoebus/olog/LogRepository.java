/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
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

import java.io.IOException;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.phoebus.olog.ElasticConfig.ES_LOG_INDEX;
import static org.phoebus.olog.ElasticConfig.ES_LOG_ARCHIVE_INDEX;

@Repository
public class LogRepository implements CrudRepository<Log, String> {

    private static final Logger logger = Logger.getLogger(LogRepository.class.getName());

    @SuppressWarnings("unused")
    @Autowired
    @Qualifier("client")
    ElasticsearchClient client;

    @Autowired
    AttachmentRepository attachmentRepository;

    @Autowired
    SequenceGenerator generator;

    @Override
    public <S extends Log> S save(S log) {
        try {
            Long id = generator.getID();
            LogBuilder validatedLog = LogBuilder.createLog(log).id(id).createDate(Instant.now());
            if (log.getAttachments() != null && !log.getAttachments().isEmpty()) {
                SortedSet<Attachment> createdAttachments = new TreeSet<>();
                log.getAttachments().stream().filter(attachment -> attachment.getAttachment() != null).forEach(attachment ->
                        createdAttachments.add(attachmentRepository.save(attachment))
                );
                validatedLog = validatedLog.setAttachments(createdAttachments);
            }

            Log document = validatedLog.build();

            IndexRequest<Object> indexRequest =
                    IndexRequest.of(i ->
                            i.index(ES_LOG_INDEX)
                                    .id(String.valueOf(id))
                                    .document(document)
                                    .refresh(Refresh.True));
            IndexResponse response = client.index(indexRequest);

            if (response.result().equals(Result.Created)) {
                GetRequest getRequest =
                        GetRequest.of(g ->
                                g.index(ES_LOG_INDEX).id(response.id()));
                GetResponse<Log> resp =
                        client.get(getRequest, Log.class);
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
        logs.forEach(log ->
                createdLogs.add(save(log))
        );
        return createdLogs;
    }

    public Log update(Log log) {
        try {
            Log document = LogBuilder.createLog(log).build();
            IndexRequest<Log> indexRequest =
                    IndexRequest.of(i ->
                            i.index(ES_LOG_INDEX)
                                    .id(String.valueOf(document.getId()))
                                    .refresh(Refresh.True)
                                    .document(document));

            IndexResponse response = client.index(indexRequest);

            if (response.result().equals(Result.Updated)) {
                GetRequest getRequest =
                        GetRequest.of(g ->
                                g.index(ES_LOG_INDEX).id(response.id()));
                GetResponse<Log> resp =
                        client.get(getRequest, Log.class);
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
            GetResponse<Log> resp = client.get(GetRequest.of(g ->
                    g.index(ES_LOG_INDEX).id(String.valueOf(log.getId()))), Log.class);
            if (!resp.found()) {
                logger.log(Level.SEVERE, () -> MessageFormat.format(TextUtil.LOG_NOT_ARCHIVED, log.getId()));
            } else {
                Log originalDocument = resp.source();
                String updatedVersion = originalDocument.getId() + "_v" + resp.version();
                IndexRequest<Log> indexRequest =
                        IndexRequest.of(i ->
                                i.index(ES_LOG_ARCHIVE_INDEX)
                                        .id(updatedVersion)
                                        .document(originalDocument)
                                        .refresh(Refresh.True));
                IndexResponse response = client.index(indexRequest);
                if (response.result().equals(Result.Created)) {
                    GetRequest getRequest =
                            GetRequest.of(g ->
                                    g.index(ES_LOG_ARCHIVE_INDEX).id(response.id()));
                    return client.get(getRequest, Log.class).source();
                } else {
                    logger.log(Level.SEVERE, () -> MessageFormat.format(TextUtil.LOG_NOT_ARCHIVED, updatedVersion));
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

        SearchRequest searchRequest = SearchRequest.of(s -> s.index(ES_LOG_ARCHIVE_INDEX)
                .query(WildcardQuery.of(q -> q.field("id").caseInsensitive(true).value(id + "*"))._toQuery())
                .timeout("60s")
                .sort(SortOptions.of(so -> so.field(fb.build()))));
        try {
            final SearchResponse<Log> searchResponse = client.search(searchRequest, Log.class);
            List<Log> result = searchResponse.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
            SearchResult searchResult = new SearchResult();
            searchResult.setHitCount(searchResponse.hits().total().value());
            searchResult.setLogs(result);
            return searchResult;
        } catch (IOException | IllegalArgumentException e) {
            logger.log(Level.SEVERE, TextUtil.LOGS_SEARCH_NOT_COMPLETED, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, TextUtil.LOGS_SEARCH_NOT_COMPLETED);
        }
    }

    @Override
    public Optional<Log> findById(String id) {
        try {
            GetRequest getRequest =
                    GetRequest.of(g ->
                            g.index(ES_LOG_INDEX).id(id));
            GetResponse<Log> resp =
                    client.get(getRequest, Log.class);

            if (!resp.found()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, MessageFormat.format(TextUtil.LOG_NOT_FOUND, id));
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
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, TextUtil.LOGS_RETRIEVE_ALL_NOT_SUPPORTED);
    }

    @Override
    public Iterable<Log> findAllById(Iterable<String> logIds) {
        List<String> ids = new ArrayList<>();
        logIds.forEach(ids::add);
        MgetRequest mgetRequest =
                MgetRequest.of(r -> r.index(ES_LOG_INDEX).ids(ids));
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

    @Autowired
    LogSearchUtil logSearchUtil;

    public SearchResult search(MultiValueMap<String, String> searchParameters) {

        SearchRequest searchRequest = logSearchUtil.buildSearchRequest(searchParameters);
        try {
            final SearchResponse<Log> searchResponse = client.search(searchRequest, Log.class);
            List<Log> result = searchResponse.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
            SearchResult searchResult = new SearchResult();
            searchResult.setHitCount(searchResponse.hits().total().value());
            searchResult.setLogs(result);
            return searchResult;
        } catch (IOException | IllegalArgumentException e) {
            logger.log(Level.SEVERE, TextUtil.SEARCH_NOT_COMPLETED, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, TextUtil.SEARCH_NOT_COMPLETED);
        }
    }

    @Override
    public void deleteAllById(Iterable ids) {
        Iterator<String> iterator = ids.iterator();
        while (iterator.hasNext()) {
            deleteById(iterator.next());
        }
    }
}
