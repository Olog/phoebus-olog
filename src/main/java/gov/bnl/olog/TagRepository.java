/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package gov.bnl.olog;

import static org.elasticsearch.action.DocWriteResponse.Result.CREATED;
import static org.elasticsearch.action.DocWriteResponse.Result.UPDATED;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.bnl.olog.entity.State;
import gov.bnl.olog.entity.Tag;

@Repository
public class TagRepository implements CrudRepository<Tag, String> {

    @Value("${elasticsearch.tag.index:olog_tags}")
    private String ES_TAG_INDEX;
    @Value("${elasticsearch.tag.type:olog_tag}")
    private String ES_TAG_TYPE;

    @Value("${elasticsearch.result.size.tags:10}")
    private int tagsResultSize;

    @Autowired
    @Qualifier("indexClient")
    RestHighLevelClient client;
    
    @Autowired
    @Qualifier("searchClient")
    RestHighLevelClient searchClient;

    private static final ObjectMapper mapper = new ObjectMapper();

    private Logger logger = Logger.getLogger(TagRepository.class.getName());

    /**
     * 
     */
    @Override
    public <S extends Tag> S save(S tag)
    {
        try
        {
            IndexRequest indexRequest = new IndexRequest(ES_TAG_INDEX, ES_TAG_TYPE, tag.getName())
                    .source(mapper.writeValueAsBytes(tag), XContentType.JSON)
                    .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

            IndexResponse response = client.index(indexRequest, RequestOptions.DEFAULT);
            if (response.getResult().equals(CREATED) || response.getResult().equals(UPDATED))
            {
                BytesReference ref = client
                        .get(new GetRequest(ES_TAG_INDEX, ES_TAG_TYPE, response.getId()), RequestOptions.DEFAULT)
                        .getSourceAsBytesRef();
                Tag createdTag = mapper.readValue(ref.streamInput(), Tag.class);
                return (S) createdTag;
            }
            return null;
        } catch (Exception e)
        {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create tag: " + tag, e);
        }
    }

    @Override
    public <S extends Tag> Iterable<S> saveAll(Iterable<S> tags)
    {
        BulkRequest bulk = new BulkRequest();
        tags.forEach(tag -> {
            try
            {
                bulk.add(new IndexRequest(ES_TAG_INDEX, ES_TAG_TYPE, tag.getName())
                        .source(mapper.writeValueAsBytes(tag), XContentType.JSON));
            } catch (JsonProcessingException e)
            {
                logger.log(Level.SEVERE, e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create tags: " + tags, e);
            }
        });
        BulkResponse bulkResponse;
        try
        {
            bulk.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
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
                        "Failed to create tags: " + tags);
            } else
            {
                return tags;
            }
        } catch (IOException e)
        {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create tags: " + tags, e);
        }
    }

    @Override
    public Optional<Tag> findById(String tagName)
    {
        try
        {
            GetResponse result = client.get(new GetRequest(ES_TAG_INDEX, ES_TAG_TYPE, tagName), RequestOptions.DEFAULT);
            if (result.isExists())
            {
                return Optional.of(mapper.readValue(result.getSourceAsBytesRef().streamInput(), Tag.class));
            }
            return Optional.empty();
        } catch (Exception e)
        {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to find tag: " + tagName, e);
        }
    }

    @Override
    public boolean existsById(String tagName) {
        try
        {
            return client.exists(new GetRequest(ES_TAG_INDEX, ES_TAG_TYPE, tagName), RequestOptions.DEFAULT);
        } catch (IOException e)
        {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to find tag: " + tagName, e);
        }
    }

    @Override
    public Iterable<Tag> findAll() {
        try
        {
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(QueryBuilders.termQuery("state", State.Active.toString()));
            sourceBuilder.timeout(new TimeValue(10, TimeUnit.SECONDS));
            sourceBuilder.size(tagsResultSize);

            SearchResponse response = client.search(
                    new SearchRequest(ES_TAG_INDEX).types(ES_TAG_TYPE).source(sourceBuilder), RequestOptions.DEFAULT);
            List<Tag> result = new ArrayList<Tag>();
            response.getHits().forEach(h -> {
                BytesReference b = h.getSourceRef();
                try {
                    result.add(mapper.readValue(b.streamInput(), Tag.class));
                } catch (IOException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                }
            });
            return result;
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to find tags: ", e);
        }
    }

    @Override
    public Iterable<Tag> findAllById(Iterable<String> tagNames)
    {
        MultiGetRequest request = new MultiGetRequest();
        for (String tagName : tagNames)
        {
            request.add(new MultiGetRequest.Item(ES_TAG_INDEX, ES_TAG_TYPE, tagName));
        }
        try
        {
            List<Tag> foundTags = new ArrayList<Tag>();
            MultiGetResponse response = client.mget(request, RequestOptions.DEFAULT);
            for (MultiGetItemResponse multiGetItemResponse : response)
            {
                if (!multiGetItemResponse.isFailed())
                {
                    foundTags.add(mapper.readValue(
                            multiGetItemResponse.getResponse().getSourceAsBytesRef().streamInput(), Tag.class));
                }
            }
            return foundTags;
        } catch (Exception e)
        {
            logger.log(Level.SEVERE, "Failed to find tags: " + tagNames, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to find tags: " + tagNames, null);
        }
    }

    @Override
    public long count() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void deleteById(String tagName) {
        try
        {
            UpdateResponse response = client.update(
                    new UpdateRequest(ES_TAG_INDEX, ES_TAG_TYPE, tagName)
                            .doc(jsonBuilder().startObject().field("state", State.Inactive).endObject())
                            .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE),
                    RequestOptions.DEFAULT);

            if (response.getResult().equals(UPDATED)) {
                BytesReference ref = client
                        .get(new GetRequest(ES_TAG_INDEX, ES_TAG_TYPE, response.getId()), RequestOptions.DEFAULT)
                        .getSourceAsBytesRef();
                Tag deletedTag = mapper.readValue(ref.streamInput(), Tag.class);
                logger.log(Level.INFO, "Deleted tag " + deletedTag.toLogger());
            }
        } catch (DocumentMissingException e) {
            logger.log(Level.SEVERE, tagName + " Does not exist and thus cannot be deleted");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to delete tag: " + tagName + " because it does not exist", e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to delete tag: " + tagName, e);
        }
    }

    @Override
    public void delete(Tag tag)
    {
        deleteById(tag.getName());
    }

    @Override
    public void deleteAll(Iterable<? extends Tag> tags)
    {
        tags.forEach(tag -> deleteById(tag.getName()));
    }

    @Override
    public void deleteAll()
    {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Deleteting all tags not allowed");
    }

}
