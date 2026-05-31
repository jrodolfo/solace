package net.jrodolfo.solace.broker.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Data Transfer Object representing a request to retry publishing multiple messages to the Solace broker.
 */
@Setter
@Getter
public class BulkRetryRequestDTO {

    /**
     * The list of database IDs of the messages to be retried.
     */
    private List<Long> messageIds;
}
