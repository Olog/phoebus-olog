package org.phoebus.olog;

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
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${mongo.database:ologAttachments}")
    @SuppressWarnings("unused")
    private String mongoDbName;
    @Value("${mongo.host:localhost}")
    @SuppressWarnings("unused")
    private String mongoHost;
    @Value("${mongo.port:27017}")
    @SuppressWarnings("unused")
    private int mongoPort;


    @SuppressWarnings("unused")
    @Bean
    public GridFsTemplate gridFsTemplate() {
        MongoDatabaseFactory dbFactory = new SimpleMongoClientDatabaseFactory("mongodb://" + mongoHost + ":" + mongoPort + "/" + mongoDbName);
        DefaultDbRefResolver dbRefResolver = new DefaultDbRefResolver(dbFactory);

        MongoMappingContext mappingContext = new MongoMappingContext();
        mappingContext.setAutoIndexCreation(true);
        mappingContext.afterPropertiesSet();
        return new GridFsTemplate(mongoDbFactory(), new MappingMongoConverter(dbRefResolver, mappingContext));
    }

    @SuppressWarnings("unused")
    @Bean
    public GridFSBucket gridFSBucket() {
        return GridFSBuckets.create(mongoClient().getDatabase(mongoDbName));
    }


    @Override
    protected String getDatabaseName() {
        return mongoDbName;
    }
}