package org.orgname.solace.broker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.orgname.solace.broker.api.jpa.Payload;
import org.orgname.solace.broker.api.jpa.PayloadType;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing the payload of a stored message.
 * Includes content, type, and database timestamps.
 */
@Data
@AllArgsConstructor
public class StoredMessagePayloadDTO {

    /**
     * The type of the payload (e.g., TEXT, BINARY, JSON).
     */
    private final PayloadType type;

    /**
     * The actual content of the stored message.
     */
    private final String content;

    /**
     * Timestamp indicating when the payload record was created in the database.
     */
    private final LocalDateTime createdAt;

    /**
     * Timestamp indicating when the payload record was last updated.
     */
    private final LocalDateTime updatedAt;

    /**
     * Constructs a {@link StoredMessagePayloadDTO} from a JPA {@link Payload} entity.
     *
     * @param payload The payload entity to map.
     */
    public StoredMessagePayloadDTO(Payload payload) {
        this.type = payload.getType();
        this.content = payload.getContent();
        this.createdAt = payload.getCreatedAt();
        this.updatedAt = payload.getUpdatedAt();
    }
}
