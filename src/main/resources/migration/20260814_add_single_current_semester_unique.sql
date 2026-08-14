ALTER TABLE semesters
    ADD COLUMN current_semester_guard TINYINT
        GENERATED ALWAYS AS (CASE WHEN is_current = 1 THEN 1 ELSE NULL END) STORED,
    ADD CONSTRAINT uk_semesters_single_current UNIQUE (current_semester_guard);
