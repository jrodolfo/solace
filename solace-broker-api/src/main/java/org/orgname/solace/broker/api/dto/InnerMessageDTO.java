package org.orgname.solace.broker.api.dto;

import lombok.Data;

import java.util.Map;

@Data
public class InnerMessageDTO {
    private String innerMessageId;
    private String destination;
    private String deliveryMode;
    private Integer priority;

    // Properties as a key-value map.
    private Map<String, String> properties;

    // Nested payload.
    private PayloadDTO payload;
}
