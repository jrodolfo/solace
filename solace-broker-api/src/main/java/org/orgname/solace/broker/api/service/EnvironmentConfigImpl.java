package org.orgname.solace.broker.api.service;

import org.springframework.stereotype.Service;

@Service
public class EnvironmentConfigImpl implements EnvironmentConfig {

    public String getEnv(String name) {
        return System.getenv(name);
    }
}
