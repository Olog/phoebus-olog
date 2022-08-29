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
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

@Configuration
@PropertySource("classpath:application.properties")
@SuppressWarnings("unused")
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${mongo.database:ologAttachments}")
    private String mongoDbName;
    @Value("${mongo.host:localhost}")
    private String mongoHost;
    @Value("${mongo.port:27017}")
    private int mongoPort;

    @SuppressWarnings("unused")
    @Bean
    public GridFsTemplate gridFsTemplate() {
        MongoDatabaseFactory databaseFactory = mongoDbFactory();
        DefaultDbRefResolver dbRefResolver = new DefaultDbRefResolver(databaseFactory);

        MongoMappingContext mappingContext = new MongoMappingContext();
        mappingContext.setAutoIndexCreation(true);
        mappingContext.afterPropertiesSet();
        return new GridFsTemplate(databaseFactory, new MappingMongoConverter(dbRefResolver, mappingContext));
    }


    @SuppressWarnings("unused")
    @Bean
    public GridFSBucket gridFSBucket() {
        return GridFSBuckets.create(mongoClient().getDatabase(mongoDbName));
    }

    @Override
    public String getDatabaseName() {
        return mongoDbName;
    }

    @Override
    public MongoClient mongoClient() {
        return MongoClients.create("mongodb://" + mongoHost + ":" + mongoPort);
    }

    @Override
    public MongoDatabaseFactory mongoDbFactory() {
        return new SimpleMongoClientDatabaseFactory(mongoClient(), mongoDbName);
    }
}