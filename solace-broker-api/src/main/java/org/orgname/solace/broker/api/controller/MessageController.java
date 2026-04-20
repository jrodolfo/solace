
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
import org.orgname.solace.broker.api.exception.BadRequestException;
import org.orgname.solace.broker.api.exception.ErrorMessage;
import org.orgname.solace.broker.api.service.Database;
import org.orgname.solace.broker.api.service.DirectPublisherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;


@RestController
@RequestMapping("/api/v1/messages")
@Tag(name = "messages", description = "the Solace Broker API")
public class MessageController {

    private static final Logger logger = Logger.getLogger(MessageController.class.getName());
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
                                    summary = "Representative paginated message response",
                                    value = """
                                            {
                                              "items": [
                                                {
                                                  "id": 1,
                                                  "innerMessageId": "001",
                                                  "destination": "solace/java/direct/system-01",
                                                  "deliveryMode": "PERSISTENT",
                                                  "priority": 3,
                                                  "properties": [
                                                    {
                                                      "id": 10,
                                                      "propertyKey": "property01",
                                                      "propertyValue": "value01",
                                                      "createdAt": null,
                                                      "updatedAt": null
                                                    }
                                                  ],
                                                  "payload": {
                                                    "id": 20,
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
                                                  "properties": [],
                                                  "payload": {
                                                    "id": 21,
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
                                              "size": 20,
                                              "totalElements": 2,
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
            @Parameter(description = "Number of messages per page", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0) {
            throw new BadRequestException("page must be greater than or equal to 0");
        }
        if (size < 1) {
            throw new BadRequestException("size must be greater than or equal to 1");
        }
        return database.getAllMessages(page, size);
    }

    @CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"}) // Allow React app origin
    @Operation(summary = "Send a message", description = "Send a message to the Solace Broker", tags = {"messages"})
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Message published successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = String.class),
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
    public ResponseEntity<String> sendMessage(@Valid @RequestBody MessageWrapperDTO wrapper) {
        if (wrapper == null || wrapper.getMessage() == null) {
            logger.log(Level.WARNING, "Rejected publish request with missing message payload");
            throw new BadRequestException("Message is null");
        }

        database.saveMessage(wrapper);

        String topicName = wrapper.getMessage().getDestination();
        String content = wrapper.getMessage().getPayload().getContent();
        String responseMessage;

        try {
            if (wrapper.parametersAreValid()) {
                ParameterDTO parameterDTO = getParameterDTO(wrapper);
                responseMessage = directPublisherService.sendMessage(topicName, content, Optional.of(parameterDTO));
            } else {
                responseMessage = directPublisherService.sendMessage(topicName, content, Optional.empty());
            }
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Rejected publish request for topic {0}: {1}", new Object[]{topicName, e.getMessage()});
            throw new BadRequestException(e.getMessage(), e);
        }

        logger.log(Level.INFO, "Accepted publish request for topic {0}", topicName);
        return ResponseEntity.status(201).body(responseMessage);
    }

    private static ParameterDTO getParameterDTO(MessageWrapperDTO wrapper) {
        ParameterDTO parameterDTO = new ParameterDTO();
        parameterDTO.setHost(wrapper.getHost());
        parameterDTO.setVpnName(wrapper.getVpnName());
        parameterDTO.setUserName(wrapper.getUserName());
        parameterDTO.setPassword(wrapper.getPassword());
        return parameterDTO;
    }
}
