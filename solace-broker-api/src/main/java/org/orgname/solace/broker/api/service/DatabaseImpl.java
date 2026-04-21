package org.orgname.solace.broker.api.service;

import org.orgname.solace.broker.api.dto.InnerMessageDTO;
import org.orgname.solace.broker.api.dto.MessageWrapperDTO;
import org.orgname.solace.broker.api.dto.PagedMessagesResponseDTO;
import org.orgname.solace.broker.api.dto.PayloadDTO;
import org.orgname.solace.broker.api.jpa.Message;
import org.orgname.solace.broker.api.jpa.Payload;
import org.orgname.solace.broker.api.jpa.PublishStatus;
import org.orgname.solace.broker.api.jpa.Property;
import org.orgname.solace.broker.api.repository.MessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class DatabaseImpl implements Database {

    private final MessageRepository messageRepository;

    public DatabaseImpl(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public PagedMessagesResponseDTO getAllMessages(
            int page,
            int size,
            String destination,
            String deliveryMode,
            String innerMessageId,
            String sortBy,
            String sortDirection) {
        Specification<Message> specification = Specification.where(null);

        if (hasText(destination)) {
            specification = specification.and(stringContains("destination", destination));
        }
        if (hasText(deliveryMode)) {
            specification = specification.and(stringContains("deliveryMode", deliveryMode));
        }
        if (hasText(innerMessageId)) {
            specification = specification.and(stringContains("innerMessageId", innerMessageId));
        }

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return PagedMessagesResponseDTO.fromMessages(messageRepository.findAll(specification, pageRequest));
    }

    @Override
    public Message savePendingMessage(MessageWrapperDTO wrapper) {

        // Create the main Message entity
        Message message = new Message();
        InnerMessageDTO inner = wrapper.getMessage();
        message.setInnerMessageId(inner.getInnerMessageId());
        message.setDestination(inner.getDestination());
        message.setDeliveryMode(inner.getDeliveryMode());
        message.setPriority(inner.getPriority());
        message.setPublishStatus(PublishStatus.PENDING);
        message.setFailureReason(null);
        message.setPublishedAt(null);

        // Create and attach Payload entity
        Payload payload = new Payload();
        PayloadDTO payloadDTO = inner.getPayload();
        payload.setType(payloadDTO.getType());
        payload.setContent(payloadDTO.getContent());
        payload.setMessage(message);
        message.setPayload(payload);

        // Create and attach Property entities if provided
        Map<String, String> propMap = inner.getProperties();
        if (propMap != null && !propMap.isEmpty()) {
            List<Property> properties = new ArrayList<>();
            propMap.forEach((key, value) -> {
                Property prop = new Property();
                prop.setPropertyKey(key);
                prop.setPropertyValue(value);
                prop.setMessage(message);
                properties.add(prop);
            });
            message.setProperties(properties);
        }

        // Save the entire structure (cascade persists related entities)
        messageRepository.save(message);
        return message;
    }

    @Override
    @Transactional
    public Message markMessagePublished(Long messageId) {
        messageRepository.markPublished(messageId, PublishStatus.PUBLISHED, LocalDateTime.now());
        return getRequiredMessage(messageId);
    }

    @Override
    @Transactional
    public Message markMessageFailed(Long messageId, String failureReason) {
        messageRepository.markFailed(messageId, PublishStatus.FAILED, failureReason);
        return getRequiredMessage(messageId);
    }

    private Message getRequiredMessage(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("Message not found for id " + messageId));
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static Specification<Message> stringContains(String fieldName, String value) {
        String normalizedValue = "%" + value.trim().toLowerCase() + "%";
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get(fieldName)), normalizedValue);
    }

}
