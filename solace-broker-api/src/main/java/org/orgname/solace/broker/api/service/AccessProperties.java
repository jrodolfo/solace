package org.orgname.solace.broker.api.service;

import org.orgname.solace.broker.api.dto.ParameterDTO;

import java.util.Properties;


public interface AccessProperties {

    Properties getPropertiesPublisher();

    Properties getPropertiesPublisher(ParameterDTO solaceParameters);

    Properties getPropertiesReceiver();
}
