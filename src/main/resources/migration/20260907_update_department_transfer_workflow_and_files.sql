-- 전과를 학생 신청 -> 현재 담당 지도교수 검토 -> 관리자 최종 처리 흐름으로 확장합니다.
-- 기존 PENDING 전과 신청은 그대로 유지되어 현재 담당 지도교수 검토 목록에 포함됩니다.
ALTER TABLE academic_change_requests
    DROP CHECK ck_academic_change_requests_processing,
    DROP CHECK ck_academic_change_requests_status,
    DROP INDEX uk_academic_change_requests_active_type,
    ADD COLUMN advisor_reviewed_by BIGINT NULL AFTER status,
    ADD COLUMN advisor_reviewed_at DATETIME NULL AFTER advisor_reviewed_by,
    ADD COLUMN advisor_reject_reason VARCHAR(500) NULL AFTER advisor_reviewed_at,
    MODIFY COLUMN active_student_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN status IN ('PENDING', 'ADVISOR_APPROVED') THEN student_id ELSE NULL END
    ) STORED,
    ADD CONSTRAINT uk_academic_change_requests_active_type UNIQUE (request_type, active_student_id),
    ADD INDEX idx_academic_change_requests_advisor_status (advisor_reviewed_by, status),
    ADD CONSTRAINT fk_academic_change_requests_advisor_reviewer
        FOREIGN KEY (advisor_reviewed_by) REFERENCES users (id) ON DELETE RESTRICT,
    ADD CONSTRAINT ck_academic_change_requests_status CHECK (
        status IN ('PENDING', 'ADVISOR_APPROVED', 'ADVISOR_REJECTED', 'APPROVED', 'REJECTED', 'CANCELLED')
    ),
    ADD CONSTRAINT ck_academic_change_requests_processing CHECK (
        (status = 'PENDING' AND advisor_reviewed_by IS NULL AND advisor_reviewed_at IS NULL
            AND advisor_reject_reason IS NULL AND processed_by IS NULL AND processed_at IS NULL
            AND reject_reason IS NULL AND cancelled_by IS NULL AND cancelled_at IS NULL AND cancel_reason IS NULL)
        OR (status = 'ADVISOR_APPROVED' AND advisor_reviewed_by IS NOT NULL AND advisor_reviewed_at IS NOT NULL
            AND advisor_reject_reason IS NULL AND processed_by IS NULL AND processed_at IS NULL
            AND reject_reason IS NULL AND cancelled_by IS NULL AND cancelled_at IS NULL AND cancel_reason IS NULL)
        OR (status = 'ADVISOR_REJECTED' AND advisor_reviewed_by IS NOT NULL AND advisor_reviewed_at IS NOT NULL
            AND advisor_reject_reason IS NOT NULL AND CHAR_LENGTH(TRIM(advisor_reject_reason)) > 0
            AND processed_by IS NULL AND processed_at IS NULL AND reject_reason IS NULL
            AND cancelled_by IS NULL AND cancelled_at IS NULL AND cancel_reason IS NULL)
        OR (status = 'APPROVED' AND processed_by IS NOT NULL AND processed_at IS NOT NULL
            AND reject_reason IS NULL AND cancelled_by IS NULL AND cancelled_at IS NULL AND cancel_reason IS NULL)
        OR (status = 'REJECTED' AND processed_by IS NOT NULL AND processed_at IS NOT NULL
            AND reject_reason IS NOT NULL AND CHAR_LENGTH(TRIM(reject_reason)) > 0
            AND cancelled_by IS NULL AND cancelled_at IS NULL AND cancel_reason IS NULL)
        OR (status = 'CANCELLED' AND processed_by IS NULL AND processed_at IS NULL AND reject_reason IS NULL
            AND cancelled_by IS NOT NULL AND cancelled_at IS NOT NULL
            AND cancel_reason IS NOT NULL AND CHAR_LENGTH(TRIM(cancel_reason)) > 0)
    );

-- 첨부파일의 문서 종류 분류와 종류별 유일성 제약을 제거하고 file id로 식별합니다.
ALTER TABLE academic_change_request_files
    DROP CHECK ck_academic_change_request_files_type,
    DROP INDEX uk_academic_change_request_files_type,
    DROP COLUMN document_type;
