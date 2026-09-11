-- 휴·복학 신청은 지도교수 승인 후 관리자 최종 승인으로 처리한다.
-- 기존 PENDING·종결 이력은 그대로 보존하며, 새 상태 ADVISOR_APPROVED도 학생당 진행 중 신청 1건 제약에 포함한다.
ALTER TABLE academic_requests
    ADD COLUMN advisor_reviewed_by BIGINT NULL AFTER status,
    ADD COLUMN advisor_reviewed_at DATETIME NULL AFTER advisor_reviewed_by,
    ADD COLUMN advisor_reject_reason VARCHAR(500) NULL AFTER advisor_reviewed_at,
    MODIFY COLUMN active_student_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN status IN ('PENDING', 'ADVISOR_APPROVED') THEN student_id ELSE NULL END
    ) STORED,
    ADD INDEX idx_academic_requests_advisor_status (advisor_reviewed_by, status),
    ADD CONSTRAINT fk_academic_requests_advisor_reviewer
        FOREIGN KEY (advisor_reviewed_by) REFERENCES users (id) ON DELETE RESTRICT;
