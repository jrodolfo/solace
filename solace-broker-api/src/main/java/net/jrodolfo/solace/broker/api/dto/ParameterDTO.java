package net.jrodolfo.solace.broker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object containing connection parameters for the Solace broker.
 * This includes connection endpoint and authentication details.
 */
@Data // Generates getters, setters, toString, equals, and hashCode
@AllArgsConstructor // Generates a constructor with all fields
@NoArgsConstructor // Generates a no-argument constructor
public class ParameterDTO {
    /**
     * The Solace broker host URL (e.g., "tcp://localhost:55555").
     */
    String host;

    /**
     * The Message VPN name on the Solace broker.
     */
    String vpnName;

    /**
     * The username for authentication.
     */
    String userName;

    /**
     * The password for authentication.
     */
    String password;
}
