package net.jrodolfo.solace.broker.api.service;

import net.jrodolfo.solace.broker.api.dto.FilteredMessagesExportResponseDTO;
import net.jrodolfo.solace.broker.api.dto.MessageWrapperDTO;
import net.jrodolfo.solace.broker.api.dto.PagedMessagesResponseDTO;
import net.jrodolfo.solace.broker.api.jpa.DeliveryMode;
import net.jrodolfo.solace.broker.api.jpa.Message;
import net.jrodolfo.solace.broker.api.jpa.PayloadType;
import net.jrodolfo.solace.broker.api.jpa.PublishStatus;

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
     *
     * @param wrapper the DTO containing the message content and properties to be saved
     * @return the persisted {@link Message} entity
     */
    Message savePendingMessage(MessageWrapperDTO wrapper);

    /**
     * Loads one stored publish attempt by database id.
     *
     * @param messageId the database ID of the message to find
     * @return the found {@link Message} entity
     * @throws java.util.NoSuchElementException when the id does not exist
     */
    Message findMessageById(Long messageId);

    /**
     * Marks an existing stored message as {@code PENDING}.
     *
     * <p>This is used before retrying a previously failed publish attempt.
     *
     * @param messageId the database ID of the message to mark as pending
     * @return the updated {@link Message} entity
     */
    Message markMessagePending(Long messageId);

    /**
     * Marks an existing stored message as {@code PUBLISHED} and sets
     * implementation-defined publish timestamps.
     *
     * @param messageId the database ID of the message to mark as published
     * @return the updated {@link Message} entity
     */
    Message markMessagePublished(Long messageId);

    /**
     * Marks an existing stored message as {@code FAILED} and records the
     * supplied reason for operator visibility.
     *
     * @param messageId     the database ID of the message to mark as failed
     * @param failureReason the reason for the failure
     * @return the updated {@link Message} entity
     */
    Message markMessageFailed(Long messageId, String failureReason);

    /**
     * Returns stored messages using the broker API read contract.
     *
     * <p>Filters are optional. Implementations should preserve paging and
     * sorting semantics used by the read endpoint.
     *
     * @param page             page number (zero-based)
     * @param size             page size
     * @param destination      filter by Solace destination/topic
     * @param deliveryMode     filter by {@link DeliveryMode}
     * @param payloadType      filter by {@link PayloadType}
     * @param innerMessageId   filter by the ID assigned by the client
     * @param publishStatus    filter by {@link PublishStatus}
     * @param stalePendingOnly if true, only return messages stuck in {@code PENDING} longer than a threshold
     * @param createdAtFrom    filter by creation start timestamp
     * @param createdAtTo      filter by creation end timestamp
     * @param publishedAtFrom  filter by publish start timestamp
     * @param publishedAtTo    filter by publish end timestamp
     * @param sortBy           field name to sort by
     * @param sortDirection    sort direction (e.g., "ASC", "DESC")
     * @return a {@link PagedMessagesResponseDTO} containing the page of messages and summary counts
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
     *
     * @param destination      filter by Solace destination/topic
     * @param deliveryMode     filter by {@link DeliveryMode}
     * @param payloadType      filter by {@link PayloadType}
     * @param innerMessageId   filter by the ID assigned by the client
     * @param publishStatus    filter by {@link PublishStatus}
     * @param stalePendingOnly if true, only return messages stuck in {@code PENDING} longer than a threshold
     * @param createdAtFrom    filter by creation start timestamp
     * @param createdAtTo      filter by creation end timestamp
     * @param publishedAtFrom  filter by publish start timestamp
     * @param publishedAtTo    filter by publish end timestamp
     * @param sortBy           field name to sort by
     * @param sortDirection    sort direction (e.g., "ASC", "DESC")
     * @return a {@link FilteredMessagesExportResponseDTO} containing all matching messages
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
