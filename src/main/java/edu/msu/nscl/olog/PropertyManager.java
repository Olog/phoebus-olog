package edu.msu.nscl.olog;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.engine.DocumentMissingException;
import org.elasticsearch.index.query.QueryBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.msu.nscl.olog.entity.Property;
import edu.msu.nscl.olog.entity.State;

public class PropertyManager {
//
//    public static final String ologPropertyIndex = "olog_properties";
//    // instance a json mapper
//    private static final ObjectMapper mapper = new ObjectMapper();
//    private static final Logger log = Logger.getLogger(PropertyManager.class.getName());
//
//    public static List<Property> list() {
//        try {
//            SearchResponse response = getSearchClient().search(new SearchRequest(ologPropertyIndex)).get();
//            List<Property> result = new ArrayList<Property>();
//            response.getHits().forEach(h -> {
//                BytesReference b = h.getSourceRef();
//                try {
//                    result.add(mapper.readValue(b.streamInput(), Property.class));
//                } catch (IOException e) {
//                    log.log(Level.SEVERE, e.getMessage(), e);
//                }
//            });
//            return result;
//        } catch (Exception e) {
//            log.log(Level.SEVERE, e.getMessage(), e);
//        }
//        return Collections.emptyList();
//    }
//
//    /**
//     * List all the currently active {@link Property}
//     * 
//     * @return List of all the active {@link Property}s
//     */
//    public static List<Property> listActive() {
//        try {
//            SearchResponse response = getSearchClient().prepareSearch(ologPropertyIndex).setTypes("property")
//                    .setQuery(QueryBuilders.termQuery("state", State.Active.toString())).get();
//            List<Property> result = new ArrayList<Property>();
//            response.getHits().forEach(h -> {
//                BytesReference b = h.getSourceRef();
//                try {
//                    result.add(mapper.readValue(b.streamInput(), Property.class));
//                } catch (IOException e) {
//                    log.log(Level.SEVERE, e.getMessage(), e);
//                }
//            });
//            return result;
//        } catch (Exception e) {
//            log.log(Level.SEVERE, e.getMessage(), e);
//        }
//        return Collections.emptyList();
//    }
//
//    /**
//     * Create a new property
//     * 
//     * @param property
//     * @return the created property
//     */
//    public static Optional<Property> create(Property property) {
//        try {
//            IndexResponse response = getSearchClient().prepareIndex(ologPropertyIndex, "property", property.getName())
//                    .setSource(mapper.writeValueAsBytes(property), XContentType.JSON).get();
//
//            if (response.getResult().equals(Result.CREATED)) {
//                BytesReference ref = getSearchClient().prepareGet(ologPropertyIndex, "property", response.getId()).get()
//                        .getSourceAsBytesRef();
//                Property createdProperty = mapper.readValue(ref.streamInput(), Property.class);
//                return Optional.of(createdProperty);
//            }
//        } catch (Exception e) {
//            log.log(Level.SEVERE, e.getMessage(), e);
//        }
//        return Optional.empty();
//    }
//
//    /**
//     * Delete a property
//     * 
//     * @param property
//     * @return the deleted property
//     */
//    public static Optional<Property> delete(Property property) {
//        try {
//            UpdateResponse response = getSearchClient().prepareUpdate(ologPropertyIndex, "property", property.getName())
//                    .setDoc(jsonBuilder().startObject().field("state", State.Inactive).endObject()).get();
//            if (response.getResult().equals(Result.UPDATED)) {
//                BytesReference ref = getSearchClient().prepareGet(ologPropertyIndex, "property", response.getId()).get()
//                        .getSourceAsBytesRef();
//                Property deletedProperty = mapper.readValue(ref.streamInput(), Property.class);
//                return Optional.of(deletedProperty);
//            }
//        } catch (DocumentMissingException e) {
//            log.log(Level.SEVERE, property.getName() + " Does not exist and thus cannot be deleted");
//        } catch (Exception e) {
//            log.log(Level.SEVERE, e.getMessage(), e);
//        }
//        return Optional.empty();
//    }
}
