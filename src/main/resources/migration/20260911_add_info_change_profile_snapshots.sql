-- 정보 변경 심사·이력 조회에서 신청 당시 프로필을 정확히 비교할 수 있도록 원본 값을 보존한다.
-- 기존 신청 이력은 당시 원본 값을 복원할 수 없으므로 NULL을 유지한다.
ALTER TABLE student_info_change_requests
    ADD COLUMN previous_name VARCHAR(50) NULL AFTER student_id,
    ADD COLUMN previous_phone_number VARCHAR(20) NULL AFTER previous_name,
    ADD COLUMN previous_email VARCHAR(100) NULL AFTER previous_phone_number,
    ADD COLUMN previous_address VARCHAR(255) NULL AFTER previous_email,
    ADD COLUMN previous_profile_image_key VARCHAR(500) NULL AFTER previous_address;

ALTER TABLE professor_info_change_requests
    ADD COLUMN previous_name VARCHAR(50) NULL AFTER professor_id,
    ADD COLUMN previous_phone_number VARCHAR(20) NULL AFTER previous_name,
    ADD COLUMN previous_email VARCHAR(100) NULL AFTER previous_phone_number,
    ADD COLUMN previous_address VARCHAR(255) NULL AFTER previous_email,
    ADD COLUMN previous_profile_image_key VARCHAR(500) NULL AFTER previous_address;
