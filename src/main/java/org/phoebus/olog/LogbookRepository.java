/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.ExistsRequest;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.MgetRequest;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.core.UpdateResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.core.mget.MultiGetResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.phoebus.olog.ElasticConfig.ES_LOGBOOK_INDEX;

@Repository
public class LogbookRepository implements CrudRepository<Logbook, String> {

    @SuppressWarnings("unused")
    @Value("${elasticsearch.result.size.logbooks:100}")
    private int logbooksResultSize;

    @SuppressWarnings("unused")
    @Autowired
    @Qualifier("client")
    private ElasticsearchClient client;

    private final Logger logger = Logger.getLogger(LogbookRepository.class.getName());

    @Override
    public <S extends Logbook> S save(S logbook) {
        try {
            IndexRequest<Logbook> indexRequest =
                    IndexRequest.of(i ->
                            i.index(ES_LOGBOOK_INDEX)
                                    .id(logbook.getName())
                                    .document(logbook)
                                    .refresh(Refresh.True));
            IndexResponse response = client.index(indexRequest);

            if (response.result().equals(Result.Created) ||
                    response.result().equals(Result.Updated)) {
                GetRequest getRequest =
                        co.elastic.clients.elasticsearch.core.GetRequest.of(g ->
                                g.index(ES_LOGBOOK_INDEX).id(response.id()));
                GetResponse<Logbook> resp =
                        client.get(getRequest, Logbook.class);
                return (S) resp.source();
            }
            return null;
        } catch (Exception e) {
            String message = MessageFormat.format(TextUtil.LOGBOOK_NOT_CREATED, logbook);
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
        }
    }

    @Override
    public <S extends Logbook> Iterable<S> saveAll(Iterable<S> logbooks) {
        List<BulkOperation> bulkOperations = new ArrayList<>();
        logbooks.forEach(logbook -> bulkOperations.add(IndexOperation.of(i ->
                i.index(ES_LOGBOOK_INDEX).document(logbook).id(logbook.getName()))._toBulkOperation()));
        BulkRequest bulkRequest =
                BulkRequest.of(r ->
                        r.operations(bulkOperations).refresh(Refresh.True));
        BulkResponse bulkResponse;
        try {
            bulkResponse = client.bulk(bulkRequest);
            if (bulkResponse.errors()) {
                // process failures by iterating through each bulk response item
                bulkResponse.items().forEach(responseItem -> {
                    if (responseItem.error() != null) {
                        logger.log(Level.SEVERE, responseItem.error().reason());
                    }
                });
                String message = MessageFormat.format(TextUtil.LOGBOOKS_NOT_CREATED, logbooks);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
            } else {
                return logbooks;
            }
        } catch (IOException e) {
            String message = MessageFormat.format(TextUtil.LOGBOOKS_NOT_CREATED, logbooks);
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
        }
    }

    @Override
    public Optional<Logbook> findById(String logbookName) {
        try {
            GetRequest getRequest =
                    GetRequest.of(g ->
                            g.index(ES_LOGBOOK_INDEX).id(logbookName));
            GetResponse<Logbook> resp =
                    client.get(getRequest, Logbook.class);
            if (resp.found()) {
                return Optional.of(resp.source());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            String message = MessageFormat.format(TextUtil.LOGBOOK_NOT_FOUND, logbookName);
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    @Override
    public boolean existsById(String logbookName) {
        try {
            ExistsRequest.Builder builder = new ExistsRequest.Builder();
            builder.index(ES_LOGBOOK_INDEX).id(logbookName);
            return client.exists(builder.build()).value();
        } catch (ElasticsearchException | IOException e) {
            String message = MessageFormat.format(TextUtil.LOGBOOK_EXISTS_FAILED, logbookName);
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, null);
        }
    }

    public boolean existsByIds(List<String> logbookNames) {
        try {
            boolean b =  logbookNames.stream().allMatch(id -> existsById(id));
            return  b;
        } catch (Exception e) {
            String message = MessageFormat.format(TextUtil.LOGBOOKS_NOT_FOUND_1, logbookNames);
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    @Override
    public Iterable<Logbook> findAll() {
        try {
            SearchRequest searchRequest =
                    SearchRequest.of(s ->
                            s.index(ES_LOGBOOK_INDEX)
                                    .query(q -> q.match(t -> t.field("state").query(State.Active.toString())))
                                    .timeout("10s")
                                    .size(logbooksResultSize)
                                    .sort(SortOptions.of(so -> so.field(FieldSort.of(f -> f.field("name"))))));

            SearchResponse<Logbook> response =
                    client.search(searchRequest, Logbook.class);
            return response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
        } catch (Exception e) {
            logger.log(Level.SEVERE, TextUtil.LOGBOOKS_NOT_FOUND, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, TextUtil.LOGBOOKS_NOT_FOUND);
        }
    }

    @Override
    public Iterable<Logbook> findAllById(Iterable<String> logbookNames) {
        List<String> ids = new ArrayList<>();
        logbookNames.forEach(ids::add);
        MgetRequest mgetRequest = MgetRequest.of(r -> r.index(ES_LOGBOOK_INDEX).ids(ids));
        try {
            List<Logbook> foundLogbooks = new ArrayList<>();
            MgetResponse<Logbook> resp = client.mget(mgetRequest, Logbook.class);
            for (MultiGetResponseItem<Logbook> multiGetResponseItem : resp.docs()) {
                if (!multiGetResponseItem.isFailure()) {
                    foundLogbooks.add(multiGetResponseItem.result().source());
                }
            }
            return foundLogbooks;
        } catch (Exception e) {
            String message = MessageFormat.format(TextUtil.LOGBOOKS_NOT_FOUND_1, logbookNames);
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    @Override
    public long count() {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, TextUtil.COUNT_NOT_IMPLEMENTED);
    }

    @Override
    public void deleteById(String logbookName) {
        try {
            GetRequest getRequest =
                    GetRequest.of(g ->
                            g.index(ES_LOGBOOK_INDEX).id(logbookName));
            GetResponse<Logbook> resp =
                    client.get(getRequest, Logbook.class);
            if (resp.found()) {
                Logbook logbook = resp.source();
                logbook.setState(State.Inactive);
                UpdateRequest<Logbook, Logbook> updateRequest =
                        UpdateRequest.of(u ->
                                u.index(ES_LOGBOOK_INDEX).id(logbookName)
                                        .doc(logbook));
                UpdateResponse<Logbook> updateResponse =
                        client.update(updateRequest, Logbook.class);
                if (updateResponse.result().equals(co.elastic.clients.elasticsearch._types.Result.Updated)) {
                    String message = MessageFormat.format(TextUtil.LOGBOOK_DELETE, logbookName);
                    logger.log(Level.INFO, () -> message);
                }
            }
        } catch (Exception e) {
            String message = MessageFormat.format(TextUtil.LOGBOOK_NOT_DELETED, logbookName);
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
        }
    }

    @Override
    public void delete(Logbook logbook) {
        deleteById(logbook.getName());
    }

    @Override
    public void deleteAll(Iterable<? extends Logbook> logbooks) {
        logbooks.forEach(logbook -> deleteById(logbook.getName()));
    }

    @Override
    public void deleteAll() {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, TextUtil.LOGBOOKS_DELETE_ALL_NOT_ALLOWED);
    }

    @Override
    public void deleteAllById(Iterable ids) {
        while (ids.iterator().hasNext()) {
            deleteById((String) ids.iterator().next());
        }
    }
}
