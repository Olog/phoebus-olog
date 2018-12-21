package edu.msu.nscl.olog;

import static edu.msu.nscl.olog.OlogResourceDescriptors.ES_LOG_INDEX;
import static edu.msu.nscl.olog.OlogResourceDescriptors.ES_LOG_TYPE;

import java.time.Instant;
import java.util.Optional;
import java.util.logging.Level;

import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.msu.nscl.olog.entity.Log;
import edu.msu.nscl.olog.entity.Log.LogBuilder;

import static edu.msu.nscl.olog.entity.Log.LogBuilder.*;
import edu.msu.nscl.olog.entity.Tag;

@Repository
public class LogRepository implements ElasticsearchRepository<Log, String>
{

    @Autowired
    Client client;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Iterable<Log> findAll(Sort sort)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Page<Log> findAll(Pageable pageable)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends Log> S save(S entity)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends Log> Iterable<S> saveAll(Iterable<S> entities)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Optional<Log> findById(String id)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean existsById(String id)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Iterable<Log> findAll()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<Log> findAllById(Iterable<String> ids)
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
    public void deleteById(String id)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(Log entity)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteAll(Iterable<? extends Log> entities)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteAll()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public Iterable<Log> search(QueryBuilder query)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Page<Log> search(QueryBuilder query, Pageable pageable)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Page<Log> search(SearchQuery searchQuery)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Page<Log> searchSimilar(Log entity, String[] fields, Pageable pageable)
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
    public Class<Log> getEntityClass()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends Log> S index(S log)
    {
        try
        {
            Long id = SequenceGenerator.getID();
            Log validatedLog = LogBuilder.createLog(log).id(id).createDate(Instant.now()).build();
            byte[] bytes = mapper.writeValueAsBytes(validatedLog);
            IndexRequestBuilder request = client.prepareIndex(ES_LOG_INDEX, ES_LOG_TYPE, String.valueOf(id))
                    .setSource(mapper.writeValueAsBytes(validatedLog), XContentType.JSON);
            IndexResponse response = request.get();

            if (response.getResult().equals(Result.CREATED))
            {
                BytesReference ref = client.prepareGet(ES_LOG_INDEX, ES_LOG_TYPE, response.getId()).get()
                        .getSourceAsBytesRef();
                Log createdLog = mapper.readValue(ref.streamInput(), Log.class);
                return (S) createdLog;
            }
        } catch (Exception e)
        {
            TagsResource.log.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }
}
