package org.orgname.solace.broker.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data // Generates getters, setters, toString, equals, and hashCode
public class MessageWithParameters {

    @JsonProperty("userName")
    String userName;

    @JsonProperty("password")
    String password;

    @JsonProperty("host")
    String host;

    @JsonProperty("vpnName")
    String vpnName;

    @JsonProperty("topicName")
    String topicName;

    @JsonProperty("message")
    Message message;

    // Default no-args constructor for Jackson
    public MessageWithParameters() {}

    // All-arguments constructor
    @JsonCreator
    public MessageWithParameters(
            @JsonProperty("userName") String userName,
            @JsonProperty("password") String password,
            @JsonProperty("host") String host,
            @JsonProperty("vpnName") String vpnName,
            @JsonProperty("topicName") String topicName,
            @JsonProperty("message") Message message) {
        this.userName = userName;
        this.password = password;
        this.host = host;
        this.vpnName = vpnName;
        this.topicName = topicName;
        this.message = message;
    }
}
