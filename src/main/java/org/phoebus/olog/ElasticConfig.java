package org.phoebus.olog;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexTemplatesRequest;
import org.elasticsearch.client.indices.GetIndexTemplatesResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.Property;
import org.phoebus.olog.entity.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A element which creates the elastic rest clients used the olog service for
 * creating and retrieving logs and other resources
 * 
 * @author kunal
 *
 */
@Configuration
@ComponentScan(basePackages = { "org.phoebus.olog" })
@PropertySource("classpath:application.properties")
public class ElasticConfig
{

    private static final Logger logger = Logger.getLogger(ElasticConfig.class.getName());

    // Read the elatic index and type from the application.properties
    @Value("${elasticsearch.tag.index:olog_tags}")
    private String ES_TAG_INDEX;
    @Value("${elasticsearch.tag.type:olog_tag}")
    private String ES_TAG_TYPE;
    @Value("${elasticsearch.logbook.index:olog_logbooks}")
    private String ES_LOGBOOK_INDEX;
    @Value("${elasticsearch.logbook.type:olog_logbook}")
    private String ES_LOGBOOK_TYPE;
    @Value("${elasticsearch.property.index:olog_properties}")
    private String ES_PROPERTY_INDEX;
    @Value("${elasticsearch.property.type:olog_property}")
    private String ES_PROPERTY_TYPE;
    @Value("${elasticsearch.log.index:olog_logs}")
    private String ES_LOG_INDEX;
    @Value("${elasticsearch.log.type:olog_log}")
    private String ES_LOG_TYPE;
    @Value("${elasticsearch.sequence.index:olog_sequence}")
    private String ES_SEQ_INDEX;
    @Value("${elasticsearch.sequence.type:olog_sequence}")
    private String ES_SEQ_TYPE;

    @Value("${elasticsearch.cluster.name:elasticsearch}")
    private String clusterName;
    @Value("${elasticsearch.network.host:localhost}")
    private String host;
    @Value("${elasticsearch.http.port:9200}")
    private int port;

    @Value("${default.logbook.url}")
    private String defaultLogbooksURL;
    @Value("${default.tags.url}")
    private String defaultTagsURL;
    @Value("${default.properties.url}")
    private String defaultPropertiesURL;

    private RestHighLevelClient searchClient;
    private RestHighLevelClient indexClient;

    @Bean({ "searchClient" })
    public RestHighLevelClient getSearchClient()
    {
        if (searchClient == null)
        {
            searchClient = new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, "http")));
        }
        return searchClient;
    }

    @Bean({ "indexClient" })
    public RestHighLevelClient getIndexClient()
    {
        if (indexClient == null)
        {
            indexClient = new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, "http")));
            elasticIndexValidation(indexClient);
            elasticIndexInitialization(indexClient);
        }
        return indexClient;
    }

    /**
     * Checks for the existence of the elastic indices needed for Olog and creates
     * them with the appropriate mapping is they are missing.
     * 
     * @param indexClient the elastic client instance used to validate and create
     *                    olog indices
     */
    private synchronized void elasticIndexValidation(RestHighLevelClient indexClient)
    {
        // Create/migrate the tag index

        try
        {
            if (!indexClient.indices().exists(new GetIndexRequest().indices(ES_TAG_INDEX), RequestOptions.DEFAULT))
            {
                CreateIndexRequest createRequest = new CreateIndexRequest(ES_TAG_INDEX);
                ObjectMapper mapper = new ObjectMapper();
                InputStream is = ElasticConfig.class.getResourceAsStream("/tag_mapping.json");
                Map<String, String> jsonMap = mapper.readValue(is, Map.class);
                createRequest.mapping(ES_TAG_TYPE, jsonMap);

                indexClient.indices().create(createRequest, RequestOptions.DEFAULT);
                logger.info("Successfully created index: " + ES_TAG_INDEX);
            }
        } catch (IOException e)
        {
            logger.log(Level.WARNING, "Failed to create index " + ES_TAG_INDEX, e);
        }
        // Create/migrate the logbook index
        try
        {
            if (!indexClient.indices().exists(new GetIndexRequest().indices(ES_LOGBOOK_INDEX), RequestOptions.DEFAULT))
            {
                CreateIndexRequest createRequest = new CreateIndexRequest(ES_LOGBOOK_INDEX);
                ObjectMapper mapper = new ObjectMapper();
                InputStream is = ElasticConfig.class.getResourceAsStream("/logbook_mapping.json");
                Map<String, String> jsonMap = mapper.readValue(is, Map.class);
                createRequest.mapping(ES_LOGBOOK_TYPE, jsonMap);

                indexClient.indices().create(createRequest, RequestOptions.DEFAULT);
                logger.info("Successfully created index: " + ES_LOGBOOK_INDEX);
            }
        } catch (IOException e)
        {
            logger.log(Level.WARNING, "Failed to create index " + ES_LOGBOOK_INDEX, e);
        }
        // Create/migrate the property index
        try
        {
            if (!indexClient.indices().exists(new GetIndexRequest().indices(ES_PROPERTY_INDEX), RequestOptions.DEFAULT))
            {
                CreateIndexRequest createRequest = new CreateIndexRequest(ES_PROPERTY_INDEX);
                ObjectMapper mapper = new ObjectMapper();
                InputStream is = ElasticConfig.class.getResourceAsStream("/property_mapping.json");

                Map<String, String> jsonMap = mapper.readValue(is, Map.class);
                createRequest.mapping(ES_PROPERTY_TYPE, jsonMap);

                indexClient.indices().create(createRequest, RequestOptions.DEFAULT);
                logger.info("Successfully created index: " + ES_PROPERTY_INDEX);
            }
        } catch (IOException e)
        {
            logger.log(Level.WARNING, "Failed to create index " + ES_PROPERTY_INDEX, e);
        }

        // Create/migrate the sequence index
        try
        {
            if (!indexClient.indices().exists(new GetIndexRequest().indices(ES_SEQ_INDEX), RequestOptions.DEFAULT))
            {
                CreateIndexRequest createRequest = new CreateIndexRequest(ES_SEQ_INDEX);
                createRequest.settings(
                        Settings.builder().put("index.number_of_shards", 1).put("auto_expand_replicas", "0-all"));
                ObjectMapper mapper = new ObjectMapper();
                InputStream is = ElasticConfig.class.getResourceAsStream("/seq_mapping.json");
                Map<String, String> jsonMap = mapper.readValue(is, Map.class);
                createRequest.mapping(ES_SEQ_TYPE, jsonMap);
                logger.info("Successfully created index: " + ES_SEQ_TYPE);
            }
        } catch (IOException e)
        {
            logger.log(Level.WARNING, "Failed to create index " + ES_SEQ_INDEX, e);
        }

        // create/migrate log template
        try
        {
            GetIndexTemplatesResponse templates = indexClient.indices().getIndexTemplate(new GetIndexTemplatesRequest("*"), RequestOptions.DEFAULT);
            if (!templates.getIndexTemplates().stream().anyMatch(i -> {
                return i.name().equalsIgnoreCase(ES_LOG_INDEX + "_template");
            }))
            {
                PutIndexTemplateRequest templateRequest = new PutIndexTemplateRequest(ES_LOG_INDEX + "_template");

                templateRequest.patterns(Arrays.asList(ES_LOG_INDEX));

                ObjectMapper mapper = new ObjectMapper();
                InputStream is = ElasticConfig.class.getResourceAsStream("/log_template_mapping.json");

                Map<String, String> jsonMap = mapper.readValue(is, Map.class);
                templateRequest.mapping(ES_LOG_TYPE, XContentFactory.jsonBuilder().map(jsonMap));
                templateRequest.create(true);
                indexClient.indices().putTemplate(templateRequest, RequestOptions.DEFAULT);
            }

            // Get the index templates again...
            templates = indexClient.indices().getIndexTemplate(new GetIndexTemplatesRequest("*"), RequestOptions.DEFAULT);

            if (templates.getIndexTemplates().stream().anyMatch(i -> {
                return i.name().equalsIgnoreCase(ES_LOG_INDEX + "_template") && i.version() == null;
            }))
            {
                PutIndexTemplateRequest templateRequest = new PutIndexTemplateRequest(ES_LOG_INDEX + "_template");

                templateRequest.patterns(Arrays.asList(ES_LOG_INDEX));

                ObjectMapper mapper = new ObjectMapper();
                InputStream is = ElasticConfig.class.getResourceAsStream("/log_template_mapping_with_title.json");

                Map<String, String> jsonMap = mapper.readValue(is, Map.class);
                templateRequest.mapping(ES_LOG_TYPE, XContentFactory.jsonBuilder().map(jsonMap)).version(2);
                templateRequest.create(false);
                indexClient.indices().putTemplate(templateRequest, RequestOptions.DEFAULT);
            }
        } catch (IOException e)
        {
            logger.log(Level.WARNING, "Failed to create template for index " + ES_LOG_TYPE, e);
        }

    }


    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Create the default logbooks, tags, and properties
     * @param indexClient the elastic client instance used to create the default resources
     */
    private void elasticIndexInitialization(RestHighLevelClient indexClient) {
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
                    if(!indexClient.exists(new GetRequest(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, logbook.getName()),
                            RequestOptions.DEFAULT)) {
                        IndexRequest indexRequest = new IndexRequest(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, logbook.getName())
                                .source(mapper.writeValueAsBytes(logbook), XContentType.JSON)
                                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
                        IndexResponse response = indexClient.index(indexRequest, RequestOptions.DEFAULT);
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
            List<Tag> jsonTags = mapper.readValue(input, new TypeReference<List<Tag>>(){});

            jsonTags.stream().forEach(tag -> {
                try {
                    if(!indexClient.exists(new GetRequest(ES_TAG_INDEX, ES_TAG_TYPE, tag.getName()),
                            RequestOptions.DEFAULT)) {
                        IndexRequest indexRequest = new IndexRequest(ES_TAG_INDEX, ES_TAG_TYPE, tag.getName())
                                .source(mapper.writeValueAsBytes(tag), XContentType.JSON)
                                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
                        IndexResponse response = indexClient.index(indexRequest, RequestOptions.DEFAULT);
                    }
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to initialize tag : " +tag.getName(), e);
                }
            });
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to initialize tags ", ex);
        }

        // Setup the default properties
        String propertiesURL = defaultPropertiesURL;
        if (propertiesURL.isEmpty())
        {
            final URL resource = getClass().getResource("/default_properties.json");
            propertiesURL = resource.toExternalForm();
        }
        try (InputStream input = new URL(propertiesURL).openStream() ) {
            List<Property> jsonProperties = mapper.readValue(input, new TypeReference<List<Property>>(){});

            jsonProperties.stream().forEach(property -> {
                try {
                    if(!indexClient.exists(new GetRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, property.getName()),
                            RequestOptions.DEFAULT)) {
                        IndexRequest indexRequest = new IndexRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, property.getName())
                                .source(mapper.writeValueAsBytes(property), XContentType.JSON)
                                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
                        IndexResponse response = indexClient.index(indexRequest, RequestOptions.DEFAULT);
                    }
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to initialize property : " +property.getName(), e);
                }
            });
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to initialize properties ", ex);
        }
    }

}