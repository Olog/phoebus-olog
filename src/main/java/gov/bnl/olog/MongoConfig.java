package gov.bnl.olog;

import com.mongodb.MongoClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import java.util.logging.Level;
import java.util.logging.Logger;

@Configuration
@PropertySource("classpath:application.properties")
public class MongoConfig extends AbstractMongoConfiguration
{
    private static final Logger log = Logger.getLogger(MongoClient.class.getName());
    
    @Value("${mongo.database:ologAttachments}")
    private String mongoDbName;
    @Value("${mongo.host:localhost}")
    private String mongoHost;
    @Value("${mongo.port:27017}")
    private int mongoPort;

    @Bean
    public GridFsTemplate gridFsTemplate() throws Exception
    {
        return new GridFsTemplate(mongoDbFactory(), mappingMongoConverter());
    }

    @Override
    protected String getDatabaseName()
    {
        return mongoDbName;
    }

    @Override
    @Bean
    public MongoClient mongoClient()
    {
        try
        {
            return new MongoClient(mongoHost, Integer.valueOf(mongoPort));
        } catch (Exception e)
        {
            log.log(Level.SEVERE, "Failed to create mongo gridFS client for attachments " , e);
            return null;
        }
    }
}