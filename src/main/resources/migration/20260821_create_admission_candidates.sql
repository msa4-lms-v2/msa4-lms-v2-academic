-- 입학 예정자는 Auth 계정·Academic 학생 생성 전까지 별도 원본으로 관리한다.
CREATE TABLE IF NOT EXISTS admission_candidates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    application_number VARCHAR(50) NOT NULL,
    name VARCHAR(50) NOT NULL,
    birth_date DATE NOT NULL,
    email VARCHAR(100) NULL,
    phone_number VARCHAR(20) NULL,
    address VARCHAR(255) NULL,
    department_id BIGINT NOT NULL,
    admission_year SMALLINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'REGISTERED',
    student_id BIGINT NULL,
    created_by BIGINT NOT NULL,
    status_changed_by BIGINT NULL,
    status_changed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_admission_candidates_application_number UNIQUE (application_number),
    CONSTRAINT uk_admission_candidates_student_id UNIQUE (student_id),
    CONSTRAINT fk_admission_candidates_department
        FOREIGN KEY (department_id) REFERENCES departments (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_admission_candidates_student
        FOREIGN KEY (student_id) REFERENCES students (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_admission_candidates_created_by
        FOREIGN KEY (created_by) REFERENCES users (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_admission_candidates_status_changed_by
        FOREIGN KEY (status_changed_by) REFERENCES users (id)
        ON DELETE RESTRICT,
    INDEX idx_admission_candidates_department_year_status (department_id, admission_year, status),
    INDEX idx_admission_candidates_name (name),
    INDEX idx_admission_candidates_created_at (created_at)
) ENGINE=InnoDB;
