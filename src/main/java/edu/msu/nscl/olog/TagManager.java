package edu.msu.nscl.olog;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TagManager {

    // instance a json mapper
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final Logger log = Logger.getLogger(TagManager.class.getName());
    public static final String ologTagIndex = "olog_tags";

    /**
     * List all the tags
     * 
     * @return List tags
     */
    public static List<Tag> list() {
        try (TransportClient client = new PreBuiltTransportClient(Settings.EMPTY)
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300))) {
            SearchResponse response = client.search(new SearchRequest(ologTagIndex)).get();
            List<Tag> result = new ArrayList<Tag>();
            response.getHits().forEach(h -> {
                BytesReference b = h.getSourceRef();
                try {
                    result.add(mapper.readValue(b.streamInput(), Tag.class));
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
     * List all {@link State#Active} tags
     * 
     * @return a list of all active tags
     */
    public static List<Tag> listActive() {
        try (TransportClient client = new PreBuiltTransportClient(Settings.EMPTY)
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300))) {
            SearchResponse response = client.prepareSearch(ologTagIndex).setTypes("tag")
                    .setQuery(QueryBuilders.termQuery("state", State.Active.toString())).get();
            List<Tag> result = new ArrayList<Tag>();
            response.getHits().forEach(h -> {
                BytesReference b = h.getSourceRef();
                try {
                    result.add(mapper.readValue(b.streamInput(), Tag.class));
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
     * Create a new tag
     * 
     * @param tag
     * @return
     */
    public static Optional<Tag> createTag(Tag tag) {
        try (TransportClient client = new PreBuiltTransportClient(Settings.EMPTY)
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300))) {
            IndexResponse response = client.prepareIndex(ologTagIndex, "tag", tag.getName())
                    .setSource(mapper.writeValueAsBytes(tag), XContentType.JSON).get();
            Result result = response.getResult();
            if (response.getResult().equals(Result.CREATED)) {
                BytesReference ref = client.prepareGet(ologTagIndex, "tag", response.getId()).get()
                        .getSourceAsBytesRef();
                Tag createdTag = mapper.readValue(ref.streamInput(), Tag.class);
                createdTag.setId(response.getId());
                return Optional.of(createdTag);
            }
            System.out.println(result);
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        return Optional.empty();
    }

}
