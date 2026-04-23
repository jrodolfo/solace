package org.orgname.solace.broker.api.jpa;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString(exclude = "message")
@EqualsAndHashCode(callSuper = true, exclude = "message") // explicitly indicated that I want the call to the superclass’s equals and hashCode implementations
@NoArgsConstructor
@Entity
@Table(name = "Payload")
public class Payload extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PayloadType type;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @JsonBackReference
    @OneToOne
    @JoinColumn(name = "message_id", nullable = false, unique = true)
    private Message message;
}
