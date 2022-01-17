package io.mglobe;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;


@SpringBootApplication
@Component
@Configuration
@PropertySource("classpath:application.properties")
@ComponentScan(basePackages = "io.mglobe")
public class Application {
    public static final Logger LOG = LogManager.getLogger(Application.class);
    public static void main(String[] args) {

        SpringApplication.run(Application.class, args);
        LOG.info("===================================== STARTING ====================================");
        LOG.info("================== GOOGLE CLOUD STORAGE SERVICE ==================");
        LOG.info("===================================================================================");

    }

}