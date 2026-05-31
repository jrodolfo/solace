package net.jrodolfo.solace.broker.api.jpa;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents the main content or body of a message.
 * <p>
 * Each message has exactly one payload, which can be of various types as defined by {@link PayloadType}.
 */
@Data
@ToString(exclude = "message")
@EqualsAndHashCode(callSuper = true, exclude = "message")
// explicitly indicated that I want the call to the superclass’s equals and hashCode implementations
@NoArgsConstructor
@Entity
@Table(name = "Payload")
public class Payload extends Auditable {

    /**
     * Unique identifier for the payload record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The type of content stored in this payload (e.g., TEXT, JSON).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PayloadType type;

    /**
     * The actual content of the message.
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * The message associated with this payload.
     */
    @JsonBackReference
    @OneToOne
    @JoinColumn(name = "message_id", nullable = false, unique = true)
    private Message message;
}
