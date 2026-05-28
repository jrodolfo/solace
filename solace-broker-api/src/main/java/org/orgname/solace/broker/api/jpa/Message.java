package org.orgname.solace.broker.api.jpa;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a message entity for publication to a Solace PubSub+ Broker.
 * <p>
 * This entity stores the message metadata, its destination, delivery characteristics,
 * and current publication status. It acts as the root of the message aggregate,
 * containing both {@link Payload} and {@link Property} collections.
 */
@Data
@ToString(exclude = {"properties", "payload"})
@EqualsAndHashCode(callSuper = true, exclude = {"properties", "payload"})
// explicitly indicated that I want the call to the superclass’s equals and hashCode implementations
@NoArgsConstructor
@Entity
@Table(
        name = "Message",
        indexes = {
                @Index(name = "idx_message_inner_message_id", columnList = "inner_message_id"),
                @Index(name = "idx_message_destination", columnList = "destination"),
                @Index(name = "idx_message_delivery_mode", columnList = "delivery_mode"),
                @Index(name = "idx_message_created_at", columnList = "created_at")
        })
public class Message extends Auditable {

    /**
     * Primary key for the message record in the local database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // Primary key for our table

    /**
     * The internal message ID provided within the message JSON.
     * This is different from the database primary key.
     */
    @Column(name = "inner_message_id", nullable = false)
    private String innerMessageId;  // The ID that comes inside the message JSON

    /**
     * The Solace destination (Topic or Queue) where the message should be published.
     */
    @Column(name = "destination", nullable = false)
    private String destination;

    /**
     * The delivery mode for the message (e.g., PERSISTENT, DIRECT).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode", nullable = false)
    private DeliveryMode deliveryMode;

    /**
     * The relative priority of the message. Higher values indicate higher priority.
     */
    @Column(name = "priority", nullable = false)
    private Integer priority;

    /**
     * The current status of the message publication process.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "publish_status", nullable = false)
    private PublishStatus publishStatus;

    /**
     * Provides details on why a publication attempt failed, if applicable.
     */
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    /**
     * The timestamp when the message was successfully published to the Solace broker.
     */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /**
     * Indicates whether retrying the publication is supported for this message.
     */
    @Column(name = "retry_supported", nullable = false)
    private boolean retrySupported;

    /**
     * If {@code retrySupported} is false, this field explains why retry is blocked.
     */
    @Column(name = "retry_blocked_reason", columnDefinition = "TEXT")
    private String retryBlockedReason;

    /**
     * The collection of metadata properties associated with this message.
     * <p>
     * The Message entity has a list of Property objects, and each Property holds a reference back to the Message.
     * When Jackson (the JSON serializer used by Spring Boot) tries to serialize the User, it recursively serializes
     * the Property objects, which in turn try to serialize the Message again, and so on. To resolve this, we can
     * break the recursion using this approach: annotate with @JsonManagedReference and @JsonBackReference
     */
    @JsonManagedReference
    // One-to-Many relationship with Property
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Property> properties;

    /**
     * The content body of the message.
     */
    // One-to-One relationship with Payload
    @JsonManagedReference
    @OneToOne(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    private Payload payload;
}
