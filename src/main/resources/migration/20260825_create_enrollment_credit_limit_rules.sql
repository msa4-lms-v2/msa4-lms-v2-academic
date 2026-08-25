-- 학기별 최대 신청학점 기준은 수강신청 시작 전에 확정하고 물리 삭제하지 않는다.
CREATE TABLE IF NOT EXISTS enrollment_credit_limit_rules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    semester_id BIGINT NOT NULL,
    max_credits TINYINT NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_enrollment_credit_limit_rules_semester UNIQUE (semester_id),
    CONSTRAINT ck_enrollment_credit_limit_rules_max_credits
        CHECK (max_credits BETWEEN 1 AND 30),
    CONSTRAINT fk_enrollment_credit_limit_rules_semester
        FOREIGN KEY (semester_id) REFERENCES semesters (id)
        ON DELETE RESTRICT,
    INDEX idx_enrollment_credit_limit_rules_active_semester (is_active, semester_id)
) ENGINE=InnoDB;
