package net.jrodolfo.solace.broker.api.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Abstract base class for entities that require auditing of creation and modification timestamps.
 * <p>
 * This class uses JPA and Hibernate annotations to automatically track when an entity
 * was first persisted and when it was last updated. It is intended to be extended by
 * other JPA entities.
 */
@MappedSuperclass
@Data
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    /**
     * The timestamp when the entity was first created and persisted in the database.
     * This field is not updatable after initial creation.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * The timestamp when the entity was last modified in the database.
     * This field is updated automatically on every update operation.
     */
    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
