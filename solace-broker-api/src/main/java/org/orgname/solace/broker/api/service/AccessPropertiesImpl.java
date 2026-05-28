package org.orgname.solace.broker.api.service;

import com.solace.messaging.config.SolaceProperties;
import com.solace.messaging.config.SolaceProperties.AuthenticationProperties;
import com.solace.messaging.config.SolaceProperties.TransportLayerProperties;
import jakarta.validation.constraints.NotNull;
import org.orgname.solace.broker.api.dto.ParameterDTO;
import org.orgname.solace.broker.api.exception.BrokerConfigurationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of {@link AccessProperties} that provides connection parameters
 * for the Solace PubSub+ Broker.
 * <p>
 * This service supports retrieving configuration from either system environment
 * variables or from provided {@link ParameterDTO} objects. It uses the Solace
 * Java API properties for configuration.
 * </p>
 * <p>
 * See: <a href="https://docs.solace.com/API/API-Developer-Guide/Developer-Guide-Home.htm">Solace Developer Guide</a>
 * </p>
 */
@Service
public class AccessPropertiesImpl implements AccessProperties {

    private static final Logger logger = Logger.getLogger(AccessPropertiesImpl.class.getName());
    /**
     * Service to retrieve environment variables.
     */
    private final EnvironmentConfig environmentConfig;

    /**
     * Constructs a new {@code AccessPropertiesImpl} with the required environment configuration.
     *
     * @param environmentConfig the service to retrieve environment variables
     */
    @Autowired
    public AccessPropertiesImpl(EnvironmentConfig environmentConfig) {
        this.environmentConfig = environmentConfig;
    }

    /**
     * Extracts connection parameters from the provided DTO and validates them.
     *
     * @param parameterDTO the DTO containing host, vpn, username, and password
     * @return a {@link Properties} object with the extracted values
     * @throws IllegalArgumentException if any required parameter is missing or empty
     */
    private Properties getPropertiesFromMethodParameters(ParameterDTO parameterDTO) {

        String host = parameterDTO.getHost();
        String vpnName = parameterDTO.getVpnName();
        String userName = parameterDTO.getUserName();
        String password = parameterDTO.getPassword();

        // Check if they are good
        if (host == null || host.trim().isEmpty() ||
                vpnName == null || vpnName.trim().isEmpty() ||
                userName == null || userName.trim().isEmpty() ||
                password == null || password.trim().isEmpty()
        ) {
            String errorMessage = "The set of parameters (host, vpnName, userName and password) passed to this method is problematic.";
            logger.log(Level.WARNING, errorMessage);
            throw new IllegalArgumentException(errorMessage); // instead of System.exit(-1);
        }

        // Set the properties using the values from the method parameters
        return getProperties(host, vpnName, userName, password);
    }

    /**
     * Populates a {@link Properties} object with the core Solace connection settings.
     * Includes recommended reconnection settings.
     *
     * @param host     the Solace broker host and port
     * @param vpnName  the message VPN name
     * @param userName the client username
     * @param password the client password
     * @return a {@link Properties} object configured for Solace connection
     */
    @NotNull
    private Properties getProperties(String host, String vpnName, String userName, String password) {
        final Properties properties = new Properties();
        properties.setProperty(TransportLayerProperties.HOST, host); // host:port
        properties.setProperty(SolaceProperties.ServiceProperties.VPN_NAME, vpnName); // message-vpn
        properties.setProperty(AuthenticationProperties.SCHEME_BASIC_USER_NAME, userName); // client-username
        properties.setProperty(AuthenticationProperties.SCHEME_BASIC_PASSWORD, password); // client-password
        properties.setProperty(TransportLayerProperties.RECONNECTION_ATTEMPTS, "20");  // recommended settings
        properties.setProperty(TransportLayerProperties.CONNECTION_RETRIES_PER_HOST, "5");
        return properties;
    }


    /**
     * Retrieves connection parameters from system environment variables and validates them.
     *
     * @return a {@link Properties} object with values from the environment
     * @throws BrokerConfigurationException if required environment variables are missing
     */
    private Properties getPropertiesFromEnv() {

        // Retrieve environment variables
        String host = environmentConfig.getEnv("SOLACE_CLOUD_HOST");
        String vpnName = environmentConfig.getEnv("SOLACE_CLOUD_VPN");
        String userName = environmentConfig.getEnv("SOLACE_CLOUD_USERNAME");
        String password = environmentConfig.getEnv("SOLACE_CLOUD_PASSWORD");

        // Check if they are good
        if (host == null || host.trim().isEmpty() ||
                vpnName == null || vpnName.trim().isEmpty() ||
                userName == null || userName.trim().isEmpty() ||
                password == null || password.trim().isEmpty()
        ) {
            String errorMessage = "System environment variables SOLACE_CLOUD_HOST, " +
                    "SOLACE_CLOUD_VPN, SOLACE_CLOUD_USERNAME, SOLACE_CLOUD_PASSWORD are not set.";
            logger.log(Level.SEVERE, errorMessage);
            throw missingEnvironmentConfiguration(errorMessage);
        }

        // Set the properties using the values from the environment variables
        return getProperties(host, vpnName, userName, password);
    }

    @Override
    public Properties getPropertiesPublisher() {
        return getPropertiesFromEnv();
    }

    @Override
    public Properties getPropertiesPublisher(ParameterDTO parameterDTO) {
        return getPropertiesFromMethodParameters(parameterDTO);
    }

    @Override
    public Properties getPropertiesReceiver() {
        final Properties properties = getPropertiesFromEnv();
        properties.setProperty(SolaceProperties.ServiceProperties.RECEIVER_DIRECT_SUBSCRIPTION_REAPPLY, "true");  // subscribe Direct subs after reconnect
        return properties;
    }

    /**
     * Creates a {@link BrokerConfigurationException} with the specified error message.
     *
     * @param errorMessage the detail message
     * @return a new exception instance
     */
    private BrokerConfigurationException missingEnvironmentConfiguration(String errorMessage) {
        return new BrokerConfigurationException(errorMessage);
    }
}
