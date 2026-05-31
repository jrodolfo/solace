package net.jrodolfo.solace.subscriber;

import com.solace.messaging.MessagingService;
import com.solace.messaging.config.profile.ConfigurationProfile;
import com.solace.messaging.receiver.DirectMessageReceiver;
import com.solace.messaging.receiver.MessageReceiver.MessageHandler;
import com.solace.messaging.resources.TopicSubscription;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.jrodolfo.solace.subscriber.Constants.TOPIC_NAME;

/**
 * Command-line subscriber for observing direct Solace topic traffic.
 *
 * <p>This class owns runtime lifecycle concerns for the subscriber module:
 * service startup, reconnect/discard logging, asynchronous inbound message
 * handling, throughput reporting, and orderly shutdown on user request.
 */
public class DirectReceiver {

    private static final Logger logger = Logger.getLogger(DirectReceiver.class.getName());
    private final AccessProperties accessProperties;
    private final AtomicBoolean shutdownTriggered = new AtomicBoolean(false);
    private volatile int msgRecvCounter = 0;              // num messages received
    private volatile boolean hasDetectedDiscard = false;  // detected any discards yet?
    private volatile boolean isShutdown = false;          // are we done yet?

    /**
     * Default constructor for {@link DirectReceiver} using default {@link AccessProperties}.
     */
    public DirectReceiver() {
        this(new AccessProperties());
    }

    /**
     * Constructor for {@link DirectReceiver} with a custom {@link AccessProperties}.
     *
     * @param accessProperties the properties resolver for Solace connection
     */
    DirectReceiver(AccessProperties accessProperties) {
        this.accessProperties = accessProperties;
    }

    /**
     * Entrypoint for the standalone subscriber process.
     */
    public static void main(String... args) {
        System.exit(new DirectReceiver().runSafely());
    }

    /**
     * Runs the subscriber and converts fatal startup/runtime errors into a
     * process exit code suitable for wrapper scripts.
     *
     * @return 0 if the subscriber runs and shuts down successfully, 1 otherwise
     */
    int runSafely() {
        try {
            run();
            return 0;
        } catch (SubscriberConfigurationException e) {
            logger.log(Level.SEVERE, "Subscriber configuration error: {0}", e.getMessage());
            return 1;
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Subscriber failed to start or run.", e);
            return 1;
        }
    }

    /**
     * Runs the subscriber lifecycle until shutdown is requested.
     * <p>This method connects to the broker, starts the receiver, and waits for a shutdown signal.
     *
     * @throws SubscriberConfigurationException if the connection properties are invalid
     * @throws RuntimeException if a fatal error occurs during execution
     */
    void run() {
        logger.log(Level.INFO, "Initializing subscriber.");

        final MessagingService messagingService = buildMessagingService(accessProperties.getPropertiesReceiver());
        messagingService.connect();  // blocking connection to the Solace Broker
        logger.log(Level.INFO, "Connected to Solace broker.");
        configureMessagingServiceListeners(messagingService);

        final DirectMessageReceiver receiver = createAndStartReceiver(messagingService);
        Thread shutdownHook = registerShutdownHook(receiver, messagingService);
        receiver.receiveAsync(createMessageHandler());

        waitForShutdownSignal();
        shutdown(receiver, messagingService);
        removeShutdownHook(shutdownHook);
    }

    /**
     * Builds the Solace messaging service from already-resolved connection
     * properties.
     *
     * @param properties the {@link Properties} for the Solace connection
     * @return a configured {@link MessagingService} instance
     */
    MessagingService buildMessagingService(Properties properties) {
        return MessagingService.builder(ConfigurationProfile.V1)
                .fromProperties(properties)
                .build();
    }

    /**
     * Registers runtime listeners used for interruption and reconnection
     * visibility.
     *
     * @param messagingService the service to attach listeners to
     */
    void configureMessagingServiceListeners(MessagingService messagingService) {
        messagingService.addServiceInterruptionListener(serviceEvent ->
                logger.log(Level.SEVERE, "Messaging service interrupted.", serviceEvent.getCause()));
        messagingService.addReconnectionAttemptListener(serviceEvent ->
                logger.log(Level.WARNING, "Attempting to reconnect messaging service: {0}", serviceEvent));
        messagingService.addReconnectionListener(serviceEvent ->
                logger.log(Level.INFO, "Messaging service reconnected: {0}", serviceEvent));
    }

    /**
     * Creates and starts the direct receiver subscribed to the configured topic.
     *
     * @param messagingService the connected messaging service
     * @return the started {@link DirectMessageReceiver}
     */
    DirectMessageReceiver createAndStartReceiver(MessagingService messagingService) {
        final DirectMessageReceiver receiver = messagingService.createDirectMessageReceiverBuilder()
                .withSubscriptions(TopicSubscription.of(TOPIC_NAME))
                .build();
        receiver.start();
        logger.log(Level.INFO, "Direct receiver started for topic subscription {0}.", TOPIC_NAME);
        receiver.setReceiveFailureListener(failedReceiveEvent ->
                logger.log(Level.SEVERE, "Direct receiver failed to process an inbound message: {0}", failedReceiveEvent));
        return receiver;
    }

    /**
     * Creates the async inbound-message handler used during normal runtime.
     *
     * @return a {@link MessageHandler} for processing inbound messages
     */
    MessageHandler createMessageHandler() {
        return inboundMessage -> {
            logger.log(Level.INFO, "InboundMessage: " + inboundMessage);
            handleInboundMessageState(
                    inboundMessage.getMessageDiscardNotification().hasBrokerDiscardIndication(),
                    inboundMessage.getMessageDiscardNotification().hasInternalDiscardIndication()
            );
        };
    }

    /**
     * Updates in-memory counters for throughput and discard reporting.
     *
     * @param brokerDiscardIndication true if the broker indicated a message discard
     * @param internalDiscardIndication true if the internal API indicated a message discard
     */
    void handleInboundMessageState(boolean brokerDiscardIndication, boolean internalDiscardIndication) {
        msgRecvCounter++;
        if (brokerDiscardIndication || internalDiscardIndication) {
            hasDetectedDiscard = true;
        }
    }

    /**
     * Blocks until the operator requests shutdown from standard input.
     */
    void waitForShutdownSignal() {
        logger.log(Level.INFO, "Subscriber is running. Press [ENTER] to quit.");

        try {
            while (System.in.available() == 0 && !isShutdown) {
                Thread.sleep(1000);
                logThroughputAndReset();
            }
        } catch (InterruptedException | IOException e) {
            logger.log(Level.WARNING, "Subscriber shutdown wait interrupted.");
        }
    }

    /**
     * Emits a one-second throughput snapshot and any discard warning detected
     * since the previous interval.
     */
    void logThroughputAndReset() {
        logger.log(Level.INFO, "Received messages in the last second: {0}", msgRecvCounter);
        msgRecvCounter = 0;
        if (hasDetectedDiscard) {
            logger.log(Level.WARNING, "Egress discard detected; subscriber could not keep up with the full message rate.");
            hasDetectedDiscard = false;
        }
    }

    /**
     * Terminates receiver resources and disconnects from the broker.
     *
     * @param receiver the receiver to terminate
     * @param messagingService the service to disconnect
     */
    void shutdown(DirectMessageReceiver receiver, MessagingService messagingService) {
        if (!shutdownTriggered.compareAndSet(false, true)) {
            return;
        }

        isShutdown = true;
        try {
            receiver.terminate(500);
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Failed to terminate subscriber receiver cleanly.", e);
        }
        try {
            messagingService.disconnect();
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Failed to disconnect subscriber messaging service cleanly.", e);
        }
        logger.log(Level.INFO, "Subscriber shutdown complete.");
    }

    /**
     * Registers a JVM shutdown hook so SIGTERM and normal JVM shutdown paths use
     * the same cleanup sequence as interactive shutdown.
     *
     * @param receiver the receiver to terminate on shutdown
     * @param messagingService the service to disconnect on shutdown
     * @return the created {@link Thread} used as a shutdown hook
     */
    Thread registerShutdownHook(DirectMessageReceiver receiver, MessagingService messagingService) {
        Thread shutdownHook = new Thread(
                () -> shutdown(receiver, messagingService),
                "solace-subscriber-shutdown-hook"
        );
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        return shutdownHook;
    }

    /**
     * Removes a previously registered shutdown hook.
     *
     * @param shutdownHook the hook to remove
     */
    void removeShutdownHook(Thread shutdownHook) {
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException ignored) {
            // JVM shutdown is already in progress.
        }
    }

    int getMsgRecvCounter() {
        return msgRecvCounter;
    }

    boolean hasDetectedDiscard() {
        return hasDetectedDiscard;
    }

    boolean isShutdown() {
        return isShutdown;
    }
}
