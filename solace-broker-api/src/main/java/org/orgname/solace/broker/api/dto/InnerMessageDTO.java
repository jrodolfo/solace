package org.orgname.solace.broker.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.orgname.solace.broker.api.jpa.DeliveryMode;

import java.util.Map;

/**
 * Data Transfer Object representing the core message data for publication.
 * This includes the message identifier, its destination on the Solace broker,
 * delivery mode, and the actual payload.
 */
@Data
public class InnerMessageDTO {
    /**
     * Unique identifier for the message, typically provided by the originating system.
     */
    @NotBlank(message = "message.innerMessageId is required")
    private String innerMessageId;

    /**
     * The Solace topic or queue where the message should be published.
     */
    @NotBlank(message = "message.destination is required")
    private String destination;

    /**
     * The delivery mode for the message (e.g., PERSISTENT or DIRECT).
     * Persistent messages are stored on the broker until acknowledged.
     */
    @NotNull(message = "message.deliveryMode is required")
    private DeliveryMode deliveryMode;

    /**
     * The priority level of the message.
     */
    @NotNull(message = "message.priority is required")
    @PositiveOrZero(message = "message.priority must be zero or greater")
    private Integer priority;

    /**
     * Optional metadata properties associated with the message, provided as a key-value map.
     */
    private Map<String, String> properties;

    /**
     * The message payload, including its type and content.
     */
    @NotNull(message = "message.payload is required")
    @Valid
    private PayloadDTO payload;
}
