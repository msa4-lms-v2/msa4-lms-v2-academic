CREATE TABLE IF NOT EXISTS syllabus_files (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lecture_id BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    uploaded_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_syllabus_files_lecture
        FOREIGN KEY (lecture_id) REFERENCES lectures (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_syllabus_files_uploader
        FOREIGN KEY (uploaded_by) REFERENCES users (id)
        ON DELETE RESTRICT,
    INDEX idx_syllabus_files_lecture_id (lecture_id),
    INDEX idx_syllabus_files_lecture_duplicate (lecture_id, original_name, size)
) ENGINE=InnoDB;
