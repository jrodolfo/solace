package org.orgname.solace.subscriber;

import com.solace.messaging.config.SolaceProperties;
import com.solace.messaging.config.SolaceProperties.AuthenticationProperties;
import com.solace.messaging.config.SolaceProperties.TransportLayerProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccessPropertiesTest {

    @Test
    void getPropertiesReceiverAddsReceiverSpecificSetting() {
        AccessProperties accessProperties = new AccessProperties(buildEnvironmentProvider(Map.of(
                "SOLACE_CLOUD_HOST", "tcps://localhost:55443",
                "SOLACE_CLOUD_VPN", "default",
                "SOLACE_CLOUD_USERNAME", "subscriber-user",
                "SOLACE_CLOUD_PASSWORD", "subscriber-password"
        )));

        Properties properties = accessProperties.getPropertiesReceiver();

        assertEquals("tcps://localhost:55443", properties.getProperty(TransportLayerProperties.HOST));
        assertEquals("default", properties.getProperty(SolaceProperties.ServiceProperties.VPN_NAME));
        assertEquals("subscriber-user", properties.getProperty(AuthenticationProperties.SCHEME_BASIC_USER_NAME));
        assertEquals("subscriber-password", properties.getProperty(AuthenticationProperties.SCHEME_BASIC_PASSWORD));
        assertEquals("true", properties.getProperty(SolaceProperties.ServiceProperties.RECEIVER_DIRECT_SUBSCRIPTION_REAPPLY));
    }

    @Test
    void getPropertiesReceiverThrowsWhenEnvironmentIsIncomplete() {
        AccessProperties accessProperties = new AccessProperties(buildEnvironmentProvider(Map.of(
                "SOLACE_CLOUD_HOST", "tcps://localhost:55443",
                "SOLACE_CLOUD_VPN", "default"
        )));

        SubscriberConfigurationException exception = assertThrows(
                SubscriberConfigurationException.class,
                accessProperties::getPropertiesReceiver
        );

        assertEquals(
                "System environment variables SOLACE_CLOUD_HOST, SOLACE_CLOUD_VPN, SOLACE_CLOUD_USERNAME, SOLACE_CLOUD_PASSWORD are not set.",
                exception.getMessage()
        );
    }

    private static java.util.function.Function<String, String> buildEnvironmentProvider(Map<String, String> environment) {
        return environment::get;
    }
}
