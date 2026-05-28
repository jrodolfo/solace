package org.orgname.solace.broker.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.orgname.solace.broker.api.jpa.PayloadType;

/**
 * Data Transfer Object representing the payload of a message.
 * It specifies the content type and the actual message content.
 */
@Data
public class PayloadDTO {
    /**
     * The type of the payload (e.g., TEXT, BINARY, JSON).
     */
    @NotNull(message = "payload.type is required")
    private PayloadType type;

    /**
     * The actual content of the message payload.
     */
    @NotBlank(message = "payload.content is required")
    private String content;
}
