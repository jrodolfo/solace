package org.orgname.solace.broker.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orgname.solace.broker.api.dto.InnerMessageDTO;
import org.orgname.solace.broker.api.dto.MessageWrapperDTO;
import org.orgname.solace.broker.api.dto.PayloadDTO;
import org.orgname.solace.broker.api.dto.PublishMessageResponseDTO;
import org.orgname.solace.broker.api.exception.BrokerPublishFailureException;
import org.orgname.solace.broker.api.jpa.DeliveryMode;
import org.orgname.solace.broker.api.jpa.PayloadType;
import org.orgname.solace.broker.api.jpa.PublishStatus;
import org.orgname.solace.broker.api.repository.MessageRepository;
import org.orgname.solace.broker.api.service.DirectPublisherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;
import java.sql.Timestamp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:message-api-contract-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.docker.compose.enabled=false"
})
@AutoConfigureMockMvc
class MessageApiContractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private DirectPublisherService directPublisherService;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
    }

    @Test
    void shouldPublishPersistAndReadBackNormalizedStoredMessage() throws Exception {
        MessageWrapperDTO wrapper = validWrapper();
        when(directPublisherService.sendMessage(
                eq("solace/java/direct/system-01"),
                eq("01001000 01100101 01101100"),
                eq(DeliveryMode.PERSISTENT),
                any(Optional.class)))
                .thenReturn(new PublishMessageResponseDTO("solace/java/direct/system-01", "01001000 01100101 01101100"));

        mockMvc.perform(post("/api/v1/messages/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrapper)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.destination").value("solace/java/direct/system-01"))
                .andExpect(jsonPath("$.content").value("01001000 01100101 01101100"));

        mockMvc.perform(get("/api/v1/messages/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].innerMessageId").value("001"))
                .andExpect(jsonPath("$.items[0].destination").value("solace/java/direct/system-01"))
                .andExpect(jsonPath("$.items[0].deliveryMode").value("PERSISTENT"))
                .andExpect(jsonPath("$.items[0].priority").value(3))
                .andExpect(jsonPath("$.items[0].publishStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.items[0].retrySupported").value(true))
                .andExpect(jsonPath("$.items[0].publishedAt").isNotEmpty())
                .andExpect(jsonPath("$.items[0].payload.type").value("BINARY"))
                .andExpect(jsonPath("$.items[0].payload.content").value("01001000 01100101 01101100"))
                .andExpect(jsonPath("$.items[0].properties.region").value("ca-east"))
                .andExpect(jsonPath("$.items[0].properties.source").value("integration-test"));

        verify(directPublisherService).sendMessage(
                eq("solace/java/direct/system-01"),
                eq("01001000 01100101 01101100"),
                eq(DeliveryMode.PERSISTENT),
                any(Optional.class));
    }

    @Test
    void shouldPersistFailedPublishAttemptWithFailureReason() throws Exception {
        MessageWrapperDTO wrapper = validWrapper();
        doThrow(new BrokerPublishFailureException("Failed to publish message to Solace broker", new RuntimeException("Client error")))
                .when(directPublisherService)
                .sendMessage(eq("solace/java/direct/system-01"), eq("01001000 01100101 01101100"), eq(DeliveryMode.PERSISTENT), any(Optional.class));

        mockMvc.perform(post("/api/v1/messages/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrapper)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Failed to publish message to Solace broker"));

        org.orgname.solace.broker.api.jpa.Message storedMessage = messageRepository.findAll().getFirst();
        org.junit.jupiter.api.Assertions.assertEquals(PublishStatus.FAILED, storedMessage.getPublishStatus());
        org.junit.jupiter.api.Assertions.assertEquals("Failed to publish message to Solace broker", storedMessage.getFailureReason());
        org.junit.jupiter.api.Assertions.assertNull(storedMessage.getPublishedAt());
    }

    @Test
    void shouldRetryFailedMessageAndUpdateLifecycle() throws Exception {
        MessageWrapperDTO wrapper = validWrapper();
        doThrow(new BrokerPublishFailureException("Failed to publish message to Solace broker", new RuntimeException("Client error")))
                .doReturn(new PublishMessageResponseDTO("solace/java/direct/system-01", "01001000 01100101 01101100"))
                .when(directPublisherService)
                .sendMessage(eq("solace/java/direct/system-01"), eq("01001000 01100101 01101100"), eq(DeliveryMode.PERSISTENT), any(Optional.class));

        mockMvc.perform(post("/api/v1/messages/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrapper)))
                .andExpect(status().isBadGateway());

        org.orgname.solace.broker.api.jpa.Message failedMessage = messageRepository.findAll().getFirst();
        mockMvc.perform(post("/api/v1/messages/{messageId}/retry", failedMessage.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destination").value("solace/java/direct/system-01"))
                .andExpect(jsonPath("$.content").value("01001000 01100101 01101100"));

        org.orgname.solace.broker.api.jpa.Message retriedMessage = messageRepository.findById(failedMessage.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(PublishStatus.PUBLISHED, retriedMessage.getPublishStatus());
        org.junit.jupiter.api.Assertions.assertNull(retriedMessage.getFailureReason());
        org.junit.jupiter.api.Assertions.assertNotNull(retriedMessage.getPublishedAt());
    }

    @Test
    void shouldBulkRetryMessagesAndReturnMixedResults() throws Exception {
        MessageWrapperDTO wrapper = validWrapper();
        doThrow(new BrokerPublishFailureException("Failed to publish message to Solace broker", new RuntimeException("Client error")))
                .doReturn(new PublishMessageResponseDTO("solace/java/direct/system-01", "01001000 01100101 01101100"))
                .when(directPublisherService)
                .sendMessage(eq("solace/java/direct/system-01"), eq("01001000 01100101 01101100"), eq(DeliveryMode.PERSISTENT), any(Optional.class));

        mockMvc.perform(post("/api/v1/messages/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrapper)))
                .andExpect(status().isBadGateway());

        org.orgname.solace.broker.api.jpa.Message failedMessage = messageRepository.findAll().getFirst();

        mockMvc.perform(post("/api/v1/messages/retry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"messageIds":[%d,999999]}
                                """.formatted(failedMessage.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequested").value(2))
                .andExpect(jsonPath("$.retriedSuccessfully").value(1))
                .andExpect(jsonPath("$.failedToRetry").value(1))
                .andExpect(jsonPath("$.skipped").value(0))
                .andExpect(jsonPath("$.results[0].messageId").value(failedMessage.getId()))
                .andExpect(jsonPath("$.results[0].outcome").value("RETRIED"))
                .andExpect(jsonPath("$.results[1].messageId").value(999999))
                .andExpect(jsonPath("$.results[1].outcome").value("FAILED"));

        org.orgname.solace.broker.api.jpa.Message retriedMessage = messageRepository.findById(failedMessage.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(PublishStatus.PUBLISHED, retriedMessage.getPublishStatus());
        org.junit.jupiter.api.Assertions.assertNull(retriedMessage.getFailureReason());
        org.junit.jupiter.api.Assertions.assertNotNull(retriedMessage.getPublishedAt());
    }

    @Test
    void shouldReconcileStalePendingMessageAndPersistFailureReason() throws Exception {
        MessageWrapperDTO wrapper = validWrapper();
        when(directPublisherService.sendMessage(
                eq("solace/java/direct/system-01"),
                eq("01001000 01100101 01101100"),
                eq(DeliveryMode.PERSISTENT),
                any(Optional.class)))
                .thenReturn(new PublishMessageResponseDTO("solace/java/direct/system-01", "01001000 01100101 01101100"));

        mockMvc.perform(post("/api/v1/messages/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrapper)))
                .andExpect(status().isCreated());

        org.orgname.solace.broker.api.jpa.Message pendingMessage = messageRepository.findAll().getFirst();
        LocalDateTime staleTimestamp = LocalDateTime.now().minusMinutes(10);
        jdbcTemplate.update(
                "update message set publish_status = ?, published_at = null, failure_reason = null, created_at = ?, updated_at = ? where id = ?",
                PublishStatus.PENDING.name(),
                Timestamp.valueOf(staleTimestamp),
                Timestamp.valueOf(staleTimestamp),
                pendingMessage.getId()
        );

        mockMvc.perform(post("/api/v1/messages/{messageId}/reconcile-stale-pending", pendingMessage.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishStatus").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("Marked as FAILED after manual reconciliation of a stale PENDING message"));

        org.orgname.solace.broker.api.jpa.Message reconciledMessage = messageRepository.findById(pendingMessage.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(PublishStatus.FAILED, reconciledMessage.getPublishStatus());
        org.junit.jupiter.api.Assertions.assertEquals("Marked as FAILED after manual reconciliation of a stale PENDING message", reconciledMessage.getFailureReason());
        org.junit.jupiter.api.Assertions.assertNull(reconciledMessage.getPublishedAt());
    }

    private static MessageWrapperDTO validWrapper() {
        PayloadDTO payload = new PayloadDTO();
        payload.setType(PayloadType.BINARY);
        payload.setContent("01001000 01100101 01101100");

        InnerMessageDTO message = new InnerMessageDTO();
        message.setInnerMessageId("001");
        message.setDestination("solace/java/direct/system-01");
        message.setDeliveryMode(DeliveryMode.PERSISTENT);
        message.setPriority(3);
        message.setProperties(Map.of(
                "region", "ca-east",
                "source", "integration-test"));
        message.setPayload(payload);

        MessageWrapperDTO wrapper = new MessageWrapperDTO();
        wrapper.setMessage(message);
        return wrapper;
    }
}
