package gov.bnl.olog;

import static gov.bnl.olog.OlogResourceDescriptors.ES_TAG_INDEX;
import static gov.bnl.olog.OlogResourceDescriptors.ES_TAG_TYPE;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

    @Autowired
    @Qualifier("indexClient")
    RestHighLevelClient client;
    
    @Autowired
    @Qualifier("searchClient")
    RestHighLevelClient searchClient;

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 
     */
    @Override
    public <S extends Tag> S save(S tag)
    {
        try
        {
            IndexRequest indexRequest = new IndexRequest(ES_TAG_INDEX, ES_TAG_TYPE, tag.getName())
                    .source(mapper.writeValueAsBytes(tag), XContentType.JSON);

            IndexResponse response = client.index(indexRequest, RequestOptions.DEFAULT);
            if (response.getResult().equals(Result.CREATED))
            {
                BytesReference ref = client
                        .get(new GetRequest(ES_TAG_INDEX, ES_TAG_TYPE, response.getId()), RequestOptions.DEFAULT)
                        .getSourceAsBytesRef();
                Tag createdTag = mapper.readValue(ref.streamInput(), Tag.class);
                return (S) createdTag;
            }
        } catch (Exception e)
        {
            TagsResource.log.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
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
                TagsResource.log.log(Level.SEVERE, e.getMessage(), e);
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
                        TagsResource.log.log(Level.SEVERE, response.getFailureMessage(),
                                response.getFailure().getCause());
                    }
                });
            } else
            {
                return tags;
            }
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
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
        } catch (Exception e)
        {
            TagsResource.log.log(Level.SEVERE, e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public boolean existsById(String tagName) {
        
        try
        {
            return client.exists(new GetRequest(ES_TAG_INDEX, ES_TAG_TYPE, tagName), RequestOptions.DEFAULT);
        } catch (IOException e)
        {
            TagsResource.log.log(Level.SEVERE, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public Iterable<Tag> findAll() {
        try
        {
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(QueryBuilders.termQuery("state", State.Active.toString()));
            sourceBuilder.timeout(new TimeValue(10, TimeUnit.SECONDS));

            SearchResponse response = client.search(
                    new SearchRequest(ES_TAG_INDEX).types(ES_TAG_TYPE).source(sourceBuilder), RequestOptions.DEFAULT);
            List<Tag> result = new ArrayList<Tag>();
            response.getHits().forEach(h -> {
                BytesReference b = h.getSourceRef();
                try {
                    result.add(mapper.readValue(b.streamInput(), Tag.class));
                } catch (IOException e) {
                    TagsResource.log.log(Level.SEVERE, e.getMessage(), e);
                }
            });
            return result;
        } catch (Exception e) {
            TagsResource.log.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
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
            List<Tag> foundChannels = new ArrayList<Tag>();
            MultiGetResponse response = client.mget(request, RequestOptions.DEFAULT);
            for (MultiGetItemResponse multiGetItemResponse : response)
            {
                if (!multiGetItemResponse.isFailed())
                {
                    foundChannels.add(mapper.readValue(
                            multiGetItemResponse.getResponse().getSourceAsBytesRef().streamInput(), Tag.class));
                }
            }
            return foundChannels;
        } catch (Exception e)
        {
            TagsResource.log.log(Level.SEVERE, "Failed to find tags: " + tagNames, e);
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

            if (response.getResult().equals(Result.UPDATED)) {
                BytesReference ref = client
                        .get(new GetRequest(ES_TAG_INDEX, ES_TAG_TYPE, response.getId()), RequestOptions.DEFAULT)
                        .getSourceAsBytesRef();
                Tag deletedTag = mapper.readValue(ref.streamInput(), Tag.class);
                TagsResource.log.log(Level.INFO, "Deleted tag " + deletedTag.toLogger());
            }
        } catch (DocumentMissingException e) {
            TagsResource.log.log(Level.SEVERE, tagName + " Does not exist and thus cannot be deleted");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to delete tag: " + tagName + " because it does not exist", e);
        } catch (Exception e) {
            TagsResource.log.log(Level.SEVERE, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to delete tag: " + tagName, e);
        }
    }

    @Override
    public void delete(Tag tag) {
        deleteById(tag.getName());
    }

    @Override
    public void deleteAll(Iterable<? extends Tag> tags) {
        tags.forEach(tag -> deleteById(tag.getName()));
    }

    @Override
    public void deleteAll() {
        // TODO Auto-generated method stub

    }

    public Iterable<Tag> search(String tagName) {
        // QueryBuilders.wildcardQuery(TAG_INDEX, tagName);
        return null;
    }

}
