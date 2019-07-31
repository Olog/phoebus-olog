package gov.bnl.olog;

import static gov.bnl.olog.OlogResourceDescriptors.ES_LOG_INDEX;
import static gov.bnl.olog.OlogResourceDescriptors.ES_LOG_TYPE;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.get.GetResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.bnl.olog.entity.Attachment;
import gov.bnl.olog.entity.Log;
import gov.bnl.olog.entity.Log.LogBuilder;

@Repository
public class LogRepository implements ElasticsearchRepository<Log, String>
{

    @Autowired
    Client client;
    @Autowired
    AttachmentRepository attachmentRepository;

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
        try
        {
            GetResponse result = client.prepareGet(ES_LOG_INDEX, ES_LOG_TYPE, id).get();
            Log createdLog = mapper.readValue(result.getSourceAsBytesRef().streamInput(), Log.class);
            return Optional.of(createdLog);
        } catch (IOException e)
        {
            // TODO improve the exception handling based on
            // https://www.baeldung.com/exception-handling-for-rest-with-spring#controlleradvice
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to retrieve log with id " + id, e);
        }
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
            LogBuilder validatedLog = LogBuilder.createLog(log).id(id).createDate(Instant.now());
            if(log.getAttachments() != null && !log.getAttachments().isEmpty()) {
                Set<Attachment> createdAttachments = new HashSet<Attachment>(); 
                log.getAttachments().forEach(attachment -> {
                    createdAttachments.add(attachmentRepository.save(attachment));
                });
                validatedLog = validatedLog.setAttachments(createdAttachments);
            }

            IndexRequestBuilder request = client.prepareIndex(ES_LOG_INDEX, ES_LOG_TYPE, String.valueOf(id))
                    .setSource(mapper.writeValueAsBytes(validatedLog.build()), XContentType.JSON);
            IndexResponse response = request.get();

            if (response.getResult().equals(Result.CREATED))
            {
                BytesReference ref = client.prepareGet(ES_LOG_INDEX, ES_LOG_TYPE, response.getId()).get().getSourceAsBytesRef();
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
