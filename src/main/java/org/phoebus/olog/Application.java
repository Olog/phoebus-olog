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
import org.springframework.util.FileCopyUtils;

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

    @Bean
    public AcceptHeaderResolver acceptHeaderResolver(){
        return new AcceptHeaderResolver();
    }

    @Bean
    public LogEntryValidator logEntryValidator(){
        return new LogEntryValidator();
    }
}