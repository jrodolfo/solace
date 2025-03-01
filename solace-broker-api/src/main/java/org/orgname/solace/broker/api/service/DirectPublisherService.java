package org.orgname.solace.broker.api.service;

import org.orgname.solace.broker.api.dto.ParameterDTO;

import java.util.Optional;

public interface DirectPublisherService {
    String sendMessage(String topicName, String content, Optional<ParameterDTO> solaceParametersOptional) throws Exception;
}
