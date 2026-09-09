-- 전과·복수전공에서 관리자는 최종 승인자가 아니라 학장 날인 결과를 학적에 반영하는 집행자입니다.
-- 기존 APPROVED 행은 과거 처리 결과로 유지하고, 신규 지도교수 승인 건은 APPLIED로 종결합니다.
ALTER TABLE academic_change_requests
    DROP CHECK ck_academic_change_requests_processing,
    DROP CHECK ck_academic_change_requests_status,
    ADD CONSTRAINT ck_academic_change_requests_status CHECK (
        status IN ('PENDING', 'ADVISOR_APPROVED', 'ADVISOR_REJECTED', 'APPROVED', 'APPLIED', 'REJECTED', 'CANCELLED')
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
        OR (status = 'APPLIED' AND advisor_reviewed_by IS NOT NULL AND advisor_reviewed_at IS NOT NULL
            AND advisor_reject_reason IS NULL AND processed_by IS NOT NULL AND processed_at IS NOT NULL
            AND reject_reason IS NULL AND cancelled_by IS NULL AND cancelled_at IS NULL AND cancel_reason IS NULL)
        OR (status = 'REJECTED' AND processed_by IS NOT NULL AND processed_at IS NOT NULL
            AND reject_reason IS NOT NULL AND CHAR_LENGTH(TRIM(reject_reason)) > 0
            AND cancelled_by IS NULL AND cancelled_at IS NULL AND cancel_reason IS NULL)
        OR (status = 'CANCELLED' AND processed_by IS NULL AND processed_at IS NULL AND reject_reason IS NULL
            AND cancelled_by IS NOT NULL AND cancelled_at IS NOT NULL
            AND cancel_reason IS NOT NULL AND CHAR_LENGTH(TRIM(cancel_reason)) > 0)
    );
