package org.phoebus.olog;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchVersionInfo;
import co.elastic.clients.elasticsearch.core.InfoResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mongodb.client.MongoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import static org.phoebus.olog.OlogResourceDescriptors.OLOG_SERVICE_INFO;
//import com.mongodb.client.MongoClient;

@RestController
@RequestMapping(OLOG_SERVICE_INFO)
@SuppressWarnings("unused")
public class InfoResource
{

    @Value("${olog.version:1.0.0}")
    private String version;

    @Autowired
    private ElasticConfig esService;
    @Autowired
    private MongoClient mongoClient;

    @Value("${elasticsearch.network.host:localhost}")
    private String host;
    @Value("${elasticsearch.http.port:9200}")
    private int port;

    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxFileSize;

    @Value("${spring.servlet.multipart.max-request-size}")
    private String maxRequestSize;

    private final static ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * 
     * @return Information about the Olog service
     */
    @GetMapping
    public String info() {

        Map<String, Object> ologServiceInfo = new LinkedHashMap<>();
        ologServiceInfo.put("name", "Olog Service");
        ologServiceInfo.put("version", version);

        ElasticsearchClient client = esService.getClient();
        Map<String, String> elasticInfo = new LinkedHashMap<>();
        try {
            InfoResponse response = client.info();
            elasticInfo.put("status", "Connected");
            elasticInfo.put("clusterName", response.clusterName());
            elasticInfo.put("clusterUuid", response.clusterUuid());
            ElasticsearchVersionInfo version = response.version();
            elasticInfo.put("version", version.toString());
            elasticInfo.put("elasticHost", host);
            elasticInfo.put("elasticPort", String.valueOf(port));
        } catch (IOException e) {
            Application.logger.log(Level.WARNING, "Failed to create Olog service info resource.", e);
            elasticInfo.put("status", "Failed to connect to elastic " + e.getLocalizedMessage());
        }
        ologServiceInfo.put("elastic", elasticInfo);
        ologServiceInfo.put("mongoDB", mongoClient.getClusterDescription().getShortDescription());

        Map<String, Object> serverConfigInfo = new LinkedHashMap<>();
        // Provide sizes in MB, arithmetics needed to avoid rounding to 0.
        serverConfigInfo.put("Max file size", 1.0 * DataSize.parse(maxFileSize).toKilobytes() / 1024);
        serverConfigInfo.put("Max request size", 1.0 * DataSize.parse(maxRequestSize).toKilobytes() / 1024);

        ologServiceInfo.put("server config", serverConfigInfo);

        try {
            return objectMapper.writeValueAsString(ologServiceInfo);
        } catch (JsonProcessingException e) {
            Application.logger.log(Level.WARNING, "Failed to create Olog service info resource.", e);
            return "Failed to gather Olog service info";
        }
    }
}
