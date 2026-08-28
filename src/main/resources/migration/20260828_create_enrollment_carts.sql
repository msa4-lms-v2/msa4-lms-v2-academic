CREATE TABLE IF NOT EXISTS enrollment_carts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    lecture_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_enrollment_carts_student_lecture UNIQUE (student_id, lecture_id),
    CONSTRAINT fk_enrollment_carts_student
        FOREIGN KEY (student_id) REFERENCES students (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_enrollment_carts_lecture
        FOREIGN KEY (lecture_id) REFERENCES lectures (id)
        ON DELETE RESTRICT,
    INDEX idx_enrollment_carts_student_id (student_id),
    INDEX idx_enrollment_carts_lecture_id (lecture_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
