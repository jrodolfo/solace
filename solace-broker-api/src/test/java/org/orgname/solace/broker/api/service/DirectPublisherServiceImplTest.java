package org.orgname.solace.broker.api.service;

import com.solace.messaging.MessagingService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.orgname.solace.broker.api.constants.Constants.ERROR_EMPTY_MESSAGE_OR_TOPIC_NAME;

class DirectPublisherServiceImplTest {

    private static final Logger logger = Logger.getLogger(DirectPublisherServiceImplTest.class.getName());

    @Mock
    private MessagingService mockSolaceMessagingService;

    @Mock
    private Properties properties;

    @Mock
    private AccessPropertiesImpl accessPropertiesImpl;

    @InjectMocks
    private DirectPublisherServiceImpl directPublisherService;

    final String VALID_TOPIC_NAME = "solace/java/direct/system-01";
    final String VALID_CONTENT = "Message content";
    private static final String VALID_MESSAGE = "{\n" +
            "\t\"messageId\": \"001\",\n" +
            "\t\"destination\": \"solace/java/direct/system-01\",\n" +
            "\t\"deliveryMode\": \"PERSISTENT\",\n" +
            "\t\"priority\": 3,\n" +
            "\t\"properties\": {\n" +
            "\t\t\"property01\": \"value01\",\n" +
            "\t\t\"property02\": \"value02\"\n" +
            "\t\t},\n" +
            "\t\"payload\": {\n" +
            "\t\t\t\"type\": \"binary\",\n" +
            "\t\t\t\"content\": \"01001000 01100101 01101100 01101100 01101111 00101100 00100000 01010111 01101111 01110010 01101100 01100100 00100001\"\n" +
            "\t\t}\n" +
            "}\n";
    final String INVALID_TOPIC_NAME = "";
    final String EMPTY_CONTENT = "";

    public DirectPublisherServiceImplTest() {
        MockitoAnnotations.openMocks(this); // Initialize MockitoMocks
    }

    @Test
    void sendMessage() throws Exception {
        String answer = directPublisherService.sendMessage(VALID_TOPIC_NAME, VALID_CONTENT, Optional.empty());
        assertNotNull(answer);
        logger.log(Level.INFO, "Answer from sendMessage(): " + answer);
    }

    @Test
    void testSendMessage_InvalidTopicName() {
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> directPublisherService.sendMessage(INVALID_TOPIC_NAME, VALID_CONTENT, Optional.empty())
        );

        logger.log(Level.INFO, "Exception: " + exception.getMessage());
        assertEquals(ERROR_EMPTY_MESSAGE_OR_TOPIC_NAME, exception.getMessage());
    }

    @Test
    void testSendMessage_EmptyContent() {
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> directPublisherService.sendMessage(VALID_TOPIC_NAME, EMPTY_CONTENT, Optional.empty())
        );

        logger.log(Level.INFO, "Exception: " + exception.getMessage());
        assertEquals(ERROR_EMPTY_MESSAGE_OR_TOPIC_NAME, exception.getMessage());
    }
}