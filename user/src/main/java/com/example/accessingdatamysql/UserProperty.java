package com.example.accessingdatamysql;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class UserProperty {
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
     * break the recursion using this approaches: annotate with @JsonManagedReference and @JsonBackReference
     */
    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "user_id")
    private User user;
}