package org.orgname.solace.subscriber;

import com.solace.messaging.config.SolaceProperties;
import com.solace.messaging.config.SolaceProperties.AuthenticationProperties;
import com.solace.messaging.config.SolaceProperties.TransportLayerProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccessPropertiesTest {

    private static final Logger ACCESS_PROPERTIES_LOGGER = Logger.getLogger(AccessProperties.class.getName());

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

        SubscriberConfigurationException exception;
        try (LoggerCapture ignored = LoggerCapture.capture(ACCESS_PROPERTIES_LOGGER)) {
            exception = assertThrows(
                    SubscriberConfigurationException.class,
                    accessProperties::getPropertiesReceiver
            );
        }

        assertEquals(
                "System environment variables SOLACE_CLOUD_HOST, SOLACE_CLOUD_VPN, SOLACE_CLOUD_USERNAME, SOLACE_CLOUD_PASSWORD are not set.",
                exception.getMessage()
        );
    }

    private static java.util.function.Function<String, String> buildEnvironmentProvider(Map<String, String> environment) {
        return environment::get;
    }

    private static final class LoggerCapture extends Handler implements AutoCloseable {
        private final Logger logger;
        private final boolean originalUseParentHandlers;

        private LoggerCapture(Logger logger) {
            this.logger = logger;
            this.originalUseParentHandlers = logger.getUseParentHandlers();
        }

        static LoggerCapture capture(Logger logger) {
            LoggerCapture capture = new LoggerCapture(logger);
            logger.setUseParentHandlers(false);
            logger.addHandler(capture);
            return capture;
        }

        @Override
        public void publish(LogRecord record) {
            // Intentionally swallow expected test log output.
        }

        @Override
        public void flush() {
            // No-op.
        }

        @Override
        public void close() {
            logger.removeHandler(this);
            logger.setUseParentHandlers(originalUseParentHandlers);
        }
    }
}
