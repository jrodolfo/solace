package org.orgname.solace.broker.api.service;

import org.orgname.solace.broker.api.dto.MessageWrapperDTO;
import org.orgname.solace.broker.api.dto.PagedMessagesResponseDTO;
import org.orgname.solace.broker.api.jpa.Message;

public interface Database {

    Message saveMessage(MessageWrapperDTO wrapper);

    PagedMessagesResponseDTO getAllMessages(int page, int size);

}
