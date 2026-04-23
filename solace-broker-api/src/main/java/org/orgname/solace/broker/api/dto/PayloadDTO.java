package org.orgname.solace.broker.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.orgname.solace.broker.api.jpa.PayloadType;

@Data
public class PayloadDTO {
    @NotNull(message = "payload.type is required")
    private PayloadType type;

    @NotBlank(message = "payload.content is required")
    private String content;
}
