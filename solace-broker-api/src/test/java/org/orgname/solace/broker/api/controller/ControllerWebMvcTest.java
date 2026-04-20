package org.orgname.solace.broker.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.orgname.solace.broker.api.dto.InnerMessageDTO;
import org.orgname.solace.broker.api.dto.MessageWrapperDTO;
import org.orgname.solace.broker.api.dto.ParameterDTO;
import org.orgname.solace.broker.api.dto.PayloadDTO;
import org.orgname.solace.broker.api.exception.ApiExceptionHandler;
import org.orgname.solace.broker.api.jpa.Message;
import org.orgname.solace.broker.api.service.Database;
import org.orgname.solace.broker.api.service.DirectPublisherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = Controller.class)
@Import(ApiExceptionHandler.class)
class ControllerWebMvcTest {

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
        doThrow(new RuntimeException("Client error"))
                .when(directPublisherService)
                .sendMessage(eq("solace/java/direct/system-01"), eq("01001000 01100101 01101100"), any());

        mockMvc.perform(post("/api/v1/messages/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrapper)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.error").value("Bad Gateway"))
                .andExpect(jsonPath("$.message").value("Client error"))
                .andExpect(jsonPath("$.path").value("/api/v1/messages/message"));
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
}
