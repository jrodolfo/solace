package com.example.accessingdatamysql;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true) // explicitly indicated that I want the call to the superclass’s equals and hashCode implementations
@Entity
public class UserProperty extends Auditable {  // Extend Auditable to inherit timestamps

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "property_id")
    private Integer id;

    @Column(name = "property_key")
    private String propertyKey;

    @Column(name = "property_value")
    private String propertyValue;

    /**
     * The User entity has a list of UserProperty objects, and each UserProperty holds a reference back to the User.
     * When Jackson (the JSON serializer used by Spring Boot) tries to serialize the User, it recursively serializes
     * the UserProperty objects, which in turn try to serialize the User again, and so on. To resolve this, you can
     * break the recursion using this approach: annotate with @JsonManagedReference and @JsonBackReference
     */
    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "user_id")
    private User user;
}