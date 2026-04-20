package org.orgname.solace.broker.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orgname.solace.broker.api.dto.InnerMessageDTO;
import org.orgname.solace.broker.api.dto.MessageWrapperDTO;
import org.orgname.solace.broker.api.dto.PayloadDTO;
import org.orgname.solace.broker.api.jpa.Message;
import org.orgname.solace.broker.api.repository.MessageRepository;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseImplTest {

    private MessageRepository messageRepository;
    private DatabaseImpl database;

    @BeforeEach
    void setUp() {
        messageRepository = mock(MessageRepository.class);
        database = new DatabaseImpl(messageRepository);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldPersistMessageGraphWithoutSensitiveConnectionParameters() {
        MessageWrapperDTO wrapper = validWrapper();

        Message savedMessage = database.saveMessage(wrapper);

        assertEquals("001", savedMessage.getInnerMessageId());
        assertEquals("solace/java/direct/system-01", savedMessage.getDestination());
        assertNotNull(savedMessage.getPayload());
        assertEquals("binary", savedMessage.getPayload().getType());
        assertEquals("01001000 01100101 01101100", savedMessage.getPayload().getContent());
        assertNotNull(savedMessage.getProperties());
        assertEquals(1, savedMessage.getProperties().size());
        assertNull(savedMessage.getParameter());
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
