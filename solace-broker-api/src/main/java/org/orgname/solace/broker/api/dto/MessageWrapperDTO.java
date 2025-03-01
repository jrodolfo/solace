package org.orgname.solace.broker.api.dto;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;


@Data
public class MessageWrapperDTO {

    // These fields are used for the Parameter table.
    private String userName;
    private String password;
    private String host;
    private String vpnName;

    // The nested message part.
    private InnerMessageDTO message;

    public boolean parametersAreValid() {
        return (StringUtils.isNotEmpty(userName) && StringUtils.isNotEmpty(password) && StringUtils.isNotEmpty(host) && StringUtils.isNotEmpty(vpnName));
    }
}