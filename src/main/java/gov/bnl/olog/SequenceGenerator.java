/**
 * 
 */
package gov.bnl.olog;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * @author Kunal Shroff
 *
 */
@Service
public class SequenceGenerator
{

    public static final String seqIndex = "olog_sequence";

    @Autowired
    @Qualifier("indexClient")
    RestHighLevelClient indexClient;

//    private static SequenceGenerator instance = new SequenceGenerator();
    private static RestHighLevelClient client;


    @PostConstruct
    public void init()
    {
        Application.logger.config("Initializing the unique sequence id generator");
        SequenceGenerator.client = indexClient;
    }

    /**
     * get a new unique id from the olog_sequnce index
     * 
     * @return a new unique id for a olog entry
     * @throws IOException 
     */
    public static long getID() throws IOException
    {
        IndexResponse response = client.index(
                new IndexRequest("olog_sequence", "olog_sequence", "id").source(0, XContentType.class),
                RequestOptions.DEFAULT);
        return response.getVersion();
    }

}
