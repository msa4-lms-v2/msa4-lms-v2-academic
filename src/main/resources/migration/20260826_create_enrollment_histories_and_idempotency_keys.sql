-- 수강신청과 성공 이력·멱등 응답의 원자적 저장. ERD 컬럼 그대로 사용합니다.
CREATE TABLE IF NOT EXISTS enrollment_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    lecture_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_enrollment_histories_student_id (student_id),
    INDEX idx_enrollment_histories_lecture_id (lecture_id),
    CONSTRAINT fk_enrollment_histories_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
    CONSTRAINT fk_enrollment_histories_lecture FOREIGN KEY (lecture_id) REFERENCES lectures (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS idempotency_keys (
    id BIGINT NOT NULL AUTO_INCREMENT,
    idempotency_key VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    requester_student_id BIGINT NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_snapshot JSON NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_idempotency_keys_key UNIQUE (idempotency_key),
    INDEX idx_idempotency_keys_requester (requester_student_id),
    INDEX idx_idempotency_keys_expiry (status, expires_at),
    CONSTRAINT fk_idempotency_keys_requester FOREIGN KEY (requester_student_id) REFERENCES students (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
