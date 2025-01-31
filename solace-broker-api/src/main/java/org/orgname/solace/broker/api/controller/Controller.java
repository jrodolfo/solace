package org.orgname.solace.broker.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.orgname.solace.broker.api.model.Message;
import org.orgname.solace.broker.api.model.MessageWithParameters;
import org.orgname.solace.broker.api.model.Payload;
import org.orgname.solace.broker.api.model.SolaceParameters;
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
    private Message msg;

    // Final field is initialized via this constructor
    @Autowired
    public Controller(DirectPublisherServiceImpl directPublisherServiceImpl) {
        this.directPublisherServiceImpl = directPublisherServiceImpl;
    }

    @CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"}) // Allow React app origin
    @Operation(summary = "Send a message", description = "Send a message to the Solace Broker", tags = {"messages"})
    @ApiResponses(value = {@ApiResponse(description = "successful operation", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Message.class)), @Content(mediaType = "application/xml", schema = @Schema(implementation = Message.class))})})
    @PostMapping(value = "/message", consumes = {"application/json", "application/xml", "application/x-www-form-urlencoded"})
    public ResponseEntity<String> message(@RequestBody String message) {

        logger.log(Level.INFO, "Received POST request to send this message to Solace Broker: " + message);

        Message msg = null;
        MessageWithParameters msgWithParameters = null;

        // TODO: remove logging the messages after development is done
        // map the json message to an object
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            msg = objectMapper.readValue(message, Message.class);
            logger.log(Level.INFO, "Message deserialized: " + msg);
        } catch (Exception eFirst) {
            logger.log(Level.INFO, "Error! Not able to deserialize the message: " + message);
            eFirst.printStackTrace();
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                msgWithParameters = objectMapper.readValue(message, MessageWithParameters.class);
                logger.log(Level.INFO, "Message deserialized: " + msgWithParameters);
            } catch (Exception eSecond) {
                logger.log(Level.INFO, "Error! Not able to deserialize the message: " + message);
                eSecond.printStackTrace();
            }
        }

        boolean isMessageWithParameters = msgWithParameters != null;
        SolaceParameters solaceParameters = null;

        if (isMessageWithParameters) {
            msg = msgWithParameters.getMessage();
            solaceParameters = new SolaceParameters();
            solaceParameters.setHost(msgWithParameters.getHost());
            solaceParameters.setVpnName(msgWithParameters.getVpnName());
            solaceParameters.setUserName(msgWithParameters.getUserName());
            solaceParameters.setPassword(msgWithParameters.getPassword());
        }

        String responseMessage;
        String topicName;
        Payload payload;
        String content;

        // Use the service to send the message
        if (msg != null) {

            // TODO: We have a redundancy: topic name is described in two different places.
            //  Should we get the topic name from the form via MessageWithParameters
            //  or from the message which the format is described at doc/how-to/09-solace-message.txt?
            //  The way it is implemented here is: try to get this info from the message first, if it
            //  fails, get it from the parameters.
            topicName = msg.getDestination();
            if ((topicName == null || topicName.trim().isEmpty()) && isMessageWithParameters) {
                topicName = msgWithParameters.getTopicName();
            }

            payload = msg.getPayload();
            content = payload.getContent();
            try {
                if (isMessageWithParameters) {
                    responseMessage = directPublisherServiceImpl.sendMessage(topicName, content, Optional.of(solaceParameters));
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
        } else {
            responseMessage = "Message is null";
            logger.log(Level.INFO, responseMessage);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseMessage);
        }
    }
}
