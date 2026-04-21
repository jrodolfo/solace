
package org.orgname.solace.broker.api.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.orgname.solace.broker.api.dto.MessageWrapperDTO;
import org.orgname.solace.broker.api.dto.ParameterDTO;
import org.orgname.solace.broker.api.dto.PagedMessagesResponseDTO;
import org.orgname.solace.broker.api.dto.PublishMessageResponseDTO;
import org.orgname.solace.broker.api.exception.BadRequestException;
import org.orgname.solace.broker.api.exception.ErrorMessage;
import org.orgname.solace.broker.api.jpa.Message;
import org.orgname.solace.broker.api.jpa.PublishStatus;
import org.orgname.solace.broker.api.service.Database;
import org.orgname.solace.broker.api.service.DirectPublisherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


@RestController
@RequestMapping("/api/v1/messages")
@Tag(name = "messages", description = "the Solace Broker API")
public class MessageController {

    private static final Logger logger = Logger.getLogger(MessageController.class.getName());
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "priority", "destination", "innerMessageId");
    private static final String RETRY_BLOCKED_MESSAGE = "Only messages published with server-side broker configuration can be retried";
    private static final String PUBLISH_STATE_UPDATE_FAILURE_MESSAGE =
            "Message was published to the broker but the database state could not be updated";
    private final Database database;
    private final DirectPublisherService directPublisherService;

    // The final field is initialized via this constructor
    @Autowired
    public MessageController(Database database, DirectPublisherService directPublisherService) {
        this.database = database;
        this.directPublisherService = directPublisherService;
    }

    @Operation(summary = "List stored messages", description = "Return stored messages using paginated reads", tags = {"messages"})
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Stored messages returned successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PagedMessagesResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "stored-messages-page",
                                    summary = "Representative filtered and sorted message response",
                                    value = """
                                            {
                                              "items": [
                                                {
                                                  "id": 1,
                                                  "innerMessageId": "001",
                                                  "destination": "solace/java/direct/system-01",
                                                  "deliveryMode": "PERSISTENT",
                                                  "priority": 3,
                                                  "publishStatus": "PUBLISHED",
                                                  "failureReason": null,
                                                  "publishedAt": "2026-04-20T19:55:10",
                                                  "properties": {
                                                    "property01": "value01"
                                                  },
                                                  "payload": {
                                                    "type": "binary",
                                                    "content": "01001000 01100101 01101100",
                                                    "createdAt": null,
                                                    "updatedAt": null
                                                  },
                                                  "createdAt": null,
                                                  "updatedAt": null
                                                },
                                                {
                                                  "id": 2,
                                                  "innerMessageId": "002",
                                                  "destination": "solace/java/direct/system-02",
                                                  "deliveryMode": "PERSISTENT",
                                                  "priority": 1,
                                                  "publishStatus": "FAILED",
                                                  "failureReason": "Failed to publish message to Solace broker",
                                                  "publishedAt": null,
                                                  "properties": {},
                                                  "payload": {
                                                    "type": "binary",
                                                    "content": "01010111 01101111 01110010 01101100 01100100",
                                                    "createdAt": null,
                                                    "updatedAt": null
                                                  },
                                                  "createdAt": null,
                                                  "updatedAt": null
                                                }
                                              ],
                                              "page": 0,
                                              "size": 10,
                                              "totalElements": 1,
                                              "totalPages": 1,
                                              "first": true,
                                              "last": true
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/all")
    public PagedMessagesResponseDTO getAllMessages(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of messages per page. Maximum allowed value is 100", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Optional case-insensitive filter for destination", example = "solace/java/direct/system-01")
            @RequestParam(required = false) String destination,
            @Parameter(description = "Optional case-insensitive filter for delivery mode", example = "PERSISTENT")
            @RequestParam(required = false) String deliveryMode,
            @Parameter(description = "Optional case-insensitive filter for the inner message id", example = "001")
            @RequestParam(required = false) String innerMessageId,
            @Parameter(description = "Optional exact filter for publish status. Allowed values: PENDING, PUBLISHED, FAILED", example = "FAILED")
            @RequestParam(required = false) String publishStatus,
            @Parameter(description = "Optional lower bound for createdAt using ISO-8601 local date-time", example = "2026-04-21T00:00:00")
            @RequestParam(required = false) String createdAtFrom,
            @Parameter(description = "Optional upper bound for createdAt using ISO-8601 local date-time", example = "2026-04-21T23:59:59")
            @RequestParam(required = false) String createdAtTo,
            @Parameter(description = "Optional lower bound for publishedAt using ISO-8601 local date-time", example = "2026-04-21T00:00:00")
            @RequestParam(required = false) String publishedAtFrom,
            @Parameter(description = "Optional upper bound for publishedAt using ISO-8601 local date-time", example = "2026-04-21T23:59:59")
            @RequestParam(required = false) String publishedAtTo,
            @Parameter(description = "Field to sort by. Allowed values: createdAt, priority, destination, innerMessageId", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction. Allowed values: asc, desc", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDirection) {
        if (page < 0) {
            throw new BadRequestException("page must be greater than or equal to 0");
        }
        if (size < 1) {
            throw new BadRequestException("size must be greater than or equal to 1");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new BadRequestException("size must be less than or equal to 100");
        }
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException("sortBy must be one of createdAt, priority, destination, innerMessageId");
        }
        if (!"asc".equalsIgnoreCase(sortDirection) && !"desc".equalsIgnoreCase(sortDirection)) {
            throw new BadRequestException("sortDirection must be asc or desc");
        }
        return database.getAllMessages(
                page,
                size,
                destination,
                deliveryMode,
                innerMessageId,
                parsePublishStatus(publishStatus),
                parseDateTime("createdAtFrom", createdAtFrom),
                parseDateTime("createdAtTo", createdAtTo),
                parseDateTime("publishedAtFrom", publishedAtFrom),
                parseDateTime("publishedAtTo", publishedAtTo),
                sortBy,
                sortDirection);
    }

    @CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"}) // Allow React app origin
    @Operation(summary = "Send a message", description = "Send a message to the Solace Broker", tags = {"messages"})
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Message published successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PublishMessageResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "publish-success",
                                    summary = "Successful publish response",
                                    value = "{\"destination\":\"solace/java/direct/system-01\",\"content\":\"01001000 01100101 01101100\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed or publisher input was invalid",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorMessage.class),
                            examples = {
                                    @ExampleObject(
                                            name = "validation-failure",
                                            summary = "Nested request validation failure",
                                            value = """
                                                    {
                                                      "timestamp": "2026-04-20T19:55:00Z",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "Request validation failed",
                                                      "path": "/api/v1/messages/message",
                                                      "validationErrors": {
                                                        "message.innerMessageId": "message.innerMessageId is required",
                                                        "message.payload": "message.payload is required"
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "publisher-input-failure",
                                            summary = "Publisher rejected request input",
                                            value = """
                                                    {
                                                      "timestamp": "2026-04-20T19:55:00Z",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "Topic name cannot be empty",
                                                      "path": "/api/v1/messages/message",
                                                      "validationErrors": null
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Server-side Solace configuration is missing or invalid",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorMessage.class),
                            examples = @ExampleObject(
                                    name = "broker-configuration-failure",
                                    summary = "Missing server configuration",
                                    value = """
                                            {
                                              "timestamp": "2026-04-20T19:55:00Z",
                                              "status": 500,
                                              "error": "Internal Server Error",
                                              "message": "System environment variables SOLACE_CLOUD_HOST, SOLACE_CLOUD_VPN, SOLACE_CLOUD_USERNAME, SOLACE_CLOUD_PASSWORD are not set.",
                                              "path": "/api/v1/messages/message",
                                              "validationErrors": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Broker connection or publisher startup failed",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorMessage.class),
                            examples = @ExampleObject(
                                    name = "broker-connection-failure",
                                    summary = "Unable to connect to broker",
                                    value = """
                                            {
                                              "timestamp": "2026-04-20T19:55:00Z",
                                              "status": 503,
                                              "error": "Service Unavailable",
                                              "message": "Failed to connect to Solace broker",
                                              "path": "/api/v1/messages/message",
                                              "validationErrors": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Downstream publish request failed",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorMessage.class),
                            examples = @ExampleObject(
                                    name = "broker-publish-failure",
                                    summary = "Broker accepted connection but publish failed",
                                    value = """
                                            {
                                              "timestamp": "2026-04-20T19:55:00Z",
                                              "status": 502,
                                              "error": "Bad Gateway",
                                              "message": "Failed to publish message to Solace broker",
                                              "path": "/api/v1/messages/message",
                                              "validationErrors": null
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping(value = "/message", consumes = {"application/json", "application/xml", "application/x-www-form-urlencoded"})
    public ResponseEntity<PublishMessageResponseDTO> sendMessage(@Valid @RequestBody MessageWrapperDTO wrapper) {
        if (wrapper == null || wrapper.getMessage() == null) {
            logger.log(Level.WARNING, "Rejected publish request with missing message payload");
            throw new BadRequestException("Message is null");
        }

        Message savedMessage = database.savePendingMessage(wrapper);

        String topicName = wrapper.getMessage().getDestination();
        String content = wrapper.getMessage().getPayload().getContent();
        PublishMessageResponseDTO responseMessage;

        try {
            if (wrapper.parametersAreValid()) {
                ParameterDTO parameterDTO = getParameterDTO(wrapper);
                responseMessage = directPublisherService.sendMessage(topicName, content, Optional.of(parameterDTO));
            } else {
                responseMessage = directPublisherService.sendMessage(topicName, content, Optional.empty());
            }
        } catch (IllegalArgumentException e) {
            markFailedSafely(savedMessage.getId(), topicName, e.getMessage());
            logger.log(Level.WARNING, "Rejected publish request for topic {0}: {1}", new Object[]{topicName, e.getMessage()});
            throw new BadRequestException(e.getMessage(), e);
        } catch (RuntimeException e) {
            markFailedSafely(savedMessage.getId(), topicName, e.getMessage());
            throw e;
        }

        markPublishedOrThrow(savedMessage.getId(), topicName);

        logger.log(Level.INFO, "Accepted publish request for topic {0}", topicName);
        return ResponseEntity.status(201).body(responseMessage);
    }

    @PostMapping("/{messageId}/retry")
    public ResponseEntity<PublishMessageResponseDTO> retryMessage(@PathVariable Long messageId) {
        Message storedMessage = database.findMessageById(messageId);
        if (storedMessage.getPublishStatus() != PublishStatus.FAILED) {
            throw new BadRequestException("Only FAILED messages can be retried");
        }
        if (!storedMessage.isRetrySupported()) {
            throw new BadRequestException(RETRY_BLOCKED_MESSAGE);
        }

        String topicName = storedMessage.getDestination();
        String content = storedMessage.getPayload().getContent();
        PublishMessageResponseDTO responseMessage;

        database.markMessagePending(messageId);

        try {
            responseMessage = directPublisherService.sendMessage(topicName, content, Optional.empty());
        } catch (IllegalArgumentException e) {
            markFailedSafely(messageId, topicName, e.getMessage());
            logger.log(Level.WARNING, "Rejected retry request for stored message {0}: {1}", new Object[]{messageId, e.getMessage()});
            throw new BadRequestException(e.getMessage(), e);
        } catch (RuntimeException e) {
            markFailedSafely(messageId, topicName, e.getMessage());
            throw e;
        }

        markPublishedOrThrow(messageId, topicName);

        logger.log(Level.INFO, "Retried stored message {0} for topic {1}", new Object[]{messageId, topicName});
        return ResponseEntity.ok(responseMessage);
    }

    private static ParameterDTO getParameterDTO(MessageWrapperDTO wrapper) {
        ParameterDTO parameterDTO = new ParameterDTO();
        parameterDTO.setHost(wrapper.getHost());
        parameterDTO.setVpnName(wrapper.getVpnName());
        parameterDTO.setUserName(wrapper.getUserName());
        parameterDTO.setPassword(wrapper.getPassword());
        return parameterDTO;
    }

    private static PublishStatus parsePublishStatus(String publishStatus) {
        if (publishStatus == null || publishStatus.trim().isEmpty()) {
            return null;
        }

        try {
            return PublishStatus.valueOf(publishStatus.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("publishStatus must be one of PENDING, PUBLISHED, FAILED", e);
        }
    }

    private static LocalDateTime parseDateTime(String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new BadRequestException(fieldName + " must be a valid ISO-8601 date-time", e);
        }
    }

    private void markPublishedOrThrow(Long messageId, String topicName) {
        try {
            database.markMessagePublished(messageId);
        } catch (RuntimeException exception) {
            logger.log(
                    Level.SEVERE,
                    "Published message {0} for topic {1} to the broker, but failed to update database state to PUBLISHED",
                    new Object[]{messageId, topicName}
            );
            throw new IllegalStateException(PUBLISH_STATE_UPDATE_FAILURE_MESSAGE, exception);
        }
    }

    private void markFailedSafely(Long messageId, String topicName, String failureReason) {
        try {
            database.markMessageFailed(messageId, failureReason);
        } catch (RuntimeException updateException) {
            logger.log(
                    Level.SEVERE,
                    "Failed to update database state to FAILED for message {0} on topic {1}: {2}",
                    new Object[]{messageId, topicName, updateException.getMessage()}
            );
        }
    }
}
