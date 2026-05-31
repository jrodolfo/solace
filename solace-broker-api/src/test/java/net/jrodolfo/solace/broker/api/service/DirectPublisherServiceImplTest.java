package net.jrodolfo.solace.broker.api.service;

import org.junit.jupiter.api.Test;
import net.jrodolfo.solace.broker.api.dto.ParameterDTO;
import net.jrodolfo.solace.broker.api.dto.PublishMessageResponseDTO;
import net.jrodolfo.solace.broker.api.exception.BrokerConfigurationException;
import net.jrodolfo.solace.broker.api.jpa.DeliveryMode;

import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static net.jrodolfo.solace.broker.api.constants.Constants.ERROR_EMPTY_MESSAGE_OR_TOPIC_NAME;

/**
 * Unit tests for {@link DirectPublisherServiceImpl}.
 * Verifies message publishing logic, input validation, and configuration handling
 * for the Solace broker integration.
 */
class DirectPublisherServiceImplTest {

    private final DirectPublisherServiceImpl directPublisherService = new DirectPublisherServiceImpl(new StubAccessProperties());

    @Test
    void testSendMessageInvalidTopicName() {
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> directPublisherService.sendMessage("", "Message content", DeliveryMode.PERSISTENT, Optional.empty())
        );

        assertEquals(ERROR_EMPTY_MESSAGE_OR_TOPIC_NAME, exception.getMessage());
    }

    @Test
    void testSendMessageEmptyContent() {
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> directPublisherService.sendMessage("solace/java/direct/system-01", "", DeliveryMode.PERSISTENT, Optional.empty())
        );

        assertEquals(ERROR_EMPTY_MESSAGE_OR_TOPIC_NAME, exception.getMessage());
    }

    @Test
    void testSendMessageMissingEnvironmentConfiguration() {
        DirectPublisherServiceImpl service = new DirectPublisherServiceImpl(new MissingConfigAccessProperties());

        BrokerConfigurationException exception = assertThrows(
                BrokerConfigurationException.class,
                () -> service.sendMessage("solace/java/direct/system-01", "Message content", DeliveryMode.PERSISTENT, Optional.empty())
        );

        assertEquals("System environment variables SOLACE_CLOUD_HOST, SOLACE_CLOUD_VPN, SOLACE_CLOUD_USERNAME, SOLACE_CLOUD_PASSWORD are not set.", exception.getMessage());
    }

    @Test
    void buildResponseReturnsTypedPublishResponse() {
        PublishMessageResponseDTO response = directPublisherService.buildResponse(
                "solace/java/direct/system-01",
                "message with \"quotes\" and newline\ncontent"
        );

        assertEquals("solace/java/direct/system-01", response.getDestination());
        assertEquals("message with \"quotes\" and newline\ncontent", response.getContent());
    }

    private static final class StubAccessProperties implements AccessProperties {
        @Override
        public Properties getPropertiesPublisher() {
            return new Properties();
        }

        @Override
        public Properties getPropertiesPublisher(ParameterDTO solaceParameters) {
            return new Properties();
        }

        @Override
        public Properties getPropertiesReceiver() {
            return new Properties();
        }
    }

    private static final class MissingConfigAccessProperties implements AccessProperties {
        @Override
        public Properties getPropertiesPublisher() {
            throw new BrokerConfigurationException("System environment variables SOLACE_CLOUD_HOST, SOLACE_CLOUD_VPN, SOLACE_CLOUD_USERNAME, SOLACE_CLOUD_PASSWORD are not set.");
        }

        @Override
        public Properties getPropertiesPublisher(ParameterDTO solaceParameters) {
            return new Properties();
        }

        @Override
        public Properties getPropertiesReceiver() {
            return new Properties();
        }
    }
}
