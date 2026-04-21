package org.orgname.solace.broker.api.service;

import com.solace.messaging.MessagingService;
import com.solace.messaging.config.SolaceProperties.MessageProperties;
import com.solace.messaging.config.profile.ConfigurationProfile;
import com.solace.messaging.publisher.DirectMessagePublisher;
import com.solace.messaging.publisher.OutboundMessage;
import com.solace.messaging.publisher.OutboundMessageBuilder;
import com.solace.messaging.resources.Topic;
import org.orgname.solace.broker.api.dto.ParameterDTO;
import org.orgname.solace.broker.api.dto.PublishMessageResponseDTO;
import org.orgname.solace.broker.api.exception.BrokerConfigurationException;
import org.orgname.solace.broker.api.exception.BrokerConnectionException;
import org.orgname.solace.broker.api.exception.BrokerPublishFailureException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.orgname.solace.broker.api.constants.Constants.ERROR_EMPTY_MESSAGE_OR_TOPIC_NAME;

/**
 * Solace direct-publish implementation used by the broker API write path.
 *
 * <p>This class isolates broker-specific concerns from the controller:
 * property resolution, messaging-service lifecycle, publisher startup,
 * message construction, and cleanup. It does not manage persistence or
 * publish-status transitions; those remain in the controller/database layer.
 */
@Service
public class DirectPublisherServiceImpl implements DirectPublisherService {

    private static final Logger logger = Logger.getLogger(DirectPublisherServiceImpl.class.getName());
    private static final int APPROX_MSG_RATE_PER_SEC = 100;
    private final AccessProperties accessProperties;

    public DirectPublisherServiceImpl(AccessProperties accessProperties) {
        this.accessProperties = accessProperties;
    }


    @Override
    /**
     * Publishes one message to Solace and returns the typed HTTP success body.
     *
     * <p>If explicit broker parameters are supplied they are used only for this
     * publish attempt. Otherwise server-side configuration is used.
     */
    public PublishMessageResponseDTO sendMessage(String topicName, String content, Optional<ParameterDTO> solaceParametersOptional) {
        validateInput(topicName, content);
        Properties properties = resolveProperties(solaceParametersOptional);
        MessagingService messagingService = createMessagingService(properties);
        DirectMessagePublisher publisher = null;

        try {
            connectMessagingService(messagingService);
            publisher = createAndStartPublisher(messagingService);
            OutboundMessage message = buildOutboundMessage(messagingService, content);
            publishMessage(publisher, topicName, message);
            pauseAfterPublish();
            PublishMessageResponseDTO returnMessage = buildResponse(topicName, content);
            logger.log(Level.INFO, "Published message to topic {0}", topicName);
            return returnMessage;
        } finally {
            cleanup(messagingService, publisher);
        }
    }

    /**
     * Rejects empty topic or payload values before any broker work begins.
     */
    private void validateInput(String topicName, String content) {
        if (topicName == null || topicName.isEmpty() || content == null || content.isEmpty()) {
            throw new IllegalArgumentException(ERROR_EMPTY_MESSAGE_OR_TOPIC_NAME);
        }
    }

    /**
     * Resolves the broker connection properties for this publish attempt.
     *
     * <p>Per-request parameters take precedence when provided; otherwise the
     * service falls back to server-side broker configuration.
     */
    private Properties resolveProperties(Optional<ParameterDTO> solaceParametersOptional) {
        try {
            ParameterDTO parameterDTO = solaceParametersOptional.orElse(null);
            if (parameterDTO == null) {
                return accessProperties.getPropertiesPublisher();
            }
            return accessProperties.getPropertiesPublisher(parameterDTO);
        } catch (IllegalArgumentException | BrokerConfigurationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new BrokerConfigurationException("Failed to prepare Solace connection properties", e);
        }
    }

    /**
     * Builds an unconnected Solace messaging service from already validated
     * connection properties.
     */
    private MessagingService createMessagingService(Properties properties) {
        try {
            return MessagingService.builder(ConfigurationProfile.V1)
                    .fromProperties(properties)
                    .build();
        } catch (RuntimeException e) {
            throw new BrokerConfigurationException("Failed to build Solace messaging service", e);
        }
    }

    /**
     * Connects the messaging service and registers lifecycle listeners used for
     * operational logging.
     */
    private void connectMessagingService(MessagingService messagingService) {
        try {
            messagingService.connect();
        } catch (RuntimeException e) {
            throw new BrokerConnectionException("Failed to connect to Solace broker", e);
        }

        messagingService.addServiceInterruptionListener(serviceEvent ->
                logger.log(Level.SEVERE, "Solace service interruption: {0}", serviceEvent.getCause()));
        messagingService.addReconnectionAttemptListener(serviceEvent ->
                logger.log(Level.WARNING, "Attempting Solace reconnection: {0}", serviceEvent));
        messagingService.addReconnectionListener(serviceEvent ->
                logger.log(Level.INFO, "Reconnected to Solace broker: {0}", serviceEvent));
    }

    /**
     * Creates and starts the direct publisher associated with the connected
     * messaging service.
     */
    private DirectMessagePublisher createAndStartPublisher(MessagingService messagingService) {
        try {
            DirectMessagePublisher publisher = messagingService.createDirectMessagePublisherBuilder()
                    .onBackPressureWait(1)
                    .build();
            publisher.setPublishFailureListener(e ->
                    logger.log(Level.SEVERE, "Solace publish failure reported by broker: {0}", e.getMessage()));
            publisher.start();
            return publisher;
        } catch (RuntimeException e) {
            throw new BrokerConnectionException("Failed to start Solace direct publisher", e);
        }
    }

    /**
     * Creates the outbound payload to publish. The broker API currently treats
     * the request payload content as UTF-8 text.
     */
    private OutboundMessage buildOutboundMessage(MessagingService messagingService, String content) {
        byte[] payload = content.getBytes(StandardCharsets.UTF_8);
        OutboundMessageBuilder messageBuilder = messagingService.messageBuilder();
        messageBuilder.withProperty(MessageProperties.APPLICATION_MESSAGE_ID, UUID.randomUUID().toString());
        return messageBuilder.build(payload);
    }

    /**
     * Sends the outbound message to the target Solace topic.
     */
    private void publishMessage(DirectMessagePublisher publisher, String topicName, OutboundMessage message) {
        try {
            publisher.publish(message, Topic.of(topicName));
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Failed to publish message to topic {0}: {1}", new Object[]{topicName, e.getMessage()});
            throw new BrokerPublishFailureException("Failed to publish message to Solace broker", e);
        }
    }

    /**
     * Adds a short pacing pause after publish to keep the sample publisher flow
     * predictable under local runtime conditions.
     */
    private void pauseAfterPublish() {
        try {
            Thread.sleep(1000 / APPROX_MSG_RATE_PER_SEC);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Interrupted while pacing Solace publish rate");
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Attempts to release Solace publisher and service resources even when
     * publish or startup steps fail.
     */
    private void cleanup(MessagingService messagingService, DirectMessagePublisher publisher) {
        if (publisher != null) {
            try {
                publisher.terminate(500);
            } catch (RuntimeException e) {
                logger.log(Level.WARNING, "Failed to terminate Solace publisher cleanly: {0}", e.getMessage());
            }
        }
        try {
            messagingService.disconnect();
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Failed to disconnect Solace messaging service cleanly: {0}", e.getMessage());
        }
    }

    /**
     * Creates the typed HTTP success body returned by the controller.
     */
    PublishMessageResponseDTO buildResponse(String topicName, String content) {
        return new PublishMessageResponseDTO(topicName, content);
    }
}
