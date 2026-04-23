package org.orgname.solace.broker.api.jpa;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum PayloadType {
    TEXT,
    BINARY,
    JSON,
    XML;

    @JsonCreator
    public static PayloadType fromString(String value) {
        if (value == null) {
            return null;
        }
        return PayloadType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    @JsonValue
    public String toJson() {
        return name();
    }
}
