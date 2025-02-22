package com.example.accessingdatamysql;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data // Generates getters, setters, toString, equals, and hashCode
@Entity // This tells Hibernate to make a table out of this class
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;


    /**
     * The User entity has a list of UserProperty objects, and each UserProperty holds a reference back to the User.
     * When Jackson (the JSON serializer used by Spring Boot) tries to serialize the User, it recursively serializes
     * the UserProperty objects, which in turn try to serialize the User again, and so on. To resolve this, you can
     * break the recursion using this approaches: annotate with @JsonManagedReference and @JsonBackReference
     */
    @JsonManagedReference
    // One-to-Many relationship with UserProperty
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserProperty> properties;
}