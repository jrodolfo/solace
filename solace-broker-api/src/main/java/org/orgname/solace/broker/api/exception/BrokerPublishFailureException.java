package org.orgname.solace.broker.api.exception;

public class BrokerPublishFailureException extends RuntimeException {

    public BrokerPublishFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
