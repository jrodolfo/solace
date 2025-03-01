package com.example.accessingdatamysql.jpa;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true) // explicitly indicated that I want the call to the superclass’s equals and hashCode implementations
@NoArgsConstructor
@Entity
@Table(name = "Message")
public class Message extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // Primary key for our table

    @Column(name = "inner_message_id", nullable = false)
    private String innerMessageId;  // The ID that comes inside the message JSON

    @Column(name = "destination")
    private String destination;

    @Column(name = "delivery_mode")
    private String deliveryMode;

    @Column(name = "priority")
    private Integer priority;

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
    @OneToOne(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private Payload payload;

    // One-to-One relationship with Parameter (optional)
    @JsonManagedReference
    @OneToOne(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private Parameter parameter;
}
