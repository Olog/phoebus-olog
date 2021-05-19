/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.engine.DocumentMissingException;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class LogbookRepository implements CrudRepository<Logbook, String>
{

    // Read the elatic index and type from the application.properties
    @Value("${elasticsearch.logbook.index:olog_logbooks}")
    private String ES_LOGBOOK_INDEX;
    @Value("${elasticsearch.logbook.type:olog_logbook}")
    private String ES_LOGBOOK_TYPE;

    @Value("${elasticsearch.result.size.logbooks:10}")
    private int logbooksResultSize;

    @Autowired
    @Qualifier("indexClient")
    RestHighLevelClient client;

    private static final ObjectMapper mapper = new ObjectMapper();

    private Logger logger = Logger.getLogger(LogbookRepository.class.getName());

    @Override
    public <S extends Logbook> S save(S logbook)
    {
        try
        {
            IndexRequest indexRequest = new IndexRequest(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, logbook.getName())
                    .source(mapper.writeValueAsBytes(logbook), XContentType.JSON)
                    .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

            IndexResponse response = client.index(indexRequest, RequestOptions.DEFAULT);
            
            if (response.getResult().equals(Result.CREATED) ||
                response.getResult().equals(Result.UPDATED))
            {
                BytesReference ref = client
                        .get(new GetRequest(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, response.getId()), RequestOptions.DEFAULT)
                        .getSourceAsBytesRef();
                Logbook createdLogbook = mapper.readValue(ref.streamInput(), Logbook.class);
                return (S) createdLogbook;
            }
            return null;
        } catch (Exception e)
        {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create logbook: " + logbook, e);
        }
    }

    @Override
    public <S extends Logbook> Iterable<S> saveAll(Iterable<S> logbooks)
    {
        BulkRequest bulk = new BulkRequest();
        logbooks.forEach(logbook -> {
            try
            {
                bulk.add(new IndexRequest(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, logbook.getName())
                        .source(mapper.writeValueAsBytes(logbook), XContentType.JSON));
            } catch (JsonProcessingException e)
            {
                logger.log(Level.SEVERE, e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create logbook: " + logbook, e);
                
            }
        });
        bulk.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        BulkResponse bulkResponse;
        try
        {
            bulkResponse = client.bulk(bulk, RequestOptions.DEFAULT);
            if (bulkResponse.hasFailures())
            {
                // process failures by iterating through each bulk response item
                bulkResponse.forEach(response -> {
                    if (response.getFailure() != null)
                    {
                        logger.log(Level.SEVERE, response.getFailureMessage(),
                                response.getFailure().getCause());
                    }
                });
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to create logbooks: " + logbooks);
            } else
            {
                return logbooks;
            }
        } catch (IOException e)
        {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to create logbooks: " + logbooks, e);
        }
    }

    @Override
    public Optional<Logbook> findById(String logbookName)
    {
        try
        {
            GetResponse result = client.get(new GetRequest(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, logbookName),
                    RequestOptions.DEFAULT);
            if (result.isExists())
            {
                return Optional.of(mapper.readValue(result.getSourceAsBytesRef().streamInput(), Logbook.class));
            } else
            {
                return Optional.empty();
            }
        } catch (Exception e)
        {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public boolean existsById(String logbookName)
    {
        try
        {
            return client.exists(new GetRequest(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, logbookName), RequestOptions.DEFAULT);
        } catch (IOException e)
        {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to find logbook: " + logbookName, e);
        }
    }

    public boolean existsByIds(List<String> logbookNames)
    {
        try
        {
            return logbookNames.stream().allMatch(logbook -> {
                return existsById(logbook);
            });
        } catch (Exception e)
        {
            logger.log(Level.SEVERE, "Failed to find logbooks: " + logbookNames, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to find logbooks: " + logbookNames, null);
        }
    }

    @Override
    public Iterable<Logbook> findAll()
    {
        try
        {
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(QueryBuilders.termQuery("state", State.Active.toString()));
            sourceBuilder.timeout(new TimeValue(10, TimeUnit.SECONDS));
            sourceBuilder.size(logbooksResultSize);

            SearchResponse response = client.search(
                    new SearchRequest(ES_LOGBOOK_INDEX).types(ES_LOGBOOK_TYPE).source(sourceBuilder), RequestOptions.DEFAULT);

            List<Logbook> result = new ArrayList<Logbook>();
            response.getHits().forEach(h -> {
                BytesReference b = h.getSourceRef();
                try
                {
                    result.add(mapper.readValue(b.streamInput(), Logbook.class));
                } catch (IOException e)
                {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                }
            });
            return result;
        } catch (Exception e)
        {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Iterable<Logbook> findAllById(Iterable<String> logbookNames)
    {
        MultiGetRequest request = new MultiGetRequest();
        for (String logbookName : logbookNames)
        {
            request.add(new MultiGetRequest.Item(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, logbookName));
        }
        try
        {
            List<Logbook> foundLogbooks = new ArrayList<Logbook>();
            MultiGetResponse response = client.mget(request, RequestOptions.DEFAULT);
            for (MultiGetItemResponse multiGetItemResponse : response)
            {
                if (!multiGetItemResponse.isFailed())
                {
                    foundLogbooks.add(mapper.readValue(
                            multiGetItemResponse.getResponse().getSourceAsBytesRef().streamInput(), Logbook.class));
                }
            }
            return foundLogbooks;
        } catch (Exception e)
        {
            logger.log(Level.SEVERE, "Failed to find logbooks: " + logbookNames, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to find logbooks: " + logbookNames, null);
        }
    }

    @Override
    public long count()
    {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Count is not implemented");
    }

    @Override
    public void deleteById(String logbookName)
    {
        try
        {
            UpdateResponse response = client.update(
                    new UpdateRequest(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, logbookName)
                            .doc(jsonBuilder().startObject().field("state", State.Inactive).endObject())
                            .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE),
                    RequestOptions.DEFAULT);

            if (response.getResult().equals(Result.UPDATED))
            {
                BytesReference ref = client
                        .get(new GetRequest(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, response.getId()), RequestOptions.DEFAULT)
                        .getSourceAsBytesRef();

                Logbook deletedLogbook = mapper.readValue(ref.streamInput(), Logbook.class);
                logger.log(Level.INFO, "Deleted logbook " + deletedLogbook.toLogger());
            }
        } catch (DocumentMissingException e)
        {
            logger.log(Level.SEVERE, logbookName + " Does not exist and thus cannot be deleted");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to delete logbook: " + logbookName + " because it does not exist", e);
        } catch (Exception e)
        {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to delete logbook: " + logbookName + " because it does not exist", e);
        }

    }

    @Override
    public void delete(Logbook logbook)
    {
        deleteById(logbook.getName());
    }

    @Override
    public void deleteAll(Iterable<? extends Logbook> logbooks)
    {
        logbooks.forEach(logbook -> deleteById(logbook.getName()));
    }

    @Override
    public void deleteAll()
    {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Deleteting all logbooks is not allowed");
    }


}
