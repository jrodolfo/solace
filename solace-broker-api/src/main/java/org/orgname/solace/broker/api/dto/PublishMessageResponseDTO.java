package org.orgname.solace.broker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Data Transfer Object representing the response after a message is published to the Solace broker.
 */
@Data
@AllArgsConstructor
public class PublishMessageResponseDTO {

    /**
     * The Solace destination (topic or queue) where the message was published.
     */
    private final String destination;

    /**
     * The content of the published message.
     */
    private final String content;
}
