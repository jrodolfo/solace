package org.orgname.solace.broker.api.dto;

import lombok.Data;

@Data
public class PayloadDTO {
    private String type;
    private String content;
}