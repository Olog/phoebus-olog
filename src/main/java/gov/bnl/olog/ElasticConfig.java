package gov.bnl.olog;

import static gov.bnl.olog.OlogResourceDescriptors.ES_LOGBOOK_INDEX;
import static gov.bnl.olog.OlogResourceDescriptors.ES_LOGBOOK_TYPE;
import static gov.bnl.olog.OlogResourceDescriptors.ES_LOG_INDEX;
import static gov.bnl.olog.OlogResourceDescriptors.ES_LOG_TYPE;
import static gov.bnl.olog.OlogResourceDescriptors.ES_PROPERTY_INDEX;
import static gov.bnl.olog.OlogResourceDescriptors.ES_PROPERTY_TYPE;
import static gov.bnl.olog.OlogResourceDescriptors.ES_SEQ_INDEX;
import static gov.bnl.olog.OlogResourceDescriptors.ES_SEQ_TYPE;
import static gov.bnl.olog.OlogResourceDescriptors.ES_TAG_INDEX;
import static gov.bnl.olog.OlogResourceDescriptors.ES_TAG_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateAction;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequestBuilder;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@ComponentScan(basePackages = { "gov.bnl.olog" })
@PropertySource("classpath:application.properties")
public class ElasticConfig
{

    private static final Logger logger = Logger.getLogger(ElasticConfig.class.getName());

    @Value("${elasticsearch.cluster.name:elasticsearch}")
    private String clusterName;
    @Value("${elasticsearch.network.host:localhost}")
    private String host;
    @Value("${elasticsearch.http.port:9200}")
    private int port;

    private RestHighLevelClient searchClient;
    private RestHighLevelClient indexClient;

    @Bean
    public RestHighLevelClient getSearchClient()
    {
        if (searchClient == null)
        {
            searchClient = new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, "http")));
        }
        return searchClient;
    }

    @Bean
    public RestHighLevelClient getIndexClient()
    {
        if (indexClient == null)
        {
            indexClient = new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, "http")));
            elasticIndexValidation(indexClient);
        }
        return indexClient;
    }

    @SuppressWarnings("resource")
    public RestHighLevelClient getNewClient()
    {
        try
        {
            RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, "http")));
            return client;
        } catch (ElasticsearchException e)
        {
            logger.log(Level.SEVERE, "failed to create elastic client", e.getDetailedMessage());
            return null;
        }
    }
    

    private static synchronized void elasticIndexValidation(RestHighLevelClient indexClient) {
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

            }
        } catch (IOException e)
        {
            logger.log(Level.WARNING, "Failed to create index " + ES_SEQ_INDEX, e);
        }

        // create/migrate log template

        try
        {
            PutIndexTemplateRequest templateRequest = new PutIndexTemplateRequest(ES_LOG_INDEX + "_template");
            templateRequest.patterns(Arrays.asList(ES_LOG_INDEX));

            ObjectMapper mapper = new ObjectMapper();
            InputStream is = ElasticConfig.class.getResourceAsStream("/log _template_mapping.json");
            Map<String, String> jsonMap = mapper.readValue(is, Map.class);
            templateRequest.mapping(ES_LOG_TYPE, XContentFactory.jsonBuilder().map(jsonMap));
            templateRequest.create(true);

            PutIndexTemplateResponse putTemplateResponse = indexClient.indices().putTemplate(templateRequest,
                    RequestOptions.DEFAULT);
        } catch (IOException e)
        {
            logger.log(Level.WARNING, "Failed to create template for index " + ES_LOG_TYPE, e);
        }

    }

}