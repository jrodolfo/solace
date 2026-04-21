package org.orgname.solace.broker.api.repository;

import org.junit.jupiter.api.Test;
import org.orgname.solace.broker.api.dto.PagedMessagesResponseDTO;
import org.orgname.solace.broker.api.jpa.Message;
import org.orgname.solace.broker.api.jpa.Payload;
import org.orgname.solace.broker.api.jpa.PublishStatus;
import org.orgname.solace.broker.api.service.DatabaseImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:message-repository-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class MessageRepositoryDataJpaTest {

    @Autowired
    private MessageRepository messageRepository;

    @Test
    void shouldFilterMessagesCaseInsensitivelyByStoredFields() {
        DatabaseImpl database = new DatabaseImpl(messageRepository);
        persistMessage("001", "solace/java/direct/system-01", "PERSISTENT", 3, LocalDateTime.of(2026, 4, 20, 10, 0));
        persistMessage("002", "solace/java/direct/system-02", "DIRECT", 1, LocalDateTime.of(2026, 4, 19, 10, 0));
        persistMessage("abc-003", "solace/java/direct/system-03", "PERSISTENT", 2, LocalDateTime.of(2026, 4, 18, 10, 0));

        PagedMessagesResponseDTO response = database.getAllMessages(0, 20, "SYSTEM-03", "persistent", "ABC", "createdAt", "desc");

        assertEquals(1L, response.getTotalElements());
        assertEquals("abc-003", response.getItems().getFirst().getInnerMessageId());
        assertEquals("solace/java/direct/system-03", response.getItems().getFirst().getDestination());
    }

    @Test
    void shouldSortMessagesByPriorityAscending() {
        DatabaseImpl database = new DatabaseImpl(messageRepository);
        persistMessage("001", "solace/java/direct/system-01", "PERSISTENT", 3, LocalDateTime.of(2026, 4, 20, 10, 0));
        persistMessage("002", "solace/java/direct/system-02", "DIRECT", 1, LocalDateTime.of(2026, 4, 19, 10, 0));
        persistMessage("003", "solace/java/direct/system-03", "PERSISTENT", 2, LocalDateTime.of(2026, 4, 18, 10, 0));

        PagedMessagesResponseDTO response = database.getAllMessages(0, 20, null, null, null, "priority", "asc");

        assertEquals(List.of("002", "003", "001"),
                response.getItems().stream().map(item -> item.getInnerMessageId()).toList());
    }

    @Test
    void shouldSortMessagesByCreatedAtDescendingAndPaginateResults() {
        DatabaseImpl database = new DatabaseImpl(messageRepository);
        persistMessage("001", "solace/java/direct/system-01", "PERSISTENT", 3, LocalDateTime.of(2026, 4, 18, 10, 0));
        persistMessage("002", "solace/java/direct/system-02", "DIRECT", 1, LocalDateTime.of(2026, 4, 19, 10, 0));
        persistMessage("003", "solace/java/direct/system-03", "PERSISTENT", 2, LocalDateTime.of(2026, 4, 20, 10, 0));

        PagedMessagesResponseDTO response = database.getAllMessages(1, 1, null, null, null, "createdAt", "desc");

        assertEquals(3L, response.getTotalElements());
        assertEquals(3, response.getTotalPages());
        assertEquals(1, response.getPage());
        assertEquals(1, response.getItems().size());
        assertEquals("002", response.getItems().getFirst().getInnerMessageId());
    }

    private void persistMessage(
            String innerMessageId,
            String destination,
            String deliveryMode,
            int priority,
            LocalDateTime createdAt) {
        Message message = new Message();
        message.setInnerMessageId(innerMessageId);
        message.setDestination(destination);
        message.setDeliveryMode(deliveryMode);
        message.setPriority(priority);
        message.setPublishStatus(PublishStatus.PUBLISHED);
        message.setFailureReason(null);
        message.setPublishedAt(createdAt);
        message.setCreatedAt(createdAt);
        message.setUpdatedAt(createdAt);

        Payload payload = new Payload();
        payload.setType("binary");
        payload.setContent("01001000 01100101 01101100");
        payload.setCreatedAt(createdAt);
        payload.setUpdatedAt(createdAt);
        payload.setMessage(message);
        message.setPayload(payload);
        message.setProperties(List.of());

        messageRepository.saveAndFlush(message);
    }
}
