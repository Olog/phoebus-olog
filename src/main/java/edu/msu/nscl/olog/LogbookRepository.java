package edu.msu.nscl.olog;

import static edu.msu.nscl.olog.OlogResourceDescriptors.ES_LOGBOOK_INDEX;
import static edu.msu.nscl.olog.OlogResourceDescriptors.ES_LOGBOOK_TYPE;
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

import edu.msu.nscl.olog.entity.Logbook;
import edu.msu.nscl.olog.entity.State;

@Repository
public class LogbookRepository implements ElasticsearchRepository<Logbook, String>
{

    @Autowired
    Client client;

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Iterable<Logbook> findAll(Sort sort)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Page<Logbook> findAll(Pageable pageable)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends Logbook> S save(S entity)
    {
        return index(entity);
    }

    @Override
    public <S extends Logbook> Iterable<S> saveAll(Iterable<S> logbooks)
    {
        BulkRequestBuilder bulk = client.prepareBulk();
        logbooks.forEach(logbook -> {
            try
            {
                bulk.add(client.prepareIndex(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, logbook.getName())
                        .setSource(mapper.writeValueAsBytes(logbook), XContentType.JSON));
            } catch (JsonProcessingException e)
            {
                LogbooksResource.log.log(Level.SEVERE, e.getMessage(), e);
            }
        });
        BulkResponse bulkResponse = bulk.get("10s");
        if (bulkResponse.hasFailures())
        {
            // process failures by iterating through each bulk response item
            bulkResponse.forEach(response -> {
                if (response.getFailure() != null)
                {
                    LogbooksResource.log.log(Level.SEVERE, response.getFailureMessage(), response.getFailure().getCause());
                }
                ;
            });
            return null;
        } else
        {
            return logbooks;
        }
    }

    @Override
    public Optional<Logbook> findById(String logbookName)
    {
        try
        {
            GetResponse result = client.prepareGet(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, logbookName).get();
            if (result.isExists())
            {
                return Optional.of(mapper.readValue(result.getSourceAsBytesRef().streamInput(), Logbook.class));
            } else
            {
                return Optional.empty();
            }
        } catch (Exception e)
        {
            LogbooksResource.log.log(Level.SEVERE, e.getMessage(), e);
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
    public Iterable<Logbook> findAll()
    {

        try
        {
            SearchResponse response = client.prepareSearch(ES_LOGBOOK_INDEX).setTypes(ES_LOGBOOK_TYPE)
                    .setQuery(QueryBuilders.termQuery("state", State.Active.toString())).get("10s");
            List<Logbook> result = new ArrayList<Logbook>();
            response.getHits().forEach(h -> {
                BytesReference b = h.getSourceRef();
                try
                {
                    result.add(mapper.readValue(b.streamInput(), Logbook.class));
                } catch (IOException e)
                {
                    LogbooksResource.log.log(Level.SEVERE, e.getMessage(), e);
                }
            });
            return result;
        } catch (Exception e)
        {
            LogbooksResource.log.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Iterable<Logbook> findAllById(Iterable<String> ids)
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
    public void deleteById(String logbookName)
    {
        try
        {
            UpdateResponse response = client.prepareUpdate(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, logbookName)
                    .setDoc(jsonBuilder().startObject().field("state", State.Inactive).endObject()).get();

            if (response.getResult().equals(Result.UPDATED))
            {
                BytesReference ref = client.prepareGet(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, response.getId()).get()
                        .getSourceAsBytesRef();
                Logbook deletedLogbook = mapper.readValue(ref.streamInput(), Logbook.class);
                LogbooksResource.log.log(Level.INFO, "Deleted logbook " + deletedLogbook.toLogger());
            }
        } catch (DocumentMissingException e)
        {
            LogbooksResource.log.log(Level.SEVERE, logbookName + " Does not exist and thus cannot be deleted");
        } catch (Exception e)
        {
            LogbooksResource.log.log(Level.SEVERE, e.getMessage(), e);
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
        // TODO Auto-generated method stub
    }

    @Override
    public <S extends Logbook> S index(S logbook)
    {
        try
        {
            IndexResponse response = client.prepareIndex(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, logbook.getName())
                    .setSource(mapper.writeValueAsBytes(logbook), XContentType.JSON).get();

            if (response.getResult().equals(Result.CREATED))
            {
                BytesReference ref = client.prepareGet(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, response.getId()).get()
                        .getSourceAsBytesRef();
                Logbook createdLogbook = mapper.readValue(ref.streamInput(), Logbook.class);
                return (S) createdLogbook;
            }
        } catch (Exception e)
        {
            LogbooksResource.log.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Iterable<Logbook> search(QueryBuilder query)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Page<Logbook> search(QueryBuilder query, Pageable pageable)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Page<Logbook> search(SearchQuery searchQuery)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Page<Logbook> searchSimilar(Logbook entity, String[] fields, Pageable pageable)
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
    public Class<Logbook> getEntityClass()
    {
        // TODO Auto-generated method stub
        return null;
    }

}
