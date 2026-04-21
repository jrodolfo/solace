package org.orgname.solace.broker.api.service;

import org.orgname.solace.broker.api.dto.MessageWrapperDTO;
import org.orgname.solace.broker.api.dto.PagedMessagesResponseDTO;
import org.orgname.solace.broker.api.jpa.Message;

public interface Database {

    Message savePendingMessage(MessageWrapperDTO wrapper);

    Message markMessagePublished(Long messageId);

    Message markMessageFailed(Long messageId, String failureReason);

    PagedMessagesResponseDTO getAllMessages(
            int page,
            int size,
            String destination,
            String deliveryMode,
            String innerMessageId,
            String sortBy,
            String sortDirection);

}
