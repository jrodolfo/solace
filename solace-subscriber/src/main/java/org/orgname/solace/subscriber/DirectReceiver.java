package org.orgname.solace.subscriber;

import com.solace.messaging.MessagingService;
import com.solace.messaging.config.profile.ConfigurationProfile;
import com.solace.messaging.receiver.DirectMessageReceiver;
import com.solace.messaging.receiver.MessageReceiver.MessageHandler;
import com.solace.messaging.resources.TopicSubscription;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.orgname.solace.subscriber.Constants.TOPIC_NAME;

/**
 * Application that subscribes to a topic.
 * Disclaimer: most of the code below comes from the Solace training material
 */
public class DirectReceiver {

    private static final Logger logger = Logger.getLogger(DirectReceiver.class.getName());
    private final AccessProperties accessProperties;
    private volatile int msgRecvCounter = 0;              // num messages received
    private volatile boolean hasDetectedDiscard = false;  // detected any discards yet?
    private volatile boolean isShutdown = false;          // are we done yet?

    public DirectReceiver() {
        this(new AccessProperties());
    }

    DirectReceiver(AccessProperties accessProperties) {
        this.accessProperties = accessProperties;
    }

    /** Main method. */
    public static void main(String... args) {
        try {
            new DirectReceiver().run();
        } catch (SubscriberConfigurationException e) {
            logger.log(Level.SEVERE, "Subscriber configuration error: {0}", e.getMessage());
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Subscriber failed to start or run.", e);
        }
    }

    void run() {
        logger.log(Level.INFO, "Initializing...");

        final MessagingService messagingService = buildMessagingService(accessProperties.getPropertiesReceiver());
        messagingService.connect();  // blocking connection to the Solace Broker
        configureMessagingServiceListeners(messagingService);

        final DirectMessageReceiver receiver = createAndStartReceiver(messagingService);
        receiver.receiveAsync(createMessageHandler());

        waitForShutdownSignal();
        shutdown(receiver, messagingService);
    }

    MessagingService buildMessagingService(Properties properties) {
        return MessagingService.builder(ConfigurationProfile.V1)
                .fromProperties(properties)
                .build();
    }

    void configureMessagingServiceListeners(MessagingService messagingService) {
        messagingService.addServiceInterruptionListener(serviceEvent ->
                logger.log(Level.SEVERE, "### SERVICE INTERRUPTION: " + serviceEvent.getCause()));
        messagingService.addReconnectionAttemptListener(serviceEvent ->
                logger.log(Level.SEVERE, "### RECONNECTING ATTEMPT: " + serviceEvent));
        messagingService.addReconnectionListener(serviceEvent ->
                logger.log(Level.SEVERE, "### RECONNECTED: " + serviceEvent));
    }

    DirectMessageReceiver createAndStartReceiver(MessagingService messagingService) {
        final DirectMessageReceiver receiver = messagingService.createDirectMessageReceiverBuilder()
                .withSubscriptions(TopicSubscription.of(TOPIC_NAME))
                .build();
        receiver.start();
        receiver.setReceiveFailureListener(failedReceiveEvent ->
                logger.log(Level.SEVERE, "### FAILED RECEIVE EVENT: " + failedReceiveEvent));
        return receiver;
    }

    MessageHandler createMessageHandler() {
        return inboundMessage -> {
            logger.log(Level.INFO, "InboundMessage: " + inboundMessage);
            handleInboundMessageState(
                    inboundMessage.getMessageDiscardNotification().hasBrokerDiscardIndication(),
                    inboundMessage.getMessageDiscardNotification().hasInternalDiscardIndication()
            );
        };
    }

    void handleInboundMessageState(boolean brokerDiscardIndication, boolean internalDiscardIndication) {
        msgRecvCounter++;
        if (brokerDiscardIndication || internalDiscardIndication) {
            hasDetectedDiscard = true;
        }
    }

    void waitForShutdownSignal() {
        logger.log(Level.INFO, "Connected and running. Press [ENTER] to quit.");

        try {
            while (System.in.available() == 0 && !isShutdown) {
                Thread.sleep(1000);
                logThroughputAndReset();
            }
        } catch (InterruptedException | IOException e) {
            // Thread.sleep() interrupted... probably getting shut down
        }
    }

    void logThroughputAndReset() {
        logger.log(Level.INFO, "Received msg/s: " + msgRecvCounter);
        msgRecvCounter = 0;
        if (hasDetectedDiscard) {
            logger.log(Level.INFO, "*** Egress discard detected *** : unable to keep up with full message rate");
            hasDetectedDiscard = false;
        }
    }

    void shutdown(DirectMessageReceiver receiver, MessagingService messagingService) {
        isShutdown = true;
        receiver.terminate(500);
        messagingService.disconnect();
        logger.log(Level.INFO, "Main thread quitting.");
    }

    int getMsgRecvCounter() {
        return msgRecvCounter;
    }

    boolean hasDetectedDiscard() {
        return hasDetectedDiscard;
    }
}
