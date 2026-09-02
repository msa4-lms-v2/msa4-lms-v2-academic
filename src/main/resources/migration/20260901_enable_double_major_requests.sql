-- 기존 전과 공용 테이블에 복수전공 신청의 모집 회차 연결과 희망 학기 미사용 구조를 추가합니다.
ALTER TABLE academic_change_requests
    MODIFY COLUMN target_semester_id BIGINT NULL,
    ADD COLUMN request_period_id BIGINT NULL AFTER target_semester_id,
    ADD INDEX idx_academic_change_requests_period (request_period_id),
    ADD CONSTRAINT fk_academic_change_requests_period
        FOREIGN KEY (request_period_id) REFERENCES academic_change_request_periods (id) ON DELETE RESTRICT,
    ADD CONSTRAINT ck_academic_change_requests_target_scope CHECK (
        (request_type = 'TRANSFER_DEPARTMENT' AND target_semester_id IS NOT NULL)
        OR (request_type = 'DOUBLE_MAJOR' AND target_semester_id IS NULL AND request_period_id IS NOT NULL)
    );

-- 기존 전과 행은 request_period_id가 NULL이어도 유지됩니다. 신규 전과와 모든 복수전공 신청은 코드에서
-- 실제 접수에 사용한 academic_change_request_periods.id를 저장합니다.
