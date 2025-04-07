/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.ExistsRequest;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.MgetRequest;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.core.mget.MultiGetResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.phoebus.olog.ElasticConfig.ES_LEVEL_INDEX;

@Repository
public class LevelRepository implements CrudRepository<org.phoebus.olog.entity.Level, String> {

    private final Logger logger = Logger.getLogger(LevelRepository.class.getName());

    @Autowired
    @Qualifier("client")
    ElasticsearchClient client;

    /**
     *
     */
    @Override
    public <S extends org.phoebus.olog.entity.Level> S save(S level) {
        try {
            IndexRequest<org.phoebus.olog.entity.Level> indexRequest =
                    IndexRequest.of(i ->
                            i.index(ES_LEVEL_INDEX)
                                    .id(level.name())
                                    .document(level)
                                    .refresh(Refresh.True));
            IndexResponse response = client.index(indexRequest);

            if (response.result().equals(Result.Created) ||
                    response.result().equals(Result.Updated)) {
                GetRequest getRequest =
                        GetRequest.of(g ->
                                g.index(ES_LEVEL_INDEX).id(response.id()));
                GetResponse<org.phoebus.olog.entity.Level> resp =
                        client.get(getRequest, org.phoebus.olog.entity.Level.class);
                return (S) resp.source();
            }
            return null;
        } catch (Exception e) {
            String message = MessageFormat.format(TextUtil.LEVELS_NOT_CREATED, level.name());
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
        }
    }

    @Override
    public <S extends org.phoebus.olog.entity.Level> Iterable<S> saveAll(Iterable<S> levels) {
        List<BulkOperation> bulkOperations = new ArrayList<>();
        levels.forEach(level -> bulkOperations.add(IndexOperation.of(i ->
                i.index(ES_LEVEL_INDEX).document(level).id(level.name()))._toBulkOperation()));
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
                String message = MessageFormat.format(TextUtil.LEVELS_NOT_CREATED, levels);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
            } else {
                return levels;
            }
        } catch (IOException e) {
            String message = MessageFormat.format(TextUtil.LEVELS_NOT_CREATED, levels);
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
        }
    }

    @Override
    public Optional<org.phoebus.olog.entity.Level> findById(String levelName) {
        try {
            GetRequest getRequest =
                    GetRequest.of(g ->
                            g.index(ES_LEVEL_INDEX).id(levelName));
            GetResponse<org.phoebus.olog.entity.Level> resp =
                    client.get(getRequest, org.phoebus.olog.entity.Level.class);
            if (resp.found()) {
                return Optional.of(resp.source());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            String message = MessageFormat.format(TextUtil.LEVEL_NOT_FOUND, levelName);
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    @Override
    public boolean existsById(String levelName) {
        try {
            ExistsRequest.Builder builder = new ExistsRequest.Builder();
            builder.index(ES_LEVEL_INDEX).id(levelName);
            return client.exists(builder.build()).value();
        } catch (ElasticsearchException | IOException e) {
            String message = MessageFormat.format(TextUtil.LEVEL_EXISTS_FAILED, levelName);
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, null);
        }
    }

    @Override
    public Iterable<org.phoebus.olog.entity.Level> findAll() {

        try {
            SearchRequest searchRequest = SearchRequest.of(s ->
                    s.index(ES_LEVEL_INDEX)
                            .query(new MatchAllQuery.Builder().build()._toQuery())
                            .timeout("10s")
                            .size(1000));

            SearchResponse<org.phoebus.olog.entity.Level> response =
                    client.search(searchRequest, org.phoebus.olog.entity.Level.class);

            return response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
        } catch (Exception e) {
            logger.log(Level.SEVERE, TextUtil.LEVEL_NOT_FOUND, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, TextUtil.LEVEL_NOT_FOUND);
        }
    }

    @Override
    public Iterable<org.phoebus.olog.entity.Level> findAllById(Iterable<String> levelNames) {

        List<String> ids = new ArrayList<>();
        levelNames.forEach(ids::add);
        MgetRequest mgetRequest = MgetRequest.of(r -> r.index(ES_LEVEL_INDEX).ids(ids));
        try {
            List<org.phoebus.olog.entity.Level> foundLevels = new ArrayList<>();
            MgetResponse<org.phoebus.olog.entity.Level> resp =
                    client.mget(mgetRequest, org.phoebus.olog.entity.Level.class);
            for (MultiGetResponseItem<org.phoebus.olog.entity.Level> multiGetResponseItem : resp.docs()) {
                if (!multiGetResponseItem.isFailure()) {
                    foundLevels.add(multiGetResponseItem.result().source());
                }
            }
            return foundLevels;
        } catch (Exception e) {
            logger.log(Level.SEVERE, TextUtil.LEVELS_NOT_FOUND, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, TextUtil.LEVEL_NOT_FOUND);
        }
    }

    @Override
    public long count() {
        AtomicInteger count = new AtomicInteger();
        findAll().forEach(l -> count.incrementAndGet());
        return count.get();
    }

    @Override
    public void deleteById(String levelName) {
        try {
            DeleteRequest deleteRequest = DeleteRequest.of(d ->
                    d.index(ES_LEVEL_INDEX).id(levelName).refresh(Refresh.True));
            DeleteResponse deleteResponse = client.delete(deleteRequest);
            if (deleteResponse.result().equals(Result.Deleted)) {
                logger.log(Level.INFO, MessageFormat.format(TextUtil.LEVEL_DELETED, levelName));
            } else {
                logger.log(Level.INFO, MessageFormat.format(TextUtil.LEVEL_NOT_DELETED, levelName));
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, MessageFormat.format(TextUtil.LEVEL_NOT_DELETED, levelName), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(org.phoebus.olog.entity.Level level) {
        deleteById(level.name());
    }

    @Override
    public void deleteAll(Iterable<? extends org.phoebus.olog.entity.Level> levels) {
        levels.forEach(level -> deleteById(level.name()));
    }

    @Override
    public void deleteAll() {
        findAll().forEach(l -> delete(l));
    }

    @Override
    public void deleteAllById(Iterable ids) {
        while (ids.iterator().hasNext()) {
            deleteById((String) ids.iterator().next());
        }
    }
}
