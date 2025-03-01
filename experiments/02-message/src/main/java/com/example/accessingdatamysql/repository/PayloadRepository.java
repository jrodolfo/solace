package com.example.accessingdatamysql.repository;

import com.example.accessingdatamysql.jpa.Payload;
import org.springframework.data.repository.CrudRepository;

public interface PayloadRepository extends CrudRepository<Payload, Long> {
}