-- 수동 적용용: 사용자 DB에 자동 실행하지 않습니다.
-- 먼저 SELECT DATABASE(), SHOW TABLES LIKE 'dismissal_candidates'로 대상과 기존 테이블 존재 여부를 확인하십시오.
-- 기존 테이블이 있다면 SHOW CREATE TABLE 결과를 아래 구조와 대조하고 백업하십시오.
-- CREATE IF NOT EXISTS는 기존 구조를 변경하지 않습니다. 기존 ERD 7컬럼만 있다면 실행을 중단하십시오.
-- 기존 데이터의 reason_type, 확정/취소 처리자·시각·취소 사유를 사실 확인 없이 기본값으로 채우지 마십시오.
-- 그 경우 실제 DDL/행을 확인한 별도 ALTER·보정 절차가 필요합니다.
-- 기존 동일 학생 PENDING 중복도 먼저 사실에 따라 정리해야 UNIQUE를 적용할 수 있습니다.
-- prerequisite: users, students, audit_logs, academic_status_histories, academic_requests,
-- withdrawal_requests(취소 컬럼 포함), requester_user_id 기반 idempotency_keys.
-- 이 SQL은 기존 테이블/데이터를 DROP, DELETE, ALTER하지 않습니다. MySQL DDL은 암묵적으로 COMMIT됩니다.

-- 관리자 제적. 학생 자퇴와 분리하며 확정 시 학적 이력/감사/진행 중 신청 취소를 함께 저장합니다.
CREATE TABLE IF NOT EXISTS dismissal_candidates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    student_id BIGINT NOT NULL,
    reason_type VARCHAR(30) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    registered_by BIGINT NOT NULL,
    processed_by BIGINT NULL DEFAULT NULL,
    processed_at DATETIME NULL DEFAULT NULL,
    cancel_reason VARCHAR(500) NULL DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active_student_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN status = 'PENDING' THEN student_id ELSE NULL END
    ) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_dismissal_candidates_active_student UNIQUE (active_student_id),
    INDEX idx_dismissal_candidates_student_status (student_id, status),
    INDEX idx_dismissal_candidates_status_created (status, created_at),
    CONSTRAINT fk_dismissal_candidates_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
    CONSTRAINT fk_dismissal_candidates_registrant FOREIGN KEY (registered_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_dismissal_candidates_processor FOREIGN KEY (processed_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_dismissal_candidates_version CHECK (version >= 0),
    CONSTRAINT ck_dismissal_candidates_reason_type CHECK (
        reason_type IN ('LEAVE_EXPIRED', 'NON_REGISTRATION', 'DISCIPLINARY', 'ACADEMIC_WARNING', 'ACADEMIC_WARNING_REPEAT')
    ),
    CONSTRAINT ck_dismissal_candidates_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED')),
    CONSTRAINT ck_dismissal_candidates_reason CHECK (CHAR_LENGTH(TRIM(reason)) > 0),
    CONSTRAINT ck_dismissal_candidates_processing CHECK (
        (status = 'PENDING' AND processed_by IS NULL AND processed_at IS NULL AND cancel_reason IS NULL)
        OR (status = 'CONFIRMED' AND processed_by IS NOT NULL AND processed_at IS NOT NULL AND cancel_reason IS NULL)
        OR (status = 'CANCELLED' AND processed_by IS NOT NULL AND processed_at IS NOT NULL
            AND cancel_reason IS NOT NULL AND CHAR_LENGTH(TRIM(cancel_reason)) > 0)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
