/**
 * 
 */
package gov.bnl.olog;

import javax.annotation.PostConstruct;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

/**
 * @author Kunal Shroff
 *
 */
@Service
public class SequenceGenerator
{

    public static final String seqIndex = "olog_sequence";

    private static Client client;

    @Autowired
    ElasticsearchOperations es;

    private static SequenceGenerator instance = new SequenceGenerator();


    @PostConstruct
    public void init()
    {
        SequenceGenerator.client = es.getClient();
    }

    /**
     * get a new unique id from the olog_sequnce index
     * 
     * @return
     */
    public static long getID()
    {
        IndexResponse response = client.prepareIndex("olog_sequence", "olog_sequence", "id")
                .setSource(0, XContentType.class).get();
        return response.getVersion();
    }

}
