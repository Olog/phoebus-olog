package gov.bnl.olog;

import static gov.bnl.olog.OlogResourceDescriptors.ES_LOGBOOK_INDEX;
import static gov.bnl.olog.OlogResourceDescriptors.ES_LOGBOOK_TYPE;
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
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
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
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.bnl.olog.entity.Logbook;
import gov.bnl.olog.entity.State;

@Repository
public class LogbookRepository implements CrudRepository<Logbook, String>
{

    @Autowired
    @Qualifier("indexClient")
    RestHighLevelClient client;

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public <S extends Logbook> S save(S entity)
    {
        return index(entity);
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
                LogbooksResource.log.log(Level.SEVERE, e.getMessage(), e);
            }
        });
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
                        LogbooksResource.log.log(Level.SEVERE, response.getFailureMessage(),
                                response.getFailure().getCause());
                    }
                    ;
                });
            } else
            {
                return logbooks;
            }
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
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

            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(QueryBuilders.termQuery("state", State.Active.toString()));
            sourceBuilder.timeout(new TimeValue(10, TimeUnit.SECONDS));

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

            UpdateResponse response = client.update(
                    new UpdateRequest(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, logbookName)
                            .doc(jsonBuilder().startObject().field("state", State.Inactive).endObject()),
                    RequestOptions.DEFAULT);

            if (response.getResult().equals(Result.UPDATED))
            {
                BytesReference ref = client
                        .get(new GetRequest(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, response.getId()), RequestOptions.DEFAULT)
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

    public <S extends Logbook> S index(S logbook)
    {
        try
        {

            IndexRequest indexRequest = new IndexRequest(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, logbook.getName())
                    .source(mapper.writeValueAsBytes(logbook), XContentType.JSON);

            IndexResponse response = client.index(indexRequest, RequestOptions.DEFAULT);
            
            if (response.getResult().equals(Result.CREATED))
            {
                BytesReference ref = client
                        .get(new GetRequest(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, response.getId()), RequestOptions.DEFAULT)
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


}
