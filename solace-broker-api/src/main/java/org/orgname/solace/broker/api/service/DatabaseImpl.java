package org.orgname.solace.broker.api.service;

import org.orgname.solace.broker.api.dto.InnerMessageDTO;
import org.orgname.solace.broker.api.dto.MessageWrapperDTO;
import org.orgname.solace.broker.api.dto.PayloadDTO;
import org.orgname.solace.broker.api.jpa.Message;
import org.orgname.solace.broker.api.jpa.Payload;
import org.orgname.solace.broker.api.jpa.Property;
import org.orgname.solace.broker.api.repository.MessageRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DatabaseImpl implements Database {

    private final MessageRepository messageRepository;

    public DatabaseImpl(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public Iterable<Message> getAllMessages() {
        return messageRepository.findAll();
    }

    @Override
    public Message saveMessage(MessageWrapperDTO wrapper) {

        // Create the main Message entity
        Message message = new Message();
        InnerMessageDTO inner = wrapper.getMessage();
        message.setInnerMessageId(inner.getInnerMessageId());
        message.setDestination(inner.getDestination());
        message.setDeliveryMode(inner.getDeliveryMode());
        message.setPriority(inner.getPriority());

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

}
