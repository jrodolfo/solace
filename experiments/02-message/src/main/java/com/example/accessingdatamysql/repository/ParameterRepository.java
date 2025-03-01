package com.example.accessingdatamysql.repository;

import com.example.accessingdatamysql.jpa.Parameter;
import org.springframework.data.repository.CrudRepository;

public interface ParameterRepository extends CrudRepository<Parameter, Long> {}