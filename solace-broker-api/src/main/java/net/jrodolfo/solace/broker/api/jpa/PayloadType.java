package net.jrodolfo.solace.broker.api.jpa;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Defines the type of content contained within a message payload.
 */
public enum PayloadType {
    /**
     * Plain text content.
     */
    TEXT,

    /**
     * Binary data content.
     */
    BINARY,

    /**
     * JSON formatted string content.
     */
    JSON,

    /**
     * XML formatted string content.
     */
    XML;

    /**
     * Converts a string value to the corresponding {@code PayloadType}.
     *
     * @param value the string representation of the payload type
     * @return the {@link PayloadType} matching the input string, or {@code null} if input is null
     * @throws IllegalArgumentException if the provided value does not match any valid payload type
     */
    @JsonCreator
    public static PayloadType fromString(String value) {
        if (value == null) {
            return null;
        }
        return PayloadType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * Serializes the payload type to its string representation for JSON output.
     *
     * @return the name of the payload type
     */
    @JsonValue
    public String toJson() {
        return name();
    }
}
