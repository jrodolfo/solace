package org.orgname.solace.broker.api.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorMessage> handleUnreadableMessage(HttpMessageNotReadableException exception, HttpServletRequest request) {
        Throwable cause = exception.getCause();
        if (cause instanceof InvalidFormatException invalidFormatException && !invalidFormatException.getPath().isEmpty()) {
            String field = invalidFormatException.getPath().stream()
                    .map(reference -> reference.getFieldName())
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
        }

        return buildResponse(HttpStatus.BAD_REQUEST, "Request body could not be parsed", request.getRequestURI(), null);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorMessage> handleBadRequest(BadRequestException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(BrokerConfigurationException.class)
    public ResponseEntity<ErrorMessage> handleBrokerConfiguration(BrokerConfigurationException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(BrokerConnectionException.class)
    public ResponseEntity<ErrorMessage> handleBrokerConnection(BrokerConnectionException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler({BrokerPublishFailureException.class, BrokerPublishException.class})
    public ResponseEntity<ErrorMessage> handleBrokerPublish(RuntimeException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_GATEWAY, exception.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> handleUnexpected(Exception exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), request.getRequestURI(), null);
    }

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
