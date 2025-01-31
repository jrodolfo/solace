package org.orgname.solace.broker.api.service;

import org.orgname.solace.broker.api.model.SolaceParameters;

import java.util.Optional;

public interface DirectPublisherService {
    String sendMessage(String topicName, String content, Optional<SolaceParameters> solaceParametersOptional) throws Exception;
}
