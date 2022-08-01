/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Repository
public class LogRepository implements CrudRepository<Log, String> {

    private static final Logger logger = Logger.getLogger(LogRepository.class.getName());

    @SuppressWarnings("unused")
    @Value("${elasticsearch.log.index:olog_logs}")
    private String ES_LOG_INDEX;

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
                Set<Attachment> createdAttachments = new HashSet<>();
                log.getAttachments().stream().filter(attachment -> attachment.getAttachment() != null).forEach(attachment -> {
                    createdAttachments.add(attachmentRepository.save(attachment));
                });
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
            logger.log(Level.SEVERE, "Failed to save log entry: " + log, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save log entry: " + log);
        }
        return null;
    }

    @Override
    public <S extends Log> Iterable<S> saveAll(Iterable<S> logs) {
        List<S> createdLogs = new ArrayList<>();
        logs.forEach(log -> {
            createdLogs.add(save(log));
        });
        return createdLogs;
    }

    public Log update(Log log) {
        try {
            Log document = LogBuilder.createLog(log).build();

            IndexRequest<Log> indexRequest =
                    IndexRequest.of(i ->
                            i.index(ES_LOG_INDEX)
                                    .id(String.valueOf(document.getId()))
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
            logger.log(Level.SEVERE, "Failed to save log entry: " + log, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save log entry: " + log);
        }
        return null;
    }

    @Override
    public Optional<Log> findById(String id) {
        try {
            GetRequest getRequest =
                    co.elastic.clients.elasticsearch.core.GetRequest.of(g ->
                            g.index(ES_LOG_INDEX).id(id));
            GetResponse<Log> resp =
                    client.get(getRequest, Log.class);

            if (!resp.found()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Log with id " + id + " not found.");
            }
            return Optional.of(resp.source());
        } catch (Exception e) {
            // https://www.baeldung.com/exception-handling-for-rest-with-spring#controlleradvice
            logger.log(Level.SEVERE, "Failed to retrieve log with id: " + id, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to retrieve log with id: " + id);
        }
    }

    @Override
    public boolean existsById(String logId) {
        try {
            GetRequest getRequest =
                    GetRequest.of(g ->
                            g.index(ES_LOG_INDEX).id(logId));
            GetResponse<Log> resp =
                    client.get(getRequest, Log.class);
            return resp.found();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to check existence of log with id: " + logId, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to check existence of log with id: " + logId);
        }
    }

    @Override
    public Iterable<Log> findAll() {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Retrieving all log entries is not supported. Use Search with scroll.");
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
            logger.log(Level.SEVERE, "Failed to find logs: " + logIds, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to find logs: " + logIds);
        }
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public void deleteById(String id) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Deleting log entries is not supported");
    }

    @Override
    public void delete(Log entity) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Deleting log entries is not supported");
    }

    @Override
    public void deleteAll(Iterable<? extends Log> entities) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Deleting log entries is not supported");
    }

    @Override
    public void deleteAll() {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Deleting log entries is not supported");
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
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to complete search", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to complete search");
        }
    }

    @Override
    public void deleteAllById(Iterable ids) {
        while (ids.iterator().hasNext()) {
            deleteById((String) ids.iterator().next());
        }
    }
}
