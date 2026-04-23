package org.orgname.solace.broker.api.service;

import org.orgname.solace.broker.api.dto.ParameterDTO;
import org.orgname.solace.broker.api.dto.PublishMessageResponseDTO;
import org.orgname.solace.broker.api.jpa.DeliveryMode;

import java.util.Optional;

/**
 * Contract for sending one message to a Solace destination.
 *
 * <p>The controller decides whether to pass explicit per-request broker
 * parameters or to rely on server-side configuration. Implementations are
 * responsible only for broker interaction and success-response creation.
 */
public interface DirectPublisherService {

    /**
     * Publishes a message to the supplied Solace topic.
     *
     * @param topicName Solace destination/topic name
     * @param content outbound payload content
     * @param solaceParametersOptional optional per-request broker parameters;
     *                                 empty means use server-side configuration
     * @return typed success response for the HTTP layer
     * @throws IllegalArgumentException when input is invalid
     * @throws RuntimeException when broker configuration, connection, or publish
     *                          steps fail
     */
    PublishMessageResponseDTO sendMessage(String topicName, String content, DeliveryMode deliveryMode, Optional<ParameterDTO> solaceParametersOptional);
}
