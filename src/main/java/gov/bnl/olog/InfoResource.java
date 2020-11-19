package gov.bnl.olog;

import static gov.bnl.olog.OlogResourceDescriptors.OLOG_SERVICE_INFO;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.elasticsearch.Version;
import org.elasticsearch.action.main.MainResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mongodb.client.MongoClient;

@RestController
@RequestMapping(OLOG_SERVICE_INFO)
public class InfoResource
{

    @Value("${olog.version:1.0.0}")
    private String version;

    @Autowired
    private ElasticConfig esService;

    @Autowired
    private MongoConfig monoConfig;

    private MongoClient mongoClient;

    private final static ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * 
     * @return Information about the Olog service
     */
    @GetMapping
    public String info() {

        Map<String, Object> cfServiceInfo = new LinkedHashMap<String, Object>();
        cfServiceInfo.put("name", "Olog Service");
        cfServiceInfo.put("version", version);

        RestHighLevelClient client = esService.getSearchClient();
        Map<String, String> elasticInfo = new LinkedHashMap<String, String>();
        try {
            MainResponse response = client.info(RequestOptions.DEFAULT);
            
            elasticInfo.put("status", "Connected");
            elasticInfo.put("clusterName", response.getClusterName().value());
            elasticInfo.put("clusterUuid", response.getClusterUuid());
            Version version = response.getVersion();
            elasticInfo.put("version", version.toString());
        } catch (IOException e) {
            Application.logger.log(Level.WARNING, "Failed to create Olog service info resource.", e);
            elasticInfo.put("status", "Failed to connect to elastic " + e.getLocalizedMessage());
        }
        cfServiceInfo.put("elastic", elasticInfo);

        Map<String, String> mongoInfo = new LinkedHashMap<String, String>();
        mongoClient = monoConfig.mongoClient();
        if (mongoClient != null) {
            mongoInfo.put("status", "Connected");
            mongoInfo.put("mongo", monoConfig.mongoClient().getClusterDescription().getShortDescription());
        } else {
            mongoInfo.put("status", "Discconnected");
        }

        cfServiceInfo.put("mongo-gridfs", mongoInfo);
        try {
            return objectMapper.writeValueAsString(cfServiceInfo);
        } catch (JsonProcessingException e) {
            Application.logger.log(Level.WARNING, "Failed to create Olog service info resource.", e);
            return "Failed to gather Olog service info";
        }
    }

}
