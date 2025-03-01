package org.orgname.solace.broker.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orgname.solace.broker.api.dto.ParameterDTO;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AccessPropertiesImplTest {

    // private final String INDEX_RETRIES_PER_HOST = "solace.messaging.transport.connection.retries-per-host"; // 5
    private final String INDEX_HOST_URL = "solace.messaging.transport.host"; // test-host
    private final String INDEX_VPN_NAME = "solace.messaging.service.vpn-name"; // test-vpn
    private final String INDEX_PASSWORD = "solace.messaging.authentication.basic.password"; // test-pass
    private final String INDEX_USER_NAME = "solace.messaging.authentication.basic.username"; // test-user
    // private final String INDEX_RECONNECTION_ATTEMPTS = "solace.messaging.transport.reconnection-attempts"; // 20

    private EnvironmentConfigImpl environmentConfigImpl;
    private AccessPropertiesImpl accessPropertiesImpl;

    @BeforeEach
    void setUp() {
        // Create a new instance for each test to ensure a clean slate
        environmentConfigImpl = new EnvironmentConfigImpl();
        accessPropertiesImpl = new AccessPropertiesImpl(environmentConfigImpl);
    }

    @Test
    void testPropertiesFromEnvironmentVariables() throws Exception {
        // If AccessPropertiesImpl reads from environment variables, you can
        // use a library like System Stubs or set environment variables in
        // your CI/test configuration. The following is just placeholder logic:

        // Example: verifying that calling getPropertiesPublisher() returns non-null
        Properties props = accessPropertiesImpl.getPropertiesPublisher();
        assertNotNull(props, "Properties object should not be null");

        // If environment variables are actually set, you can validate them here
        // Example (adjust keys/values as needed):
        // assertEquals("someValue", props.getProperty("host"));
    }

    @Test
    void testPropertiesFromSolaceParameters() throws Exception {
        // Create a ParameterDTO instance
        ParameterDTO mockParams = new ParameterDTO();
        mockParams.setHost("test-host");
        mockParams.setVpnName("test-vpn");
        mockParams.setUserName("test-user");
        mockParams.setPassword("test-pass");

        // Invoke the method that uses ParameterDTO
        Properties props = accessPropertiesImpl.getPropertiesPublisher(mockParams);
        assertNotNull(props, "Properties object should not be null");

        // Verify the properties match the parameters
        assertEquals("test-host", props.getProperty(INDEX_HOST_URL));
        assertEquals("test-vpn", props.getProperty(INDEX_VPN_NAME));
        assertEquals("test-user", props.getProperty(INDEX_USER_NAME));
        assertEquals("test-pass", props.getProperty(INDEX_PASSWORD));
    }
}
