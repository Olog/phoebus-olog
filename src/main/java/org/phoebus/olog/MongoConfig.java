package org.phoebus.olog;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import javax.annotation.PostConstruct;

@Configuration
@PropertySource("classpath:application.properties")
@SuppressWarnings("unused")
public class MongoConfig {

    @Value("${spring.data.mongodb.databaseç:ologAttachments}")
    private String mongoDbName;
    @Value("${spring.data.mongodb.host:localhost}")
    private String mongoHost;
    @Value("${spring.data.mongodb.port:27017}")
    private int mongoPort;

    private MongoClient mongoClient;

    @PostConstruct
    public void setupClient() {
        mongoClient = MongoClients.create("mongodb://" + mongoHost + ":" + mongoPort);
    }

    @SuppressWarnings("unused")
    @Bean
    public GridFsTemplate gridFsTemplate() {
        MongoDatabaseFactory dbFactory = new SimpleMongoClientDatabaseFactory(mongoClient, mongoDbName);
        DefaultDbRefResolver dbRefResolver = new DefaultDbRefResolver(dbFactory);

        MongoMappingContext mappingContext = new MongoMappingContext();
        mappingContext.setAutoIndexCreation(true);
        mappingContext.afterPropertiesSet();
        return new GridFsTemplate(dbFactory, new MappingMongoConverter(dbRefResolver, mappingContext));
    }


    @SuppressWarnings("unused")
    @Bean
    public GridFSBucket gridFSBucket() {
        return GridFSBuckets.create(mongoClient.getDatabase(mongoDbName));
    }
}