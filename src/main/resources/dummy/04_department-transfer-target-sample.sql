-- 전과 신청 Postman 테스트용 희망 학과 데이터입니다.
-- 운영/공유 DB에 적용하지 않고 Academic 로컬 DB에서만 사용합니다.
-- 기존 학생 소속과 신청·접수 기간은 변경하지 않습니다.

START TRANSACTION;

INSERT INTO colleges (code, name, active)
VALUES ('DTR', '전과테스트경영대학', TRUE)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    active = VALUES(active);

SET @department_transfer_college_id = (
    SELECT id
    FROM colleges
    WHERE code = 'DTR'
    LIMIT 1
);

INSERT INTO departments (code, college_id, name, active)
VALUES ('DTR', @department_transfer_college_id, '전과테스트경영학과', TRUE)
ON DUPLICATE KEY UPDATE
    college_id = VALUES(college_id),
    name = VALUES(name),
    active = VALUES(active);

SET @department_transfer_department_id = (
    SELECT id
    FROM departments
    WHERE code = 'DTR'
    LIMIT 1
);

COMMIT;

SELECT
    d.id AS target_department_id,
    d.name AS target_department_name,
    d.active AS department_active
FROM departments d
WHERE d.code = 'DTR';
