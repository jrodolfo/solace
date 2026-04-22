package org.orgname.solace.broker.api.dto;

import java.util.List;

public class BulkRetryResponseDTO {

    private final int totalRequested;
    private final int retriedSuccessfully;
    private final int failedToRetry;
    private final int skipped;
    private final List<BulkRetryResultItemDTO> results;

    public BulkRetryResponseDTO(
            int totalRequested,
            int retriedSuccessfully,
            int failedToRetry,
            int skipped,
            List<BulkRetryResultItemDTO> results) {
        this.totalRequested = totalRequested;
        this.retriedSuccessfully = retriedSuccessfully;
        this.failedToRetry = failedToRetry;
        this.skipped = skipped;
        this.results = results;
    }

    public int getTotalRequested() {
        return totalRequested;
    }

    public int getRetriedSuccessfully() {
        return retriedSuccessfully;
    }

    public int getFailedToRetry() {
        return failedToRetry;
    }

    public int getSkipped() {
        return skipped;
    }

    public List<BulkRetryResultItemDTO> getResults() {
        return results;
    }
}
