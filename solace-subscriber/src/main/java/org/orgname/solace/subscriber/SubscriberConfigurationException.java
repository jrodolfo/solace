package org.orgname.solace.subscriber;

/**
 * Thrown when there is an issue with the subscriber's configuration,
 * such as missing or invalid environment variables for Solace connection.
 */
public class SubscriberConfigurationException extends RuntimeException {

    /**
     * Constructs a new {@code SubscriberConfigurationException} with the specified detail message.
     *
     * @param message the detail message
     */
    public SubscriberConfigurationException(String message) {
        super(message);
    }
}
