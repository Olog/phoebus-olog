package gov.bnl.olog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.util.FileCopyUtils;

@SpringBootApplication
@ComponentScan(basePackages = { "gov.bnl.olog" })
public class Application {
    static final Logger logger = Logger.getLogger("Olog");

    public static void main(String[] args)
    {
        logger.info("Starting Olog Service");
        configureTruststore();
        ConfigurableApplicationContext olog = SpringApplication.run(Application.class, args);
        logger.info(olog.getApplicationName() + " : " + olog.getId());
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
}