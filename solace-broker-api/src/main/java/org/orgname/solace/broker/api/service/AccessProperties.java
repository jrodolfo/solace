package org.orgname.solace.broker.api.service;

import org.orgname.solace.broker.api.dto.ParameterDTO;

import java.util.Properties;


public interface AccessProperties {

    static Properties getPropertiesPublisher() throws Exception {
        return null;
    }

    static Properties getPropertiesPublisher(ParameterDTO solaceParameters) throws Exception {
        return null;
    }

    static Properties getPropertiesReceiver() throws Exception {
        return null;
    }
}
