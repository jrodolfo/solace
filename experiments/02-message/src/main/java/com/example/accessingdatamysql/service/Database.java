package com.example.accessingdatamysql.service;

import com.example.accessingdatamysql.dto.MessageWrapper;
import com.example.accessingdatamysql.jpa.Message;

import java.util.Properties;

public interface Database {

    static Message saveMessage(MessageWrapper wrapper) {
        return null;
    }

    static Iterable<Message> getAllMessages() {
        return null;
    }

}
