-- 확정 ERD에 따라 학과와 전공을 departments 하나로 통합합니다.
-- 실행 전 SELECT DATABASE(), SHOW CREATE TABLE students,
-- SHOW CREATE TABLE academic_change_requests를 확인하고 백업하십시오.
ALTER TABLE students
    DROP FOREIGN KEY fk_students_double_major,
    DROP CHECK ck_students_distinct_majors;

UPDATE students s
JOIN majors m ON m.id = s.double_major_id
SET s.double_major_id = CASE
    WHEN m.department_id = s.department_id THEN NULL
    ELSE m.department_id
END
WHERE s.double_major_id IS NOT NULL;

ALTER TABLE students
    ADD CONSTRAINT fk_students_double_major
        FOREIGN KEY (double_major_id) REFERENCES departments (id) ON DELETE RESTRICT,
    ADD CONSTRAINT ck_students_distinct_departments
        CHECK (double_major_id IS NULL OR department_id <> double_major_id);

ALTER TABLE academic_change_requests
    DROP FOREIGN KEY fk_academic_change_requests_source_major,
    DROP FOREIGN KEY fk_academic_change_requests_target_major,
    DROP CHECK ck_academic_change_requests_target_scope;

ALTER TABLE academic_change_requests
    DROP COLUMN source_major_id,
    DROP COLUMN target_major_id,
    ADD CONSTRAINT ck_academic_change_requests_target_scope CHECK (
        (request_type = 'TRANSFER_DEPARTMENT'
            AND target_semester_id IS NOT NULL)
        OR (request_type = 'DOUBLE_MAJOR'
            AND target_semester_id IS NULL
            AND request_period_id IS NOT NULL)
    );

ALTER TABLE students
    DROP FOREIGN KEY fk_students_major,
    DROP INDEX idx_students_major_id,
    DROP COLUMN major_id;

DROP TABLE majors;
