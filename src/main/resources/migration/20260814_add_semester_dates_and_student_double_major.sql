ALTER TABLE semesters
    ADD COLUMN start_date DATE NULL AFTER term,
    ADD COLUMN end_date DATE NULL AFTER start_date;

UPDATE semesters
SET start_date = DATE(enrollment_start_at),
    end_date = DATE(enrollment_end_at)
WHERE start_date IS NULL
   OR end_date IS NULL;

ALTER TABLE semesters
    MODIFY COLUMN start_date DATE NOT NULL,
    MODIFY COLUMN end_date DATE NOT NULL;

ALTER TABLE students
    ADD COLUMN double_major_id BIGINT NULL AFTER major_id,
    ADD CONSTRAINT fk_students_double_major
        FOREIGN KEY (double_major_id) REFERENCES majors (id)
        ON DELETE RESTRICT,
    ADD CONSTRAINT ck_students_distinct_majors
        CHECK (major_id IS NULL OR double_major_id IS NULL OR major_id <> double_major_id),
    ADD INDEX idx_students_double_major_id (double_major_id);
