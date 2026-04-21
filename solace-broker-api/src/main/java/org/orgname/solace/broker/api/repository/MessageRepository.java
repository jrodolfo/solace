package org.orgname.solace.broker.api.repository;

import org.orgname.solace.broker.api.jpa.Message;
import org.orgname.solace.broker.api.jpa.PublishStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface MessageRepository extends JpaRepository<Message, Long>, JpaSpecificationExecutor<Message> {

    @Modifying
    @Query("update Message m set m.publishStatus = :publishStatus, m.failureReason = null, m.publishedAt = :publishedAt where m.id = :messageId")
    int markPublished(@Param("messageId") Long messageId, @Param("publishStatus") PublishStatus publishStatus, @Param("publishedAt") LocalDateTime publishedAt);

    @Modifying
    @Query("update Message m set m.publishStatus = :publishStatus, m.failureReason = :failureReason, m.publishedAt = null where m.id = :messageId")
    int markFailed(@Param("messageId") Long messageId, @Param("publishStatus") PublishStatus publishStatus, @Param("failureReason") String failureReason);
}
