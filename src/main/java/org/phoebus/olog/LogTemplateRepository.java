/**
 * Copyright (C) 2025 European Spallation Source ERIC.
 */
package org.phoebus.olog;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import org.phoebus.olog.entity.LogTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link org.springframework.data.repository.Repository} for {@link LogTemplate}s.
 */
@Repository
public class LogTemplateRepository implements CrudRepository<LogTemplate, String> {

    private static final Logger logger = Logger.getLogger(LogTemplateRepository.class.getName());

    @SuppressWarnings("unused")
    @Autowired
    @Qualifier("client")
    ElasticsearchClient client;

    @Override
    public <S extends LogTemplate> S save(S logTemplate) {

        try {
            logTemplate.setCreatedDate(Instant.now());
            IndexRequest<Object> indexRequest =
                    IndexRequest.of(i ->
                            i.index(ElasticConfig.ES_LOG_TEMPLATE_INDEX)
                                    .document(logTemplate)
                                    .refresh(Refresh.True));
            IndexResponse response = client.index(indexRequest);

            if (response.result().equals(Result.Created)) {
                GetRequest getRequest =
                        GetRequest.of(g ->
                                g.index(ElasticConfig.ES_LOG_TEMPLATE_INDEX).id(response.id()));
                GetResponse<LogTemplate> resp =
                        client.get(getRequest, LogTemplate.class);
                LogTemplate saved = resp.source();
                saved.setId(response.id());
                return (S) saved;
            }
        } catch (Exception e) {
            String message = MessageFormat.format(TextUtil.LOG_TEMPLATE_NOT_SAVED, logTemplate.getName());
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
        }
        return null;
    }

    @Override
    public <S extends LogTemplate> Iterable<S> saveAll(Iterable<S> logTemplates) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, TextUtil.LOG_TEMPLATES_SAVE_ALL);
    }

    public LogTemplate update(LogTemplate logTemplate) {
        try {
            logTemplate.setModifyDate(Instant.now());
            IndexRequest<LogTemplate> indexRequest =
                    IndexRequest.of(i ->
                            i.index(ElasticConfig.ES_LOG_TEMPLATE_INDEX)
                                    .refresh(Refresh.True)
                                    .id(logTemplate.getId())
                                    .document(logTemplate));

            IndexResponse response = client.index(indexRequest);

            if (response.result().equals(Result.Updated)) {
                GetRequest getRequest =
                        GetRequest.of(g ->
                                g.index(ElasticConfig.ES_LOG_TEMPLATE_INDEX).id(response.id()));
                GetResponse<LogTemplate> resp =
                        client.get(getRequest, LogTemplate.class);
                return resp.source();
            }
        } catch (Exception e) {
            String message = MessageFormat.format(TextUtil.LOG_TEMPLATE_NOT_UPDATED, logTemplate.getName());
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
        }
        return null;
    }

    @Override
    public Optional<LogTemplate> findById(String id) {
        try {
            GetRequest getRequest =
                    GetRequest.of(g ->
                            g.index(ElasticConfig.ES_LOG_TEMPLATE_INDEX).id(id));
            GetResponse<LogTemplate> resp =
                    client.get(getRequest, LogTemplate.class);

            if (!resp.found()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, MessageFormat.format(TextUtil.LOG_TEMPLATE_NOT_FOUND, id));
            }
            return resp.source() != null ? Optional.of(resp.source()) : Optional.empty();
        } catch (Exception e) {
            // https://www.baeldung.com/exception-handling-for-rest-with-spring#controlleradvice
            String message = MessageFormat.format(TextUtil.LOG_TEMPLATE_NOT_RETRIEVED, id);
            logger.log(Level.SEVERE, message, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    @Override
    public boolean existsById(String logId) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, TextUtil.LOG_TEMPLATE_EXISTS_UNSUPPORTED);
    }

    @Override
    public Iterable<LogTemplate> findAll() {
        try {
            SearchRequest searchRequest =
                    SearchRequest.of(s ->
                            s.index(ElasticConfig.ES_LOG_TEMPLATE_INDEX)
                                    .query(new MatchAllQuery.Builder().build()._toQuery())
                                    .size(1000) // Assume number of templates is within reason, so returning "all" should be the wanted behavior
                                    .from(0)
                                    .sort(SortOptions.of(so -> so.field(FieldSort.of(f -> f.field("name"))))));

            SearchResponse<LogTemplate> response =
                    client.search(searchRequest, LogTemplate.class);
            List<LogTemplate> templates = new ArrayList<>();
            response.hits().hits().forEach(h -> {
                LogTemplate logTemplate = h.source();
                logTemplate.setId(h.id());
                templates.add(logTemplate);
            });
            return templates;
        } catch (Exception e) {
            logger.log(Level.SEVERE, TextUtil.LOG_TEMPLATES_NOT_RETRIEVED, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, TextUtil.LOG_TEMPLATES_NOT_RETRIEVED);
        }
    }

    @Override
    public Iterable<LogTemplate> findAllById(Iterable<String> logIds) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, TextUtil.LOG_TEMPLATES_FIND_ALL_BY_ID);
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public void deleteById(String id) {
        try {
            DeleteRequest deleteRequest = DeleteRequest.of(d ->
                    d.index(ElasticConfig.ES_LOG_TEMPLATE_INDEX).id(id).refresh(Refresh.True));
            DeleteResponse deleteResponse = client.delete(deleteRequest);
            if (deleteResponse.result().equals(Result.Deleted)) {
                logger.log(Level.INFO, MessageFormat.format(TextUtil.LOG_TEMPLATE_DELETED, id));
            } else {
                logger.log(Level.INFO, MessageFormat.format(TextUtil.LOG_TEMPLATE_NOT_DELETED, id));
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, MessageFormat.format(TextUtil.LOG_TEMPLATE_NOT_DELETED, id), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(LogTemplate logTemplate) {
        deleteById(logTemplate.getId());
    }

    @Override
    public void deleteAll(Iterable<? extends LogTemplate> entities) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, TextUtil.LOG_TEMPLATE_DELETE_ALL_NOT_SUPPORTED);
    }

    @Override
    public void deleteAll() {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, TextUtil.LOG_TEMPLATE_DELETE_ALL_NOT_SUPPORTED);
    }

    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, TextUtil.LOG_TEMPLATE_DELETE_ALL_NOT_SUPPORTED);
    }
}
