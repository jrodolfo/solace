package org.orgname.solace.broker.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data // Generates getters, setters, toString, equals, and hashCode
public class Message {

    /**
     * The unique identifier for the message.
     */
    @NotBlank // Ensures the field is not null/empty
    @JsonProperty("messageId")
    private String messageId;

    /**
     * The destination for the message (e.g., name of a topic or queue).
     */
    @NotBlank // Ensures the field is not null/empty
    @JsonProperty("destination")
    private String destination;

    /**
     * The deliveryMode for the message (e.g., Direct Messaging, Persistent, Non-Persistent, Transacted Delivery).
     * <p>
     * Solace Event Brokers support several message delivery modes, each suited for different use cases:
     * <p>
     * 1. Direct Messaging: This mode is designed for high-speed applications that can tolerate occasional message loss.
     * Messages are delivered to consumers with matching topic subscriptions but are not persisted on the event broker.
     * <p>
     * 2. Persistent (Guaranteed) Messaging: This mode ensures that messages are never lost. Messages are saved in the event
     * broker's message spool before being acknowledged back to the publishers. This mode is used when data must be received
     * by the consuming application, even if they are offline.
     * <p>
     * 3. Non-Persistent Messaging: This mode is used to fulfill JMS specification requirements and is similar to persistent
     * messaging in function but without the persistence guarantee.
     * <p>
     * 4. Transacted Delivery: This mode supports session-based and XA transactions, ensuring that a series of operations are
     * completed successfully before being committed.
     * <p>
     * Each mode offers different levels of reliability and performance, allowing you to choose the best option for your
     * specific application needs.
     * <p>
     * References:
     * https://docs.solace.com/API/API-Developer-Guide/Message-Delivery-Modes.htm
     * https://solace.com/blog/delivery-modes-direct-messaging-vs-persistent-messaging/
     */
    @NotBlank // Ensures the field is not null/empty
    @JsonProperty("deliveryMode")
    private String deliveryMode;

    /**
     * Solace Event Brokers support ten levels of message priority, ranging from 0 (lowest priority) to
     * 9 (highest priority).If a message does not have a priority field, it is treated as priority 4 by default.
     * This priority system helps ensure that higher-priority messages are delivered before lower-priority ones,
     * optimizing the message flow based on importance.
     * <p>
     * Reference:
     * https://docs.solace.com/Messaging/Guaranteed-Msg/Message-Priority.htm
     */
    @Min(0)
    @Max(9) // Ensures priority is within a valid range
    @JsonProperty("priority")
    private Integer priority;

    /**
     * Solace Event Broker messages have several properties that can be set to control
     * their behavior and routing. Here are some key properties and their possible values:
     * <p>
     * 1. Time-to-Live (TTL): This property sets the lifespan of a message in milliseconds.
     * A value of 0 means the message never expires.
     * <p>
     * 2. Dead Message Queue (DMQ) Eligibility: Messages can be flagged as eligible for
     * a Dead Message Queue if they exceed their TTL or maximum redelivery attempts.
     * <p>
     * 3. Eliding Eligibility: This property determines if a message can be elided
     * (i.e., skipped) under certain conditions to optimize bandwidth.
     * <p>
     * 4. Partition Key: Used to ensure messages with the same key are routed to the
     * same partition, maintaining order.
     * <p>
     * These properties help manage how messages are handled, ensuring efficient and reliable
     * message delivery within your event-driven architecture.
     * <p>
     * Reference:
     * https://docs.solace.com/API/Solace-JMS-API/Setting-Message-Properties.htm
     */
    @JsonProperty("properties")
    private Map<String, String> properties;

    @NotBlank // Ensures the field is not null/empty
    @JsonProperty("payload")
    private Payload payload;

    // Default no-args constructor for Jackson
    public Message() {}

    // All-arguments constructor
    @JsonCreator
    public Message(
            @JsonProperty("messageId") String messageId,
            @JsonProperty("destination") String destination,
            @JsonProperty("deliveryMode") String deliveryMode,
            @JsonProperty("priority") int priority,
            @JsonProperty("properties") Map<String, String> properties,
            @JsonProperty("payload") Payload payload) {
        this.messageId = messageId;
        this.destination = destination;
        this.deliveryMode = deliveryMode;
        this.priority = priority;
        this.properties = properties;
        this.payload = payload;
    }
}
