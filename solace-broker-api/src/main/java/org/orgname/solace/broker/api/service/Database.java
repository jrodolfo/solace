package org.orgname.solace.broker.api.service;

import org.orgname.solace.broker.api.dto.MessageWrapperDTO;
import org.orgname.solace.broker.api.jpa.Message;

public interface Database {

    static Message saveMessage(MessageWrapperDTO wrapper) {
        return null;
    }

    static Iterable<Message> getAllMessages() {
        return null;
    }

}
