
package org.orgname.solace.broker.api.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.orgname.solace.broker.api.dto.MessageWrapperDTO;
import org.orgname.solace.broker.api.dto.ParameterDTO;
import org.orgname.solace.broker.api.exception.BadRequestException;
import org.orgname.solace.broker.api.exception.ErrorMessage;
import org.orgname.solace.broker.api.jpa.Message;
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

    @GetMapping("/all")
    public Iterable<Message> getAllMessages() {
        return database.getAllMessages();
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
            logger.log(Level.INFO, "Message is null");
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
            logger.log(Level.INFO, e.getMessage());
            throw new BadRequestException(e.getMessage(), e);
        }

        logger.log(Level.INFO, responseMessage);
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
