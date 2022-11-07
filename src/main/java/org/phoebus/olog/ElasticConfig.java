package org.phoebus.olog;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.Property;
import org.phoebus.olog.entity.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A element which creates the elastic rest clients used the olog service for
 * creating and retrieving logs and other resources
 *
 * @author kunal
 */
@Configuration
@ComponentScan(basePackages = {"org.phoebus.olog"})
@PropertySource("classpath:application.properties")
public class ElasticConfig {

    private static final Logger logger = Logger.getLogger(ElasticConfig.class.getName());

    // Read the elatic index and type from the application.properties
    @Value("${elasticsearch.tag.index:olog_tags}")
    private String ES_TAG_INDEX;
    @Value("${elasticsearch.logbook.index:olog_logbooks}")
    private String ES_LOGBOOK_INDEX;
    @Value("${elasticsearch.property.index:olog_properties}")
    private String ES_PROPERTY_INDEX;
    @Value("${elasticsearch.log.index:olog_logs}")
    private String ES_LOG_INDEX;
    @Value("${elasticsearch.sequence.index:olog_sequence}")
    private String ES_SEQ_INDEX;

    @Value("${elasticsearch.cluster.name:elasticsearch}")
    private String clusterName;
    @Value("${elasticsearch.network.host:localhost}")
    private String host;
    @Value("${elasticsearch.http.port:9200}")
    private int port;
    @Value("${elasticsearch.http.protocol:http}")
    private String protocol;
    @Value("${elasticsearch.create.indices:true}")
    private String createIndices;

    @Value("${default.logbook.url}")
    private String defaultLogbooksURL;
    @Value("${default.tags.url}")
    private String defaultTagsURL;
    @Value("${default.properties.url}")
    private String defaultPropertiesURL;

    private ElasticsearchClient client;
    private static final AtomicBoolean esInitialized = new AtomicBoolean();

    @Bean({"client"})
    public ElasticsearchClient getClient() {
        if (client == null) {
            // Create the low-level client
            RestClient httpClient = RestClient.builder(new HttpHost(host, port, protocol)).build();

            // Create the Java API Client with the same low level client
            ElasticsearchTransport transport = new RestClientTransport(
                    httpClient,
                    new JacksonJsonpMapper()
            );
            client = new ElasticsearchClient(transport);
            esInitialized.set(!Boolean.parseBoolean(createIndices));
            if (esInitialized.compareAndSet(false, true)) {
                elasticIndexValidation(client);
                elasticIndexInitialization(client);
            }
        }
        return client;
    }

    /**
     * Create the olog indices and templates if they don't exist
     * @param client
     */
    void elasticIndexValidation(ElasticsearchClient client) {

        // Olog Sequence Index
        try (InputStream is = ElasticConfig.class.getResourceAsStream("/seq_mapping.json")) {
            BooleanResponse exits = client.indices().exists(ExistsRequest.of(e -> e.index(ES_SEQ_INDEX)));
            if(!exits.value()) {

                CreateIndexResponse result = client.indices().create(
                        CreateIndexRequest.of(
                                c -> c.index(ES_SEQ_INDEX).withJson(is)));
                logger.info("Created index: " + ES_SEQ_INDEX + " : acknowledged " + result.acknowledged());
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create index " + ES_SEQ_INDEX, e);
        }

        // Olog Logbook Index
        try (InputStream is = ElasticConfig.class.getResourceAsStream("/logbook_mapping.json")) {
            BooleanResponse exits = client.indices().exists(ExistsRequest.of(e -> e.index(ES_LOGBOOK_INDEX)));
            if(!exits.value()) {

                CreateIndexResponse result = client.indices().create(
                        CreateIndexRequest.of(
                                c -> c.index(ES_LOGBOOK_INDEX).withJson(is)));
                logger.info("Created index: " + ES_LOGBOOK_INDEX + " : acknowledged " + result.acknowledged());
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create index " + ES_LOGBOOK_INDEX, e);
        }

        // Olog Tag Index
        try (InputStream is = ElasticConfig.class.getResourceAsStream("/tag_mapping.json")) {
            BooleanResponse exits = client.indices().exists(ExistsRequest.of(e -> e.index(ES_TAG_INDEX)));
            if(!exits.value()) {

                CreateIndexResponse result = client.indices().create(
                        CreateIndexRequest.of(
                                c -> c.index(ES_TAG_INDEX).withJson(is)));
                logger.info("Created index: " + ES_TAG_INDEX + " : acknowledged " + result.acknowledged());
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create index " + ES_TAG_INDEX, e);
        }

        // Olog Property Index
        try (InputStream is = ElasticConfig.class.getResourceAsStream("/property_mapping.json")) {
            BooleanResponse exits = client.indices().exists(ExistsRequest.of(e -> e.index(ES_PROPERTY_INDEX)));
            if(!exits.value()) {

                CreateIndexResponse result = client.indices().create(
                        CreateIndexRequest.of(
                                c -> c.index(ES_PROPERTY_INDEX).withJson(is)));
                logger.info("Created index: " + ES_PROPERTY_INDEX + " : acknowledged " + result.acknowledged());
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create index " + ES_PROPERTY_INDEX, e);
        }

        // Olog Log Template
        try (InputStream is = ElasticConfig.class.getResourceAsStream("/log_entry_mapping.json")) {
            BooleanResponse exits = client.indices().exists(ExistsRequest.of(e -> e.index(ES_LOG_INDEX)));
            if(!exits.value()) {

                CreateIndexResponse result = client.indices().create(
                        CreateIndexRequest.of(
                                c -> c.index(ES_LOG_INDEX).withJson(is)));
                logger.info("Created index: " + ES_LOG_INDEX + " : acknowledged " + result.acknowledged());
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create index " + ES_LOG_INDEX, e);
        }

    }

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Create the default logbooks, tags, and properties
     * @param indexClient the elastic client instance used to create the default resources
     */
    private void elasticIndexInitialization(ElasticsearchClient indexClient) {
        // Setup the default logbooks
        String logbooksURL = defaultLogbooksURL;
        if (logbooksURL.isEmpty())
        {
            final URL resource = getClass().getResource("/default_logbooks.json");
            logbooksURL = resource.toExternalForm();
        }
        try (InputStream input = new URL(logbooksURL).openStream() ) {
            List<Logbook> jsonLogbooks = mapper.readValue(input, new TypeReference<List<Logbook>>(){});

            jsonLogbooks.stream().forEach(logbook -> {
                try {
                    if(!indexClient.exists(e -> e.index(ES_LOGBOOK_INDEX).id(logbook.getName())).value()){
                        IndexRequest<Logbook> indexRequest =
                                IndexRequest.of(i ->
                                        i.index(ES_LOGBOOK_INDEX)
                                                .id(logbook.getName())
                                                .document(logbook)
                                                .refresh(Refresh.True));
                        IndexResponse response = client.index(indexRequest);
                    }
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to initialize logbook : " +logbook.getName(), e);
                }
            });
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to initialize logbooks ", ex);
        }

        // Setup the default tags
        String tagsURL = defaultTagsURL;
        if (defaultTagsURL.isEmpty())
        {
            final URL resource = getClass().getResource("/default_tags.json");
            tagsURL = resource.toExternalForm();
        }
        try (InputStream input = new URL(tagsURL).openStream() ) {
            List<Tag> jsonTag = mapper.readValue(input, new TypeReference<List<Tag>>(){});

            jsonTag.stream().forEach(tag -> {
                try {
                    if(!indexClient.exists(e -> e.index(ES_TAG_INDEX).id(tag.getName())).value()){
                        IndexRequest<Tag> indexRequest =
                                IndexRequest.of(i ->
                                        i.index(ES_TAG_INDEX)
                                                .id(tag.getName())
                                                .document(tag)
                                                .refresh(Refresh.True));
                        IndexResponse response = client.index(indexRequest);
                    }
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to initialize tag : " +tag.getName(), e);
                }
            });
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to initialize tag ", ex);
        }

        // Setup the default properties
        String propertiesURL = defaultPropertiesURL;
        if (propertiesURL.isEmpty())
        {
            final URL resource = getClass().getResource("/default_properties.json");
            propertiesURL = resource.toExternalForm();
        }
        try (InputStream input = new URL(propertiesURL).openStream() ) {
            List<Property> jsonTag = mapper.readValue(input, new TypeReference<List<Property>>(){});

            jsonTag.stream().forEach(property -> {
                try {
                    if(!indexClient.exists(e -> e.index(ES_PROPERTY_INDEX).id(property.getName())).value()){
                        IndexRequest<Property> indexRequest =
                                IndexRequest.of(i ->
                                        i.index(ES_PROPERTY_INDEX)
                                                .id(property.getName())
                                                .document(property)
                                                .refresh(Refresh.True));
                        IndexResponse response = client.index(indexRequest);
                    }
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to initialize property : " +property.getName(), e);
                }
            });
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to initialize property ", ex);
        }
    }
}