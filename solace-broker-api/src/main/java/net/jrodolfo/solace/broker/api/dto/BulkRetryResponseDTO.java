package net.jrodolfo.solace.broker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Data Transfer Object summarizing the result of a bulk retry operation.
 * It contains aggregate counts and detailed results for each attempted message retry.
 */
@Data
@AllArgsConstructor
public class BulkRetryResponseDTO {

    /**
     * Total number of messages requested for retry.
     */
    private final int totalRequested;

    /**
     * Number of messages successfully published to the Solace broker during retry.
     */
    private final int retriedSuccessfully;

    /**
     * Number of messages that failed to be published to the Solace broker during retry.
     */
    private final int failedToRetry;

    /**
     * Number of messages that were skipped (e.g., if they were already in a terminal state
     * or retry is not supported for their current status).
     */
    private final int skipped;

    /**
     * Detailed result items for each message processed in the bulk request.
     */
    private final List<BulkRetryResultItemDTO> results;
}
