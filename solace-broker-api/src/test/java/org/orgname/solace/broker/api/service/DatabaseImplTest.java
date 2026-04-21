package org.orgname.solace.broker.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orgname.solace.broker.api.dto.InnerMessageDTO;
import org.orgname.solace.broker.api.dto.MessageWrapperDTO;
import org.orgname.solace.broker.api.dto.PayloadDTO;
import org.orgname.solace.broker.api.jpa.Message;
import org.orgname.solace.broker.api.jpa.PublishStatus;
import org.orgname.solace.broker.api.repository.MessageRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseImplTest {

    private MessageRepository messageRepository;
    private DatabaseImpl database;

    @BeforeEach
    void setUp() {
        messageRepository = mock(MessageRepository.class);
        database = new DatabaseImpl(messageRepository);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            if (message.getId() == null) {
                message.setId(1L);
            }
            return message;
        });
        when(messageRepository.markPublished(anyLong(), any(PublishStatus.class), any(LocalDateTime.class))).thenAnswer(invocation -> {
            savedMessageReference.setPublishStatus(invocation.getArgument(1));
            savedMessageReference.setFailureReason(null);
            savedMessageReference.setPublishedAt(invocation.getArgument(2));
            return 1;
        });
        when(messageRepository.markPending(anyLong(), any(PublishStatus.class))).thenAnswer(invocation -> {
            savedMessageReference.setPublishStatus(invocation.getArgument(1));
            savedMessageReference.setFailureReason(null);
            savedMessageReference.setPublishedAt(null);
            return 1;
        });
        when(messageRepository.markFailed(anyLong(), any(PublishStatus.class), anyString())).thenAnswer(invocation -> {
            savedMessageReference.setPublishStatus(invocation.getArgument(1));
            savedMessageReference.setFailureReason(invocation.getArgument(2));
            savedMessageReference.setPublishedAt(null);
            return 1;
        });
        when(messageRepository.findById(1L)).thenAnswer(invocation -> Optional.of(savedMessageReference));
    }

    private Message savedMessageReference;

    @Test
    void shouldPersistMessageGraphWithoutSensitiveConnectionParameters() {
        MessageWrapperDTO wrapper = validWrapper();

        Message savedMessage = database.savePendingMessage(wrapper);
        savedMessageReference = savedMessage;

        assertEquals("001", savedMessage.getInnerMessageId());
        assertEquals("solace/java/direct/system-01", savedMessage.getDestination());
        assertEquals("PERSISTENT", savedMessage.getDeliveryMode());
        assertEquals(3, savedMessage.getPriority());
        assertEquals(PublishStatus.PENDING, savedMessage.getPublishStatus());
        assertNull(savedMessage.getFailureReason());
        assertNull(savedMessage.getPublishedAt());
        org.junit.jupiter.api.Assertions.assertFalse(savedMessage.isRetrySupported());
        assertEquals("Retries are supported only for messages published with server-side broker configuration.", savedMessage.getRetryBlockedReason());
        assertNotNull(savedMessage.getPayload());
        assertEquals("binary", savedMessage.getPayload().getType());
        assertEquals("01001000 01100101 01101100", savedMessage.getPayload().getContent());
        assertEquals(savedMessage, savedMessage.getPayload().getMessage());
        assertNotNull(savedMessage.getProperties());
        assertEquals(1, savedMessage.getProperties().size());
        assertEquals("property01", savedMessage.getProperties().getFirst().getPropertyKey());
        assertEquals("value01", savedMessage.getProperties().getFirst().getPropertyValue());
        assertEquals(savedMessage, savedMessage.getProperties().getFirst().getMessage());
    }

    @Test
    void shouldMarkPendingMessageAsPublished() {
        Message savedMessage = new Message();
        savedMessage.setId(1L);
        savedMessage.setPublishStatus(PublishStatus.PENDING);
        savedMessage.setRetrySupported(true);
        savedMessageReference = savedMessage;

        Message updatedMessage = database.markMessagePublished(1L);

        assertEquals(PublishStatus.PUBLISHED, updatedMessage.getPublishStatus());
        assertNull(updatedMessage.getFailureReason());
        assertNotNull(updatedMessage.getPublishedAt());
    }

    @Test
    void shouldMarkMessageAsPendingForRetry() {
        Message savedMessage = new Message();
        savedMessage.setId(1L);
        savedMessage.setPublishStatus(PublishStatus.FAILED);
        savedMessage.setFailureReason("Failed to publish message to Solace broker");
        savedMessage.setPublishedAt(LocalDateTime.now());
        savedMessage.setRetrySupported(true);
        savedMessageReference = savedMessage;

        Message updatedMessage = database.markMessagePending(1L);

        assertEquals(PublishStatus.PENDING, updatedMessage.getPublishStatus());
        assertNull(updatedMessage.getFailureReason());
        assertNull(updatedMessage.getPublishedAt());
    }

    @Test
    void shouldMarkPendingMessageAsFailed() {
        Message savedMessage = new Message();
        savedMessage.setId(1L);
        savedMessage.setPublishStatus(PublishStatus.PENDING);
        savedMessage.setPublishedAt(LocalDateTime.now());
        savedMessage.setRetrySupported(true);
        savedMessageReference = savedMessage;

        Message updatedMessage = database.markMessageFailed(1L, "Failed to publish message to Solace broker");

        assertEquals(PublishStatus.FAILED, updatedMessage.getPublishStatus());
        assertEquals("Failed to publish message to Solace broker", updatedMessage.getFailureReason());
        assertNull(updatedMessage.getPublishedAt());
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
        message.setProperties(Map.of("property01", "value01"));
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
