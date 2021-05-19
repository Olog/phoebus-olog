package org.phoebus.olog;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

@Configuration
@PropertySource("classpath:application.properties")
public class MongoConfig extends AbstractMongoClientConfiguration
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

    @Bean
    public GridFSBucket gridFSBucket(){
        return GridFSBuckets.create(mongoClient().getDatabase(mongoDbName));
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
            return MongoClients.create("mongodb://"+mongoHost+":"+mongoPort);
        } catch (Exception e)
        {
            log.log(Level.SEVERE, "Failed to create mongo gridFS client for attachments " , e);
            return null;
        }
    }
}