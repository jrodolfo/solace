package org.orgname.solace.broker.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class BulkRetryRequestDTO {

    private List<Long> messageIds;
}
