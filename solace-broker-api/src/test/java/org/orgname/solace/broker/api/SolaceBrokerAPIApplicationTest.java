package org.orgname.solace.broker.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit test for {@link SolaceBrokerAPIApplication}.
 * Verifies that the main application class can be instantiated.
 */
class SolaceBrokerAPIApplicationTest {

    @Test
    void applicationClassIsAvailable() {
        assertNotNull(new SolaceBrokerAPIApplication());
    }
}
