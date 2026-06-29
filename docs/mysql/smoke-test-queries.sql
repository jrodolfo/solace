-- Basic schema checks:
SHOW DATABASES;
USE solace;
SHOW TABLES;
DESCRIBE message;
DESCRIBE payload;
DESCRIBE property;

-- Delete all rows from the table:
-- DELETE FROM property;
-- DELETE FROM payload;
-- DELETE FROM message;

-- Quick table checks:
SELECT * FROM message;
SELECT * FROM payload;
SELECT * FROM property;

-- View the latest stored publish attempts:
SELECT
    id,
    inner_message_id,
    destination,
    delivery_mode,
    priority,
    publish_status,
    failure_reason,
    published_at,
    retry_supported,
    retry_blocked_reason,
    created_at,
    updated_at
FROM message
ORDER BY id DESC;

-- View message payloads:
SELECT
    id,
    type,
    content,
    message_id,
    created_at,
    updated_at
FROM payload
ORDER BY id DESC;

-- View message properties:
SELECT
    id,
    property_key,
    property_value,
    message_id,
    created_at,
    updated_at
FROM property
ORDER BY id DESC;

-- View messages with their payloads:
SELECT
    m.id,
    m.inner_message_id,
    m.destination,
    m.delivery_mode,
    m.publish_status,
    p.type AS payload_type,
    p.content AS payload_content,
    m.created_at,
    m.updated_at
FROM message m
         JOIN payload p ON p.message_id = m.id
ORDER BY m.id DESC;
