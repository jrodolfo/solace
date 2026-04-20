package org.orgname.solace.broker.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.util.Map;

@Data
public class InnerMessageDTO {
    @NotBlank(message = "message.innerMessageId is required")
    private String innerMessageId;

    @NotBlank(message = "message.destination is required")
    private String destination;

    @NotBlank(message = "message.deliveryMode is required")
    private String deliveryMode;

    @NotNull(message = "message.priority is required")
    @PositiveOrZero(message = "message.priority must be zero or greater")
    private Integer priority;

    // Properties as a key-value map.
    private Map<String, String> properties;

    // Nested payload.
    @NotNull(message = "message.payload is required")
    @Valid
    private PayloadDTO payload;
}
