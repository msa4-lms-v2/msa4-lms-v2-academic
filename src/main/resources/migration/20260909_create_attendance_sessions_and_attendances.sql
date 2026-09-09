-- QR 출석 세션과 학생별 출석 기록. QR 토큰은 Redis에만 저장한다.
CREATE TABLE IF NOT EXISTS attendance_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lecture_id BIGINT NOT NULL,
    session_date DATE NOT NULL,
    period TINYINT NOT NULL,
    opened_by BIGINT NOT NULL,
    opened_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at DATETIME NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    PRIMARY KEY (id),
    CONSTRAINT uk_attendance_sessions_lecture_date_period
        UNIQUE (lecture_id, session_date, period),
    CONSTRAINT ck_attendance_sessions_period CHECK (period BETWEEN 1 AND 20),
    CONSTRAINT ck_attendance_sessions_status CHECK (status IN ('OPEN', 'CLOSED')),
    CONSTRAINT fk_attendance_sessions_lecture
        FOREIGN KEY (lecture_id) REFERENCES lectures (id)
        ON DELETE RESTRICT,
    INDEX idx_attendance_sessions_lecture_status_opened
        (lecture_id, status, opened_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS attendances (
    id BIGINT NOT NULL AUTO_INCREMENT,
    enrollment_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    lecture_date DATE NOT NULL,
    period TINYINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PRESENT',
    remarks VARCHAR(255) NULL,
    check_in_time DATETIME NULL,
    is_modified TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_attendances_session_enrollment
        UNIQUE (session_id, enrollment_id),
    CONSTRAINT ck_attendances_period CHECK (period BETWEEN 1 AND 20),
    CONSTRAINT ck_attendances_status CHECK (status IN ('PRESENT', 'LATE', 'ABSENT', 'EXCUSED')),
    CONSTRAINT fk_attendances_enrollment
        FOREIGN KEY (enrollment_id) REFERENCES enrollments (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_attendances_session
        FOREIGN KEY (session_id) REFERENCES attendance_sessions (id)
        ON DELETE RESTRICT,
    INDEX idx_attendances_session_id (session_id),
    INDEX idx_attendances_enrollment_id (enrollment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
