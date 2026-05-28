package org.orgname.solace.broker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PublishMessageResponseDTO {

    private final String destination;
    private final String content;
}
