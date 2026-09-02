CREATE TABLE IF NOT EXISTS academic_schedules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    content TEXT NULL,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    target_role VARCHAR(20) NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    author_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_academic_schedules_author
        FOREIGN KEY (author_id) REFERENCES users (id)
        ON DELETE RESTRICT,
    INDEX idx_academic_schedules_target_active_start (target_role, is_active, start_date),
    INDEX idx_academic_schedules_end_date (end_date)
) ENGINE=InnoDB;
