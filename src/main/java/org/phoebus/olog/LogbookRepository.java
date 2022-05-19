/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.MgetRequest;
import co.elastic.clients.elasticsearch.core.MgetResponse;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Repository
public class LogbookRepository implements CrudRepository<Logbook, String> {

    // Read the elatic index and type from the application.properties
    @Value("${elasticsearch.logbook.index:olog_logbooks}")
    private String ES_LOGBOOK_INDEX;
    @Value("${elasticsearch.logbook.type:olog_logbook}")
    private String ES_LOGBOOK_TYPE;

    @Value("${elasticsearch.result.size.logbooks:10}")
    private int logbooksResultSize;

    //@Autowired
    //@Qualifier("legacyClient")
    //RestHighLevelClient legacyClient;

    @Autowired
    @Qualifier("client")
    private ElasticsearchClient client;

    private Logger logger = Logger.getLogger(LogbookRepository.class.getName());

    @Override
    public <S extends Logbook> S save(S logbook) {
        try {
            co.elastic.clients.elasticsearch.core.IndexRequest indexRequest =
                    co.elastic.clients.elasticsearch.core.IndexRequest.of(i ->
                            i.index(ES_LOGBOOK_INDEX)
                                    .id(logbook.getName())
                                    .document(logbook)
                                    .refresh(Refresh.True));
            co.elastic.clients.elasticsearch.core.IndexResponse response = client.index(indexRequest);

            if (response.result().equals(Result.Created) ||
                    response.result().equals(Result.Updated)) {
                co.elastic.clients.elasticsearch.core.GetRequest getRequest =
                        co.elastic.clients.elasticsearch.core.GetRequest.of(g ->
                                g.index(ES_LOGBOOK_INDEX).id(response.id()));
                co.elastic.clients.elasticsearch.core.GetResponse<Logbook> resp =
                        client.get(getRequest, Logbook.class);
                return (S) resp.source();
            }
            return null;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create logbook: " + logbook, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create logbook: " + logbook);
        }
    }

    @Override
    public <S extends Logbook> Iterable<S> saveAll(Iterable<S> logbooks) {
        List<BulkOperation> bulkOperations = new ArrayList<>();
        logbooks.forEach(logbook -> bulkOperations.add(IndexOperation.of(i ->
                i.index(ES_LOGBOOK_INDEX).document(logbook))._toBulkOperation()));
        co.elastic.clients.elasticsearch.core.BulkRequest bulkRequest =
                co.elastic.clients.elasticsearch.core.BulkRequest.of(r ->
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
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to create logbooks: " + logbooks);
            } else {
                return logbooks;
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create logbooks: " + logbooks, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create logbooks: " + logbooks);
        }
    }

    @Override
    public Optional<Logbook> findById(String logbookName) {
        try {
            co.elastic.clients.elasticsearch.core.GetRequest getRequest =
                    co.elastic.clients.elasticsearch.core.GetRequest.of(g ->
                            g.index(ES_LOGBOOK_INDEX).id(logbookName));
            co.elastic.clients.elasticsearch.core.GetResponse<Logbook> resp =
                    client.get(getRequest, Logbook.class);
            if (resp.found()) {
                return Optional.of(resp.source());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to find logbook: " + logbookName, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to find logbook: " + logbookName);
        }
    }

    @Override
    public boolean existsById(String logbookName) {
        Optional<Logbook> logbookOptional = findById(logbookName);
        return logbookOptional.isPresent();
    }

    public boolean existsByIds(List<String> logbookNames) {
        try {
            return logbookNames.stream().allMatch(logbook -> {
                return existsById(logbook);
            });
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to find logbooks: " + logbookNames, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to find logbooks: " + logbookNames);
        }
    }

    @Override
    public Iterable<Logbook> findAll() {
        try {
            co.elastic.clients.elasticsearch.core.SearchRequest searchRequest =
                    co.elastic.clients.elasticsearch.core.SearchRequest.of(s ->
                            s.index(ES_LOGBOOK_INDEX)
                                    .query(q -> q.match(t -> t.field("state").query(State.Active.toString())))
                                    .timeout("10s")
                                    .size(logbooksResultSize)
                                    .sort(SortOptions.of(so -> so.field(FieldSort.of(f -> f.field("name"))))));

            co.elastic.clients.elasticsearch.core.SearchResponse<Logbook> response =
                    client.search(searchRequest, Logbook.class);
            return response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to find logbooks", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to find logbooks");
        }
    }

    @Override
    public Iterable<Logbook> findAllById(Iterable<String> logbookNames) {
        List<String> ids = new ArrayList<>();
        logbookNames.forEach(ids::add);
        MgetRequest mgetRequest = MgetRequest.of(r -> r.ids(ids));
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
            logger.log(Level.SEVERE, "Failed to find logbooks: " + logbookNames, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to find logbooks: " + logbookNames);
        }
    }

    @Override
    public long count() {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Count is not implemented");
    }

    @Override
    public void deleteById(String logbookName) {
        try {
            co.elastic.clients.elasticsearch.core.GetRequest getRequest =
                    co.elastic.clients.elasticsearch.core.GetRequest.of(g ->
                            g.index(ES_LOGBOOK_INDEX).id(logbookName));
            co.elastic.clients.elasticsearch.core.GetResponse<Logbook> resp =
                    client.get(getRequest, Logbook.class);
            if (resp.found()) {
                Logbook logbook = resp.source();
                logbook.setState(State.Inactive);
                co.elastic.clients.elasticsearch.core.UpdateRequest updateRequest =
                        co.elastic.clients.elasticsearch.core.UpdateRequest.of(u ->
                                u.index(ES_LOGBOOK_INDEX).id(logbookName)
                                        .doc(logbook));
                co.elastic.clients.elasticsearch.core.UpdateResponse updateResponse =
                        client.update(updateRequest, Logbook.class);
                if (updateResponse.result().equals(co.elastic.clients.elasticsearch._types.Result.Updated)) {
                    logger.log(Level.INFO, "Deleted logbook " + logbookName);
                }
            }
        } /*catch (DocumentMissingException e) {
            logger.log(Level.SEVERE, "Failed to delete logbook: " + logbookName + " because it does not exist", e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to delete logbook: " + logbookName + " because it does not exist");
        } */catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to delete logbook: " + logbookName, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete logbook: " + logbookName);
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
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Deleting all logbooks is not allowed");
    }

    @Override
    public void deleteAllById(Iterable ids) {
        while (ids.iterator().hasNext()) {
            deleteById((String) ids.iterator().next());
        }
    }

}
