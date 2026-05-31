package net.jrodolfo.solace.broker.api.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler for the Solace Broker API.
 * <p>
 * This class intercepts various exceptions thrown by the application and transforms them
 * into standardized {@link ErrorMessage} responses with appropriate HTTP status codes.
 * It handles validation errors, message parsing issues, and broker-specific exceptions.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Handles {@link MethodArgumentNotValidException} which occurs when request body validation fails.
     *
     * @param exception the exception containing validation results
     * @param request the current HTTP request
     * @return a {@code 400 Bad Request} response with field-level error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorMessage> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            validationErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                request.getRequestURI(),
                validationErrors
        );
    }

    /**
     * Handles {@link HttpMessageNotReadableException} which occurs when the request body is malformed
     * or contains invalid values for enums.
     * <p>
     * Specifically handles validation for {@code message.deliveryMode} and {@code message.payload.type}
     * when they don't match expected Solace or application-specific values.
     *
     * @param exception the exception containing parsing details
     * @param request the current HTTP request
     * @return a {@code 400 Bad Request} response with specific error details if available
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorMessage> handleUnreadableMessage(HttpMessageNotReadableException exception, HttpServletRequest request) {
        Throwable cause = exception.getCause();
        if (cause instanceof InvalidFormatException invalidFormatException && !invalidFormatException.getPath().isEmpty()) {
            String field = invalidFormatException.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(fieldName -> fieldName != null && !fieldName.isBlank())
                    .reduce((left, right) -> left + "." + right)
                    .orElse("request");

            Map<String, String> validationErrors = new LinkedHashMap<>();
            if ("message.deliveryMode".equals(field)) {
                validationErrors.put(field, "message.deliveryMode must be one of DIRECT, NON_PERSISTENT, PERSISTENT");
                return buildResponse(
                        HttpStatus.BAD_REQUEST,
                        "Request validation failed",
                        request.getRequestURI(),
                        validationErrors
                );
            }
            if ("message.payload.type".equals(field)) {
                validationErrors.put(field, "payload.type must be one of TEXT, BINARY, JSON, XML");
                return buildResponse(
                        HttpStatus.BAD_REQUEST,
                        "Request validation failed",
                        request.getRequestURI(),
                        validationErrors
                );
            }
        }

        return buildResponse(HttpStatus.BAD_REQUEST, "Request body could not be parsed", request.getRequestURI(), null);
    }

    /**
     * Handles {@link BadRequestException} thrown for custom business logic validation failures.
     *
     * @param exception the exception containing the error message
     * @param request the current HTTP request
     * @return a {@code 400 Bad Request} response
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorMessage> handleBadRequest(BadRequestException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI(), null);
    }

    /**
     * Handles {@link BrokerConfigurationException} thrown when there are issues with Solace broker settings.
     *
     * @param exception the exception containing configuration error details
     * @param request the current HTTP request
     * @return a {@code 500 Internal Server Error} response
     */
    @ExceptionHandler(BrokerConfigurationException.class)
    public ResponseEntity<ErrorMessage> handleBrokerConfiguration(BrokerConfigurationException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), request.getRequestURI(), null);
    }

    /**
     * Handles {@link BrokerConnectionException} thrown when connection to Solace broker fails.
     *
     * @param exception the exception containing connection error details
     * @param request the current HTTP request
     * @return a {@code 503 Service Unavailable} response
     */
    @ExceptionHandler(BrokerConnectionException.class)
    public ResponseEntity<ErrorMessage> handleBrokerConnection(BrokerConnectionException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request.getRequestURI(), null);
    }

    /**
     * Handles exceptions related to message publishing failures on the Solace broker.
     *
     * @param exception the exception (either {@link BrokerPublishFailureException} or {@link BrokerPublishException})
     * @param request the current HTTP request
     * @return a {@code 502 Bad Gateway} response
     */
    @ExceptionHandler({BrokerPublishFailureException.class, BrokerPublishException.class})
    public ResponseEntity<ErrorMessage> handleBrokerPublish(RuntimeException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_GATEWAY, exception.getMessage(), request.getRequestURI(), null);
    }

    /**
     * Fallback handler for any unexpected exceptions not specifically covered.
     *
     * @param exception the unexpected exception
     * @param request the current HTTP request
     * @return a {@code 500 Internal Server Error} response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> handleUnexpected(Exception exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), request.getRequestURI(), null);
    }

    /**
     * Helper method to construct the {@link ResponseEntity} with the {@link ErrorMessage} body.
     *
     * @param status the HTTP status to return
     * @param message the error message
     * @param path the request path
     * @param validationErrors optional map of validation errors
     * @return the formatted response entity
     */
    private ResponseEntity<ErrorMessage> buildResponse(HttpStatus status, String message, String path, Map<String, String> validationErrors) {
        ErrorMessage body = new ErrorMessage(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                validationErrors
        );
        return ResponseEntity.status(status).body(body);
    }
}
