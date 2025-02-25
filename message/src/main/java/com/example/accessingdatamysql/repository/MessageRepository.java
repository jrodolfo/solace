package com.example.accessingdatamysql.repository;

import com.example.accessingdatamysql.jpa.Message;
import org.springframework.data.repository.CrudRepository;

public interface MessageRepository extends CrudRepository<Message, Long> {}
