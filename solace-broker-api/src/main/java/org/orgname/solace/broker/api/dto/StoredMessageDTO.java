package org.orgname.solace.broker.api.dto;

import org.orgname.solace.broker.api.jpa.Message;
import org.orgname.solace.broker.api.jpa.PublishStatus;
import org.orgname.solace.broker.api.jpa.Property;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StoredMessageDTO {

    private final Long id;
    private final String innerMessageId;
    private final String destination;
    private final String deliveryMode;
    private final Integer priority;
    private final PublishStatus publishStatus;
    private final String failureReason;
    private final LocalDateTime publishedAt;
    private final Map<String, String> properties;
    private final StoredMessagePayloadDTO payload;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public StoredMessageDTO(Message message) {
        this.id = message.getId();
        this.innerMessageId = message.getInnerMessageId();
        this.destination = message.getDestination();
        this.deliveryMode = message.getDeliveryMode();
        this.priority = message.getPriority();
        this.publishStatus = message.getPublishStatus();
        this.failureReason = message.getFailureReason();
        this.publishedAt = message.getPublishedAt();
        this.properties = toPropertyMap(message.getProperties());
        this.payload = new StoredMessagePayloadDTO(message.getPayload());
        this.createdAt = message.getCreatedAt();
        this.updatedAt = message.getUpdatedAt();
    }

    private static Map<String, String> toPropertyMap(List<Property> properties) {
        Map<String, String> propertyMap = new LinkedHashMap<>();
        if (properties == null) {
            return propertyMap;
        }

        for (Property property : properties) {
            propertyMap.put(property.getPropertyKey(), property.getPropertyValue());
        }
        return propertyMap;
    }

    public Long getId() {
        return id;
    }

    public String getInnerMessageId() {
        return innerMessageId;
    }

    public String getDestination() {
        return destination;
    }

    public String getDeliveryMode() {
        return deliveryMode;
    }

    public Integer getPriority() {
        return priority;
    }

    public PublishStatus getPublishStatus() {
        return publishStatus;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public StoredMessagePayloadDTO getPayload() {
        return payload;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
