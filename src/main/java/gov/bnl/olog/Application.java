package gov.bnl.olog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@ComponentScan(basePackages = { "gov.bnl.olog" })
public class Application {
    static final Logger logger = Logger.getLogger("Olog");

    /**
     * Specifies the allowed origins for CORS requests. Defaults to http://localhost:3000,
     * which is useful during development of the web front-end in NodeJS.
     */
    //@Value("${cors.allowed.origins:http://localhost:3000}")
    @Value("#{'${cors.allowed.origins:http://localhost:3000}'.split(',')}")
    private String[] corsAllowedOrigins;

    public static void main(String[] args)
    {
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
                File tempFile= File.createTempFile("olog-", "-truststore");
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
     * needed. Without a suitable configuration a client deployed to a different server than the olog-es service will not
     * be able to request resources.
     * Note: configuring this in {@link WebSecurityConfig#configure(HttpSecurity)}, it will have no effect. Not sure why,
     * but probably related to the order in which Spring Security loads stuff.
     * @return
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins(corsAllowedOrigins);
            }
        };
    }
}