package org.orgname.solace.broker.api.exception;

/**
 * Exception thrown when there is an issue with the Solace broker configuration.
 * <p>
 * This typically indicates that the application is unable to correctly interpret
 * or apply the provided broker settings, preventing successful integration with
 * the Solace PubSub+ Broker.
 */
public class BrokerConfigurationException extends RuntimeException {

    /**
     * Constructs a new {@code BrokerConfigurationException} with the specified detail message.
     *
     * @param message the detail message
     */
    public BrokerConfigurationException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code BrokerConfigurationException} with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public BrokerConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
