package net.jrodolfo.solace.broker.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;


/**
 * Data Transfer Object that wraps a message along with optional Solace connection parameters.
 * This is used for requests that trigger immediate publication to a broker.
 */
@Data
public class MessageWrapperDTO {

    /**
     * Optional Solace username for authentication during publication.
     */
    private String userName;

    /**
     * Optional Solace password for authentication during publication.
     */
    private String password;

    /**
     * Optional Solace broker host (e.g., "tcp://localhost:55555").
     */
    private String host;

    /**
     * Optional Message VPN name on the Solace broker.
     */
    private String vpnName;

    /**
     * The nested message data to be published.
     */
    @NotNull(message = "message is required")
    @Valid
    private InnerMessageDTO message;

    /**
     * Validates whether all required Solace connection parameters are present.
     *
     * @return {@code true} if all connection fields are non-empty; {@code false} otherwise.
     */
    public boolean parametersAreValid() {
        return (StringUtils.isNotBlank(userName) && StringUtils.isNotBlank(password) && StringUtils.isNotBlank(host) && StringUtils.isNotBlank(vpnName));
    }

    /**
     * Detects malformed custom broker credentials where only some connection fields were supplied.
     *
     * @return {@code true} if at least one connection field is present but the full set is not.
     */
    public boolean hasPartialConnectionParameters() {
        boolean hasAnyParameter = StringUtils.isNotBlank(userName)
                || StringUtils.isNotBlank(password)
                || StringUtils.isNotBlank(host)
                || StringUtils.isNotBlank(vpnName);
        return hasAnyParameter && !parametersAreValid();
    }
}
