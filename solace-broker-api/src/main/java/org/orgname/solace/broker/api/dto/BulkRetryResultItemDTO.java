package org.orgname.solace.broker.api.dto;

import org.orgname.solace.broker.api.jpa.PublishStatus;

public class BulkRetryResultItemDTO {

    private final Long messageId;
    private final String outcome;
    private final String detail;
    private final PublishStatus publishStatus;
    private final PublishMessageResponseDTO response;

    public BulkRetryResultItemDTO(
            Long messageId,
            String outcome,
            String detail,
            PublishStatus publishStatus,
            PublishMessageResponseDTO response) {
        this.messageId = messageId;
        this.outcome = outcome;
        this.detail = detail;
        this.publishStatus = publishStatus;
        this.response = response;
    }

    public Long getMessageId() {
        return messageId;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getDetail() {
        return detail;
    }

    public PublishStatus getPublishStatus() {
        return publishStatus;
    }

    public PublishMessageResponseDTO getResponse() {
        return response;
    }
}
