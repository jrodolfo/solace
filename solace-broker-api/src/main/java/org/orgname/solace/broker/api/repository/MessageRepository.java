package org.orgname.solace.broker.api.repository;

import org.orgname.solace.broker.api.jpa.Message;
import org.orgname.solace.broker.api.jpa.PublishStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/**
 * Repository interface for {@link Message} entities.
 * <p>
 * Provides standard CRUD operations and custom update methods to manage message publishing state.
 * This repository is used to track the lifecycle of messages as they are processed and sent to the Solace broker.
 */
public interface MessageRepository extends JpaRepository<Message, Long>, JpaSpecificationExecutor<Message> {

    /**
     * Updates the status of a message to indicate it has been successfully published to the Solace broker.
     *
     * @param messageId     The unique identifier of the message.
     * @param publishStatus The {@link PublishStatus} indicating success (e.g., {@code PUBLISHED}).
     * @param publishedAt   The timestamp when the message was successfully published.
     * @return The number of records updated (should be 1).
     */
    @Modifying
    @Query("update Message m set m.publishStatus = :publishStatus, m.failureReason = null, m.publishedAt = :publishedAt where m.id = :messageId")
    int markPublished(@Param("messageId") Long messageId, @Param("publishStatus") PublishStatus publishStatus, @Param("publishedAt") LocalDateTime publishedAt);

    /**
     * Resets the status of a message to pending.
     * <p>
     * Useful when a message needs to be retried or is being prepared for publishing.
     * Clears any previous failure reasons and publication timestamps.
     *
     * @param messageId     The unique identifier of the message.
     * @param publishStatus The {@link PublishStatus} indicating it is pending (e.g., {@code PENDING}).
     * @return The number of records updated.
     */
    @Modifying
    @Query("update Message m set m.publishStatus = :publishStatus, m.failureReason = null, m.publishedAt = null where m.id = :messageId")
    int markPending(@Param("messageId") Long messageId, @Param("publishStatus") PublishStatus publishStatus);

    /**
     * Updates the status of a message to indicate a failure during the publishing process.
     * <p>
     * Records the reason for the failure and ensures the publication timestamp is cleared.
     *
     * @param messageId     The unique identifier of the message.
     * @param publishStatus The {@link PublishStatus} indicating failure (e.g., {@code FAILED}).
     * @param failureReason A description of why the message could not be published.
     * @return The number of records updated.
     */
    @Modifying
    @Query("update Message m set m.publishStatus = :publishStatus, m.failureReason = :failureReason, m.publishedAt = null where m.id = :messageId")
    int markFailed(@Param("messageId") Long messageId, @Param("publishStatus") PublishStatus publishStatus, @Param("failureReason") String failureReason);
}
