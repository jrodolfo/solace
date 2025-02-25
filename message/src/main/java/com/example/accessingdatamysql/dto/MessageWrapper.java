package com.example.accessingdatamysql.dto;

import lombok.Data;

@Data
public class MessageWrapper {
    // These fields are used for the Parameter table.
    private String userName;
    private String password;
    private String host;
    private String vpnName;
    private String topicName;

    // The nested message part.
    private InnerMessage message;
}