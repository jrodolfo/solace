package org.orgname.solace.broker.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SolaceBrokerAPIApplicationTest {

    @Test
    void applicationClassIsAvailable() {
        assertNotNull(new SolaceBrokerAPIApplication());
    }
}
