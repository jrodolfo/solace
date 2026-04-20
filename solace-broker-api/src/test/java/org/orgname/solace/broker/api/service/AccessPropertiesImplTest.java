package org.orgname.solace.broker.api.service;

import com.solace.messaging.config.SolaceProperties;
import com.solace.messaging.config.SolaceProperties.AuthenticationProperties;
import com.solace.messaging.config.SolaceProperties.TransportLayerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orgname.solace.broker.api.dto.ParameterDTO;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AccessPropertiesImplTest {

    private static final String HOST = "test-host";
    private static final String VPN_NAME = "test-vpn";
    private static final String USER_NAME = "test-user";
    private static final String PASSWORD = "test-pass";

    @BeforeEach
    void setUp() {
        new AccessPropertiesImpl(new StubEnvironmentConfig(Map.of(
                "SOLACE_CLOUD_HOST", HOST,
                "SOLACE_CLOUD_VPN", VPN_NAME,
                "SOLACE_CLOUD_USERNAME", USER_NAME,
                "SOLACE_CLOUD_PASSWORD", PASSWORD
        )));
    }

    @Test
    void testPropertiesFromEnvironmentVariables() throws Exception {
        Properties props = AccessPropertiesImpl.getPropertiesPublisher();

        assertProperties(props, HOST, VPN_NAME, USER_NAME, PASSWORD);
    }

    @Test
    void testPropertiesFromSolaceParameters() throws Exception {
        ParameterDTO mockParams = new ParameterDTO(HOST, VPN_NAME, USER_NAME, PASSWORD);

        Properties props = AccessPropertiesImpl.getPropertiesPublisher(mockParams);

        assertProperties(props, HOST, VPN_NAME, USER_NAME, PASSWORD);
    }

    private static void assertProperties(Properties props, String host, String vpnName, String userName, String password) {
        assertNotNull(props);
        assertEquals(host, props.getProperty(TransportLayerProperties.HOST));
        assertEquals(vpnName, props.getProperty(SolaceProperties.ServiceProperties.VPN_NAME));
        assertEquals(userName, props.getProperty(AuthenticationProperties.SCHEME_BASIC_USER_NAME));
        assertEquals(password, props.getProperty(AuthenticationProperties.SCHEME_BASIC_PASSWORD));
    }

    private static final class StubEnvironmentConfig extends EnvironmentConfigImpl {
        private final Map<String, String> values;

        private StubEnvironmentConfig(Map<String, String> values) {
            this.values = values;
        }

        @Override
        public String getEnv(String name) {
            return values.get(name);
        }
    }
}
