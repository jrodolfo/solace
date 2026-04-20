package org.orgname.solace.broker.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orgname.solace.broker.api.dto.InnerMessageDTO;
import org.orgname.solace.broker.api.dto.MessageWrapperDTO;
import org.orgname.solace.broker.api.dto.ParameterDTO;
import org.orgname.solace.broker.api.dto.PayloadDTO;
import org.orgname.solace.broker.api.exception.ApiExceptionHandler;
import org.orgname.solace.broker.api.jpa.Message;
import org.orgname.solace.broker.api.service.Database;
import org.orgname.solace.broker.api.service.DirectPublisherServiceImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ControllerTest {

    private static final String MESSAGE_SENT = "{\"destination\":\"solace/java/direct/system-01\",\"content\":\"01001000 01100101 01101100\"}";

    private StubDatabase database;
    private StubDirectPublisherService directPublisherServiceImpl;
    private Controller controller;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        database = new StubDatabase();
        directPublisherServiceImpl = new StubDirectPublisherService();
        controller = new Controller(database, directPublisherServiceImpl);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldReturnAllMessagesFromRepository() throws Exception {
        Message first = new Message();
        first.setInnerMessageId("001");
        Message second = new Message();
        second.setInnerMessageId("002");
        database.storedMessages.add(first);
        database.storedMessages.add(second);

        Iterable<Message> messages = controller.getAllMessages();
        assertIterableEquals(List.of(first, second), messages);

        mockMvc.perform(get("/api/v1/messages/all"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldSendMessageSuccessfully() throws Exception {
        MessageWrapperDTO wrapper = validWrapper();
        directPublisherServiceImpl.response = MESSAGE_SENT;

        mockMvc.perform(post("/api/v1/messages/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrapper)))
                .andExpect(status().isCreated())
                .andExpect(content().string(MESSAGE_SENT));
    }

    @Test
    void shouldRejectNullMessagePayload() throws Exception {
        MessageWrapperDTO wrapper = new MessageWrapperDTO();

        mockMvc.perform(post("/api/v1/messages/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrapper)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Message is null"))
                .andExpect(jsonPath("$.path").value("/api/v1/messages/message"));
    }

    @Test
    void shouldReturnErrorMessageWhenPublisherFails() throws Exception {
        MessageWrapperDTO wrapper = validWrapper();
        directPublisherServiceImpl.exception = new RuntimeException("Client error");

        mockMvc.perform(post("/api/v1/messages/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrapper)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.error").value("Bad Gateway"))
                .andExpect(jsonPath("$.message").value("Client error"))
                .andExpect(jsonPath("$.path").value("/api/v1/messages/message"));
    }

    @Test
    void shouldReturnBadRequestWhenPublisherRejectsInput() throws Exception {
        MessageWrapperDTO wrapper = validWrapper();
        directPublisherServiceImpl.illegalArgumentException = new IllegalArgumentException("Topic name cannot be empty");

        mockMvc.perform(post("/api/v1/messages/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrapper)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Topic name cannot be empty"))
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

    private static final class StubDirectPublisherService extends DirectPublisherServiceImpl {
        private String response;
        private IllegalArgumentException illegalArgumentException;
        private RuntimeException exception;

        @Override
        public String sendMessage(String topicName, String content, Optional<ParameterDTO> solaceParametersOptional) throws Exception {
            if (illegalArgumentException != null) {
                throw illegalArgumentException;
            }
            if (exception != null) {
                throw exception;
            }
            return response;
        }
    }

    private static final class StubDatabase implements Database {
        private final List<Message> storedMessages = new ArrayList<>();

        @Override
        public Message saveMessage(MessageWrapperDTO wrapper) {
            Message message = new Message();
            message.setInnerMessageId(wrapper.getMessage().getInnerMessageId());
            message.setDestination(wrapper.getMessage().getDestination());
            storedMessages.add(message);
            return message;
        }

        @Override
        public Iterable<Message> getAllMessages() {
            return List.copyOf(storedMessages);
        }
    }
}
