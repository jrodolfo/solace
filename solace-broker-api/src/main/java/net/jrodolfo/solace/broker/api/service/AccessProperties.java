package net.jrodolfo.solace.broker.api.service;

import net.jrodolfo.solace.broker.api.dto.ParameterDTO;

import java.util.Properties;


/**
 * Provides configuration properties for connecting to a Solace PubSub+ Broker.
 * This interface defines methods to retrieve connection settings for different
 * roles (publisher, receiver) using either environment variables or explicit parameters.
 */
public interface AccessProperties {

    /**
     * Retrieves connection properties for a publisher using system environment variables.
     * Expected variables include {@code SOLACE_CLOUD_HOST}, {@code SOLACE_CLOUD_VPN},
     * {@code SOLACE_CLOUD_USERNAME}, and {@code SOLACE_CLOUD_PASSWORD}.
     *
     * @return a {@link Properties} object containing Solace connection settings
     * @throws net.jrodolfo.solace.broker.api.exception.BrokerConfigurationException if required environment variables are missing
     */
    Properties getPropertiesPublisher();

    /**
     * Retrieves connection properties for a publisher using explicit parameters.
     *
     * @param solaceParameters a {@link ParameterDTO} containing host, vpn, username, and password
     * @return a {@link Properties} object containing Solace connection settings
     * @throws IllegalArgumentException if any required parameter in {@code solaceParameters} is missing or empty
     */
    Properties getPropertiesPublisher(ParameterDTO solaceParameters);

    /**
     * Retrieves connection properties for a receiver using system environment variables.
     * In addition to standard connection properties, it enables automatic re-application
     * of direct subscriptions upon reconnection.
     *
     * @return a {@link Properties} object containing Solace connection settings for a receiver
     * @throws net.jrodolfo.solace.broker.api.exception.BrokerConfigurationException if required environment variables are missing
     */
    Properties getPropertiesReceiver();
}
