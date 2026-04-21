package org.orgname.solace.broker.api.dto;

public class PublishMessageResponseDTO {

    private final String destination;
    private final String content;

    public PublishMessageResponseDTO(String destination, String content) {
        this.destination = destination;
        this.content = content;
    }

    public String getDestination() {
        return destination;
    }

    public String getContent() {
        return content;
    }
}
