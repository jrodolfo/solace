package org.orgname.solace.broker.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orgname.solace.broker.api.dto.InnerMessageDTO;
import org.orgname.solace.broker.api.dto.MessageWrapperDTO;
import org.orgname.solace.broker.api.dto.ParameterDTO;
import org.orgname.solace.broker.api.dto.PagedMessagesResponseDTO;
import org.orgname.solace.broker.api.dto.PayloadDTO;
import org.orgname.solace.broker.api.dto.PublishMessageResponseDTO;
import org.orgname.solace.broker.api.exception.ApiExceptionHandler;
import org.orgname.solace.broker.api.exception.BrokerPublishFailureException;
import org.orgname.solace.broker.api.jpa.Message;
import org.orgname.solace.broker.api.jpa.Payload;
import org.orgname.solace.broker.api.jpa.PublishStatus;
import org.orgname.solace.broker.api.jpa.Property;
import org.orgname.solace.broker.api.service.Database;
import org.orgname.solace.broker.api.service.DirectPublisherService;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MessageControllerTest {

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
        Message first = storedMessage("001", "solace/java/direct/system-01", "PERSISTENT", 3, "2026-04-20T10:00:00");
        Message second = storedMessage("002", "solace/java/direct/system-02", "DIRECT", 1, "2026-04-19T10:00:00");
        database.storedMessages.add(first);
        database.storedMessages.add(second);

        PagedMessagesResponseDTO messages = controller.getAllMessages(0, 20, null, null, null, null, null, null, null, null, "createdAt", "desc");
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
                .andExpect(jsonPath("$.items[0].properties.property01").value("value01"))
                .andExpect(jsonPath("$.items[1].innerMessageId").value("002"))
                .andExpect(jsonPath("$.items[1].destination").value("solace/java/direct/system-02"));
    }

    @Test
    void shouldRespectExplicitPaginationParameters() throws Exception {
        database.storedMessages.add(storedMessage("001", "solace/java/direct/system-01", "PERSISTENT", 3, "2026-04-20T10:00:00"));
        database.storedMessages.add(storedMessage("002", "solace/java/direct/system-02", "DIRECT", 1, "2026-04-19T10:00:00"));

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
    void shouldFilterMessagesByDestination() throws Exception {
        database.storedMessages.add(storedMessage("001", "solace/java/direct/system-01", "PERSISTENT", 3, "2026-04-20T10:00:00"));
        database.storedMessages.add(storedMessage("002", "solace/java/direct/system-02", "DIRECT", 1, "2026-04-19T10:00:00"));

        mockMvc.perform(get("/api/v1/messages/all?destination=system-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].innerMessageId").value("002"))
                .andExpect(jsonPath("$.items[0].destination").value("solace/java/direct/system-02"));
    }

    @Test
    void shouldFilterMessagesByPublishStatus() throws Exception {
        database.storedMessages.add(storedMessage("001", "solace/java/direct/system-01", "PERSISTENT", 3, "2026-04-20T10:00:00"));
        Message failedMessage = storedMessage("002", "solace/java/direct/system-02", "DIRECT", 1, "2026-04-19T10:00:00");
        failedMessage.setPublishStatus(PublishStatus.FAILED);
        failedMessage.setFailureReason("Failed to publish message to Solace broker");
        failedMessage.setPublishedAt(null);
        database.storedMessages.add(failedMessage);

        mockMvc.perform(get("/api/v1/messages/all?publishStatus=FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].innerMessageId").value("002"))
                .andExpect(jsonPath("$.items[0].publishStatus").value("FAILED"));
    }

    @Test
    void shouldFilterMessagesByCreatedAtRange() throws Exception {
        database.storedMessages.add(storedMessage("001", "solace/java/direct/system-01", "PERSISTENT", 3, "2026-04-20T10:00:00"));
        database.storedMessages.add(storedMessage("002", "solace/java/direct/system-02", "DIRECT", 1, "2026-04-19T10:00:00"));

        mockMvc.perform(get("/api/v1/messages/all?createdAtFrom=2026-04-20T00:00:00&createdAtTo=2026-04-20T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].innerMessageId").value("001"));
    }

    @Test
    void shouldRejectInvalidCreatedAtFromDateTime() throws Exception {
        mockMvc.perform(get("/api/v1/messages/all?createdAtFrom=not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("createdAtFrom must be a valid ISO-8601 date-time"))
                .andExpect(jsonPath("$.path").value("/api/v1/messages/all"));
    }

    @Test
    void shouldRespectExplicitSortingParameters() throws Exception {
        database.storedMessages.add(storedMessage("001", "solace/java/direct/system-01", "PERSISTENT", 3, "2026-04-20T10:00:00"));
        database.storedMessages.add(storedMessage("002", "solace/java/direct/system-02", "DIRECT", 1, "2026-04-19T10:00:00"));

        mockMvc.perform(get("/api/v1/messages/all?sortBy=priority&sortDirection=asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].innerMessageId").value("002"))
                .andExpect(jsonPath("$.items[1].innerMessageId").value("001"));
    }

    @Test
    void shouldRejectInvalidPaginationAndSortParameters() throws Exception {
        mockMvc.perform(get("/api/v1/messages/all?page=-1&size=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("page must be greater than or equal to 0"))
                .andExpect(jsonPath("$.path").value("/api/v1/messages/all"));
    }

    @Test
    void shouldRejectOversizedPageRequest() throws Exception {
        mockMvc.perform(get("/api/v1/messages/all?size=101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("size must be less than or equal to 100"))
                .andExpect(jsonPath("$.path").value("/api/v1/messages/all"));
    }

    @Test
    void shouldRejectInvalidSortField() throws Exception {
        mockMvc.perform(get("/api/v1/messages/all?sortBy=updatedAt"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("sortBy must be one of createdAt, priority, destination, innerMessageId"))
                .andExpect(jsonPath("$.path").value("/api/v1/messages/all"));
    }

    @Test
    void shouldRejectInvalidPublishStatus() throws Exception {
        mockMvc.perform(get("/api/v1/messages/all?publishStatus=done"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("publishStatus must be one of PENDING, PUBLISHED, FAILED"))
                .andExpect(jsonPath("$.path").value("/api/v1/messages/all"));
    }

    @Test
    void shouldSendMessageSuccessfully() throws Exception {
        MessageWrapperDTO wrapper = validWrapper();
        directPublisherService.response = new PublishMessageResponseDTO("solace/java/direct/system-01", "01001000 01100101 01101100");

        mockMvc.perform(post("/api/v1/messages/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrapper)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.destination").value("solace/java/direct/system-01"))
                .andExpect(jsonPath("$.content").value("01001000 01100101 01101100"));

        assertEquals(PublishStatus.PUBLISHED, database.lastSavedMessage.getPublishStatus());
        assertEquals(null, database.lastSavedMessage.getFailureReason());
        org.junit.jupiter.api.Assertions.assertNotNull(database.lastSavedMessage.getPublishedAt());
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

        assertEquals(PublishStatus.FAILED, database.lastSavedMessage.getPublishStatus());
        assertEquals("Failed to publish message to Solace broker", database.lastSavedMessage.getFailureReason());
        assertEquals(null, database.lastSavedMessage.getPublishedAt());
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

        assertEquals(PublishStatus.FAILED, database.lastSavedMessage.getPublishStatus());
        assertEquals("Topic name cannot be empty", database.lastSavedMessage.getFailureReason());
    }

    @Test
    void shouldRetryFailedMessageSuccessfully() throws Exception {
        Message failedMessage = storedMessage("002", "solace/java/direct/system-02", "DIRECT", 1, "2026-04-19T10:00:00");
        failedMessage.setPublishStatus(PublishStatus.FAILED);
        failedMessage.setFailureReason("Failed to publish message to Solace broker");
        failedMessage.setPublishedAt(null);
        database.storedMessages.add(failedMessage);
        directPublisherService.response = new PublishMessageResponseDTO("solace/java/direct/system-02", "01001000 01100101 01101100");

        mockMvc.perform(post("/api/v1/messages/2/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destination").value("solace/java/direct/system-02"))
                .andExpect(jsonPath("$.content").value("01001000 01100101 01101100"));

        assertEquals(PublishStatus.PUBLISHED, failedMessage.getPublishStatus());
        assertEquals(null, failedMessage.getFailureReason());
        org.junit.jupiter.api.Assertions.assertNotNull(failedMessage.getPublishedAt());
    }

    @Test
    void shouldRejectRetryForNonFailedMessage() throws Exception {
        database.storedMessages.add(storedMessage("001", "solace/java/direct/system-01", "PERSISTENT", 3, "2026-04-20T10:00:00"));

        mockMvc.perform(post("/api/v1/messages/1/retry"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only FAILED messages can be retried"));
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

    private static Message storedMessage(String innerMessageId, String destination, String deliveryMode, int priority, String createdAt) {
        Message message = new Message();
        message.setId("001".equals(innerMessageId) ? 1L : 2L);
        message.setInnerMessageId(innerMessageId);
        message.setDestination(destination);
        message.setDeliveryMode(deliveryMode);
        message.setPriority(priority);
        message.setPublishStatus(PublishStatus.PUBLISHED);
        message.setFailureReason(null);
        message.setPublishedAt(java.time.LocalDateTime.parse(createdAt));
        message.setCreatedAt(java.time.LocalDateTime.parse(createdAt));
        message.setUpdatedAt(java.time.LocalDateTime.parse(createdAt));

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
        private PublishMessageResponseDTO response;
        private IllegalArgumentException illegalArgumentException;
        private RuntimeException exception;

        @Override
        public PublishMessageResponseDTO sendMessage(String topicName, String content, Optional<ParameterDTO> solaceParametersOptional) {
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
        private Message lastSavedMessage;

        @Override
        public Message savePendingMessage(MessageWrapperDTO wrapper) {
            Message message = new Message();
            message.setId((long) (storedMessages.size() + 1));
            message.setInnerMessageId(wrapper.getMessage().getInnerMessageId());
            message.setDestination(wrapper.getMessage().getDestination());
            message.setDeliveryMode(wrapper.getMessage().getDeliveryMode());
            message.setPriority(wrapper.getMessage().getPriority());
            message.setPublishStatus(PublishStatus.PENDING);
            storedMessages.add(message);
            lastSavedMessage = message;
            return message;
        }

        @Override
        public Message markMessagePublished(Long messageId) {
            Message message = getRequiredMessage(messageId);
            message.setPublishStatus(PublishStatus.PUBLISHED);
            message.setFailureReason(null);
            message.setPublishedAt(java.time.LocalDateTime.now());
            return message;
        }

        @Override
        public Message findMessageById(Long messageId) {
            return getRequiredMessage(messageId);
        }

        @Override
        public Message markMessagePending(Long messageId) {
            Message message = getRequiredMessage(messageId);
            message.setPublishStatus(PublishStatus.PENDING);
            message.setFailureReason(null);
            message.setPublishedAt(null);
            return message;
        }

        @Override
        public Message markMessageFailed(Long messageId, String failureReason) {
            Message message = getRequiredMessage(messageId);
            message.setPublishStatus(PublishStatus.FAILED);
            message.setFailureReason(failureReason);
            message.setPublishedAt(null);
            return message;
        }

        @Override
        public PagedMessagesResponseDTO getAllMessages(
                int page,
                int size,
                String destination,
                String deliveryMode,
                String innerMessageId,
                PublishStatus publishStatus,
                LocalDateTime createdAtFrom,
                LocalDateTime createdAtTo,
                LocalDateTime publishedAtFrom,
                LocalDateTime publishedAtTo,
                String sortBy,
                String sortDirection) {
            List<Message> filteredMessages = storedMessages.stream()
                    .filter(message -> matches(message.getDestination(), destination))
                    .filter(message -> matches(message.getDeliveryMode(), deliveryMode))
                    .filter(message -> matches(message.getInnerMessageId(), innerMessageId))
                    .filter(message -> publishStatus == null || publishStatus == message.getPublishStatus())
                    .filter(message -> matchesOnOrAfter(message.getCreatedAt(), createdAtFrom))
                    .filter(message -> matchesOnOrBefore(message.getCreatedAt(), createdAtTo))
                    .filter(message -> matchesOnOrAfter(message.getPublishedAt(), publishedAtFrom))
                    .filter(message -> matchesOnOrBefore(message.getPublishedAt(), publishedAtTo))
                    .sorted(comparatorFor(sortBy, sortDirection))
                    .collect(Collectors.toList());

            int start = Math.min(page * size, filteredMessages.size());
            int end = Math.min(start + size, filteredMessages.size());
            List<Message> content = filteredMessages.subList(start, end);
            return PagedMessagesResponseDTO.fromMessages(new PageImpl<>(content, PageRequest.of(page, size), filteredMessages.size()));
        }

        private Message getRequiredMessage(Long messageId) {
            return storedMessages.stream()
                    .filter(message -> messageId.equals(message.getId()))
                    .findFirst()
                    .orElseThrow();
        }

        private static boolean matches(String actualValue, String filterValue) {
            return filterValue == null || actualValue.toLowerCase().contains(filterValue.toLowerCase());
        }

        private static boolean matchesOnOrAfter(LocalDateTime actualValue, LocalDateTime lowerBound) {
            return lowerBound == null || (actualValue != null && !actualValue.isBefore(lowerBound));
        }

        private static boolean matchesOnOrBefore(LocalDateTime actualValue, LocalDateTime upperBound) {
            return upperBound == null || (actualValue != null && !actualValue.isAfter(upperBound));
        }

        private static Comparator<Message> comparatorFor(String sortBy, String sortDirection) {
            Comparator<Message> comparator = switch (sortBy) {
                case "priority" -> Comparator.comparing(Message::getPriority);
                case "destination" -> Comparator.comparing(Message::getDestination, String.CASE_INSENSITIVE_ORDER);
                case "innerMessageId" -> Comparator.comparing(Message::getInnerMessageId, String.CASE_INSENSITIVE_ORDER);
                default -> Comparator.comparing(Message::getCreatedAt);
            };
            return "asc".equalsIgnoreCase(sortDirection) ? comparator : comparator.reversed();
        }
    }
}
