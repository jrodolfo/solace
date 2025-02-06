package org.orgname.solace.broker.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data // Generates getters, setters, toString, equals, and hashCode
public class Payload {

    /**
     * Type of the content (e.g. binary, text)
     */
    @NotBlank // Ensures the field is not null/empty
    private String type;

    /**
     * Content of the message
     */
    @NotBlank // Ensures the field is not null/empty
    private String content;

    public Payload() {
    }

    @JsonCreator
    public Payload(
            @JsonProperty("type") String type,
            @JsonProperty("content") String content) {
        this.type = type;
        this.content = content;
    }
}
