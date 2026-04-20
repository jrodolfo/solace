package org.orgname.solace.broker.api.service;

import com.solace.messaging.MessagingService;
import com.solace.messaging.config.SolaceProperties.MessageProperties;
import com.solace.messaging.config.profile.ConfigurationProfile;
import com.solace.messaging.publisher.DirectMessagePublisher;
import com.solace.messaging.publisher.OutboundMessage;
import com.solace.messaging.publisher.OutboundMessageBuilder;
import com.solace.messaging.resources.Topic;
import org.orgname.solace.broker.api.dto.ParameterDTO;
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
 * Code that publishes to a topic
 * Disclaimer: most of the code below comes from the Solace training material
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
    public String sendMessage(String topicName, String content, Optional<ParameterDTO> solaceParametersOptional) {
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
            String returnMessage = buildResponse(topicName, content);
            logger.log(Level.INFO, "Published message to topic {0}", topicName);
            logger.log(Level.INFO, "Publisher response {0}", returnMessage);
            return returnMessage;
        } finally {
            cleanup(messagingService, publisher);
        }
    }

    private void validateInput(String topicName, String content) {
        if (topicName == null || topicName.isEmpty() || content == null || content.isEmpty()) {
            throw new IllegalArgumentException(ERROR_EMPTY_MESSAGE_OR_TOPIC_NAME);
        }
    }

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

    private MessagingService createMessagingService(Properties properties) {
        try {
            return MessagingService.builder(ConfigurationProfile.V1)
                    .fromProperties(properties)
                    .build();
        } catch (RuntimeException e) {
            throw new BrokerConfigurationException("Failed to build Solace messaging service", e);
        }
    }

    private void connectMessagingService(MessagingService messagingService) {
        try {
            messagingService.connect();
        } catch (RuntimeException e) {
            throw new BrokerConnectionException("Failed to connect to Solace broker", e);
        }

        messagingService.addServiceInterruptionListener(serviceEvent ->
                logger.log(Level.SEVERE, "### SERVICE INTERRUPTION: {0}", serviceEvent.getCause()));
        messagingService.addReconnectionAttemptListener(serviceEvent ->
                logger.log(Level.SEVERE, "### RECONNECTING ATTEMPT: {0}", serviceEvent));
        messagingService.addReconnectionListener(serviceEvent ->
                logger.log(Level.SEVERE, "### RECONNECTED: {0}", serviceEvent));
    }

    private DirectMessagePublisher createAndStartPublisher(MessagingService messagingService) {
        try {
            DirectMessagePublisher publisher = messagingService.createDirectMessagePublisherBuilder()
                    .onBackPressureWait(1)
                    .build();
            publisher.setPublishFailureListener(e ->
                    logger.log(Level.SEVERE, "### FAILED PUBLISH: {0}", e.getMessage()));
            publisher.start();
            return publisher;
        } catch (RuntimeException e) {
            throw new BrokerConnectionException("Failed to start Solace direct publisher", e);
        }
    }

    private OutboundMessage buildOutboundMessage(MessagingService messagingService, String content) {
        byte[] payload = content.getBytes(StandardCharsets.UTF_8);
        OutboundMessageBuilder messageBuilder = messagingService.messageBuilder();
        messageBuilder.withProperty(MessageProperties.APPLICATION_MESSAGE_ID, UUID.randomUUID().toString());
        OutboundMessage message = messageBuilder.build(payload);
        logger.log(Level.INFO, "OutboundMessage: {0}", message);
        return message;
    }

    private void publishMessage(DirectMessagePublisher publisher, String topicName, OutboundMessage message) {
        try {
            publisher.publish(message, Topic.of(topicName));
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "### Caught while trying to publisher.publish(): {0}", e.getMessage());
            throw new BrokerPublishFailureException("Failed to publish message to Solace broker", e);
        }
    }

    private void pauseAfterPublish() {
        try {
            Thread.sleep(1000 / APPROX_MSG_RATE_PER_SEC);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

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

    private String buildResponse(String topicName, String content) {
        return "{\"destination\":\"" + topicName + "\",\"content\":\"" + content + "\"}";
    }
}
