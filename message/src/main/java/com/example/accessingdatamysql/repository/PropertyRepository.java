package com.example.accessingdatamysql.repository;

import com.example.accessingdatamysql.jpa.Property;
import org.springframework.data.repository.CrudRepository;

public interface PropertyRepository extends CrudRepository<Property, Long> {}
