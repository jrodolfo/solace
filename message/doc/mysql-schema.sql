-- Main Message table. Note: we use "inner_message_id" to store the message’s own id.
CREATE TABLE Message (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         inner_message_id VARCHAR(255) NOT NULL,
                         destination VARCHAR(255),
                         delivery_mode VARCHAR(50),
                         priority INT,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- One-to-Many: Each Message can have 0 or more Properties.
CREATE TABLE Property (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          property_key VARCHAR(255) NOT NULL,
                          property_value VARCHAR(255),
                          message_id BIGINT NOT NULL,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          CONSTRAINT fk_property_message FOREIGN KEY (message_id) REFERENCES Message(id) ON DELETE CASCADE
);

-- One-to-One: Each Message must have one Payload.
CREATE TABLE Payload (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         type VARCHAR(50) NOT NULL,
                         content TEXT,
                         message_id BIGINT NOT NULL UNIQUE,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         CONSTRAINT fk_payload_message FOREIGN KEY (message_id) REFERENCES Message(id) ON DELETE CASCADE
);

-- One-to-One (optional): Each Message may have one Parameter set.
CREATE TABLE Parameter (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           user_name VARCHAR(255) NOT NULL,
                           password VARCHAR(255) NOT NULL,
                           host VARCHAR(255) NOT NULL,
                           vpn_name VARCHAR(255) NOT NULL,
                           topic_name VARCHAR(255) NOT NULL,
                           message_id BIGINT UNIQUE,  -- a Message may have 0 or 1 Parameter
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           CONSTRAINT fk_parameter_message FOREIGN KEY (message_id) REFERENCES Message(id) ON DELETE CASCADE
);
