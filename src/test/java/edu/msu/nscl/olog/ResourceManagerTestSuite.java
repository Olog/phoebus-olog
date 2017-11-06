package edu.msu.nscl.olog;

import static edu.msu.nscl.olog.LogManager.ologLogIndex;
import static edu.msu.nscl.olog.LogbookManager.ologLogbookIndex;
import static edu.msu.nscl.olog.PropertyManager.ologPropertyIndex;
import static edu.msu.nscl.olog.SequenceGenerator.seqIndex;
import static edu.msu.nscl.olog.TagManager.ologTagIndex;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.msu.nscl.olog.Log.LogBuilder;

@RunWith(Suite.class)
@Suite.SuiteClasses({ 
    TagManagerIT.class,
    LogbookManagerIT.class,
    PropertyManagerIT.class,
    LogSequenceIT.class,
    LogManagerIT.class })
public class ResourceManagerTestSuite {

    // instance a json mapper
    static ObjectMapper mapper = new ObjectMapper();

    public static final List<Tag> initialTags = Arrays.asList(
            new Tag("integration-test-tag1", State.Active),
            new Tag("integration-test-tag2", State.Inactive));

    public static final List<Logbook> initialLogbooks = Arrays.asList(
            new Logbook("integration-test-logbook1", "test-owner1", State.Active),
            new Logbook("integration-test-logbook1a", "test-owner1", State.Active),
            new Logbook("integration-test-logbook1b", "test-owner1", State.Active),
            new Logbook("integration-test-logbook2", "test-owner1", State.Active),
            new Logbook("integration-test-logbook2a", "test-owner1", State.Inactive));

    public static final List<Property> initialProperties = Arrays.asList(
            new Property("integration-test-property1",
                    new HashSet<Attribute>(Arrays.asList(new Attribute("attr1"), new Attribute("attr2")))),
            new Property("integration-test-property2",
                    new HashSet<Attribute>(Arrays.asList(new Attribute("attr1")))),
            new Property("integration-test-property3", State.Inactive,
                    new HashSet<Attribute>(Arrays.asList(new Attribute("attr1")))));

    public static final List<Log> initialLogs = Arrays.asList(
            LogBuilder.createLog("The first test log").owner("test").withLogbook(initialLogbooks.get(0)).build());
    
    @SuppressWarnings("unused")
    @BeforeClass
    public static void setup() {
        try (TransportClient client = new PreBuiltTransportClient(Settings.EMPTY)
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300))) {

            // Initialize the client via its context listener
            new ElasticSearchClient().contextInitialized(null);

            // Clean up the old indices and create brand new ones
            AdminClient adminClient = client.admin();
            if (adminClient.indices().exists(new IndicesExistsRequest(ologTagIndex)).get().isExists()) {
                DeleteIndexResponse response = adminClient.indices().delete(new DeleteIndexRequest(ologTagIndex)).get();
            }
            if (adminClient.indices().exists(new IndicesExistsRequest(ologLogbookIndex)).get().isExists()) {
                DeleteIndexResponse response = adminClient.indices().delete(new DeleteIndexRequest(ologLogbookIndex))
                        .get();
            }
            if (adminClient.indices().exists(new IndicesExistsRequest(ologPropertyIndex)).get().isExists()) {
                DeleteIndexResponse response = adminClient.indices().delete(new DeleteIndexRequest(ologPropertyIndex))
                        .get();
            }
            if (adminClient.indices().exists(new IndicesExistsRequest(seqIndex)).get().isExists()) {
                DeleteIndexResponse response = adminClient.indices().delete(new DeleteIndexRequest(seqIndex))
                        .get();
            }
            if (adminClient.indices().exists(new IndicesExistsRequest(ologLogIndex)).get().isExists()) {
                DeleteIndexResponse response = adminClient.indices().delete(new DeleteIndexRequest(ologLogIndex)).get();
            }
            adminClient.indices().create(new CreateIndexRequest(ologTagIndex)).actionGet();
            adminClient.indices().preparePutMapping(ologTagIndex).setType("tag")
                    .setSource(jsonBuilder()
                            .startObject().startObject("tag").startObject("properties")
                            .startObject("name").field("type", "string").field("analyzer", "whitespace").endObject()
                            .startObject("state").field("type", "string").field("analyzer", "whitespace").endObject()
                            .endObject().endObject().endObject())
                    .get();

            adminClient.indices().create(new CreateIndexRequest(ologLogbookIndex)).actionGet();
            adminClient.indices().preparePutMapping(ologLogbookIndex).setType("logbook")
                    .setSource(jsonBuilder()
                            .startObject().startObject("logbook").startObject("properties")
                            .startObject("name").field("type", "string").field("analyzer", "whitespace").endObject()
                            .startObject("state").field("type", "string").field("analyzer", "whitespace").endObject()
                            .endObject().endObject().endObject())
                    .get();

            adminClient.indices().create(new CreateIndexRequest(ologPropertyIndex)).actionGet();
            adminClient.indices().preparePutMapping(ologPropertyIndex).setType("property")
                    .setSource(jsonBuilder()
                            .startObject().startObject("property").startObject("properties")
                            .startObject("name").field("type", "string").field("analyzer", "whitespace").endObject()
                            .startObject("state").field("type", "string").field("analyzer", "whitespace").endObject()
                            .startObject("attributes").field("type", "nested").field("include_in_parent", "true")
                                .startObject("properties")
                                    .startObject("name").field("type", "string").field("analyzer", "whitespace").endObject()
                                    .startObject("value").field("type", "string").field("analyzer", "whitespace").endObject()
                                    .startObject("state").field("type", "string").field("analyzer", "whitespace").endObject()
                                .endObject()
                            .endObject()
                            .endObject().endObject().endObject())
                    .get();


            adminClient.indices().create(new CreateIndexRequest(ologLogIndex)).actionGet();

//            adminClient.indices().create(new CreateIndexRequest("olog_sequence")).actionGet();

            BulkRequestBuilder bulkRequest = client.prepareBulk();
            for (Tag tag : initialTags) {
                IndexRequest indexRequest = new IndexRequest(ologTagIndex, "tag", tag.getName());
                indexRequest.source(mapper.writeValueAsBytes(tag), XContentType.JSON);

                bulkRequest.add(indexRequest);
            }
            for (Logbook logbook : initialLogbooks) {

                IndexRequest indexRequest = new IndexRequest(ologLogbookIndex, "logbook", logbook.getName());
                indexRequest.source(mapper.writeValueAsBytes(logbook), XContentType.JSON);

                bulkRequest.add(indexRequest);
            }
            for (Property property : initialProperties) {

                IndexRequest indexRequest = new IndexRequest(ologPropertyIndex, "property", property.getName());
                indexRequest.source(mapper.writeValueAsBytes(property), XContentType.JSON);

                bulkRequest.add(indexRequest);
            }
            for (Log log : initialLogs) {
                long id = SequenceGenerator.getID();
                log.setId(id);
                IndexRequest indexRequest = new IndexRequest(ologLogIndex, "log", String.valueOf(id));
                indexRequest.source(mapper.writeValueAsBytes(log), XContentType.JSON);
                bulkRequest.add(indexRequest);
            }
            BulkResponse bulkResponse = bulkRequest.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void cleanup() {
        try (TransportClient client = new PreBuiltTransportClient(Settings.EMPTY)
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300))) {

            AdminClient adminClient = client.admin();
            if (adminClient.indices().exists(new IndicesExistsRequest(ologTagIndex)).get().isExists()) {
                DeleteIndexResponse response = adminClient.indices().delete(new DeleteIndexRequest(ologTagIndex)).get();
            }
            if (adminClient.indices().exists(new IndicesExistsRequest(ologLogbookIndex)).get().isExists()) {
                DeleteIndexResponse response = adminClient.indices().delete(new DeleteIndexRequest(ologLogbookIndex))
                        .get();
            }
            if (adminClient.indices().exists(new IndicesExistsRequest(ologPropertyIndex)).get().isExists()) {
                DeleteIndexResponse response = adminClient.indices().delete(new DeleteIndexRequest(ologPropertyIndex))
                        .get();
            }
            if (adminClient.indices().exists(new IndicesExistsRequest(seqIndex)).get().isExists()) {
                DeleteIndexResponse response = adminClient.indices().delete(new DeleteIndexRequest(seqIndex))
                        .get();
            }
            if (adminClient.indices().exists(new IndicesExistsRequest(ologLogIndex)).get().isExists()) {
                DeleteIndexResponse response = adminClient.indices().delete(new DeleteIndexRequest(ologLogIndex)).get();
            }

            // cleanup the client via its context listener
            new ElasticSearchClient().contextDestroyed(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
