package org.orgname.solace.broker.api.service;

import org.orgname.solace.broker.api.dto.ParameterDTO;
import org.orgname.solace.broker.api.dto.PublishMessageResponseDTO;

import java.util.Optional;

public interface DirectPublisherService {
    PublishMessageResponseDTO sendMessage(String topicName, String content, Optional<ParameterDTO> solaceParametersOptional);
}
