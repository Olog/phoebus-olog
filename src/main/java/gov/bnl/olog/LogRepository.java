package gov.bnl.olog;

import static gov.bnl.olog.OlogResourceDescriptors.ES_LOG_INDEX;
import static gov.bnl.olog.OlogResourceDescriptors.ES_LOG_TYPE;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.bnl.olog.entity.Attachment;
import gov.bnl.olog.entity.Log;
import gov.bnl.olog.entity.Log.LogBuilder;

@Repository
public class LogRepository implements CrudRepository<Log, String>
{

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
                log.getAttachments().forEach(attachment -> {
                    createdAttachments.add(attachmentRepository.save(attachment));
                });
                validatedLog = validatedLog.setAttachments(createdAttachments);
            }

            IndexRequest indexRequest = new IndexRequest(ES_LOG_INDEX, ES_LOG_TYPE, String.valueOf(id))
                    .source(mapper.writeValueAsBytes(validatedLog.build()), XContentType.JSON);

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
    public <S extends Log> Iterable<S> saveAll(Iterable<S> entities)
    {
        List<S> createdLogs = new ArrayList<S>();
        entities.forEach(log -> {
            createdLogs.add(log);
        });
        return createdLogs;
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
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Deleting log entries is not supported");
    }

    @Override
    public void delete(Log entity)
    {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Deleting log entries is not supported");
    }

    @Override
    public void deleteAll(Iterable<? extends Log> entities)
    {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Deleting log entries is not supported");
    }

    @Override
    public void deleteAll()
    {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Deleting log entries is not supported");
    }
}
