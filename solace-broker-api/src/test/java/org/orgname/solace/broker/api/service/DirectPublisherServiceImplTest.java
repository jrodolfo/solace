package org.orgname.solace.broker.api.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class DirectPublisherServiceImplTest {

    private static final Logger logger = Logger.getLogger(DirectPublisherServiceImplTest.class.getName());

    @Autowired
    DirectPublisherServiceImpl directPublisherService;

    final String TOPIC_NAME = "solace/java/direct/system-01";
    final String CONTENT = "01001000 01100101 01101100 01101100 01101111 00101100 00100000 01010111 01101111 01110010 01101100 01100100 00100001";

    @Test
    void sendMessage() throws Exception {
        String answer = directPublisherService.sendMessage(TOPIC_NAME, CONTENT, Optional.empty());
        assertNotNull(answer);
        logger.log(Level.INFO, "Answer from sendMessage(): " + answer);
    }
}