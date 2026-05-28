package org.orgname.solace.broker.api.service;

import org.springframework.stereotype.Service;

/**
 * Implementation of {@link EnvironmentConfig} that retrieves values from the system environment.
 */
@Service
public class EnvironmentConfigImpl implements EnvironmentConfig {

    /**
     * {@inheritDoc}
     */
    public String getEnv(String name) {
        return System.getenv(name);
    }
}
