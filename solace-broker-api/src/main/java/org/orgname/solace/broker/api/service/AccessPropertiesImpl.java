package org.orgname.solace.broker.api.service;

import org.orgname.solace.broker.api.dto.ParameterDTO;
import com.solace.messaging.config.SolaceProperties;
import com.solace.messaging.config.SolaceProperties.AuthenticationProperties;
import com.solace.messaging.config.SolaceProperties.TransportLayerProperties;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class provides the values for the parameters to connect to a Solace Broker
 *
 * See: https://docs.solace.com/Solace-PubSub-Messaging-APIs/API-Developer-Guide/Configuring-Connection-T.htm
 */
@Component
public class AccessPropertiesImpl implements AccessProperties {

    private static final Logger logger = Logger.getLogger(AccessPropertiesImpl.class.getName());

    static public EnvironmentConfig environmentConfig;

    @Autowired
    public AccessPropertiesImpl(EnvironmentConfigImpl environmentConfigImpl) {
        this.environmentConfig = environmentConfigImpl;
    }

    private static Properties getPropertiesFromMethodParameters(ParameterDTO parameterDTO) throws Exception {

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
            logger.log(Level.SEVERE, errorMessage);
            throw new IllegalArgumentException(errorMessage); // instead of System.exit(-1);
        }

        // Set the properties using the values from the method parameters
        return getProperties(host, vpnName, userName, password);
    }

    @NotNull
    private static Properties getProperties(String host, String vpnName, String userName, String password) {
        final Properties properties = new Properties();
        properties.setProperty(TransportLayerProperties.HOST, host); // host:port
        properties.setProperty(SolaceProperties.ServiceProperties.VPN_NAME, vpnName); // message-vpn
        properties.setProperty(AuthenticationProperties.SCHEME_BASIC_USER_NAME, userName); // client-username
        properties.setProperty(AuthenticationProperties.SCHEME_BASIC_PASSWORD, password); // client-password
        properties.setProperty(TransportLayerProperties.RECONNECTION_ATTEMPTS, "20");  // recommended settings
        properties.setProperty(TransportLayerProperties.CONNECTION_RETRIES_PER_HOST, "5");
        return properties;
    }


    private static Properties getPropertiesFromEnv() throws Exception {

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
            throw new Exception(errorMessage); // instead of System.exit(-1);
        }

        // Set the properties using the values from the environment variables
        return getProperties(host, vpnName, userName, password);
    }

    public static Properties getPropertiesPublisher() throws Exception {
        return getPropertiesFromEnv();
    }

    public static Properties getPropertiesPublisher(ParameterDTO parameterDTO) throws Exception {
        return getPropertiesFromMethodParameters(parameterDTO);
    }

    public static Properties getPropertiesReceiver() throws Exception {
        final Properties properties = getPropertiesFromEnv();
        properties.setProperty(SolaceProperties.ServiceProperties.RECEIVER_DIRECT_SUBSCRIPTION_REAPPLY, "true");  // subscribe Direct subs after reconnect
        return properties;
    }
}
