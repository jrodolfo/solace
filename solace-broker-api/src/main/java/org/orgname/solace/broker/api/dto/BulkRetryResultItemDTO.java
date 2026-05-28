package org.orgname.solace.broker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.orgname.solace.broker.api.jpa.PublishStatus;

@Data
@AllArgsConstructor
public class BulkRetryResultItemDTO {

    private final Long messageId;
    private final String outcome;
    private final String detail;
    private final PublishStatus publishStatus;
    private final PublishMessageResponseDTO response;
}
