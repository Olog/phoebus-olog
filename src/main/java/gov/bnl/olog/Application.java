package gov.bnl.olog;

import java.util.logging.Logger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = { "gov.bnl.olog" })
public class Application {
    static final Logger logger = Logger.getLogger("Olog");

    public static void main(String[] args)
    {
        logger.info("Starting Olog Service");
        ConfigurableApplicationContext olog = SpringApplication.run(Application.class, args);
        logger.info(olog.getApplicationName() + " : " + olog.getId());
    }
}