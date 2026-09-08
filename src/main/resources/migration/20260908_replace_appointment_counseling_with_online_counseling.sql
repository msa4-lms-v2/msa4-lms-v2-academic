DROP TABLE IF EXISTS counseling_notifications;
DROP TABLE IF EXISTS counseling_appointments;
DROP TABLE IF EXISTS counselor_availabilities;

CREATE TABLE counselings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    professor_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    answered_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_counselings_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
    CONSTRAINT fk_counselings_professor FOREIGN KEY (professor_id) REFERENCES professors (id) ON DELETE RESTRICT,
    INDEX idx_counselings_student_status_created (student_id, status, created_at),
    INDEX idx_counselings_professor_status_created (professor_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE counseling_notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    counseling_id BIGINT NOT NULL,
    recipient_user_id BIGINT NOT NULL,
    notification_type VARCHAR(40) NOT NULL,
    previous_status VARCHAR(20) NULL,
    new_status VARCHAR(20) NOT NULL,
    message VARCHAR(500) NOT NULL,
    deduplication_key CHAR(64) NOT NULL,
    read_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_counseling_notifications_deduplication_key UNIQUE (deduplication_key),
    CONSTRAINT fk_counseling_notifications_counseling
        FOREIGN KEY (counseling_id) REFERENCES counselings (id) ON DELETE CASCADE,
    CONSTRAINT fk_counseling_notifications_recipient
        FOREIGN KEY (recipient_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_counseling_notifications_recipient_read_created (recipient_user_id, read_at, created_at),
    INDEX idx_counseling_notifications_counseling (counseling_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
