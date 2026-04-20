package org.orgname.solace.broker.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orgname.solace.broker.api.dto.InnerMessageDTO;
import org.orgname.solace.broker.api.dto.MessageWrapperDTO;
import org.orgname.solace.broker.api.dto.ParameterDTO;
import org.orgname.solace.broker.api.dto.PagedMessagesResponseDTO;
import org.orgname.solace.broker.api.dto.PayloadDTO;
import org.orgname.solace.broker.api.exception.ApiExceptionHandler;
import org.orgname.solace.broker.api.exception.BrokerPublishFailureException;
import org.orgname.solace.broker.api.jpa.Message;
import org.orgname.solace.broker.api.jpa.Payload;
import org.orgname.solace.broker.api.jpa.Property;
import org.orgname.solace.broker.api.service.Database;
import org.orgname.solace.broker.api.service.DirectPublisherService;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MessageControllerTest {

    private static final String MESSAGE_SENT = "{\"destination\":\"solace/java/direct/system-01\",\"content\":\"01001000 01100101 01101100\"}";

    private StubDatabase database;
    private StubDirectPublisherService directPublisherService;
    private MessageController controller;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        database = new StubDatabase();
        directPublisherService = new StubDirectPublisherService();
        controller = new MessageController(database, directPublisherService);
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldReturnAllMessagesFromRepository() throws Exception {
        Message first = storedMessage("001", "solace/java/direct/system-01");
        Message second = storedMessage("002", "solace/java/direct/system-02");
        database.storedMessages.add(first);
        database.storedMessages.add(second);

        PagedMessagesResponseDTO messages = controller.getAllMessages(0, 20);
        assertEquals(2, messages.getItems().size());
        assertEquals(2L, messages.getTotalElements());
        assertEquals(1, messages.getTotalPages());

        mockMvc.perform(get("/api/v1/messages/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.items[0].innerMessageId").value("001"))
                .andExpect(jsonPath("$.items[0].destination").value("solace/java/direct/system-01"))
                .andExpect(jsonPath("$.items[0].payload.type").value("binary"))
                .andExpect(jsonPath("$.items[0].properties[0].propertyKey").value("property01"))
                .andExpect(jsonPath("$.items[1].innerMessageId").value("002"))
                .andExpect(jsonPath("$.items[1].destination").value("solace/java/direct/system-02"));
    }

    @Test
    void shouldRespectExplicitPaginationParameters() throws Exception {
        database.storedMessages.add(storedMessage("001", "solace/java/direct/system-01"));
        database.storedMessages.add(storedMessage("002", "solace/java/direct/system-02"));

        mockMvc.perform(get("/api/v1/messages/all?page=1&size=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.items[0].innerMessageId").value("002"));
    }

    @Test
    void shouldRejectInvalidPaginationParameters() throws Exception {
        mockMvc.perform(get("/api/v1/messages/all?page=-1&size=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("page must be greater than or equal to 0"))
                .andExpect(jsonPath("$.path").value("/api/v1/messages/all"));
    }

    @Test
    void shouldSendMessageSuccessfully() throws Exception {
        MessageWrapperDTO wrapper = validWrapper();
        directPublisherService.response = MESSAGE_SENT;

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
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value("/api/v1/messages/message"))
                .andExpect(jsonPath("$.validationErrors.message").value("message is required"));
    }

    @Test
    void shouldReturnErrorMessageWhenPublisherFails() throws Exception {
        MessageWrapperDTO wrapper = validWrapper();
        directPublisherService.exception = new BrokerPublishFailureException("Failed to publish message to Solace broker", new RuntimeException("Client error"));

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
    void shouldReturnBadRequestWhenPublisherRejectsInput() throws Exception {
        MessageWrapperDTO wrapper = validWrapper();
        directPublisherService.illegalArgumentException = new IllegalArgumentException("Topic name cannot be empty");

        mockMvc.perform(post("/api/v1/messages/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrapper)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Topic name cannot be empty"))
                .andExpect(jsonPath("$.path").value("/api/v1/messages/message"));
    }

    @Test
    void shouldRejectMissingNestedMessageFields() throws Exception {
        MessageWrapperDTO wrapper = new MessageWrapperDTO();
        wrapper.setMessage(new InnerMessageDTO());

        mockMvc.perform(post("/api/v1/messages/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrapper)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.validationErrors['message.innerMessageId']").value("message.innerMessageId is required"))
                .andExpect(jsonPath("$.validationErrors['message.destination']").value("message.destination is required"))
                .andExpect(jsonPath("$.validationErrors['message.deliveryMode']").value("message.deliveryMode is required"))
                .andExpect(jsonPath("$.validationErrors['message.priority']").value("message.priority is required"))
                .andExpect(jsonPath("$.validationErrors['message.payload']").value("message.payload is required"));
    }

    @Test
    void shouldRejectBlankPayloadFields() throws Exception {
        MessageWrapperDTO wrapper = validWrapper();
        wrapper.getMessage().getPayload().setType(" ");
        wrapper.getMessage().getPayload().setContent(" ");

        mockMvc.perform(post("/api/v1/messages/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrapper)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.validationErrors['message.payload.type']").value("payload.type is required"))
                .andExpect(jsonPath("$.validationErrors['message.payload.content']").value("payload.content is required"));
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
        message.setId("001".equals(innerMessageId) ? 1L : 2L);
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

    private static final class StubDirectPublisherService implements DirectPublisherService {
        private String response;
        private IllegalArgumentException illegalArgumentException;
        private RuntimeException exception;

        @Override
        public String sendMessage(String topicName, String content, Optional<ParameterDTO> solaceParametersOptional) {
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
        public PagedMessagesResponseDTO getAllMessages(int page, int size) {
            int start = Math.min(page * size, storedMessages.size());
            int end = Math.min(start + size, storedMessages.size());
            List<Message> content = storedMessages.subList(start, end);
            return new PagedMessagesResponseDTO(new PageImpl<>(content, PageRequest.of(page, size), storedMessages.size()));
        }
    }
}
