package org.orgname.solace.broker.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.orgname.solace.broker.api.dto.FilteredMessagesExportResponseDTO;
import org.orgname.solace.broker.api.dto.InnerMessageDTO;
import org.orgname.solace.broker.api.dto.MessageWrapperDTO;
import org.orgname.solace.broker.api.dto.PagedMessagesResponseDTO;
import org.orgname.solace.broker.api.dto.PayloadDTO;
import org.orgname.solace.broker.api.dto.PublishMessageResponseDTO;
import org.orgname.solace.broker.api.exception.ApiExceptionHandler;
import org.orgname.solace.broker.api.exception.BrokerConfigurationException;
import org.orgname.solace.broker.api.exception.BrokerConnectionException;
import org.orgname.solace.broker.api.exception.BrokerPublishFailureException;
import org.orgname.solace.broker.api.jpa.Message;
import org.orgname.solace.broker.api.jpa.Payload;
import org.orgname.solace.broker.api.jpa.PublishStatus;
import org.orgname.solace.broker.api.jpa.Property;
import org.orgname.solace.broker.api.service.Database;
import org.orgname.solace.broker.api.service.DirectPublisherService;
import org.orgname.solace.broker.api.service.MessageLifecycleSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MessageController.class)
@Import(ApiExceptionHandler.class)
class MessageControllerWebMvcTest {

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
        when(database.getAllMessages(0, 20, null, null, null, null, false, null, null, null, null, "createdAt", "desc"))
                .thenReturn(messagePageResponse(List.of(storedMessage("001", "solace/java/direct/system-01", "PERSISTENT", 3)), 0, 20, 1));

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
                .andExpect(jsonPath("$.lifecycleCounts.publishedCount").value(1))
                .andExpect(jsonPath("$.lifecycleCounts.failedCount").value(0))
                .andExpect(jsonPath("$.lifecycleCounts.pendingCount").value(0))
                .andExpect(jsonPath("$.lifecycleCounts.stalePendingCount").value(0))
                .andExpect(jsonPath("$.lifecycleCounts.retryableFailedCount").value(0))
                .andExpect(jsonPath("$.lifecycleCounts.nonRetryableFailedCount").value(0))
                .andExpect(jsonPath("$.items[0].stalePending").value(false))
                .andExpect(jsonPath("$.items[0].retrySupported").value(true))
                .andExpect(jsonPath("$.items[0].payload.type").value("binary"))
                .andExpect(jsonPath("$.items[0].payload.content").value("01001000 01100101 01101100"))
                .andExpect(jsonPath("$.items[0].properties.property01").value("value01"));
    }

    @Test
    void shouldExposeStalePendingMessagesInReadResponse() throws Exception {
        when(database.getAllMessages(0, 20, null, null, null, PublishStatus.PENDING, false, null, null, null, null, "createdAt", "desc"))
                .thenReturn(messagePageResponse(List.of(stalePendingStoredMessage("003", "solace/java/direct/system-03", "DIRECT", 2)), 0, 20, 1));

        mockMvc.perform(get("/api/v1/messages/all?publishStatus=PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].publishStatus").value("PENDING"))
                .andExpect(jsonPath("$.items[0].stalePending").value(true));
    }

    @Test
    void shouldReturnExplicitPageOfStoredMessages() throws Exception {
        when(database.getAllMessages(1, 1, null, null, null, null, false, null, null, null, null, "createdAt", "desc"))
                .thenReturn(messagePageResponse(List.of(storedMessage("002", "solace/java/direct/system-02", "DIRECT", 1)), 1, 1, 2));

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
    void shouldFilterStoredMessagesByDeliveryMode() throws Exception {
        when(database.getAllMessages(0, 20, null, "PERSISTENT", null, null, false, null, null, null, null, "createdAt", "desc"))
                .thenReturn(messagePageResponse(List.of(storedMessage("001", "solace/java/direct/system-01", "PERSISTENT", 3)), 0, 20, 1));

        mockMvc.perform(get("/api/v1/messages/all?deliveryMode=PERSISTENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].deliveryMode").value("PERSISTENT"));
    }

    @Test
    void shouldFilterStoredMessagesByPublishStatus() throws Exception {
        when(database.getAllMessages(0, 20, null, null, null, PublishStatus.FAILED, false, null, null, null, null, "createdAt", "desc"))
                .thenReturn(messagePageResponse(List.of(failedStoredMessage("002", "solace/java/direct/system-02", "DIRECT", 1)), 0, 20, 1));

        mockMvc.perform(get("/api/v1/messages/all?publishStatus=FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].publishStatus").value("FAILED"))
                .andExpect(jsonPath("$.items[0].failureReason").value("Failed to publish message to Solace broker"));
    }

    @Test
    void shouldRespectExplicitSortFieldAndDirection() throws Exception {
        when(database.getAllMessages(0, 20, null, null, null, null, false, null, null, null, null, "priority", "asc"))
                .thenReturn(messagePageResponse(List.of(
                        storedMessage("002", "solace/java/direct/system-02", "DIRECT", 1),
                        storedMessage("001", "solace/java/direct/system-01", "PERSISTENT", 3)), 0, 20, 2));

        mockMvc.perform(get("/api/v1/messages/all?sortBy=priority&sortDirection=asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].innerMessageId").value("002"))
                .andExpect(jsonPath("$.items[1].innerMessageId").value("001"));
    }

    @Test
    void shouldFilterStoredMessagesByCreatedAtRange() throws Exception {
        LocalDateTime createdAtFrom = LocalDateTime.parse("2026-04-20T00:00:00");
        LocalDateTime createdAtTo = LocalDateTime.parse("2026-04-20T23:59:59");

        when(database.getAllMessages(0, 20, null, null, null, null, false, createdAtFrom, createdAtTo, null, null, "createdAt", "desc"))
                .thenReturn(messagePageResponse(List.of(storedMessage("001", "solace/java/direct/system-01", "PERSISTENT", 3)), 0, 20, 1));

        mockMvc.perform(get("/api/v1/messages/all?createdAtFrom=2026-04-20T00:00:00&createdAtTo=2026-04-20T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].innerMessageId").value("001"));
    }

    @Test
    void shouldFilterStoredMessagesByStalePendingOnly() throws Exception {
        when(database.getAllMessages(0, 20, null, null, null, null, true, null, null, null, null, "createdAt", "desc"))
                .thenReturn(messagePageResponse(List.of(stalePendingStoredMessage("003", "solace/java/direct/system-03", "DIRECT", 2)), 0, 20, 1));

        mockMvc.perform(get("/api/v1/messages/all?stalePendingOnly=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].publishStatus").value("PENDING"))
                .andExpect(jsonPath("$.items[0].stalePending").value(true));
    }

    @Test
    void shouldExportFilteredStoredMessagesJsonContract() throws Exception {
        when(database.exportMessages("system-02", null, null, PublishStatus.FAILED, false, null, null, null, null, "createdAt", "desc"))
                .thenReturn(filteredMessagesExportResponse(List.of(failedStoredMessage("002", "solace/java/direct/system-02", "DIRECT", 1))));

        mockMvc.perform(get("/api/v1/messages/export?destination=system-02&publishStatus=FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filters.destination").value("system-02"))
                .andExpect(jsonPath("$.filters.publishStatus").value("FAILED"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.lifecycleCounts.failedCount").value(1))
                .andExpect(jsonPath("$.items[0].innerMessageId").value("002"))
                .andExpect(jsonPath("$.items[0].publishStatus").value("FAILED"));
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
    void shouldRejectInvalidPageRequest() throws Exception {
        mockMvc.perform(get("/api/v1/messages/all?page=-1&size=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("page must be greater than or equal to 0"))
                .andExpect(jsonPath("$.path").value("/api/v1/messages/all"));
    }

    @Test
    void shouldRejectOversizedPageRequest() throws Exception {
        mockMvc.perform(get("/api/v1/messages/all?size=101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("size must be less than or equal to 100"))
                .andExpect(jsonPath("$.path").value("/api/v1/messages/all"));
    }

    @Test
    void shouldRejectInvalidSortDirection() throws Exception {
        mockMvc.perform(get("/api/v1/messages/all?sortDirection=sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("sortDirection must be asc or desc"))
                .andExpect(jsonPath("$.path").value("/api/v1/messages/all"));
    }

    @Test
    void shouldRejectInvalidPublishStatus() throws Exception {
        mockMvc.perform(get("/api/v1/messages/all?publishStatus=UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("publishStatus must be one of PENDING, PUBLISHED, FAILED"))
                .andExpect(jsonPath("$.path").value("/api/v1/messages/all"));
    }

    @Test
    void shouldReturnCreatedForSuccessfulMessagePublish() throws Exception {
        MessageWrapperDTO wrapper = validWrapper();
        when(database.savePendingMessage(any(MessageWrapperDTO.class))).thenReturn(new Message() {{
            setId(1L);
        }});
        when(database.markMessagePublished(1L)).thenReturn(new Message());
        when(directPublisherService.sendMessage(eq("solace/java/direct/system-01"), eq("01001000 01100101 01101100"), any()))
                .thenReturn(new PublishMessageResponseDTO("solace/java/direct/system-01", "01001000 01100101 01101100"));

        mockMvc.perform(post("/api/v1/messages/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrapper)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.destination").value("solace/java/direct/system-01"))
                .andExpect(jsonPath("$.content").value("01001000 01100101 01101100"));
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
        when(database.savePendingMessage(any(MessageWrapperDTO.class))).thenReturn(new Message() {{
            setId(1L);
        }});
        when(database.markMessageFailed(1L, "Failed to publish message to Solace broker")).thenReturn(new Message());
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
    void shouldReturnInternalServerErrorWhenPublishSucceedsButDatabaseStateUpdateFails() throws Exception {
        MessageWrapperDTO wrapper = validWrapper();
        when(database.savePendingMessage(any(MessageWrapperDTO.class))).thenReturn(new Message() {{
            setId(1L);
        }});
        when(directPublisherService.sendMessage(eq("solace/java/direct/system-01"), eq("01001000 01100101 01101100"), any()))
                .thenReturn(new PublishMessageResponseDTO("solace/java/direct/system-01", "01001000 01100101 01101100"));
        doThrow(new IllegalStateException("database unavailable"))
                .when(database)
                .markMessagePublished(1L);

        mockMvc.perform(post("/api/v1/messages/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrapper)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Message was published to the broker but the database state could not be updated"));

        verify(database, never()).markMessageFailed(eq(1L), any());
    }

    @Test
    void shouldReturnInternalServerErrorForMissingBrokerConfiguration() throws Exception {
        MessageWrapperDTO wrapper = validWrapper();
        when(database.savePendingMessage(any(MessageWrapperDTO.class))).thenReturn(new Message() {{
            setId(1L);
        }});
        when(database.markMessageFailed(1L, "System environment variables SOLACE_CLOUD_HOST, SOLACE_CLOUD_VPN, SOLACE_CLOUD_USERNAME, SOLACE_CLOUD_PASSWORD are not set.")).thenReturn(new Message());
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
        when(database.savePendingMessage(any(MessageWrapperDTO.class))).thenReturn(new Message() {{
            setId(1L);
        }});
        when(database.markMessageFailed(1L, "Failed to connect to Solace broker")).thenReturn(new Message());
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

    @Test
    void shouldRetryFailedMessageSuccessfully() throws Exception {
        Message failedMessage = failedStoredMessage("002", "solace/java/direct/system-02", "DIRECT", 1);
        when(database.findMessageById(2L)).thenReturn(failedMessage);
        when(database.markMessagePending(2L)).thenReturn(failedMessage);
        when(database.markMessagePublished(2L)).thenReturn(failedMessage);
        when(directPublisherService.sendMessage(eq("solace/java/direct/system-02"), eq("01001000 01100101 01101100"), any()))
                .thenReturn(new PublishMessageResponseDTO("solace/java/direct/system-02", "01001000 01100101 01101100"));

        mockMvc.perform(post("/api/v1/messages/2/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destination").value("solace/java/direct/system-02"))
                .andExpect(jsonPath("$.content").value("01001000 01100101 01101100"));
    }

    @Test
    void shouldRejectRetryForNonFailedMessage() throws Exception {
        when(database.findMessageById(1L)).thenReturn(storedMessage("001", "solace/java/direct/system-01", "PERSISTENT", 3));

        mockMvc.perform(post("/api/v1/messages/1/retry"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only FAILED messages can be retried"));
    }

    @Test
    void shouldRejectRetryForNonRetryableFailedMessage() throws Exception {
        Message failedMessage = failedStoredMessage("002", "solace/java/direct/system-02", "DIRECT", 1);
        failedMessage.setRetrySupported(false);
        failedMessage.setRetryBlockedReason("Retries are supported only for messages published with server-side broker configuration.");
        when(database.findMessageById(2L)).thenReturn(failedMessage);

        mockMvc.perform(post("/api/v1/messages/2/retry"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only messages published with server-side broker configuration can be retried"));
    }

    @Test
    void shouldBulkRetryMessagesWithMixedResults() throws Exception {
        Message retryableFailedMessage = failedStoredMessage("002", "solace/java/direct/system-02", "DIRECT", 1);
        Message nonFailedMessage = storedMessage("001", "solace/java/direct/system-01", "PERSISTENT", 3);
        Message nonRetryableFailedMessage = failedStoredMessage("003", "solace/java/direct/system-03", "DIRECT", 2);
        nonRetryableFailedMessage.setRetrySupported(false);
        nonRetryableFailedMessage.setRetryBlockedReason("Retries are supported only for messages published with server-side broker configuration.");

        when(database.findMessageById(2L)).thenReturn(retryableFailedMessage);
        when(database.findMessageById(1L)).thenReturn(nonFailedMessage);
        when(database.findMessageById(3L)).thenReturn(nonRetryableFailedMessage);
        when(database.markMessagePending(2L)).thenReturn(retryableFailedMessage);
        when(database.markMessagePublished(2L)).thenReturn(retryableFailedMessage);
        when(directPublisherService.sendMessage(eq("solace/java/direct/system-02"), eq("01001000 01100101 01101100"), any()))
                .thenReturn(new PublishMessageResponseDTO("solace/java/direct/system-02", "01001000 01100101 01101100"));

        mockMvc.perform(post("/api/v1/messages/retry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"messageIds":[2,1,3]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequested").value(3))
                .andExpect(jsonPath("$.retriedSuccessfully").value(1))
                .andExpect(jsonPath("$.failedToRetry").value(0))
                .andExpect(jsonPath("$.skipped").value(2))
                .andExpect(jsonPath("$.results[0].outcome").value("RETRIED"))
                .andExpect(jsonPath("$.results[1].outcome").value("SKIPPED"))
                .andExpect(jsonPath("$.results[2].outcome").value("SKIPPED"));
    }

    @Test
    void shouldReturnBadRequestWhenBulkRetryRequestIsEmpty() throws Exception {
        mockMvc.perform(post("/api/v1/messages/retry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"messageIds":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("messageIds must contain at least one id"));
    }

    @Test
    void shouldReconcileStalePendingMessage() throws Exception {
        Message pendingMessage = stalePendingStoredMessage("003", "solace/java/direct/system-03", "DIRECT", 2);
        pendingMessage.setId(3L);
        Message reconciledMessage = stalePendingStoredMessage("003", "solace/java/direct/system-03", "DIRECT", 2);
        reconciledMessage.setId(3L);
        reconciledMessage.setPublishStatus(PublishStatus.FAILED);
        reconciledMessage.setFailureReason("Marked as FAILED after manual reconciliation of a stale PENDING message");

        when(database.findMessageById(3L)).thenReturn(pendingMessage);
        when(database.markMessageFailed(3L, "Marked as FAILED after manual reconciliation of a stale PENDING message")).thenReturn(reconciledMessage);

        mockMvc.perform(post("/api/v1/messages/3/reconcile-stale-pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.publishStatus").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("Marked as FAILED after manual reconciliation of a stale PENDING message"));
    }

    @Test
    void shouldRejectReconciliationForFreshPendingMessage() throws Exception {
        Message freshPendingMessage = storedMessage("003", "solace/java/direct/system-03", "DIRECT", 2);
        freshPendingMessage.setId(3L);
        freshPendingMessage.setPublishStatus(PublishStatus.PENDING);
        freshPendingMessage.setPublishedAt(null);
        freshPendingMessage.setCreatedAt(LocalDateTime.now());
        freshPendingMessage.setUpdatedAt(LocalDateTime.now());
        when(database.findMessageById(3L)).thenReturn(freshPendingMessage);

        mockMvc.perform(post("/api/v1/messages/3/reconcile-stale-pending"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only stale PENDING messages can be reconciled"));
    }

    @Test
    void shouldRejectReconciliationForNonPendingMessage() throws Exception {
        Message publishedMessage = storedMessage("001", "solace/java/direct/system-01", "PERSISTENT", 3);
        publishedMessage.setId(1L);
        when(database.findMessageById(1L)).thenReturn(publishedMessage);

        mockMvc.perform(post("/api/v1/messages/1/reconcile-stale-pending"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only PENDING messages can be reconciled"));
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

    private static Message storedMessage(String innerMessageId, String destination, String deliveryMode, int priority) {
        Message message = new Message();
        message.setId("001".equals(innerMessageId) ? 1L : 2L);
        message.setInnerMessageId(innerMessageId);
        message.setDestination(destination);
        message.setDeliveryMode(deliveryMode);
        message.setPriority(priority);
        message.setPublishStatus(PublishStatus.PUBLISHED);
        message.setRetrySupported(true);
        message.setRetryBlockedReason(null);

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

    private static Message failedStoredMessage(String innerMessageId, String destination, String deliveryMode, int priority) {
        Message message = storedMessage(innerMessageId, destination, deliveryMode, priority);
        message.setPublishStatus(PublishStatus.FAILED);
        message.setFailureReason("Failed to publish message to Solace broker");
        message.setPublishedAt(null);
        return message;
    }

    private static Message stalePendingStoredMessage(String innerMessageId, String destination, String deliveryMode, int priority) {
        Message message = storedMessage(innerMessageId, destination, deliveryMode, priority);
        LocalDateTime staleCreatedAt = LocalDateTime.now().minusMinutes(10);
        message.setPublishStatus(PublishStatus.PENDING);
        message.setFailureReason(null);
        message.setPublishedAt(null);
        message.setCreatedAt(staleCreatedAt);
        message.setUpdatedAt(staleCreatedAt);
        return message;
    }

    private static PagedMessagesResponseDTO messagePageResponse(List<Message> messages, int page, int size, long totalElements) {
        return PagedMessagesResponseDTO.fromMessages(
                new PageImpl<>(messages, PageRequest.of(page, size), totalElements),
                lifecycleCounts(messages)
        );
    }

    private static FilteredMessagesExportResponseDTO filteredMessagesExportResponse(List<Message> messages) {
        return FilteredMessagesExportResponseDTO.fromMessages(
                LocalDateTime.parse("2026-04-22T13:45:00"),
                new FilteredMessagesExportResponseDTO.FiltersDTO(
                        "system-02",
                        null,
                        null,
                        PublishStatus.FAILED,
                        false,
                        null,
                        null,
                        null,
                        null,
                        "createdAt",
                        "desc"
                ),
                lifecycleCounts(messages),
                messages
        );
    }

    private static PagedMessagesResponseDTO.LifecycleCountsDTO lifecycleCounts(List<Message> messages) {
        long publishedCount = messages.stream().filter(message -> message.getPublishStatus() == PublishStatus.PUBLISHED).count();
        long failedCount = messages.stream().filter(message -> message.getPublishStatus() == PublishStatus.FAILED).count();
        long pendingCount = messages.stream().filter(message -> message.getPublishStatus() == PublishStatus.PENDING).count();
        long stalePendingCount = messages.stream()
                .filter(message -> MessageLifecycleSupport.isStalePending(message.getPublishStatus(), message.getCreatedAt()))
                .count();
        long retryableFailedCount = messages.stream()
                .filter(message -> message.getPublishStatus() == PublishStatus.FAILED && message.isRetrySupported())
                .count();
        long nonRetryableFailedCount = messages.stream()
                .filter(message -> message.getPublishStatus() == PublishStatus.FAILED && !message.isRetrySupported())
                .count();
        return new PagedMessagesResponseDTO.LifecycleCountsDTO(
                publishedCount,
                failedCount,
                pendingCount,
                stalePendingCount,
                retryableFailedCount,
                nonRetryableFailedCount
        );
    }
}
