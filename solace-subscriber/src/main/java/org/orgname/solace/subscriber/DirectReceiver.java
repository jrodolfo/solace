package org.orgname.solace.subscriber;

import com.solace.messaging.MessagingService;
import com.solace.messaging.config.profile.ConfigurationProfile;
import com.solace.messaging.receiver.DirectMessageReceiver;
import com.solace.messaging.receiver.MessageReceiver.MessageHandler;
import com.solace.messaging.resources.TopicSubscription;

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
    
    private static volatile int msgRecvCounter = 0;              // num messages received
    private static volatile boolean hasDetectedDiscard = false;  // detected any discards yet?
    private static volatile boolean isShutdown = false;          // are we done yet?

    /** Main method. */
    public static void main(String... args) throws Exception {

        logger.log(Level.INFO, "Initializing...");

        final Properties properties = AccessProperties.getPropertiesReceiver();

        final MessagingService messagingService = MessagingService.builder(ConfigurationProfile.V1)
                .fromProperties(properties)
                .build();

        messagingService.connect();  // blocking connection to the Solace Broker

        messagingService.addServiceInterruptionListener(serviceEvent -> {
            logger.log(Level.SEVERE, "### SERVICE INTERRUPTION: " + serviceEvent.getCause());
        });
        messagingService.addReconnectionAttemptListener(serviceEvent -> {
            logger.log(Level.SEVERE, "### RECONNECTING ATTEMPT: " + serviceEvent);
        });
        messagingService.addReconnectionListener(serviceEvent -> {
            logger.log(Level.SEVERE, "### RECONNECTED: " + serviceEvent);
        });

        // build the Direct receiver object
        final DirectMessageReceiver receiver = messagingService.createDirectMessageReceiverBuilder()
                .withSubscriptions(TopicSubscription.of(TOPIC_NAME))
                // add more subscriptions here if you want
                .build();
        receiver.start();
        
        receiver.setReceiveFailureListener(failedReceiveEvent -> {
            logger.log(Level.SEVERE, "### FAILED RECEIVE EVENT: " + failedReceiveEvent);
        });

        final MessageHandler messageHandler = (inboundMessage) -> {

            // do not print anything to console... too slow!
            logger.log(Level.INFO, "InboundMessage: " + inboundMessage);

            msgRecvCounter++;
            // since Direct messages, check if there have been any lost any messages
            if (inboundMessage.getMessageDiscardNotification().hasBrokerDiscardIndication() ||
                    inboundMessage.getMessageDiscardNotification().hasInternalDiscardIndication()) {
                // If the subscriber is being over-driven (i.e. publish rates too high), the broker might discard some messages for this subscriber
                // check this flag to know if that's happened
                // to avoid discards:
                //  a) reduce publish rate
                //  b) use multiple-threads or shared subscriptions for parallel processing
                //  c) increase size of subscriber's D-1 egress buffers (check client-profile) (helps more with bursts)
                hasDetectedDiscard = true;  // set my own flag
            }
        };

        receiver.receiveAsync(messageHandler);

        logger.log(Level.INFO, "Connected and running. Press [ENTER] to quit.");

        try {
            while (System.in.available() == 0 && !isShutdown) {
                Thread.sleep(1000);  // wait 1 second
                logger.log(Level.INFO, "Received msg/s: " + msgRecvCounter);
                msgRecvCounter = 0;
                if (hasDetectedDiscard) {
                    logger.log(Level.INFO, "*** Egress discard detected *** : unable to keep up with full message rate");
                    hasDetectedDiscard = false;  // only show the error once per second
                }
            }
        } catch (InterruptedException e) {
            // Thread.sleep() interrupted... probably getting shut down
        }

        isShutdown = true;
        receiver.terminate(500);
        messagingService.disconnect();
        logger.log(Level.INFO, "Main thread quitting.");
    }
}
