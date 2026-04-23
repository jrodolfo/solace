package org.orgname.solace.broker.api.dto;

import org.orgname.solace.broker.api.jpa.Payload;
import org.orgname.solace.broker.api.jpa.PayloadType;

import java.time.LocalDateTime;

public class StoredMessagePayloadDTO {

    private final PayloadType type;
    private final String content;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public StoredMessagePayloadDTO(Payload payload) {
        this.type = payload.getType();
        this.content = payload.getContent();
        this.createdAt = payload.getCreatedAt();
        this.updatedAt = payload.getUpdatedAt();
    }

    public PayloadType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
