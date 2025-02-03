package org.orgname.solace.subscriber;

import com.solace.messaging.config.SolaceProperties;
import com.solace.messaging.config.SolaceProperties.AuthenticationProperties;
import com.solace.messaging.config.SolaceProperties.TransportLayerProperties;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class provides the values for the parameters to connect to a Solace Broker
 *
 * See: https://docs.solace.com/Solace-PubSub-Messaging-APIs/API-Developer-Guide/Configuring-Connection-T.htm
 */
public class AccessProperties {

    private static final Logger logger = Logger.getLogger(AccessProperties.class.getName());

    private static Properties getProperties() throws Exception {

        // Retrieve environment variables
        String host = System.getenv("SOLACE_CLOUD_HOST");
        String vpnName = System.getenv("SOLACE_CLOUD_VPN");
        String userName = System.getenv("SOLACE_CLOUD_USERNAME");
        String password = System.getenv("SOLACE_CLOUD_PASSWORD");

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
        final Properties properties = new Properties();
        properties.setProperty(TransportLayerProperties.HOST, host); // host:port
        properties.setProperty(SolaceProperties.ServiceProperties.VPN_NAME, vpnName); // message-vpn
        properties.setProperty(AuthenticationProperties.SCHEME_BASIC_USER_NAME, userName); // client-username
        properties.setProperty(AuthenticationProperties.SCHEME_BASIC_PASSWORD, password); // client-password
        properties.setProperty(TransportLayerProperties.RECONNECTION_ATTEMPTS, "20");  // recommended settings
        properties.setProperty(TransportLayerProperties.CONNECTION_RETRIES_PER_HOST, "5");

        return properties;
    }

    public static Properties getPropertiesPublisher() throws Exception {
        return getProperties();
    }

    public static Properties getPropertiesReceiver() throws Exception {
        final Properties properties = getProperties();
        properties.setProperty(SolaceProperties.ServiceProperties.RECEIVER_DIRECT_SUBSCRIPTION_REAPPLY, "true");  // subscribe Direct subs after reconnect
        return properties;
    }
}