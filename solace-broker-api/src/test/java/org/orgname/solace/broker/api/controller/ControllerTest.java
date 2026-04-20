package org.orgname.solace.broker.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orgname.solace.broker.api.dto.InnerMessageDTO;
import org.orgname.solace.broker.api.dto.MessageWrapperDTO;
import org.orgname.solace.broker.api.dto.PayloadDTO;
import org.orgname.solace.broker.api.jpa.Message;
import org.orgname.solace.broker.api.repository.MessageRepository;
import org.orgname.solace.broker.api.service.DatabaseImpl;
import org.orgname.solace.broker.api.service.DirectPublisherServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControllerTest {

    private static final String MESSAGE_SENT = "{\"destination\":\"solace/java/direct/system-01\",\"content\":\"01001000 01100101 01101100\"}";

    private StubDirectPublisherService directPublisherServiceImpl;
    private List<Message> storedMessages;
    private Controller controller;

    @BeforeEach
    void setUp() {
        directPublisherServiceImpl = new StubDirectPublisherService();
        storedMessages = new java.util.ArrayList<>();
        new DatabaseImpl(createMessageRepository());
        controller = new Controller(directPublisherServiceImpl);
    }

    @Test
    void shouldReturnAllMessagesFromRepository() {
        Message first = new Message();
        first.setInnerMessageId("001");
        Message second = new Message();
        second.setInnerMessageId("002");
        storedMessages.add(first);
        storedMessages.add(second);

        Iterable<Message> messages = controller.getAllMessages();

        assertIterableEquals(List.of(first, second), messages);
    }

    @Test
    void shouldSendMessageSuccessfully() throws Exception {
        MessageWrapperDTO wrapper = validWrapper();
        directPublisherServiceImpl.response = MESSAGE_SENT;

        ResponseEntity<String> response = controller.sendMessage(wrapper);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(MESSAGE_SENT, response.getBody());
    }

    @Test
    void shouldRejectNullMessagePayload() {
        MessageWrapperDTO wrapper = new MessageWrapperDTO();

        ResponseEntity<String> response = controller.sendMessage(wrapper);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Message is null", response.getBody());
    }

    @Test
    void shouldReturnErrorMessageWhenPublisherFails() throws Exception {
        MessageWrapperDTO wrapper = validWrapper();
        directPublisherServiceImpl.exception = new RuntimeException("Client error");

        ResponseEntity<String> response = controller.sendMessage(wrapper);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().contains("Client error"));
        assertTrue(response.getBody().contains("details=/message"));
    }

    private MessageRepository createMessageRepository() {
        return (MessageRepository) Proxy.newProxyInstance(
                MessageRepository.class.getClassLoader(),
                new Class[]{MessageRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findAll" -> List.copyOf(storedMessages);
                    case "save" -> {
                        Message message = (Message) args[0];
                        storedMessages.add(message);
                        yield message;
                    }
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "StubMessageRepository";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
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
        private RuntimeException exception;

        @Override
        public String sendMessage(String topicName, String content, Optional<org.orgname.solace.broker.api.dto.ParameterDTO> solaceParametersOptional) throws Exception {
            if (exception != null) {
                throw exception;
            }
            return response;
        }
    }
}
