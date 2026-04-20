package org.orgname.solace.broker.api.service;

import org.orgname.solace.broker.api.dto.ParameterDTO;

import java.util.Properties;


public interface AccessProperties {

    Properties getPropertiesPublisher() throws Exception;

    Properties getPropertiesPublisher(ParameterDTO solaceParameters) throws Exception;

    Properties getPropertiesReceiver() throws Exception;
}
