package org.orgname.solace.broker.api.repository;

import org.orgname.solace.broker.api.jpa.Payload;
import org.springframework.data.repository.CrudRepository;

/**
 * Repository interface for {@link Payload} entities.
 * <p>
 * Provides standard CRUD operations to manage message payload data.
 */
public interface PayloadRepository extends CrudRepository<Payload, Long> {
}