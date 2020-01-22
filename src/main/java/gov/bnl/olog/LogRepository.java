/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package gov.bnl.olog;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.elasticsearch.action.DocWriteResponse.Result;
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
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.bnl.olog.entity.Attachment;
import gov.bnl.olog.entity.Log;
import gov.bnl.olog.entity.Log.LogBuilder;

@Repository
public class LogRepository implements CrudRepository<Log, String>
{

    private static final Logger logger = Logger.getLogger(LogRepository.class.getName());

    @Value("${elasticsearch.log.index:olog_logs}")
    private String ES_LOG_INDEX;
    @Value("${elasticsearch.log.type:olog_log}")
    private String ES_LOG_TYPE;

    @Autowired
    @Qualifier("indexClient")
    RestHighLevelClient client;
    @Autowired
    AttachmentRepository attachmentRepository;

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public <S extends Log> S save(S log)
    {
        try
        {
            Long id = SequenceGenerator.getID();
            LogBuilder validatedLog = LogBuilder.createLog(log).id(id).createDate(Instant.now());
            if (log.getAttachments() != null && !log.getAttachments().isEmpty())
            {
                Set<Attachment> createdAttachments = new HashSet<Attachment>();
                log.getAttachments().stream().filter(attachment -> {
                    return attachment.getAttachment() != null;
                }).forEach(attachment -> {
                    createdAttachments.add(attachmentRepository.save(attachment));
                });
                validatedLog = validatedLog.setAttachments(createdAttachments);
            }

            IndexRequest indexRequest = new IndexRequest(ES_LOG_INDEX, ES_LOG_TYPE, String.valueOf(id))
                                                    .source(mapper.writeValueAsBytes(validatedLog.build()), XContentType.JSON)
                                                    .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

            IndexResponse response = client.index(indexRequest, RequestOptions.DEFAULT);

            if (response.getResult().equals(Result.CREATED))
            {
                BytesReference ref = client
                        .get(new GetRequest(ES_LOG_INDEX, ES_LOG_TYPE, response.getId()), RequestOptions.DEFAULT)
                        .getSourceAsBytesRef();

                Log createdLog = mapper.readValue(ref.streamInput(), Log.class);
                return (S) createdLog;
            }
        } catch (Exception e)
        {
            TagsResource.log.log(Level.SEVERE, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save log enrty " + log.toString(), e);
        }
        return null;
    }

    @Override
    public <S extends Log> Iterable<S> saveAll(Iterable<S> logs)
    {
        List<S> createdLogs = new ArrayList<S>();
        logs.forEach(log -> {
            createdLogs.add(save(log));
        });
        return createdLogs;
    }

    public Log update(Log log)
    {
        try
        {
            LogBuilder validatedLog = LogBuilder.createLog(log);

            IndexRequest indexRequest = new IndexRequest(ES_LOG_INDEX, ES_LOG_TYPE, String.valueOf(log.getId()))
                    .source(mapper.writeValueAsBytes(validatedLog.build()), XContentType.JSON)
                    .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

            IndexResponse response = client.index(indexRequest, RequestOptions.DEFAULT);

            if (response.getResult().equals(Result.CREATED))
            {
                BytesReference ref = client
                        .get(new GetRequest(ES_LOG_INDEX, ES_LOG_TYPE, response.getId()), RequestOptions.DEFAULT)
                        .getSourceAsBytesRef();

                Log createdLog = mapper.readValue(ref.streamInput(), Log.class);
                return createdLog;
            }
        } catch (Exception e)
        {
            TagsResource.log.log(Level.SEVERE, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to save log enrty " + log.toString(), e);
        }
        return null;
    }
    
    @Override
    public Optional<Log> findById(String id)
    {
        try
        {
            GetResponse result = client.get(new GetRequest(ES_LOG_INDEX, ES_LOG_TYPE, id), RequestOptions.DEFAULT);
            Log createdLog = mapper.readValue(result.getSourceAsBytesRef().streamInput(), Log.class);
            return Optional.of(createdLog);
        } catch (IOException e)
        {
            // https://www.baeldung.com/exception-handling-for-rest-with-spring#controlleradvice
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to retrieve log with id " + id, e);
        }
    }

    @Override
    public boolean existsById(String logId)
    {
        try
        {
            return client.exists(new GetRequest(ES_LOG_INDEX, ES_LOG_TYPE, logId), RequestOptions.DEFAULT);
        } catch (IOException e)
        {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to check existance of log with id " + logId, e);
        }
    }

    @Override
    public Iterable<Log> findAll()
    {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Retrieving all log entries is not supported. Use Search with scroll.");
    }

    @Override
    public Iterable<Log> findAllById(Iterable<String> logIds)
    {
        MultiGetRequest request = new MultiGetRequest();
        for (String logId : logIds)
        {
            request.add(new MultiGetRequest.Item(ES_LOG_INDEX, ES_LOG_TYPE, logId));
        }
        try
        {
            List<Log> foundLogs = new ArrayList<Log>();
            MultiGetResponse response = client.mget(request, RequestOptions.DEFAULT);
            for (MultiGetItemResponse multiGetItemResponse : response)
            {
                if (!multiGetItemResponse.isFailed())
                {
                    foundLogs.add(mapper.readValue(
                                            multiGetItemResponse.getResponse().getSourceAsBytesRef().streamInput(),
                                            Log.class));
                }
            }
            return foundLogs;
        } catch (Exception e)
        {
            logger.log(Level.SEVERE, "Failed to find logs: " + logIds, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to find logs: " + logIds, null);
        }
    }

    @Override
    public long count()
    {
        return 0;
    }

    @Override
    public void deleteById(String id)
    {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Deleting log entries is not supported");
    }

    @Override
    public void delete(Log entity)
    {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Deleting log entries is not supported");
    }

    @Override
    public void deleteAll(Iterable<? extends Log> entities)
    {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Deleting log entries is not supported");
    }

    @Override
    public void deleteAll()
    {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Deleting log entries is not supported");
    }

    @Autowired
    LogSearchUtil logSearchUtil;

    public List<Log> search(MultiValueMap<String, String> searchParameters)
    {
        SearchRequest searchRequest = logSearchUtil.buildSearchRequest(searchParameters);
        try
        {
            final SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            List<Log> result = new ArrayList<Log>();
            searchResponse.getHits().forEach(hit -> {
                try
                {
                    result.add(mapper.readValue(hit.getSourceAsString(), Log.class));
                } catch (IOException e)
                {
                    logger.log(Level.SEVERE, "Failed to parse result for search : " + searchParameters, e);
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Failed to parse result for search : " + searchParameters + ", CAUSE: " + e.getMessage(),
                            e);
                }
            });
            return result;
        } catch (IOException e)
        {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to complete search");
        }
    }

}
