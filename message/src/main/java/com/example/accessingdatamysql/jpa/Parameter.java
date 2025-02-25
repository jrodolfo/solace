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
@Table(name = "Parameter")
public class Parameter extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "password")
    private String password;

    @Column(name = "host")
    private String host;

    @Column(name = "vpn_name")
    private String vpnName;

    @Column(name = "topic_name")
    private String topicName;

    @JsonBackReference
    @OneToOne
    @JoinColumn(name = "message_id", unique = true)
    private Message message;
}
