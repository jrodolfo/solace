package org.orgname.solace.broker.api.exception;

public class BrokerConnectionException extends RuntimeException {

    public BrokerConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
