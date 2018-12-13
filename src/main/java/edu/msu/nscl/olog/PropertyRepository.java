package edu.msu.nscl.olog;

import static edu.msu.nscl.olog.OlogResourceDescriptors.ES_PROPERTY_INDEX;
import static edu.msu.nscl.olog.OlogResourceDescriptors.ES_PROPERTY_TYPE;
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

import edu.msu.nscl.olog.entity.Property;
import edu.msu.nscl.olog.entity.State;

@Repository
public class PropertyRepository implements ElasticsearchRepository<Property, String>
{

    @Autowired
    Client client;

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Iterable<Property> findAll(Sort sort)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Page<Property> findAll(Pageable pageable)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends Property> S save(S entity)
    {
        return index(entity);
    }

    @Override
    public <S extends Property> Iterable<S> saveAll(Iterable<S> properties)
    {
        BulkRequestBuilder bulk = client.prepareBulk();
        properties.forEach(property -> {
            try
            {
                bulk.add(client.prepareIndex(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, property.getName())
                        .setSource(mapper.writeValueAsBytes(property), XContentType.JSON));
            } catch (JsonProcessingException e)
            {
                PropertiesResource.log.log(Level.SEVERE, e.getMessage(), e);
            }
        });
        BulkResponse bulkResponse = bulk.get("10s");
        if (bulkResponse.hasFailures())
        {
            // process failures by iterating through each bulk response item
            bulkResponse.forEach(response -> {
                if (response.getFailure() != null)
                {
                    PropertiesResource.log.log(Level.SEVERE, response.getFailureMessage(), response.getFailure().getCause());
                }
                ;
            });
            return null;
        } else
        {
            return properties;
        }
    }

    @Override
    public Optional<Property> findById(String propertyName)
    {
        try
        {
            GetResponse result = client.prepareGet(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, propertyName).get();
            if (result.isExists())
            {
                return Optional.of(mapper.readValue(result.getSourceAsBytesRef().streamInput(), Property.class));
            } else
            {
                return Optional.empty();
            }
        } catch (Exception e)
        {
            PropertiesResource.log.log(Level.SEVERE, e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public boolean existsById(String id)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Iterable<Property> findAll()
    {
        try
        {
            SearchResponse response = client.prepareSearch(ES_PROPERTY_INDEX).setTypes(ES_PROPERTY_TYPE)
                    .setQuery(QueryBuilders.termQuery("state", State.Active.toString())).get("10s");
            List<Property> result = new ArrayList<Property>();
            response.getHits().forEach(h -> {
                BytesReference b = h.getSourceRef();
                try
                {
                    result.add(mapper.readValue(b.streamInput(), Property.class));
                } catch (IOException e)
                {
                    PropertiesResource.log.log(Level.SEVERE, e.getMessage(), e);
                }
            });
            return result;
        } catch (Exception e)
        {
            PropertiesResource.log.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Iterable<Property> findAllById(Iterable<String> ids)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long count()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void deleteById(String propertyName)
    {
        try
        {
            UpdateResponse response = client.prepareUpdate(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, propertyName)
                    .setDoc(jsonBuilder().startObject().field("state", State.Inactive).endObject()).get();

            if (response.getResult().equals(Result.UPDATED))
            {
                BytesReference ref = client.prepareGet(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, response.getId()).get()
                        .getSourceAsBytesRef();
                Property deletedProperty = mapper.readValue(ref.streamInput(), Property.class);
                PropertiesResource.log.log(Level.INFO, "Deleted property " + deletedProperty.toLogger());
            }
        } catch (DocumentMissingException e)
        {
            PropertiesResource.log.log(Level.SEVERE, propertyName + " Does not exist and thus cannot be deleted");
        } catch (Exception e)
        {
            PropertiesResource.log.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Override
    public void delete(Property property)
    {
        deleteById(property.getName());
    }

    @Override
    public void deleteAll(Iterable<? extends Property> entities)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteAll()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public <S extends Property> S index(S property)
    {
        try
        {
            IndexResponse response = client.prepareIndex(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, property.getName())
                    .setSource(mapper.writeValueAsBytes(property), XContentType.JSON).get();

            if (response.getResult().equals(Result.CREATED))
            {
                BytesReference ref = client.prepareGet(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, response.getId()).get()
                        .getSourceAsBytesRef();
                Property createdProperty = mapper.readValue(ref.streamInput(), Property.class);
                return (S) createdProperty;
            }
        } catch (Exception e)
        {
            PropertiesResource.log.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Iterable<Property> search(QueryBuilder query)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Page<Property> search(QueryBuilder query, Pageable pageable)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Page<Property> search(SearchQuery searchQuery)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Page<Property> searchSimilar(Property entity, String[] fields, Pageable pageable)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void refresh()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public Class<Property> getEntityClass()
    {
        // TODO Auto-generated method stub
        return null;
    }

}
