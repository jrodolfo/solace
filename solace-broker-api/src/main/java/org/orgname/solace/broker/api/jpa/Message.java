package org.orgname.solace.broker.api.jpa;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Data
@ToString(exclude = {"properties", "payload"})
@EqualsAndHashCode(callSuper = true, exclude = {"properties", "payload"}) // explicitly indicated that I want the call to the superclass’s equals and hashCode implementations
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // Primary key for our table

    @Column(name = "inner_message_id", nullable = false)
    private String innerMessageId;  // The ID that comes inside the message JSON

    @Column(name = "destination", nullable = false)
    private String destination;

    @Column(name = "delivery_mode", nullable = false)
    private String deliveryMode;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "publish_status", nullable = false)
    private PublishStatus publishStatus;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "retry_supported", nullable = false)
    private boolean retrySupported;

    @Column(name = "retry_blocked_reason", columnDefinition = "TEXT")
    private String retryBlockedReason;

    /**
     * The Message entity has a list of Property objects, and each Property holds a reference back to the Message.
     * When Jackson (the JSON serializer used by Spring Boot) tries to serialize the User, it recursively serializes
     * the Property objects, which in turn try to serialize the Message again, and so on. To resolve this, we can
     * break the recursion using this approach: annotate with @JsonManagedReference and @JsonBackReference
     */
    @JsonManagedReference
    // One-to-Many relationship with Property
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Property> properties;

    // One-to-One relationship with Payload
    @JsonManagedReference
    @OneToOne(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    private Payload payload;
}
