package org.orgname.solace.broker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class BulkRetryResponseDTO {

    private final int totalRequested;
    private final int retriedSuccessfully;
    private final int failedToRetry;
    private final int skipped;
    private final List<BulkRetryResultItemDTO> results;
}
