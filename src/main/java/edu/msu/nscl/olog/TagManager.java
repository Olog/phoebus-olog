package edu.msu.nscl.olog;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.engine.DocumentMissingException;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A manager class for handling tag operations
 * 
 * @author kunalshroff
 *
 */
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

            if (response.getResult().equals(Result.CREATED)) {
                BytesReference ref = client.prepareGet(ologTagIndex, "tag", response.getId()).get()
                        .getSourceAsBytesRef();
                Tag createdTag = mapper.readValue(ref.streamInput(), Tag.class);
                return Optional.of(createdTag);
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Delete the given tag
     * 
     * @param tag
     * @return
     */
    public static Optional<Tag> deleteTag(Tag tag) {
        try (TransportClient client = new PreBuiltTransportClient(Settings.EMPTY)
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300))) {

            UpdateResponse response = client.prepareUpdate(ologTagIndex, "tag", tag.getName())
                    .setDoc(jsonBuilder().startObject().field("state", State.Inactive).endObject()).get();

            if (response.getResult().equals(Result.UPDATED)) {
                BytesReference ref = client.prepareGet(ologTagIndex, "tag", response.getId()).get()
                        .getSourceAsBytesRef();
                Tag deletedTag = mapper.readValue(ref.streamInput(), Tag.class);
                return Optional.of(deletedTag);
            }
        } catch (DocumentMissingException e) {
            log.log(Level.SEVERE, tag.getName() + " Does not exist and thus cannot be deleted");
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Find the
     * 
     * @param tag
     * @return
     */
    public static Optional<Tag> findTag(String tagName) {
        try (TransportClient client = new PreBuiltTransportClient(Settings.EMPTY)
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300))) {
            return Optional.of(mapper.readValue(
                    client.prepareGet(ologTagIndex, "tag", tagName).get().getSourceAsBytesRef().streamInput(),
                    Tag.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

}
