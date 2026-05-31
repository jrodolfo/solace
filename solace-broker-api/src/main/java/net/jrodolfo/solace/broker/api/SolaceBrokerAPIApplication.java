package net.jrodolfo.solace.broker.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * The entry point for the Solace Broker API Spring Boot application.
 * This application provides a REST API to interact with the Solace PubSub+ Broker,
 * allowing for message publishing and management.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SolaceBrokerAPIApplication {
    /**
     * Main method to start the Solace Broker API application.
     *
     * @param args command-line arguments passed to the application.
     */
    public static void main(String[] args) {
        SpringApplication.run(SolaceBrokerAPIApplication.class, args);
    }
}
