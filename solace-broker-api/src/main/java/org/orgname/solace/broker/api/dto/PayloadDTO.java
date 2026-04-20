package org.orgname.solace.broker.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PayloadDTO {
    @NotBlank(message = "payload.type is required")
    private String type;

    @NotBlank(message = "payload.content is required")
    private String content;
}
