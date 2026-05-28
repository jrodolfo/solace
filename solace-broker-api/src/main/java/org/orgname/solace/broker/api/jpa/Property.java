package org.orgname.solace.broker.api.jpa;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents a metadata property associated with a message.
 * <p>
 * These properties are typically mapped to Solace message user properties, allowing
 * for additional context or routing information to be attached to the message.
 */
@Data
@ToString(exclude = "message")
@EqualsAndHashCode(callSuper = true, exclude = "message")
// explicitly indicated that I want the call to the superclass’s equals and hashCode implementations
@NoArgsConstructor
@Entity
@Table(name = "Property")
public class Property extends Auditable {

    /**
     * Unique identifier for the property record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The key or name of the property.
     */
    @Column(name = "property_key", nullable = false)
    private String propertyKey;

    /**
     * The value associated with the property key.
     */
    @Column(name = "property_value", nullable = false)
    private String propertyValue;

    /**
     * The message that owns this property.
     * <p>
     * The Message entity has a list of Property objects, and each Property holds a reference back to the Message.
     * When Jackson (the JSON serializer used by Spring Boot) tries to serialize the User, it recursively serializes
     * the Property objects, which in turn try to serialize the Message again, and so on. To resolve this, we can
     * break the recursion using this approach: annotate with @JsonManagedReference and @JsonBackReference
     */
    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;
}
