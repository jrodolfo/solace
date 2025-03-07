package org.orgname.solace.broker.api.service;

import com.solace.messaging.MessagingService;
import com.solace.messaging.config.SolaceProperties.MessageProperties;
import com.solace.messaging.config.profile.ConfigurationProfile;
import com.solace.messaging.publisher.DirectMessagePublisher;
import com.solace.messaging.publisher.OutboundMessage;
import com.solace.messaging.publisher.OutboundMessageBuilder;
import com.solace.messaging.resources.Topic;
import org.orgname.solace.broker.api.dto.ParameterDTO;
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


    @Override
    public String sendMessage(String topicName, String content, Optional<ParameterDTO> solaceParametersOptional) throws Exception {

        // check mandatory parameters
        if (topicName == null || topicName.isEmpty() || content == null || content.isEmpty()) {
            throw new IllegalArgumentException(ERROR_EMPTY_MESSAGE_OR_TOPIC_NAME);
        }

        final Properties properties;

        ParameterDTO parameterDTO = solaceParametersOptional.orElse(null);
        if (parameterDTO == null) {
            properties = AccessPropertiesImpl.getPropertiesPublisher();
        } else {
            properties = AccessPropertiesImpl.getPropertiesPublisher(parameterDTO);
        }

        final MessagingService messagingService = MessagingService.builder(ConfigurationProfile.V1)
                .fromProperties(properties)
                .build();

        messagingService.connect();  // blocking connection to the Solace Broker

        messagingService.addServiceInterruptionListener(serviceEvent -> {
            logger.log(Level.SEVERE, "### SERVICE INTERRUPTION: " + serviceEvent.getCause());
            // isShutdown = true;
        });
        messagingService.addReconnectionAttemptListener(serviceEvent -> {
            logger.log(Level.SEVERE, "### RECONNECTING ATTEMPT: " + serviceEvent);
        });
        messagingService.addReconnectionListener(serviceEvent -> {
            final String message = "### RECONNECTED: " + serviceEvent;
            logger.log(Level.SEVERE, message);
        });

        // build the publisher object
        final DirectMessagePublisher publisher = messagingService.createDirectMessagePublisherBuilder()
                .onBackPressureWait(1)
                .build();
        publisher.start();

        // can be called for ACL violations,
        publisher.setPublishFailureListener(e -> {
            logger.log(Level.SEVERE, "### FAILED PUBLISH: " + e.getMessage());
        });

        // Transform String to byte[] since we need a binary payload message
        byte[] payload = content.getBytes(StandardCharsets.UTF_8);

        OutboundMessageBuilder messageBuilder = messagingService.messageBuilder();

        try {
            // as an example of a header
            messageBuilder.withProperty(MessageProperties.APPLICATION_MESSAGE_ID, UUID.randomUUID().toString());
            OutboundMessage message = messageBuilder.build(payload);  // binary payload message
            logger.log(Level.INFO, "OutboundMessage: " + message);
            publisher.publish(message, Topic.of(topicName));  // send the message

        } catch (RuntimeException e) {  // threw from publish(), only thing that is throwing here
            logger.log(Level.SEVERE, "### Caught while trying to publisher.publish(): %s%n" + e.getMessage());
        } finally {
            try {
                Thread.sleep(1000 / APPROX_MSG_RATE_PER_SEC);  // do Thread.sleep(0) for max speed
                // Note: STANDARD Edition Solace PubSub+ broker is limited to 10k msg/s max ingress
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, e.getMessage());
            }
        }

        publisher.terminate(500);
        messagingService.disconnect();
        String msg = "Message sent to topic: \"" + topicName + "\" with content: \"" + content + "\"";
        System.out.println(msg);
        logger.log(Level.INFO, msg);

        String returnMessage = "{\"destination\":\"" + topicName + "\",\"content\":\"" + content + "\"}";
        System.out.println("returnMessage" + returnMessage);
        logger.log(Level.INFO, "returnMessage" + returnMessage);

        return returnMessage;
    }

}
