package com.example.accessingdatamysql.jpa;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true) // explicitly indicated that I want the call to the superclass’s equals and hashCode implementations
@NoArgsConstructor
@Entity
@Table(name = "Payload")
public class Payload extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type")
    private String type;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @JsonBackReference
    @OneToOne
    @JoinColumn(name = "message_id", nullable = false, unique = true)
    private Message message;
}
