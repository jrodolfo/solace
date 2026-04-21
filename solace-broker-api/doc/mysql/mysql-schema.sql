-- Reference schema for the current solace-broker-api persistence model.
-- This file documents the table structure implied by the JPA entities.
-- The application model is the source of truth; keep this file aligned with:
--   - src/main/java/.../jpa/Message.java
--   - src/main/java/.../jpa/Payload.java
--   - src/main/java/.../jpa/Property.java
--   - src/main/java/.../jpa/Auditable.java

-- Main Message table.
-- Each row represents one stored publish attempt and its lifecycle state.
CREATE TABLE Message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inner_message_id VARCHAR(255) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    delivery_mode VARCHAR(255) NOT NULL,
    priority INT NOT NULL,
    publish_status VARCHAR(32) NOT NULL,
    failure_reason TEXT NULL,
    published_at DATETIME NULL,
    retry_supported BOOLEAN NOT NULL,
    retry_blocked_reason TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_message_inner_message_id ON Message(inner_message_id);
CREATE INDEX idx_message_destination ON Message(destination);
CREATE INDEX idx_message_delivery_mode ON Message(delivery_mode);
CREATE INDEX idx_message_created_at ON Message(created_at);

-- One-to-Many: each Message can have zero or more properties.
CREATE TABLE Property (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_key VARCHAR(255) NOT NULL,
    property_value VARCHAR(255) NOT NULL,
    message_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_property_message
        FOREIGN KEY (message_id) REFERENCES Message(id) ON DELETE CASCADE
);

-- One-to-One: each Message must have exactly one payload.
CREATE TABLE Payload (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    message_id BIGINT NOT NULL UNIQUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_payload_message
        FOREIGN KEY (message_id) REFERENCES Message(id) ON DELETE CASCADE
);

-- Notes:
-- 1. The older Parameter table was intentionally removed. Connection parameters
--    are no longer persisted with stored messages.
-- 2. publish_status currently stores one of: PENDING, PUBLISHED, FAILED.
-- 3. inner_message_id is required but intentionally not unique.
