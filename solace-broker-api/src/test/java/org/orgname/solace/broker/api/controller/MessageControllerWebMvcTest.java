package org.orgname.solace.broker.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.orgname.solace.broker.api.dto.InnerMessageDTO;
import org.orgname.solace.broker.api.dto.MessageWrapperDTO;
import org.orgname.solace.broker.api.dto.PayloadDTO;
import org.orgname.solace.broker.api.exception.ApiExceptionHandler;
import org.orgname.solace.broker.api.exception.BrokerConfigurationException;
import org.orgname.solace.broker.api.exception.BrokerConnectionException;
import org.orgname.solace.broker.api.exception.BrokerPublishFailureException;
import org.orgname.solace.broker.api.jpa.Message;
import org.orgname.solace.broker.api.jpa.Payload;
import org.orgname.solace.broker.api.jpa.Property;
import org.orgname.solace.broker.api.service.Database;
import org.orgname.solace.broker.api.service.DirectPublisherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MessageController.class)
@Import(ApiExceptionHandler.class)
class MessageControllerWebMvcTest {

    private static final String MESSAGE_SENT = "{\"destination\":\"solace/java/direct/system-01\",\"content\":\"01001000 01100101 01101100\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private Database database;

    @MockBean
    private DirectPublisherService directPublisherService;

    @Test
    void shouldReturnStoredMessagesJsonContract() throws Exception {
        when(database.getAllMessages()).thenReturn(List.of(storedMessage("001", "solace/java/direct/system-01")));

        mockMvc.perform(get("/api/v1/messages/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].innerMessageId").value("001"))
                .andExpect(jsonPath("$[0].destination").value("solace/java/direct/system-01"))
                .andExpect(jsonPath("$[0].deliveryMode").value("PERSISTENT"))
                .andExpect(jsonPath("$[0].priority").value(3))
                .andExpect(jsonPath("$[0].payload.type").value("binary"))
                .andExpect(jsonPath("$[0].payload.content").value("01001000 01100101 01101100"))
                .andExpect(jsonPath("$[0].properties[0].propertyKey").value("property01"))
                .andExpect(jsonPath("$[0].properties[0].propertyValue").value("value01"));
    }

    @Test
    void shouldReturnCreatedForSuccessfulMessagePublish() throws Exception {
        MessageWrapperDTO wrapper = validWrapper();
        when(database.saveMessage(any(MessageWrapperDTO.class))).thenReturn(new Message());
        when(directPublisherService.sendMessage(eq("solace/java/direct/system-01"), eq("01001000 01100101 01101100"), any()))
                .thenReturn(MESSAGE_SENT);

        mockMvc.perform(post("/api/v1/messages/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrapper)))
                .andExpect(status().isCreated())
                .andExpect(content().string(MESSAGE_SENT));
    }

    @Test
    void shouldReturnTypedValidationErrorForInvalidPayload() throws Exception {
        MessageWrapperDTO wrapper = new MessageWrapperDTO();
        wrapper.setMessage(new InnerMessageDTO());

        mockMvc.perform(post("/api/v1/messages/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrapper)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.validationErrors['message.innerMessageId']").value("message.innerMessageId is required"));
    }

    @Test
    void shouldReturnTypedDownstreamFailureResponse() throws Exception {
        MessageWrapperDTO wrapper = validWrapper();
        when(database.saveMessage(any(MessageWrapperDTO.class))).thenReturn(new Message());
        doThrow(new BrokerPublishFailureException("Failed to publish message to Solace broker", new RuntimeException("Client error")))
                .when(directPublisherService)
                .sendMessage(eq("solace/java/direct/system-01"), eq("01001000 01100101 01101100"), any());

        mockMvc.perform(post("/api/v1/messages/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrapper)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.error").value("Bad Gateway"))
                .andExpect(jsonPath("$.message").value("Failed to publish message to Solace broker"))
                .andExpect(jsonPath("$.path").value("/api/v1/messages/message"));
    }

    @Test
    void shouldReturnInternalServerErrorForMissingBrokerConfiguration() throws Exception {
        MessageWrapperDTO wrapper = validWrapper();
        when(database.saveMessage(any(MessageWrapperDTO.class))).thenReturn(new Message());
        doThrow(new BrokerConfigurationException("System environment variables SOLACE_CLOUD_HOST, SOLACE_CLOUD_VPN, SOLACE_CLOUD_USERNAME, SOLACE_CLOUD_PASSWORD are not set."))
                .when(directPublisherService)
                .sendMessage(eq("solace/java/direct/system-01"), eq("01001000 01100101 01101100"), any());

        mockMvc.perform(post("/api/v1/messages/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrapper)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }

    @Test
    void shouldReturnServiceUnavailableForBrokerConnectionFailure() throws Exception {
        MessageWrapperDTO wrapper = validWrapper();
        when(database.saveMessage(any(MessageWrapperDTO.class))).thenReturn(new Message());
        doThrow(new BrokerConnectionException("Failed to connect to Solace broker", new RuntimeException("connect failed")))
                .when(directPublisherService)
                .sendMessage(eq("solace/java/direct/system-01"), eq("01001000 01100101 01101100"), any());

        mockMvc.perform(post("/api/v1/messages/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrapper)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.message").value("Failed to connect to Solace broker"));
    }

    private static MessageWrapperDTO validWrapper() {
        PayloadDTO payload = new PayloadDTO();
        payload.setType("binary");
        payload.setContent("01001000 01100101 01101100");

        InnerMessageDTO message = new InnerMessageDTO();
        message.setInnerMessageId("001");
        message.setDestination("solace/java/direct/system-01");
        message.setDeliveryMode("PERSISTENT");
        message.setPriority(3);
        message.setPayload(payload);

        MessageWrapperDTO wrapper = new MessageWrapperDTO();
        wrapper.setUserName("solace-cloud-client");
        wrapper.setPassword("super-difficult");
        wrapper.setHost("wss://example.messaging.solace.cloud:443");
        wrapper.setVpnName("my-solace-broker-on-aws");
        wrapper.setMessage(message);
        return wrapper;
    }

    private static Message storedMessage(String innerMessageId, String destination) {
        Message message = new Message();
        message.setId(1L);
        message.setInnerMessageId(innerMessageId);
        message.setDestination(destination);
        message.setDeliveryMode("PERSISTENT");
        message.setPriority(3);

        Payload payload = new Payload();
        payload.setId(20L);
        payload.setType("binary");
        payload.setContent("01001000 01100101 01101100");
        payload.setMessage(message);
        message.setPayload(payload);

        Property property = new Property();
        property.setId(10L);
        property.setPropertyKey("property01");
        property.setPropertyValue("value01");
        property.setMessage(message);
        message.setProperties(List.of(property));
        return message;
    }
}
