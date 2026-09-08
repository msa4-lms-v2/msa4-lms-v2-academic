ALTER TABLE semesters
    ADD COLUMN evaluation_start_at DATETIME NULL AFTER enrollment_end_at,
    ADD COLUMN evaluation_end_at DATETIME NULL AFTER evaluation_start_at,
    ADD CONSTRAINT ck_semesters_evaluation_period CHECK (
        (evaluation_start_at IS NULL AND evaluation_end_at IS NULL)
        OR (evaluation_start_at IS NOT NULL AND evaluation_end_at IS NOT NULL
            AND evaluation_start_at < evaluation_end_at)
    );

CREATE TABLE lecture_evaluations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    enrollment_id BIGINT NOT NULL,
    ratings JSON NOT NULL,
    comment TEXT NULL,
    submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_lecture_evaluations_enrollment UNIQUE (enrollment_id),
    CONSTRAINT fk_lecture_evaluations_enrollment
        FOREIGN KEY (enrollment_id) REFERENCES enrollments (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
