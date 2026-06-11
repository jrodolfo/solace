package net.jrodolfo.solace.subscriber;

import com.solace.messaging.config.SolaceProperties;
import com.solace.messaging.config.SolaceProperties.AuthenticationProperties;
import com.solace.messaging.config.SolaceProperties.TransportLayerProperties;

import java.util.Properties;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves the subscriber's Solace connection properties from environment
 * variables.
 *
 * <p>The subscriber deliberately uses server-side configuration only. Missing
 * required variables are treated as startup errors rather than deferred
 * connection failures.
 *
 * <p>See: https://docs.solace.com/Solace-PubSub-Messaging-APIs/API-Developer-Guide/Configuring-Connection-T.htm
 */
public class AccessProperties {

    private static final Logger logger = Logger.getLogger(AccessProperties.class.getName());
    private final Function<String, String> environmentProvider;

    /**
     * Default constructor for creating an instance of {@link AccessProperties}
     * that uses {@link System#getenv(String)} as the environment variable provider.
     */
    public AccessProperties() {
        this(System::getenv);
    }

    /**
     * Constructor for creating an instance of {@link AccessProperties} with a
     * custom environment variable provider.
     *
     * @param environmentProvider the function used to resolve environment variables
     */
    AccessProperties(Function<String, String> environmentProvider) {
        this.environmentProvider = environmentProvider;
    }

    /**
     * Builds the base Solace connection properties shared by publisher/receiver
     * use cases in this module.
     */
    private Properties getProperties() {

        String host = environmentProvider.apply("SOLACE_CLOUD_HOST");
        String vpnName = environmentProvider.apply("SOLACE_CLOUD_VPN");
        String userName = environmentProvider.apply("SOLACE_CLOUD_USERNAME");
        String password = environmentProvider.apply("SOLACE_CLOUD_PASSWORD");

        if (host == null || host.trim().isEmpty() ||
                vpnName == null || vpnName.trim().isEmpty() ||
                userName == null || userName.trim().isEmpty() ||
                password == null || password.trim().isEmpty()
        ) {
            String errorMessage = "System environment variables SOLACE_CLOUD_HOST, " +
                    "SOLACE_CLOUD_VPN, SOLACE_CLOUD_USERNAME, SOLACE_CLOUD_PASSWORD are not set.";
            logger.log(Level.SEVERE, errorMessage);
            throw new SubscriberConfigurationException(errorMessage);
        }

        final Properties properties = new Properties();
        properties.setProperty(TransportLayerProperties.HOST, host);
        properties.setProperty(SolaceProperties.ServiceProperties.VPN_NAME, vpnName);
        properties.setProperty(AuthenticationProperties.SCHEME_BASIC_USER_NAME, userName);
        properties.setProperty(AuthenticationProperties.SCHEME_BASIC_PASSWORD, password);
        properties.setProperty(TransportLayerProperties.RECONNECTION_ATTEMPTS, "20");
        properties.setProperty(TransportLayerProperties.CONNECTION_RETRIES_PER_HOST, "5");

        return properties;
    }

    /**
     * Returns publisher-style properties. This exists for naming parity with
     * the broker API access-properties service.
     *
     * @return a {@link Properties} object containing Solace connection properties
     * @throws SubscriberConfigurationException if required environment variables are missing
     */
    public Properties getPropertiesPublisher() {
        return getProperties();
    }

    /**
     * Returns receiver properties, including direct-subscription reapply after
     * reconnect so the subscriber resumes topic observation automatically.
     *
     * <p>The {@code SolaceProperties.ServiceProperties.RECEIVER_DIRECT_SUBSCRIPTION_REAPPLY}
     * property is set to {@code true} to ensure that if the connection to the Solace
     * broker is lost and then re-established, the subscriber will automatically
     * re-subscribe to its topics.
     *
     * @return a {@link Properties} object containing Solace connection properties
     *         configured for a receiver
     * @throws SubscriberConfigurationException if required environment variables are missing
     */
    public Properties getPropertiesReceiver() {
        final Properties properties = getProperties();
        properties.setProperty(SolaceProperties.ServiceProperties.RECEIVER_DIRECT_SUBSCRIPTION_REAPPLY, "true");
        return properties;
    }
}
