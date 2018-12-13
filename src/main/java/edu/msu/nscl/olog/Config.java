package edu.msu.nscl.olog;

import static edu.msu.nscl.olog.OlogResourceDescriptors.ES_LOGBOOK_INDEX;
import static edu.msu.nscl.olog.OlogResourceDescriptors.ES_LOGBOOK_TYPE;
import static edu.msu.nscl.olog.OlogResourceDescriptors.ES_TAG_INDEX;
import static edu.msu.nscl.olog.OlogResourceDescriptors.ES_TAG_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableElasticsearchRepositories(basePackages = "edu.msu.nscl.olog")
@ComponentScan(basePackages = { "edu.msu.nscl.olog" })
public class Config {

    private static final Logger logger = Logger.getLogger(Config.class.getName());

    @Value("${elasticsearch.cluster.name:elasticsearch}")
    private String clusterName;

    @Bean
    public Client client() {
        Settings elasticsearchSettings = Settings.builder().put("client.transport.sniff", true)
                .put("cluster.name", clusterName).build();
        TransportClient client = new PreBuiltTransportClient(elasticsearchSettings);
        try {
            client.addTransportAddress(
                    new TransportAddress(new InetSocketAddress(InetAddress.getByName("130.199.219.217"), 9300)));
            client.connectedNodes().forEach(System.out::println);
            // Validate the indexes

            ElasticIndexValidation(client);
        } catch (UnknownHostException e) {
            logger.log(Level.WARNING, "Failed to create client", e);
        }
        return client;
    }

    private void ElasticIndexValidation(TransportClient client) {
        // Create/migrate the tag index
        if (!client.admin().indices().prepareExists(ES_TAG_INDEX).get("5s").isExists()) {
            client.admin().indices().prepareCreate(ES_TAG_INDEX).get();
            PutMappingRequestBuilder request = client.admin().indices().preparePutMapping(ES_TAG_INDEX);
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = Config.class.getResourceAsStream("/tag_mapping.json");
            try {
                Map<String, String> jsonMap = mapper.readValue(is, Map.class);
                request.setType(ES_TAG_TYPE).setSource(jsonMap).get("5s");
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to create index " + ES_TAG_INDEX, e);
            }
        }
        // Create/migrate the logbook index
        if (!client.admin().indices().prepareExists(ES_LOGBOOK_INDEX).get("5s").isExists()) {
            client.admin().indices().prepareCreate(ES_LOGBOOK_INDEX).get();
            PutMappingRequestBuilder request = client.admin().indices().preparePutMapping(ES_LOGBOOK_INDEX);
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = Config.class.getResourceAsStream("/logbook_mapping.json");
            try {
                Map<String, String> jsonMap = mapper.readValue(is, Map.class);
                request.setType(ES_LOGBOOK_TYPE).setSource(jsonMap).get("5s");
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to create index " + ES_LOGBOOK_INDEX, e);
            }
        }
    }

    @Bean
    public ElasticsearchOperations elasticsearchTemplate() {
        return new ElasticsearchTemplate(client());
    }
}