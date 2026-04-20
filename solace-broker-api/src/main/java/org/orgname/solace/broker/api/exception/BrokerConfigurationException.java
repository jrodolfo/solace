package org.orgname.solace.broker.api.exception;

public class BrokerConfigurationException extends RuntimeException {

    public BrokerConfigurationException(String message) {
        super(message);
    }

    public BrokerConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
