package net.jrodolfo.solace.broker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.jrodolfo.solace.broker.api.jpa.PublishStatus;

/**
 * Data Transfer Object representing the outcome of a retry attempt for a single message.
 */
@Data
@AllArgsConstructor
public class BulkRetryResultItemDTO {

    /**
     * Database identifier of the message.
     */
    private final Long messageId;

    /**
     * Short textual representation of the retry outcome (e.g., "SUCCESS", "FAILURE", "SKIPPED").
     */
    private final String outcome;

    /**
     * Detailed information about the outcome, such as error messages if the retry failed.
     */
    private final String detail;

    /**
     * The resulting publication status of the message after the retry attempt.
     */
    private final PublishStatus publishStatus;

    /**
     * The response received from the Solace broker for this specific publication attempt.
     */
    private final PublishMessageResponseDTO response;
}
