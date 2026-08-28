CREATE TABLE counseling_notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    appointment_id BIGINT NOT NULL,
    recipient_user_id BIGINT NOT NULL,
    notification_type VARCHAR(40) NOT NULL,
    previous_status VARCHAR(20) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    message VARCHAR(500) NOT NULL,
    deduplication_key CHAR(64) NOT NULL,
    read_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_counseling_notifications_deduplication_key UNIQUE (deduplication_key),
    CONSTRAINT fk_counseling_notifications_appointment
        FOREIGN KEY (appointment_id) REFERENCES counseling_appointments (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_counseling_notifications_recipient
        FOREIGN KEY (recipient_user_id) REFERENCES users (id)
        ON DELETE RESTRICT,
    INDEX idx_counseling_notifications_recipient_read_created (recipient_user_id, read_at, created_at),
    INDEX idx_counseling_notifications_appointment (appointment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
