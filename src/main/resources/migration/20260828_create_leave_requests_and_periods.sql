-- 수동 적용용. 사용자 DB에는 자동 실행하지 않습니다.
-- 실행 전 SELECT DATABASE(), SHOW CREATE TABLE academic_requests 를 확인하고 백업하십시오.
-- 기존 academic_requests에 cancel_reason VARCHAR(500), active_student_id 생성 컬럼/UNIQUE,
-- 첨부 메타데이터/학생 FK가 있는지 schema.sql과 비교하십시오.
-- CREATE IF NOT EXISTS는 기존 구조를 보정하지 않습니다. 다르면 중단하고 별도 이관을 검토하십시오.
-- 기존 requester_user_id 기반 idempotency_keys, audit_logs, academic_status_histories가 필요합니다.
-- 이미 ON_LEAVE이나 원본 승인 휴학·실제 학적 이력이 없으면 확인된 자료로 별도 보정해야 합니다.
-- DDL은 MySQL에서 암묵적으로 COMMIT됩니다. 기존 데이터를 삭제하거나 임의 이력을 생성하지 않습니다.

-- 기존 사용자 academic_requests 테이블과 동일한 구조를 재사용합니다. 기존 테이블을 ALTER/DROP하지 않습니다.
CREATE TABLE IF NOT EXISTS academic_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    request_type VARCHAR(20) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    target_year SMALLINT NOT NULL,
    target_semester TINYINT NOT NULL,
    return_year SMALLINT NULL,
    return_semester TINYINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reject_reason VARCHAR(500) NULL,
    cancel_reason VARCHAR(500) NULL,
    attachment_original_name VARCHAR(255) NULL,
    attachment_stored_name VARCHAR(255) NULL,
    attachment_content_type VARCHAR(100) NULL,
    attachment_size BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active_student_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN status = 'PENDING' THEN student_id ELSE NULL END
    ) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_academic_requests_active_student UNIQUE (active_student_id),
    INDEX idx_academic_requests_student_status (student_id, status),
    INDEX idx_academic_requests_status_created (status, created_at),
    CONSTRAINT fk_academic_requests_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
    CONSTRAINT ck_academic_requests_target_semester CHECK (target_semester IN (1, 2)),
    CONSTRAINT ck_academic_requests_return_semester CHECK (return_semester IS NULL OR return_semester IN (1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 학기별 수강신청 기간과 분리한 휴·복학 접수/승인 기간. semester_id는 적용 학기입니다.
CREATE TABLE IF NOT EXISTS leave_request_periods (
    id BIGINT NOT NULL AUTO_INCREMENT,
    semester_id BIGINT NOT NULL,
    request_type VARCHAR(20) NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    approval_start_at DATETIME NOT NULL,
    approval_end_at DATETIME NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_leave_request_periods_semester_type UNIQUE (semester_id, request_type),
    CONSTRAINT fk_leave_request_periods_semester FOREIGN KEY (semester_id) REFERENCES semesters (id) ON DELETE RESTRICT,
    CONSTRAINT ck_leave_request_periods_request_type CHECK (
        request_type IN ('GENERAL_LEAVE', 'GENERAL_RETURN', 'MILITARY_LEAVE', 'MILITARY_RETURN')
    ),
    CONSTRAINT ck_leave_request_periods_receipt CHECK (start_at < end_at),
    CONSTRAINT ck_leave_request_periods_approval CHECK (approval_start_at < approval_end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
