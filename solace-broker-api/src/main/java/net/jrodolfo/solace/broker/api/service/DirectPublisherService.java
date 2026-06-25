package net.jrodolfo.solace.broker.api.service;

import net.jrodolfo.solace.broker.api.dto.ParameterDTO;
import net.jrodolfo.solace.broker.api.dto.PublishMessageResponseDTO;
import net.jrodolfo.solace.broker.api.jpa.DeliveryMode;

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
     * @param topicName                Solace destination/topic name
     * @param content                  outbound payload content
     * @param deliveryMode             the {@link DeliveryMode} to use (Direct or Persistent)
     * @param priority                 Solace message priority, from 0 through 255
     * @param solaceParametersOptional optional per-request broker parameters;
     *                                 empty means use server-side configuration
     * @return typed success response for the HTTP layer
     * @throws IllegalArgumentException when input is invalid
     * @throws RuntimeException         when broker configuration, connection, or publish
     *                                  steps fail
     */
    PublishMessageResponseDTO sendMessage(String topicName, String content, DeliveryMode deliveryMode, Integer priority, Optional<ParameterDTO> solaceParametersOptional);
}
