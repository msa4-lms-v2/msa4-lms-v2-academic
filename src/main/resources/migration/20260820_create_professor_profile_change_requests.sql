-- 기존 학생 프로필 변경 신청을 취소·검색·파일 메타데이터 정책에 맞게 보완한다.
ALTER TABLE student_info_change_requests
    ADD COLUMN cancelled_at DATETIME NULL AFTER reject_reason,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP AFTER created_at,
    ADD INDEX idx_student_info_change_requests_status_created (status, created_at);

-- 기존 행은 MIME 타입과 크기를 알 수 없으므로 중립값으로 보정한 뒤 NOT NULL로 강화한다.
ALTER TABLE student_info_change_request_files
    ADD COLUMN content_type VARCHAR(100) NULL AFTER object_key,
    ADD COLUMN file_size BIGINT NULL AFTER content_type;

UPDATE student_info_change_request_files
SET content_type = 'application/octet-stream', file_size = 0
WHERE content_type IS NULL OR file_size IS NULL;

ALTER TABLE student_info_change_request_files
    MODIFY COLUMN content_type VARCHAR(100) NOT NULL,
    MODIFY COLUMN file_size BIGINT NOT NULL;

CREATE TABLE professor_info_change_requests (
    id                    BIGINT NOT NULL AUTO_INCREMENT,
    professor_id          BIGINT NOT NULL,
    new_name              VARCHAR(50) NULL,
    new_phone_number      VARCHAR(20) NULL,
    new_email             VARCHAR(100) NULL,
    new_address           VARCHAR(255) NULL,
    new_profile_image_key VARCHAR(500) NULL,
    reason                VARCHAR(500) NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    reviewed_by           BIGINT NULL,
    reviewed_at           DATETIME NULL,
    reject_reason         VARCHAR(500) NULL,
    cancelled_at          DATETIME NULL,
    created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_professor_info_change_requests_professor_status (professor_id, status),
    INDEX idx_professor_info_change_requests_status_created (status, created_at),
    CONSTRAINT fk_professor_info_change_requests_professor
        FOREIGN KEY (professor_id) REFERENCES professors (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_professor_info_change_requests_reviewer
        FOREIGN KEY (reviewed_by) REFERENCES users (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE professor_info_change_request_files (
    id           BIGINT NOT NULL AUTO_INCREMENT,
    request_id   BIGINT NOT NULL,
    file_name    VARCHAR(255) NOT NULL,
    object_key   VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size    BIGINT NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_professor_info_change_request_files_request_id (request_id),
    CONSTRAINT fk_professor_info_change_request_files_request
        FOREIGN KEY (request_id) REFERENCES professor_info_change_requests (id)
        ON DELETE CASCADE
) ENGINE=InnoDB;
