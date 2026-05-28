package org.orgname.solace.broker.api.jpa;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Enumeration of Solace message delivery modes.
 * <p>
 * Defines how the Solace PubSub+ Broker should handle the message delivery persistence.
 */
public enum DeliveryMode {
    /**
     * The broker does not store direct messages. They are high-throughput and low-latency,
     * but can be lost if the consumer is not online or if there is a network issue.
     */
    DIRECT,

    /**
     * Non-persistent messages are similar to direct messages but are intended for use with
     * guaranteed messaging. They are not persisted to disk.
     */
    NON_PERSISTENT,

    /**
     * Persistent messages are stored on the broker's disk. They are guaranteed to be delivered
     * even if the broker or consumer restarts.
     */
    PERSISTENT;

    /**
     * Converts a string value to the corresponding {@code DeliveryMode}.
     *
     * @param value the string representation of the delivery mode
     * @return the {@link DeliveryMode} matching the input string, or {@code null} if input is null or empty
     * @throws IllegalArgumentException if the provided value does not match any valid delivery mode
     */
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

    /**
     * Serializes the delivery mode to its string representation for JSON output.
     *
     * @return the name of the delivery mode
     */
    @JsonValue
    public String toJson() {
        return name();
    }
}
