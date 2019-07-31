package gov.bnl.olog;

import static gov.bnl.olog.OlogResourceDescriptors.ES_TAG_INDEX;
import static gov.bnl.olog.OlogResourceDescriptors.ES_TAG_TYPE;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.engine.DocumentMissingException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.bnl.olog.entity.State;
import gov.bnl.olog.entity.Tag;

@Repository
public class TagRepository implements ElasticsearchRepository<Tag, String> {

    @Autowired
    Client client;

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Iterable<Tag> findAll(Sort sort) {
        return null;
    }

    @Override
    public Page<Tag> findAll(Pageable pageable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends Tag> S save(S entity) {
        return index(entity);
    }

    @Override
    public <S extends Tag> Iterable<S> saveAll(Iterable<S> tags) {
        BulkRequestBuilder bulk = client.prepareBulk();
        tags.forEach(tag -> {
            try {
                bulk.add(client.prepareIndex(ES_TAG_INDEX, ES_TAG_TYPE, tag.getName())
                        .setSource(mapper.writeValueAsBytes(tag), XContentType.JSON));
            } catch (JsonProcessingException e) {
                TagsResource.log.log(Level.SEVERE, e.getMessage(), e);
            }
        });
        BulkResponse bulkResponse = bulk.get("10s");
        if (bulkResponse.hasFailures()) {
            // process failures by iterating through each bulk response item
            bulkResponse.forEach(response -> {
                if(response.getFailure() != null) {
                    TagsResource.log.log(Level.SEVERE, response.getFailureMessage(), response.getFailure().getCause());
                };});
            return null;
        } else {
            return tags;
        }
    }

    @Override
    public Optional<Tag> findById(String tagName) {
        try {
            GetResponse result = client.prepareGet(ES_TAG_INDEX, ES_TAG_TYPE, tagName).get();
            if (result.isExists()) {
                return Optional.of(mapper.readValue(result.getSourceAsBytesRef().streamInput(), Tag.class));
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            TagsResource.log.log(Level.SEVERE, e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public boolean existsById(String id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Iterable<Tag> findAll() {
        try {
            SearchResponse response = client.prepareSearch(ES_TAG_INDEX)
                                            .setTypes(ES_TAG_TYPE)
                                            .setQuery(QueryBuilders.termQuery("state", State.Active.toString()))
                                            .get("10s");
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
    public Iterable<Tag> findAllById(Iterable<String> ids) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long count() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void deleteById(String tagName) {
        try {
            UpdateResponse response = client.prepareUpdate(ES_TAG_INDEX, ES_TAG_TYPE, tagName)
                    .setDoc(jsonBuilder().startObject().field("state", State.Inactive).endObject()).get();

            if (response.getResult().equals(Result.UPDATED)) {
                BytesReference ref = client.prepareGet(ES_TAG_INDEX, ES_TAG_TYPE, response.getId()).get()
                        .getSourceAsBytesRef();
                Tag deletedTag = mapper.readValue(ref.streamInput(), Tag.class);
                TagsResource.log.log(Level.INFO, "Deleted tag " + deletedTag.toLogger());
            }
        } catch (DocumentMissingException e) {
            TagsResource.log.log(Level.SEVERE, tagName + " Does not exist and thus cannot be deleted");
        } catch (Exception e) {
            TagsResource.log.log(Level.SEVERE, e.getMessage(), e);
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

    @Override
    public Iterable<Tag> search(QueryBuilder query) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Page<Tag> search(QueryBuilder query, Pageable pageable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Page<Tag> search(SearchQuery searchQuery) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Page<Tag> searchSimilar(Tag entity, String[] fields, Pageable pageable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void refresh() {
        // TODO Auto-generated method stub

    }

    @Override
    public Class<Tag> getEntityClass() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends Tag> S index(S tag) {
        try {
            IndexResponse response = client.prepareIndex(ES_TAG_INDEX, ES_TAG_TYPE, tag.getName())
                    .setSource(mapper.writeValueAsBytes(tag), XContentType.JSON).get();

            if (response.getResult().equals(Result.CREATED)) {
                BytesReference ref = client.prepareGet(ES_TAG_INDEX, ES_TAG_TYPE, response.getId()).get()
                        .getSourceAsBytesRef();
                Tag createdTag = mapper.readValue(ref.streamInput(), Tag.class);
                return (S) createdTag;
            }
        } catch (Exception e) {
            TagsResource.log.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }
}
