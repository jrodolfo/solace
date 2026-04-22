package org.orgname.solace.broker.api.dto;

import java.util.List;

public class BulkRetryRequestDTO {

    private List<Long> messageIds;

    public List<Long> getMessageIds() {
        return messageIds;
    }

    public void setMessageIds(List<Long> messageIds) {
        this.messageIds = messageIds;
    }
}
