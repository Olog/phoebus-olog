package gov.bnl.olog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.mongodb.MongoClient;

@Configuration
@EnableMongoRepositories(basePackages = "edu.msu.nscl.olog")
@PropertySource("classpath:mongodb.properties")
public class MongoConfig extends AbstractMongoConfiguration
{
    @Autowired
    private Environment config;

    @Bean
    public GridFsTemplate gridFsTemplate() throws Exception
    {
        return new GridFsTemplate(mongoDbFactory(), mappingMongoConverter());
    }

    @Override
    protected String getDatabaseName()
    {

        return config.getProperty("mongo.database");
    }

    @Override
    @Bean
    public MongoClient mongoClient()
    {
        return new MongoClient(config.getProperty("mongo.host"), Integer.parseInt(config.getProperty("mongo.port")));
    }
}