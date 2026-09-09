-- 기존 지원번호 값은 더 이상 사용하지 않으며 컬럼을 제거한다.
ALTER TABLE admission_candidates DROP INDEX uk_admission_candidates_application_number;
ALTER TABLE admission_candidates DROP COLUMN application_number;
CREATE UNIQUE INDEX uk_admission_candidates_email ON admission_candidates (email);
ALTER TABLE professors ADD COLUMN professor_number VARCHAR(20) NULL;
CREATE UNIQUE INDEX uk_professors_professor_number ON professors (professor_number);
-- 기존 로그인 ID/학번은 변경하지 않는다. 새 등록부터 8자리 발급 규칙을 적용한다.
