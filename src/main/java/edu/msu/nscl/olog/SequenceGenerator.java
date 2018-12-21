/**
 * 
 */
package edu.msu.nscl.olog;

import javax.annotation.PostConstruct;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Kunal Shroff
 *
 */
@Component
public class SequenceGenerator
{

    private static ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private ElasticsearchTemplate initializedElasticsearchTemplate;

    public static final String seqIndex = "olog_sequence";

    @PostConstruct
    public void init()
    {
        elasticsearchTemplate = initializedElasticsearchTemplate;
    }

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
