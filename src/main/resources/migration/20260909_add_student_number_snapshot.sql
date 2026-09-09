-- 학번 생성은 기존처럼 Academic 프로비저닝이 담당하고 Auth accounts.login_id에 저장합니다.
-- 이 컬럼은 교수·관리자 학사 화면 조회를 위한 동일 값의 불변 스냅샷입니다.
-- IDENTITY student.id가 INSERT 후 확정되므로 신규 프로비저닝은 같은 transaction에서
-- 학생 INSERT 후 학번을 계산해 UPDATE합니다. 따라서 DB 컬럼은 NULL을 허용하되 서비스는
-- 프로비저닝 transaction 완료 전에 반드시 값을 저장합니다.
ALTER TABLE students
    ADD COLUMN student_number VARCHAR(150) NULL AFTER user_id,
    ADD CONSTRAINT uk_students_student_number UNIQUE (student_number);

-- 기존 데이터는 현재 학과로 재계산하지 않습니다. 전과 학생의 실제 Auth login_id와 달라질 수 있습니다.
-- Auth accounts에서 role=STUDENT인 (id, login_id)를 안전하게 export하고,
-- Academic users.id = Auth accounts.id 계약에 따라 아래 검증을 거쳐 별도 보정합니다.
-- Auth DB export query:
-- SELECT id AS user_id, login_id AS student_number
-- FROM accounts
-- WHERE role = 'STUDENT' AND login_id IS NOT NULL AND TRIM(login_id) <> '';
--
-- 보정 예시(운영 export 값을 검토한 뒤 명시적인 VALUES로 실행):
-- CREATE TEMPORARY TABLE student_number_backfill (
--     user_id BIGINT NOT NULL PRIMARY KEY,
--     student_number VARCHAR(150) NOT NULL UNIQUE
-- );
-- INSERT INTO student_number_backfill (user_id, student_number) VALUES
--     (1001, '26001001');
-- UPDATE students s
-- JOIN student_number_backfill b ON b.user_id = s.user_id
-- SET s.student_number = b.student_number
-- WHERE s.student_number IS NULL;
--
-- 누락 확인: 결과가 0이어야 이관 완료입니다.
-- SELECT COUNT(*) AS missing_student_numbers
-- FROM students
-- WHERE student_number IS NULL;
--
-- Auth와 Academic 값 불일치 확인: 결과가 0이어야 합니다.
-- SELECT s.user_id, s.student_number, b.student_number AS auth_login_id
-- FROM students s
-- JOIN student_number_backfill b ON b.user_id = s.user_id
-- WHERE s.student_number <> b.student_number;
