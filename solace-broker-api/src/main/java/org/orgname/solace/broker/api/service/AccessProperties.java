package org.orgname.solace.broker.api.service;

import org.orgname.solace.broker.api.model.SolaceParameters;

import java.util.Properties;


public interface AccessProperties {

    static Properties getPropertiesPublisher() throws Exception {
        return null;
    }

    static Properties getPropertiesPublisher(SolaceParameters solaceParameters) throws Exception {
        return null;
    }

    static Properties getPropertiesReceiver() throws Exception {
        return null;
    }
}
