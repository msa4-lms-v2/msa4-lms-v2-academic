-- 전과·복수전공 신청 UI에는 별도 신청 사유가 없고 필수 자기소개서가 신청 동기를 대신합니다.
-- 20260831_create_department_transfer_requests.sql을 이미 적용한 Academic DB에서 한 번 실행합니다.
ALTER TABLE academic_change_requests
    DROP CHECK ck_academic_change_requests_reason,
    DROP COLUMN reason;
