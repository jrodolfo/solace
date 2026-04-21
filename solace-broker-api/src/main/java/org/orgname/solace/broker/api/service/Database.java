package org.orgname.solace.broker.api.service;

import org.orgname.solace.broker.api.dto.MessageWrapperDTO;
import org.orgname.solace.broker.api.dto.PagedMessagesResponseDTO;
import org.orgname.solace.broker.api.jpa.Message;
import org.orgname.solace.broker.api.jpa.PublishStatus;

import java.time.LocalDateTime;

public interface Database {

    Message savePendingMessage(MessageWrapperDTO wrapper);

    Message findMessageById(Long messageId);

    Message markMessagePending(Long messageId);

    Message markMessagePublished(Long messageId);

    Message markMessageFailed(Long messageId, String failureReason);

    PagedMessagesResponseDTO getAllMessages(
            int page,
            int size,
            String destination,
            String deliveryMode,
            String innerMessageId,
            PublishStatus publishStatus,
            LocalDateTime createdAtFrom,
            LocalDateTime createdAtTo,
            LocalDateTime publishedAtFrom,
            LocalDateTime publishedAtTo,
            String sortBy,
            String sortDirection);

}
