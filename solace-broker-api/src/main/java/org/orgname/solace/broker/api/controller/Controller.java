
package org.orgname.solace.broker.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.orgname.solace.broker.api.dto.MessageWrapperDTO;
import org.orgname.solace.broker.api.dto.ParameterDTO;
import org.orgname.solace.broker.api.jpa.Message;
import org.orgname.solace.broker.api.service.DatabaseImpl;
import org.orgname.solace.broker.api.service.DirectPublisherServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;


@RestController
@RequestMapping("/api/v1/messages")
@Tag(name = "messages", description = "the Solace Broker API")
public class Controller {

    private static final Logger logger = Logger.getLogger(Controller.class.getName());

    private final DirectPublisherServiceImpl directPublisherServiceImpl;

    private final DatabaseImpl db;

    // Final field is initialized via this constructor
    @Autowired
    public Controller(DirectPublisherServiceImpl directPublisherServiceImpl, DatabaseImpl db) {
        this.directPublisherServiceImpl = directPublisherServiceImpl;
        this.db = db;
    }

    @GetMapping("/all")
    public Iterable<Message> getAllMessages() {
        return db.getAllMessages();
    }

    @CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"}) // Allow React app origin
    @Operation(summary = "Send a message", description = "Send a message to the Solace Broker", tags = {"messages"})
    @ApiResponses(value = {@ApiResponse(description = "successful operation", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Message.class)), @Content(mediaType = "application/xml", schema = @Schema(implementation = Message.class))})})
    @PostMapping(value = "/message", consumes = {"application/json", "application/xml", "application/x-www-form-urlencoded"})
    public ResponseEntity<String> sendMessage(@RequestBody MessageWrapperDTO wrapper) {

        String responseMessage;

        if (wrapper == null || wrapper.getMessage() == null) {
            responseMessage = "Message is null";
            logger.log(Level.INFO, responseMessage);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseMessage);
        } else {

            // persist the message before sending the request to Solace
            Message message = db.saveMessage(wrapper);

            // now send the message
            String topicName = wrapper.getMessage().getDestination();
            String content = wrapper.getMessage().getPayload().getContent();
            try {
                if (wrapper.parametersAreValid()) {
                    ParameterDTO parameterDTO = getParameterDTO(wrapper);
                    responseMessage = directPublisherServiceImpl.sendMessage(topicName, content, Optional.of(parameterDTO));
                } else {
                    responseMessage = directPublisherServiceImpl.sendMessage(topicName, content, Optional.empty());
                }
            } catch (Exception e) {
                responseMessage = e.getMessage();
                logger.log(Level.INFO, responseMessage);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseMessage);
            }
            logger.log(Level.INFO, responseMessage);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseMessage);
        }
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

