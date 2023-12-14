/**
 *
 */
package org.phoebus.olog;

import java.io.IOException;
import java.time.Instant;

import javax.annotation.PostConstruct;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * @author Kunal Shroff
 *
 */
@Service
public class SequenceGenerator
{
    @SuppressWarnings("unused")
    @Value("${elasticsearch.sequence.index:olog_sequence}")
    private String ES_LOG_SEQ;

    @Autowired
    @Qualifier("client")
    private ElasticsearchClient client;

    private OlogSequence seq;
    private ObjectMapper objectMapper;
    private IndexRequest request;

    @PostConstruct
    public void init()
    {
        seq = new OlogSequence();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        request = IndexRequest.of(i -> i.index(ES_LOG_SEQ)
                .document(JsonData.of(seq, new JacksonJsonpMapper(objectMapper)))
                .refresh(Refresh.True));

    }

    /**
     * get a new unique id from the olog_sequnce index
     *
     * @return a new unique id for a olog entry
     * @throws IOException The Elasticsearch client may throw this
     */
    public synchronized  long getID() throws IOException
    {
        return client.index(request).seqNo();
    }


    private static class OlogSequence {
        private final Instant createDate;

        OlogSequence() {
            createDate = Instant.now();
        }

        public Instant getCreateDate() {
            return createDate;
        }
    }
}
