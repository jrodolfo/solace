package org.orgname.solace.broker.api.exception;

/**
 * Exception thrown when a request is malformed or contains invalid parameters.
 * <p>
 * This exception results in a {@code 400 Bad Request} response to the client.
 */
public class BadRequestException extends RuntimeException {

    /**
     * Constructs a new {@code BadRequestException} with the specified detail message.
     *
     * @param message the detail message
     */
    public BadRequestException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code BadRequestException} with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
