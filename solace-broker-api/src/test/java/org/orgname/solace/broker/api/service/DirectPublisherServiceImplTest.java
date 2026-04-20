package org.orgname.solace.broker.api.service;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.orgname.solace.broker.api.constants.Constants.ERROR_EMPTY_MESSAGE_OR_TOPIC_NAME;

class DirectPublisherServiceImplTest {

    private final DirectPublisherServiceImpl directPublisherService = new DirectPublisherServiceImpl();

    @Test
    void testSendMessageInvalidTopicName() {
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> directPublisherService.sendMessage("", "Message content", Optional.empty())
        );

        assertEquals(ERROR_EMPTY_MESSAGE_OR_TOPIC_NAME, exception.getMessage());
    }

    @Test
    void testSendMessageEmptyContent() {
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> directPublisherService.sendMessage("solace/java/direct/system-01", "", Optional.empty())
        );

        assertEquals(ERROR_EMPTY_MESSAGE_OR_TOPIC_NAME, exception.getMessage());
    }
}
