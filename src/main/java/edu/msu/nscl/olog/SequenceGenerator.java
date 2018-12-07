/**
 * 
 */
package edu.msu.nscl.olog;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.xcontent.XContentType;

/**
 * @author Kunal Shroff
 *
 */
public class SequenceGenerator {

    public static final String seqIndex = "olog_sequence";

    /**
     * get a new unique id from the olog_sequnce index
     * 
     * @return
     */
    public static long getID() {
//        IndexResponse response = getSearchClient().prepareIndex("olog_sequence", "olog_sequence", "id")
//                .setSource(0, XContentType.class).get();
//        return response.getVersion();
    	return 0;
    }

}
