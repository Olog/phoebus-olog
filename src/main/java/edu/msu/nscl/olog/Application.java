package edu.msu.nscl.olog;

import java.util.logging.Logger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@SpringBootApplication
@SuppressWarnings("unused")
@EnableElasticsearchRepositories(basePackages = "edu.msu.nscl.olog")
@ComponentScan(basePackages = { "edu.msu.nscl.olog" })
public class Application {
    private static final Logger logger = Logger.getLogger("Olog");

    public static void main(String[] args) {
        ConfigurableApplicationContext olog = SpringApplication.run(Application.class, args);
    }
}