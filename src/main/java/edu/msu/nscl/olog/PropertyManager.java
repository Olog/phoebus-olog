package edu.msu.nscl.olog;

import static edu.msu.nscl.olog.ElasticSearchClient.getSearchClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.index.query.QueryBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PropertyManager {

    public static final String ologPropertyIndex = "olog_properties";
    // instance a json mapper
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = Logger.getLogger(PropertyManager.class.getName());

    public static List<Property> list() {
        try {
            SearchResponse response = getSearchClient().search(new SearchRequest(ologPropertyIndex)).get();
            List<Property> result = new ArrayList<Property>();
            response.getHits().forEach(h -> {
                BytesReference b = h.getSourceRef();
                try {
                    result.add(mapper.readValue(b.streamInput(), Property.class));
                } catch (IOException e) {
                    log.log(Level.SEVERE, e.getMessage(), e);
                }
            });
            return result;
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    /**
     * List all the currently active {@link Property}
     * 
     * @return List of all the active {@link Property}s
     */
    public static List<Property> listActive() {
        try {
            SearchResponse response = getSearchClient().prepareSearch(ologPropertyIndex).setTypes("property")
                    .setQuery(QueryBuilders.termQuery("state", State.Active.toString())).get();
            List<Property> result = new ArrayList<Property>();
            response.getHits().forEach(h -> {
                BytesReference b = h.getSourceRef();
                try {
                    result.add(mapper.readValue(b.streamInput(), Property.class));
                } catch (IOException e) {
                    log.log(Level.SEVERE, e.getMessage(), e);
                }
            });
            return result;
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        return Collections.emptyList();
    }
}
