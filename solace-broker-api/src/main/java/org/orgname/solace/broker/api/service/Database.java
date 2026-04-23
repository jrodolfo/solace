package org.orgname.solace.broker.api.service;

import org.orgname.solace.broker.api.dto.MessageWrapperDTO;
import org.orgname.solace.broker.api.dto.FilteredMessagesExportResponseDTO;
import org.orgname.solace.broker.api.dto.PagedMessagesResponseDTO;
import org.orgname.solace.broker.api.jpa.DeliveryMode;
import org.orgname.solace.broker.api.jpa.Message;
import org.orgname.solace.broker.api.jpa.PayloadType;
import org.orgname.solace.broker.api.jpa.PublishStatus;

import java.time.LocalDateTime;

/**
 * Persistence contract for stored publish attempts and their lifecycle transitions.
 *
 * <p>The broker API stores publish attempts before publishing to Solace and then
 * updates the same record as it moves through {@code PENDING}, {@code PUBLISHED},
 * and {@code FAILED}. Implementations are expected to return the persisted
 * message state after each transition so controllers can build consistent DTOs.
 */
public interface Database {

    /**
     * Persists a new publish attempt in {@code PENDING} state.
     *
     * <p>This method stores message payload data and retry metadata, but it does
     * not publish to the broker.
     */
    Message savePendingMessage(MessageWrapperDTO wrapper);

    /**
     * Loads one stored publish attempt by database id.
     *
     * @throws java.util.NoSuchElementException when the id does not exist
     */
    Message findMessageById(Long messageId);

    /**
     * Marks an existing stored message as {@code PENDING}.
     *
     * <p>This is used before retrying a previously failed publish attempt.
     */
    Message markMessagePending(Long messageId);

    /**
     * Marks an existing stored message as {@code PUBLISHED} and sets
     * implementation-defined publish timestamps.
     */
    Message markMessagePublished(Long messageId);

    /**
     * Marks an existing stored message as {@code FAILED} and records the
     * supplied reason for operator visibility.
     */
    Message markMessageFailed(Long messageId, String failureReason);

    /**
     * Returns stored messages using the broker API read contract.
     *
     * <p>Filters are optional. Implementations should preserve paging and
     * sorting semantics used by the read endpoint.
     */
    PagedMessagesResponseDTO getAllMessages(
            int page,
            int size,
            String destination,
            DeliveryMode deliveryMode,
            PayloadType payloadType,
            String innerMessageId,
            PublishStatus publishStatus,
            boolean stalePendingOnly,
            LocalDateTime createdAtFrom,
            LocalDateTime createdAtTo,
            LocalDateTime publishedAtFrom,
            LocalDateTime publishedAtTo,
            String sortBy,
            String sortDirection);

    /**
     * Returns all stored messages matching the read filters in one export payload.
     *
     * <p>This uses the same filter and sort semantics as the paginated read
     * endpoint but does not apply paging.
     */
    FilteredMessagesExportResponseDTO exportMessages(
            String destination,
            DeliveryMode deliveryMode,
            PayloadType payloadType,
            String innerMessageId,
            PublishStatus publishStatus,
            boolean stalePendingOnly,
            LocalDateTime createdAtFrom,
            LocalDateTime createdAtTo,
            LocalDateTime publishedAtFrom,
            LocalDateTime publishedAtTo,
            String sortBy,
            String sortDirection);

}
