CREATE TABLE IF NOT EXISTS student_grade_summaries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL,
    total_credits TINYINT UNSIGNED NOT NULL DEFAULT 0,
    gpa DECIMAL(4,2) NOT NULL DEFAULT 0.00,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_student_grade_summaries_student_semester UNIQUE (student_id, semester_id),
    CONSTRAINT fk_student_grade_summaries_student
        FOREIGN KEY (student_id) REFERENCES students (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_student_grade_summaries_semester
        FOREIGN KEY (semester_id) REFERENCES semesters (id)
        ON DELETE RESTRICT,
    INDEX idx_student_grade_summaries_student_id (student_id),
    INDEX idx_student_grade_summaries_semester_id (semester_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS grade_correction_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    enrollment_id BIGINT NOT NULL,
    field_changed VARCHAR(30) NOT NULL,
    previous_value VARCHAR(50) NULL,
    new_value VARCHAR(50) NULL,
    changed_by BIGINT NOT NULL,
    reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_grade_correction_histories_enrollment
        FOREIGN KEY (enrollment_id) REFERENCES enrollments (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_grade_correction_histories_changed_by
        FOREIGN KEY (changed_by) REFERENCES users (id)
        ON DELETE RESTRICT,
    INDEX idx_grade_correction_histories_enrollment_id (enrollment_id),
    INDEX idx_grade_correction_histories_changed_by (changed_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
