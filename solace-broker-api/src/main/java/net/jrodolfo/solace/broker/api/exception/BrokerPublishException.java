package net.jrodolfo.solace.broker.api.exception;

/**
 * Exception thrown when an error occurs during the message publishing process to the Solace broker.
 * <p>
 * This is a general exception for publishing failures. For specific failures reported by
 * the broker (e.g., NACK), {@link BrokerPublishFailureException} might be used instead.
 * This typically results in a {@code 502 Bad Gateway} response.
 */
public class BrokerPublishException extends RuntimeException {

    /**
     * Constructs a new {@code BrokerPublishException} with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public BrokerPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
