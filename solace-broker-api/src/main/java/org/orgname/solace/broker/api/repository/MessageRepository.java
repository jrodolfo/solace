package org.orgname.solace.broker.api.repository;

import org.orgname.solace.broker.api.jpa.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {}
