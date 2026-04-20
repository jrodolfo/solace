package org.orgname.solace.broker.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;


@Data
public class MessageWrapperDTO {

    // Optional connection settings used only for runtime publish requests.
    private String userName;
    private String password;
    private String host;
    private String vpnName;

    // The nested message part.
    @NotNull(message = "message is required")
    @Valid
    private InnerMessageDTO message;

    public boolean parametersAreValid() {
        return (StringUtils.isNotEmpty(userName) && StringUtils.isNotEmpty(password) && StringUtils.isNotEmpty(host) && StringUtils.isNotEmpty(vpnName));
    }
}
