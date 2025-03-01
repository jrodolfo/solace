package org.orgname.solace.broker.api.repository;

import org.orgname.solace.broker.api.jpa.Property;
import org.springframework.data.repository.CrudRepository;

public interface PropertyRepository extends CrudRepository<Property, Long> {}
