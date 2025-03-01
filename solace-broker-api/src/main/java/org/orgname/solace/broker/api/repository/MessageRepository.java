package org.orgname.solace.broker.api.repository;

import org.orgname.solace.broker.api.jpa.Message;
import org.springframework.data.repository.CrudRepository;

public interface MessageRepository extends CrudRepository<Message, Long> {}
