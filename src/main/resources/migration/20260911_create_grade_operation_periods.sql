CREATE TABLE IF NOT EXISTS grade_operation_periods (
    id BIGINT NOT NULL AUTO_INCREMENT,
    semester_id BIGINT NOT NULL,
    operation_type VARCHAR(30) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_grade_operation_periods_semester_type UNIQUE (semester_id, operation_type),
    CONSTRAINT fk_grade_operation_periods_semester
        FOREIGN KEY (semester_id) REFERENCES semesters (id) ON DELETE RESTRICT,
    CONSTRAINT ck_grade_operation_periods_type
        CHECK (operation_type IN ('GRADE_ENTRY', 'GRADE_CORRECTION')),
    CONSTRAINT ck_grade_operation_periods_range CHECK (start_date <= end_date),
    INDEX idx_grade_operation_periods_type_active (operation_type, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
