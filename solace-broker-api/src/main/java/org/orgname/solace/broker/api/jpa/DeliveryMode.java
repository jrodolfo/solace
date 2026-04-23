package org.orgname.solace.broker.api.jpa;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum DeliveryMode {
    DIRECT,
    NON_PERSISTENT,
    PERSISTENT;

    @JsonCreator
    public static DeliveryMode fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return Arrays.stream(values())
                .filter(deliveryMode -> deliveryMode.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("deliveryMode must be one of DIRECT, NON_PERSISTENT, PERSISTENT"));
    }

    @JsonValue
    public String toJson() {
        return name();
    }
}
