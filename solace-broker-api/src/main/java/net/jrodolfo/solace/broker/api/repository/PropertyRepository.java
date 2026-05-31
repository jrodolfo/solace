package net.jrodolfo.solace.broker.api.repository;

import net.jrodolfo.solace.broker.api.jpa.Property;
import org.springframework.data.repository.CrudRepository;

/**
 * Repository interface for {@link Property} entities.
 * <p>
 * Provides standard CRUD operations to manage message property data.
 * Properties are typically used for message headers or user-defined metadata.
 */
public interface PropertyRepository extends CrudRepository<Property, Long> {
}
