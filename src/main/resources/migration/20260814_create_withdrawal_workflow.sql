CREATE TABLE withdrawal_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    student_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    requested_effective_date DATE NULL,
    effective_date DATE NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_by BIGINT NOT NULL,
    advisor_reviewed_by BIGINT NULL,
    advisor_reviewed_at DATETIME NULL,
    advisor_reject_reason VARCHAR(500) NULL,
    processed_by BIGINT NULL,
    reject_reason VARCHAR(500) NULL,
    processed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active_student_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN status IN ('PENDING', 'ADVISOR_APPROVED') THEN student_id ELSE NULL END
    ) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_withdrawal_requests_active_student UNIQUE (active_student_id),
    CONSTRAINT fk_withdrawal_requests_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
    CONSTRAINT fk_withdrawal_requests_requested_by FOREIGN KEY (requested_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_withdrawal_requests_advisor_reviewer FOREIGN KEY (advisor_reviewed_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_withdrawal_requests_processor FOREIGN KEY (processed_by) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_withdrawal_requests_student_status (student_id, status),
    INDEX idx_withdrawal_requests_status_created (status, created_at)
) ENGINE=InnoDB;

CREATE TABLE academic_status_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    previous_status VARCHAR(20) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    reason VARCHAR(500) NULL,
    changed_by BIGINT NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    source_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_academic_status_histories_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
    CONSTRAINT fk_academic_status_histories_changed_by FOREIGN KEY (changed_by) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_academic_status_histories_student_created (student_id, created_at)
) ENGINE=InnoDB;
