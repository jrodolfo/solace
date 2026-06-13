package net.jrodolfo.solace.broker.api.repository;

import net.jrodolfo.solace.broker.api.testsupport.TestDestinations;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import net.jrodolfo.solace.broker.api.config.BrokerApiProperties;
import net.jrodolfo.solace.broker.api.dto.PagedMessagesResponseDTO;
import net.jrodolfo.solace.broker.api.jpa.DeliveryMode;
import net.jrodolfo.solace.broker.api.jpa.Message;
import net.jrodolfo.solace.broker.api.jpa.Payload;
import net.jrodolfo.solace.broker.api.jpa.PayloadType;
import net.jrodolfo.solace.broker.api.jpa.PublishStatus;
import net.jrodolfo.solace.broker.api.service.DatabaseImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Data JPA tests for {@link MessageRepository}.
 * This class tests the persistence layer and filtering capabilities of the message repository
 * using an in-memory H2 database configured to mimic MySQL behavior.
 */
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

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldFilterMessagesCaseInsensitivelyByStoredFields() {
        DatabaseImpl database = new DatabaseImpl(messageRepository, new BrokerApiProperties());
        persistMessage("001", TestDestinations.SYSTEM_01, DeliveryMode.PERSISTENT, 3, LocalDateTime.of(2026, 4, 20, 10, 0));
        persistMessage("002", TestDestinations.SYSTEM_02, DeliveryMode.DIRECT, 1, LocalDateTime.of(2026, 4, 19, 10, 0));
        persistMessage("abc-003", TestDestinations.SYSTEM_03, DeliveryMode.PERSISTENT, 2, LocalDateTime.of(2026, 4, 18, 10, 0));

        PagedMessagesResponseDTO response = database.getAllMessages(
                0, 20, "SYSTEM-03", DeliveryMode.PERSISTENT, null, "ABC", PublishStatus.PUBLISHED,
                false, null, null, null, null, "createdAt", "desc");

        assertEquals(1L, response.getTotalElements());
        assertEquals("abc-003", response.getItems().getFirst().getInnerMessageId());
        assertEquals(TestDestinations.SYSTEM_03, response.getItems().getFirst().getDestination());
    }

    @Test
    void shouldSortMessagesByPriorityAscending() {
        DatabaseImpl database = new DatabaseImpl(messageRepository, new BrokerApiProperties());
        persistMessage("001", TestDestinations.SYSTEM_01, DeliveryMode.PERSISTENT, 3, LocalDateTime.of(2026, 4, 20, 10, 0));
        persistMessage("002", TestDestinations.SYSTEM_02, DeliveryMode.DIRECT, 1, LocalDateTime.of(2026, 4, 19, 10, 0));
        persistMessage("003", TestDestinations.SYSTEM_03, DeliveryMode.PERSISTENT, 2, LocalDateTime.of(2026, 4, 18, 10, 0));

        PagedMessagesResponseDTO response = database.getAllMessages(
                0, 20, null, null, null, null, null,
                false, null, null, null, null, "priority", "asc");

        assertEquals(List.of("002", "003", "001"),
                response.getItems().stream().map(item -> item.getInnerMessageId()).toList());
    }

    @Test
    void shouldSortMessagesByCreatedAtDescendingAndPaginateResults() {
        DatabaseImpl database = new DatabaseImpl(messageRepository, new BrokerApiProperties());
        persistMessage("001", TestDestinations.SYSTEM_01, DeliveryMode.PERSISTENT, 3, LocalDateTime.of(2026, 4, 18, 10, 0));
        persistMessage("002", TestDestinations.SYSTEM_02, DeliveryMode.DIRECT, 1, LocalDateTime.of(2026, 4, 19, 10, 0));
        persistMessage("003", TestDestinations.SYSTEM_03, DeliveryMode.PERSISTENT, 2, LocalDateTime.of(2026, 4, 20, 10, 0));

        PagedMessagesResponseDTO response = database.getAllMessages(
                1, 1, null, null, null, null, null,
                false, null, null, null, null, "createdAt", "desc");

        assertEquals(3L, response.getTotalElements());
        assertEquals(3, response.getTotalPages());
        assertEquals(1, response.getPage());
        assertEquals(1, response.getItems().size());
        assertEquals("002", response.getItems().getFirst().getInnerMessageId());
    }

    @Test
    void shouldFilterMessagesByPublishStatus() {
        DatabaseImpl database = new DatabaseImpl(messageRepository, new BrokerApiProperties());
        persistMessage("001", TestDestinations.SYSTEM_01, DeliveryMode.PERSISTENT, 3, PublishStatus.PUBLISHED, LocalDateTime.of(2026, 4, 20, 10, 0));
        persistMessage("002", TestDestinations.SYSTEM_02, DeliveryMode.DIRECT, 1, PublishStatus.FAILED, LocalDateTime.of(2026, 4, 19, 10, 0));
        persistMessage("003", TestDestinations.SYSTEM_03, DeliveryMode.PERSISTENT, 2, PublishStatus.PENDING, LocalDateTime.of(2026, 4, 18, 10, 0));

        PagedMessagesResponseDTO response = database.getAllMessages(
                0, 20, null, null, null, null, PublishStatus.FAILED,
                false, null, null, null, null, "createdAt", "desc");

        assertEquals(1L, response.getTotalElements());
        assertEquals("002", response.getItems().getFirst().getInnerMessageId());
        assertEquals(PublishStatus.FAILED, response.getItems().getFirst().getPublishStatus());
    }

    @Test
    void shouldFilterMessagesByPayloadType() {
        DatabaseImpl database = new DatabaseImpl(messageRepository, new BrokerApiProperties());
        persistMessage("001", TestDestinations.SYSTEM_01, DeliveryMode.PERSISTENT, PayloadType.TEXT, 3, PublishStatus.PUBLISHED, LocalDateTime.of(2026, 4, 20, 10, 0));
        persistMessage("002", TestDestinations.SYSTEM_02, DeliveryMode.DIRECT, PayloadType.JSON, 1, PublishStatus.FAILED, LocalDateTime.of(2026, 4, 19, 10, 0));
        persistMessage("003", TestDestinations.SYSTEM_03, DeliveryMode.PERSISTENT, PayloadType.XML, 2, PublishStatus.PENDING, LocalDateTime.of(2026, 4, 18, 10, 0));

        PagedMessagesResponseDTO response = database.getAllMessages(
                0, 20, null, null, PayloadType.JSON, null, null, false,
                null, null, null, null, "createdAt", "desc");

        assertEquals(1L, response.getTotalElements());
        assertEquals("002", response.getItems().getFirst().getInnerMessageId());
        assertEquals(PayloadType.JSON, response.getItems().getFirst().getPayload().getType());
    }

    @Test
    void shouldFilterMessagesByCreatedAtRange() {
        DatabaseImpl database = new DatabaseImpl(messageRepository, new BrokerApiProperties());
        persistMessage("001", TestDestinations.SYSTEM_01, DeliveryMode.PERSISTENT, 3, LocalDateTime.of(2026, 4, 18, 10, 0));
        persistMessage("002", TestDestinations.SYSTEM_02, DeliveryMode.DIRECT, 1, LocalDateTime.of(2026, 4, 19, 10, 0));
        persistMessage("003", TestDestinations.SYSTEM_03, DeliveryMode.PERSISTENT, 2, LocalDateTime.of(2026, 4, 20, 10, 0));

        PagedMessagesResponseDTO response = database.getAllMessages(
                0, 20, null, null, null, null, null,
                false,
                LocalDateTime.of(2026, 4, 19, 0, 0),
                LocalDateTime.of(2026, 4, 19, 23, 59, 59),
                null,
                null,
                "createdAt",
                "asc");

        assertEquals(List.of("002"), response.getItems().stream().map(item -> item.getInnerMessageId()).toList());
    }

    @Test
    void shouldFilterMessagesByPublishedAtRangeAndPublishStatus() {
        DatabaseImpl database = new DatabaseImpl(messageRepository, new BrokerApiProperties());
        persistMessage("001", TestDestinations.SYSTEM_01, DeliveryMode.PERSISTENT, 3, PublishStatus.PUBLISHED, LocalDateTime.of(2026, 4, 18, 10, 0));
        persistMessage("002", TestDestinations.SYSTEM_02, DeliveryMode.DIRECT, 1, PublishStatus.FAILED, LocalDateTime.of(2026, 4, 19, 10, 0));
        persistMessage("003", TestDestinations.SYSTEM_03, DeliveryMode.PERSISTENT, 2, PublishStatus.PUBLISHED, LocalDateTime.of(2026, 4, 20, 10, 0));

        PagedMessagesResponseDTO response = database.getAllMessages(
                0, 20, null, null, null, null, PublishStatus.PUBLISHED,
                false,
                null,
                null,
                LocalDateTime.of(2026, 4, 19, 0, 0),
                LocalDateTime.of(2026, 4, 21, 0, 0),
                "createdAt",
                "asc");

        assertEquals(List.of("003"), response.getItems().stream().map(item -> item.getInnerMessageId()).toList());
    }

    @Test
    void shouldFilterMessagesByStalePendingOnly() {
        DatabaseImpl database = new DatabaseImpl(messageRepository, new BrokerApiProperties());
        persistMessage("001", TestDestinations.SYSTEM_01, DeliveryMode.PERSISTENT, 3, PublishStatus.PENDING, LocalDateTime.of(2026, 4, 18, 10, 0));
        persistMessage("002", TestDestinations.SYSTEM_02, DeliveryMode.DIRECT, 1, PublishStatus.PENDING, LocalDateTime.now().minusMinutes(1));
        persistMessage("003", TestDestinations.SYSTEM_03, DeliveryMode.PERSISTENT, 2, PublishStatus.PUBLISHED, LocalDateTime.of(2026, 4, 20, 10, 0));

        PagedMessagesResponseDTO response = database.getAllMessages(
                0, 20, null, null, null, null, null,
                true, null, null, null, null, "createdAt", "asc");

        assertEquals(List.of("001"), response.getItems().stream().map(item -> item.getInnerMessageId()).toList());
        assertEquals(true, response.getItems().getFirst().isStalePending());
    }

    /**
     * Persists a message with default payload type (BINARY) and status (PUBLISHED).
     */
    private void persistMessage(
            String innerMessageId,
            String destination,
            DeliveryMode deliveryMode,
            int priority,
            LocalDateTime createdAt) {
        persistMessage(innerMessageId, destination, deliveryMode, PayloadType.BINARY, priority, PublishStatus.PUBLISHED, createdAt);
    }

    /**
     * Persists a message with the default payload type (BINARY).
     */
    private void persistMessage(
            String innerMessageId,
            String destination,
            DeliveryMode deliveryMode,
            int priority,
            PublishStatus publishStatus,
            LocalDateTime createdAt) {
        persistMessage(innerMessageId, destination, deliveryMode, PayloadType.BINARY, priority, publishStatus, createdAt);
    }

    /**
     * Persists a message to the database for testing purposes.
     * This method directly interacts with the {@link EntityManager} to ensure that timestamps
     * are correctly set, as they are often handled by JPA/Hibernate lifecycle events.
     *
     * @param innerMessageId the unique identifier for the message
     * @param destination the Solace topic destination
     * @param deliveryMode the delivery mode (e.g., PERSISTENT, DIRECT)
     * @param payloadType the type of payload (e.g., TEXT, JSON, BINARY)
     * @param priority the message priority
     * @param publishStatus the current publish status
     * @param createdAt the creation timestamp
     */
    private void persistMessage(
            String innerMessageId,
            String destination,
            DeliveryMode deliveryMode,
            PayloadType payloadType,
            int priority,
            PublishStatus publishStatus,
            LocalDateTime createdAt) {
        Message message = new Message();
        message.setInnerMessageId(innerMessageId);
        message.setDestination(destination);
        message.setDeliveryMode(deliveryMode);
        message.setPriority(priority);
        message.setPublishStatus(publishStatus);
        message.setFailureReason(publishStatus == PublishStatus.FAILED ? "Failed to publish message to Solace broker" : null);
        message.setPublishedAt(publishStatus == PublishStatus.PUBLISHED ? createdAt : null);
        message.setRetrySupported(true);
        message.setRetryBlockedReason(null);
        message.setCreatedAt(createdAt);
        message.setUpdatedAt(createdAt);

        Payload payload = new Payload();
        payload.setType(payloadType);
        payload.setContent("01001000 01100101 01101100");
        payload.setCreatedAt(createdAt);
        payload.setUpdatedAt(createdAt);
        payload.setMessage(message);
        message.setPayload(payload);
        message.setProperties(List.of());

        Message savedMessage = messageRepository.saveAndFlush(message);
        entityManager.createNativeQuery("""
                        update message
                           set created_at = ?,
                               updated_at = ?,
                               published_at = ?
                         where id = ?
                        """)
                .setParameter(1, createdAt)
                .setParameter(2, createdAt)
                .setParameter(3, publishStatus == PublishStatus.PUBLISHED ? createdAt : null)
                .setParameter(4, savedMessage.getId())
                .executeUpdate();
        entityManager.clear();
    }
}
