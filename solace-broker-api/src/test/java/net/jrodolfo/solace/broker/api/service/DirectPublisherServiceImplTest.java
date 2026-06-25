package net.jrodolfo.solace.broker.api.service;

import com.solace.messaging.MessagingService;
import com.solace.messaging.publisher.OutboundMessage;
import com.solace.messaging.publisher.OutboundMessageBuilder;
import net.jrodolfo.solace.broker.api.testsupport.TestDestinations;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
                () -> directPublisherService.sendMessage("", "Message content", DeliveryMode.PERSISTENT, 3, Optional.empty())
        );

        assertEquals(ERROR_EMPTY_MESSAGE_OR_TOPIC_NAME, exception.getMessage());
    }

    @Test
    void testSendMessageEmptyContent() {
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> directPublisherService.sendMessage(TestDestinations.SYSTEM_01, "", DeliveryMode.PERSISTENT, 3, Optional.empty())
        );

        assertEquals(ERROR_EMPTY_MESSAGE_OR_TOPIC_NAME, exception.getMessage());
    }

    @Test
    void testSendMessageMissingEnvironmentConfiguration() {
        DirectPublisherServiceImpl service = new DirectPublisherServiceImpl(new MissingConfigAccessProperties());

        BrokerConfigurationException exception = assertThrows(
                BrokerConfigurationException.class,
                () -> service.sendMessage(TestDestinations.SYSTEM_01, "Message content", DeliveryMode.PERSISTENT, 3, Optional.empty())
        );

        assertEquals("System environment variables SOLACE_CLOUD_HOST, SOLACE_CLOUD_VPN, SOLACE_CLOUD_USERNAME, SOLACE_CLOUD_PASSWORD are not set.", exception.getMessage());
    }

    @Test
    void testSendMessageRejectsPriorityAboveSolaceRange() {
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> directPublisherService.sendMessage(TestDestinations.SYSTEM_01, "Message content", DeliveryMode.PERSISTENT, 256, Optional.empty())
        );

        assertEquals("Message priority must be between 0 and 255", exception.getMessage());
    }

    @Test
    void buildOutboundMessageAppliesRequestedPriority() {
        MessagingService messagingService = mock(MessagingService.class);
        OutboundMessageBuilder messageBuilder = mock(OutboundMessageBuilder.class);
        OutboundMessage outboundMessage = mock(OutboundMessage.class);
        when(messagingService.messageBuilder()).thenReturn(messageBuilder);
        when(messageBuilder.withProperty(any(String.class), any(String.class))).thenReturn(messageBuilder);
        when(messageBuilder.withPriority(0)).thenReturn(messageBuilder);
        when(messageBuilder.build(any(byte[].class))).thenReturn(outboundMessage);

        OutboundMessage result = directPublisherService.buildOutboundMessage(
                messagingService,
                "Message content",
                DeliveryMode.PERSISTENT,
                0
        );

        assertEquals(outboundMessage, result);
        verify(messageBuilder).withPriority(0);
    }

    @Test
    void buildResponseReturnsTypedPublishResponse() {
        PublishMessageResponseDTO response = directPublisherService.buildResponse(
                TestDestinations.SYSTEM_01,
                "message with \"quotes\" and newline\ncontent"
        );

        assertEquals(TestDestinations.SYSTEM_01, response.getDestination());
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
