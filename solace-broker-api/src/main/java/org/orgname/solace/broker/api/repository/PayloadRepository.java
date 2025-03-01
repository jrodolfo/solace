package org.orgname.solace.broker.api.repository;

import org.orgname.solace.broker.api.jpa.Payload;
import org.springframework.data.repository.CrudRepository;

public interface PayloadRepository extends CrudRepository<Payload, Long> {
}