-- 전과와 향후 복수전공이 공유하는 적용 학기별 접수 기간. 유형·학기별 한 설정만 허용합니다.
CREATE TABLE IF NOT EXISTS academic_change_request_periods (
    id BIGINT NOT NULL AUTO_INCREMENT,
    semester_id BIGINT NOT NULL,
    request_type VARCHAR(20) NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_academic_change_periods_semester_type UNIQUE (semester_id, request_type),
    CONSTRAINT fk_academic_change_periods_semester FOREIGN KEY (semester_id) REFERENCES semesters (id) ON DELETE RESTRICT,
    CONSTRAINT ck_academic_change_periods_request_type CHECK (
        request_type IN ('TRANSFER_DEPARTMENT', 'DOUBLE_MAJOR')
    ),
    CONSTRAINT ck_academic_change_periods_range CHECK (start_at < end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 전과/복수전공 공용 신청 원본. 현재 구현은 TRANSFER_DEPARTMENT만 생성합니다.
CREATE TABLE IF NOT EXISTS academic_change_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    request_type VARCHAR(20) NOT NULL,
    source_department_id BIGINT NOT NULL,
    source_major_id BIGINT NULL,
    target_department_id BIGINT NOT NULL,
    target_major_id BIGINT NOT NULL,
    target_semester_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reject_reason VARCHAR(500) NULL,
    processed_by BIGINT NULL,
    processed_at DATETIME NULL,
    cancel_reason VARCHAR(500) NULL,
    cancelled_by BIGINT NULL,
    cancelled_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active_student_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN status = 'PENDING' THEN student_id ELSE NULL END
    ) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_academic_change_requests_active_type UNIQUE (request_type, active_student_id),
    INDEX idx_academic_change_requests_student_status (student_id, status),
    INDEX idx_academic_change_requests_status_created (status, created_at),
    INDEX idx_academic_change_requests_target_semester (target_semester_id),
    CONSTRAINT fk_academic_change_requests_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
    CONSTRAINT fk_academic_change_requests_source_department FOREIGN KEY (source_department_id) REFERENCES departments (id) ON DELETE RESTRICT,
    CONSTRAINT fk_academic_change_requests_source_major FOREIGN KEY (source_major_id) REFERENCES majors (id) ON DELETE RESTRICT,
    CONSTRAINT fk_academic_change_requests_target_department FOREIGN KEY (target_department_id) REFERENCES departments (id) ON DELETE RESTRICT,
    CONSTRAINT fk_academic_change_requests_target_major FOREIGN KEY (target_major_id) REFERENCES majors (id) ON DELETE RESTRICT,
    CONSTRAINT fk_academic_change_requests_target_semester FOREIGN KEY (target_semester_id) REFERENCES semesters (id) ON DELETE RESTRICT,
    CONSTRAINT fk_academic_change_requests_processor FOREIGN KEY (processed_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_academic_change_requests_canceller FOREIGN KEY (cancelled_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_academic_change_requests_type CHECK (
        request_type IN ('TRANSFER_DEPARTMENT', 'DOUBLE_MAJOR')
    ),
    CONSTRAINT ck_academic_change_requests_status CHECK (
        status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')
    ),
    CONSTRAINT ck_academic_change_requests_reason CHECK (CHAR_LENGTH(TRIM(reason)) > 0),
    CONSTRAINT ck_academic_change_requests_processing CHECK (
        (status = 'PENDING' AND processed_by IS NULL AND processed_at IS NULL
            AND reject_reason IS NULL AND cancelled_by IS NULL AND cancelled_at IS NULL AND cancel_reason IS NULL)
        OR (status = 'APPROVED' AND processed_by IS NOT NULL AND processed_at IS NOT NULL
            AND reject_reason IS NULL AND cancelled_by IS NULL AND cancelled_at IS NULL AND cancel_reason IS NULL)
        OR (status = 'REJECTED' AND processed_by IS NOT NULL AND processed_at IS NOT NULL
            AND reject_reason IS NOT NULL AND CHAR_LENGTH(TRIM(reject_reason)) > 0
            AND cancelled_by IS NULL AND cancelled_at IS NULL AND cancel_reason IS NULL)
        OR (status = 'CANCELLED' AND processed_by IS NULL AND processed_at IS NULL AND reject_reason IS NULL
            AND cancelled_by IS NOT NULL AND cancelled_at IS NOT NULL
            AND cancel_reason IS NOT NULL AND CHAR_LENGTH(TRIM(cancel_reason)) > 0)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 전과 신청의 필수 PDF 3종 메타데이터. 실제 파일은 비공개 MinIO에 저장합니다.
CREATE TABLE IF NOT EXISTS academic_change_request_files (
    id BIGINT NOT NULL AUTO_INCREMENT,
    request_id BIGINT NOT NULL,
    document_type VARCHAR(30) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_academic_change_request_files_type UNIQUE (request_id, document_type),
    INDEX idx_academic_change_request_files_request (request_id),
    CONSTRAINT fk_academic_change_request_files_request FOREIGN KEY (request_id)
        REFERENCES academic_change_requests (id) ON DELETE RESTRICT,
    CONSTRAINT ck_academic_change_request_files_type CHECK (
        document_type IN ('SELF_INTRODUCTION', 'STUDY_PLAN', 'TRANSCRIPT')
    ),
    CONSTRAINT ck_academic_change_request_files_size CHECK (size > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
