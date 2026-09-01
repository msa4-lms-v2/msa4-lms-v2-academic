CREATE TABLE IF NOT EXISTS excuse_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    enrollment_id BIGINT NOT NULL,
    lecture_date DATE NOT NULL,
    period TINYINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reject_reason VARCHAR(500) NULL,
    attachment_original_name VARCHAR(255) NULL,
    attachment_stored_name VARCHAR(255) NULL,
    attachment_content_type VARCHAR(100) NULL,
    attachment_size BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_excuse_requests_enrollment_date_period
        UNIQUE (enrollment_id, lecture_date, period),
    CONSTRAINT ck_excuse_requests_period CHECK (period BETWEEN 1 AND 20),
    CONSTRAINT fk_excuse_requests_enrollment
        FOREIGN KEY (enrollment_id) REFERENCES enrollments (id)
        ON DELETE RESTRICT,
    INDEX idx_excuse_requests_enrollment_id (enrollment_id),
    INDEX idx_excuse_requests_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
