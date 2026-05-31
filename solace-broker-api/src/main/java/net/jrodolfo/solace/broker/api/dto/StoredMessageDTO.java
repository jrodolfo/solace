package net.jrodolfo.solace.broker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.jrodolfo.solace.broker.api.jpa.DeliveryMode;
import net.jrodolfo.solace.broker.api.jpa.Message;
import net.jrodolfo.solace.broker.api.jpa.Property;
import net.jrodolfo.solace.broker.api.jpa.PublishStatus;
import net.jrodolfo.solace.broker.api.service.MessageLifecycleSupport;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object representing a message that has been stored in the system's database.
 * This DTO is used to convey the state, history, and content of a message, including
 * its Solace publication status and any retry constraints.
 */
@Data
@AllArgsConstructor
public class StoredMessageDTO {

    /**
     * The internal database primary key of the stored message.
     */
    private final Long id;

    /**
     * The unique identifier provided by the originating system.
     */
    private final String innerMessageId;

    /**
     * The Solace destination (topic or queue) associated with the message.
     */
    private final String destination;

    /**
     * The delivery mode used for this message (e.g., PERSISTENT, DIRECT).
     */
    private final DeliveryMode deliveryMode;

    /**
     * The priority level assigned to the message.
     */
    private final Integer priority;

    /**
     * The current publication status on the Solace broker (e.g., PUBLISHED, FAILED, PENDING).
     */
    private final PublishStatus publishStatus;

    /**
     * Indicates if the message is in a "stale pending" state, meaning it has been in PENDING
     * status longer than a predefined threshold.
     */
    private final boolean stalePending;

    /**
     * Textual reason for publication failure, if any.
     */
    private final String failureReason;

    /**
     * Timestamp indicating when the message was successfully published to the Solace broker.
     */
    private final LocalDateTime publishedAt;

    /**
     * Whether the system currently supports retrying the publication of this message.
     */
    private final boolean retrySupported;

    /**
     * If retry is not supported, this field provides the reason why (e.g., message already published).
     */
    private final String retryBlockedReason;

    /**
     * A map of metadata properties associated with the message.
     */
    private final Map<String, String> properties;

    /**
     * The message payload details.
     */
    private final StoredMessagePayloadDTO payload;

    /**
     * Timestamp indicating when the message record was created in the database.
     */
    private final LocalDateTime createdAt;

    /**
     * Timestamp indicating when the message record was last updated.
     */
    private final LocalDateTime updatedAt;

    /**
     * Constructs a {@link StoredMessageDTO} from a JPA {@link Message} entity.
     *
     * @param message The message entity to map.
     */
    public StoredMessageDTO(Message message) {
        this(message, MessageLifecycleSupport.DEFAULT_STALE_PENDING_THRESHOLD);
    }

    /**
     * Constructs a {@link StoredMessageDTO} from a JPA {@link Message} entity.
     *
     * @param message               The message entity to map.
     * @param stalePendingThreshold The threshold used to derive the stale pending flag.
     */
    public StoredMessageDTO(Message message, Duration stalePendingThreshold) {
        this.id = message.getId();
        this.innerMessageId = message.getInnerMessageId();
        this.destination = message.getDestination();
        this.deliveryMode = message.getDeliveryMode();
        this.priority = message.getPriority();
        this.publishStatus = message.getPublishStatus();
        this.stalePending = isStalePending(message, stalePendingThreshold);
        this.failureReason = message.getFailureReason();
        this.publishedAt = message.getPublishedAt();
        this.retrySupported = message.isRetrySupported();
        this.retryBlockedReason = message.getRetryBlockedReason();
        this.properties = toPropertyMap(message.getProperties());
        this.payload = new StoredMessagePayloadDTO(message.getPayload());
        this.createdAt = message.getCreatedAt();
        this.updatedAt = message.getUpdatedAt();
    }

    /**
     * Converts a list of {@link Property} entities into a key-value map.
     *
     * @param properties The list of property entities.
     * @return A map where keys are property keys and values are property values.
     */
    private static Map<String, String> toPropertyMap(List<Property> properties) {
        Map<String, String> propertyMap = new LinkedHashMap<>();
        if (properties == null) {
            return propertyMap;
        }

        for (Property property : properties) {
            propertyMap.put(property.getPropertyKey(), property.getPropertyValue());
        }
        return propertyMap;
    }

    /**
     * Determines if a message is in a "stale pending" state.
     *
     * @param message The message entity to check.
     * @return {@code true} if the message status is PENDING and its creation time
     *         exceeds the staleness threshold; {@code false} otherwise.
     */
    private static boolean isStalePending(Message message, Duration stalePendingThreshold) {
        return MessageLifecycleSupport.isStalePending(message.getPublishStatus(), message.getCreatedAt(), stalePendingThreshold);
    }
}
