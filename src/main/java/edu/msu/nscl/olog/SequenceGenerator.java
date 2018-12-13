/**
 * 
 */
package edu.msu.nscl.olog;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

/**
 * @author Kunal Shroff
 *
 */
public class SequenceGenerator
{

    @Autowired
    private static ElasticsearchTemplate elasticsearchTemplate;

    public static final String seqIndex = "olog_sequence";

    /**
     * get a new unique id from the olog_sequnce index
     * 
     * @return
     */
    public static long getID()
    {
        IndexResponse response = elasticsearchTemplate.getClient().prepareIndex("olog_sequence", "olog_sequence", "id")
                .setSource(0, XContentType.class).get();
        return response.getVersion();
    }

}
