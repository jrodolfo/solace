package net.jrodolfo.solace.broker.api.exception;

/**
 * Exception thrown when the Solace broker explicitly rejects a message publishing attempt.
 * <p>
 * This often corresponds to a Negative Acknowledgment (NACK) from the broker, indicating
 * that the message was received but could not be accepted (e.g., due to permission issues,
 * full queues, or invalid destination).
 * This typically results in a {@code 502 Bad Gateway} response.
 */
public class BrokerPublishFailureException extends RuntimeException {

    /**
     * Constructs a new {@code BrokerPublishFailureException} with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public BrokerPublishFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
