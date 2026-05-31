package net.jrodolfo.solace.broker.api.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a standardized error response body for the Solace Broker API.
 * <p>
 * This class captures essential details about an error, including the timestamp,
 * HTTP status, a descriptive message, the request path, and any specific validation errors.
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ErrorMessage {
    /**
     * The timestamp when the error occurred.
     */
    private Instant timestamp;

    /**
     * The HTTP status code (e.g., 400, 500).
     */
    private int status;

    /**
     * The short name of the HTTP error (e.g., "Bad Request").
     */
    private String error;

    /**
     * A detailed message explaining the error.
     */
    private String message;

    /**
     * The URI path where the error occurred.
     */
    private String path;

    /**
     * A map of field-specific validation errors, if applicable.
     * The key is the field name and the value is the error message.
     */
    private Map<String, String> validationErrors;
}
