/**
 * 
 */
package org.phoebus.olog;

import java.io.IOException;

import javax.annotation.PostConstruct;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author Kunal Shroff
 *
 */
@Service
public class SequenceGenerator
{

    @Value("${elasticsearch.sequence.index:olog_sequence}")
    private String ES_SEQUENCE_INDEX;
    @Value("${elasticsearch.sequence.type:olog_sequence}")
    private String ES_SEQUENCE_TYPE;

    @Autowired
    @Qualifier("client")
    private ElasticsearchClient client;

    @PostConstruct
    public void init()
    {
        Application.logger.config("Initializing the unique sequence id generator");
    }

    /**
     * get a new unique id from the olog_sequnce index
     * 
     * @return a new unique id for a olog entry
     * @throws IOException The Elasticsearch client may throw this
     */
    public long getID() throws IOException
    {

        IndexRequest<Long> indexRequest = IndexRequest.of(i -> i.index(ES_SEQUENCE_INDEX));
        IndexResponse response = client.index(indexRequest);
        return response.version();
    }

}
