package org.phoebus.olog;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.ExistsTemplateRequest;
import co.elastic.clients.elasticsearch.indices.PutIndexTemplateRequest;
import co.elastic.clients.elasticsearch.indices.PutIndexTemplateResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;


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
            RestClient httpClient = RestClient.builder(new HttpHost(host, port)).build();

            // Create the Java API Client with the same low level client
            ElasticsearchTransport transport = new RestClientTransport(
                    httpClient,
                    new JacksonJsonpMapper()
            );
            client = new ElasticsearchClient(transport);
            esInitialized.set(!Boolean.parseBoolean(createIndices));
            if (esInitialized.compareAndSet(false, true)) {
                elasticIndexValidation(client);
            }
        }
        return client;
    }

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
        try {
            ExistsTemplateRequest request = new ExistsTemplateRequest.Builder()
                    .name(ES_LOG_INDEX+"_template")
                    .build();
            boolean exists = client.indices().existsTemplate(request).value();

            if(!exists) {
                InputStream is = ElasticConfig.class.getResourceAsStream("/log_template_mapping.json");
                PutIndexTemplateRequest templateRequest = new PutIndexTemplateRequest.Builder()
                        .name(ES_LOG_INDEX+"_template")
                        .indexPatterns(Arrays.asList(ES_LOG_INDEX+"*"))
                        .withJson(is)
                        .create(true)
                        .build();
                PutIndexTemplateResponse putTemplateResponse = client.indices().putIndexTemplate(templateRequest);
                putTemplateResponse.acknowledged();
                logger.log( Level.INFO, "Created " + ES_LOG_INDEX + " template.");
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create index " + ES_PROPERTY_INDEX, e);
        }

    }
}