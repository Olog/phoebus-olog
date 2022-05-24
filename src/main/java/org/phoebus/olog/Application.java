package org.phoebus.olog;

import org.phoebus.olog.notification.LogEntryNotifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.PrincipalMethodArgumentResolver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

@SpringBootApplication
@ComponentScan(basePackages = {"org.phoebus.olog"})
public class Application {
    static final Logger logger = Logger.getLogger("Olog");

    /**
     * Specifies the allowed origins for CORS requests. Defaults to http://localhost:3000,
     * which is useful during development of the web front-end in NodeJS.
     */
    @Value("#{'${cors.allowed.origins:http://localhost:3000}'.split(',')}")
    private String[] corsAllowedOrigins;

    @Value("${defaultMarkup:commonmark}")
    private String defaultMarkup;

    @Value("${propertyProvidersTimeout:2000}")
    private long propertyProvidersTimeout;

    public static void main(String[] args) {
        logger.info("Starting Olog Service");
        configureTruststore();
        ConfigurableApplicationContext olog = SpringApplication.run(Application.class, args);
    }

    /**
     * Set the default ssl trust store
     */
    private static void configureTruststore() {
        if (System.getProperty("javax.net.ssl.trustStore") == null) {
            logger.log(Level.INFO, "using default javax.net.ssl.trustStore");
            try (InputStream in = Application.class.getResourceAsStream("/keystore/cacerts")) {
                // read input
                File tempFile = File.createTempFile("olog-", "-truststore");
                FileOutputStream out = new FileOutputStream(tempFile);
                FileCopyUtils.copy(in, out);
                tempFile.deleteOnExit();
                System.setProperty("javax.net.ssl.trustStore", tempFile.getAbsolutePath());
            } catch (IOException e) {
                logger.log(Level.SEVERE, "failed to configure olog truststore", e);
            }
        }
        if (System.getProperty("javax.net.ssl.trustStorePassword") == null) {
            logger.log(Level.INFO, "using default javax.net.ssl.trustStorePassword");
            System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
        }
    }

    /**
     * Configures CORS policy in order to allow clients (web front-end) to do CORS requests if
     * needed. Without a suitable configuration a client deployed to a different server than the Phoebus Olog service will not
     * be able to request resources.
     * Note: configuring this in {@link WebSecurityConfig#configure(HttpSecurity)}, it will have no effect. Not sure why,
     * but probably related to the order in which Spring Security loads stuff.
     *
     * @return A {@link WebMvcConfigurer}
     */
    /*
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowCredentials(true)
                        .allowedMethods("HEAD", "GET", "POST", "PUT", "DELETE", "PATCH")
                        .allowedOrigins(corsAllowedOrigins);
            }

            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers){
                resolvers.add(new PrincipalMethodArgumentResolver());
            }
        };
    }

     */

    /**
     * List of {@link LogEntryNotifier} implementations called when a new log entry
     * has been created.
     *
     * @return A list of {@link LogEntryNotifier}s, if any have been registered over SPI.
     */
    @Bean
    public List<LogEntryNotifier> logEntryNotifiers() {
        List<LogEntryNotifier> notifiers = new ArrayList<>();
        ServiceLoader<LogEntryNotifier> loader = ServiceLoader.load(LogEntryNotifier.class);
        loader.stream().forEach(p -> {
            LogEntryNotifier notifier = p.get();
            notifiers.add(notifier);
        });
        return notifiers;
    }

    /**
     * {@link TaskExecutor} used when calling {@link LogEntryNotifier}s.
     *
     * @return A {@link TaskExecutor}
     */
    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(3);
        taskExecutor.setMaxPoolSize(10);
        taskExecutor.setQueueCapacity(25);

        return taskExecutor;
    }

    @Bean
    public String defaultMarkup() {
        return defaultMarkup;
    }

    @Bean
    public ExecutorService executorService() {
        return Executors.newCachedThreadPool();
    }

    @Bean
    public Long propertyProvidersTimeout(){
        return propertyProvidersTimeout;
    }
}