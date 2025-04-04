package com.example.accessingdatamysql.service;

import com.example.accessingdatamysql.dto.InnerMessage;
import com.example.accessingdatamysql.dto.MessageWrapper;
import com.example.accessingdatamysql.dto.PayloadDTO;
import com.example.accessingdatamysql.jpa.Message;
import com.example.accessingdatamysql.jpa.Parameter;
import com.example.accessingdatamysql.jpa.Payload;
import com.example.accessingdatamysql.jpa.Property;
import com.example.accessingdatamysql.repository.MessageRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DatabaseImpl implements Database {

    static private MessageRepository messageRepository;

    public DatabaseImpl(MessageRepository messageRepository) {
        DatabaseImpl.messageRepository = messageRepository;
    }

    static public Iterable<Message> getAllMessages() {
        return messageRepository.findAll();
    }

    static public Message saveMessage(MessageWrapper wrapper) {

        // Create the main Message entity
        Message message = new Message();
        InnerMessage inner = wrapper.getMessage();
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

        // Create and attach Parameter entity using top-level fields
        Parameter parameter = new Parameter();
        parameter.setUserName(wrapper.getUserName());
        parameter.setPassword(wrapper.getPassword());
        parameter.setHost(wrapper.getHost());
        parameter.setVpnName(wrapper.getVpnName());
        parameter.setTopicName(wrapper.getTopicName());
        parameter.setMessage(message);
        message.setParameter(parameter);

        // Save the entire structure (cascade persists related entities)
        messageRepository.save(message);
        return message;
    }

}
