package org.orgname.solace.broker.api.exception;

/**
 * Exception thrown when a connection to the Solace PubSub+ Broker cannot be established or is lost.
 * <p>
 * This exception indicates a connectivity issue between the API and the broker,
 * often resulting in a {@code 503 Service Unavailable} response.
 */
public class BrokerConnectionException extends RuntimeException {

    /**
     * Constructs a new {@code BrokerConnectionException} with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public BrokerConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
