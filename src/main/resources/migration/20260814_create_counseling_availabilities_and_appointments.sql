CREATE TABLE counselor_availabilities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    professor_id BIGINT NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    start_time VARCHAR(5) NOT NULL,
    end_time VARCHAR(5) NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_counselor_availabilities_professor
        FOREIGN KEY (professor_id) REFERENCES professors (id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_counselor_availabilities_time
        CHECK (start_time < end_time),
    CONSTRAINT ck_counselor_availabilities_validity
        CHECK (valid_to IS NULL OR valid_to >= valid_from),
    INDEX idx_counselor_availabilities_professor_day (professor_id, day_of_week),
    INDEX idx_counselor_availabilities_validity (valid_from, valid_to)
) ENGINE=InnoDB;

CREATE TABLE counseling_appointments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    professor_id BIGINT NOT NULL,
    appointment_at DATETIME NOT NULL,
    topic VARCHAR(255) NULL,
    professor_note TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_counseling_appointments_professor_time
        UNIQUE (professor_id, appointment_at),
    CONSTRAINT uk_counseling_appointments_student_time
        UNIQUE (student_id, appointment_at),
    CONSTRAINT fk_counseling_appointments_student
        FOREIGN KEY (student_id) REFERENCES students (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_counseling_appointments_professor
        FOREIGN KEY (professor_id) REFERENCES professors (id)
        ON DELETE RESTRICT,
    INDEX idx_counseling_appointments_student_status (student_id, status),
    INDEX idx_counseling_appointments_professor_status (professor_id, status),
    INDEX idx_counseling_appointments_at (appointment_at)
) ENGINE=InnoDB;
