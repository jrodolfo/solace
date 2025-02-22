package com.example.accessingdatamysql;

import org.springframework.data.repository.CrudRepository;

public interface UserPropertyRepository extends CrudRepository<UserProperty, Integer> {
}