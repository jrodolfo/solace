package org.orgname.solace.broker.api.repository;

import org.orgname.solace.broker.api.jpa.Parameter;
import org.springframework.data.repository.CrudRepository;

public interface ParameterRepository extends CrudRepository<Parameter, Long> {}