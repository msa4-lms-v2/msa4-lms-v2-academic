-- 2026-08-17: 학생 본인 학적 정보(연락처/주소/사진) 변경 신청 + 관리자 승인 워크플로우
-- Figma "03·학적 정보 변경 신청" 화면 대응. 증빙파일과 프로필 사진은 MinIO에 저장하고 objectKey만 DB에 남긴다.

ALTER TABLE users ADD COLUMN profile_image_key VARCHAR(500) NULL AFTER address;

CREATE TABLE IF NOT EXISTS student_info_change_requests (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    student_id          BIGINT NOT NULL,
    new_name            VARCHAR(50) NULL,
    new_phone_number    VARCHAR(20) NULL,
    new_email           VARCHAR(100) NULL,
    new_address         VARCHAR(255) NULL,
    new_profile_image_key VARCHAR(500) NULL,
    reason              VARCHAR(500) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    reviewed_by         BIGINT NULL,
    reviewed_at         DATETIME NULL,
    reject_reason       VARCHAR(500) NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_student_info_change_requests_student_status (student_id, status),
    CONSTRAINT fk_student_info_change_requests_student
        FOREIGN KEY (student_id) REFERENCES students (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_student_info_change_requests_reviewer
        FOREIGN KEY (reviewed_by) REFERENCES users (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS student_info_change_request_files (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    request_id  BIGINT NOT NULL,
    file_name   VARCHAR(255) NOT NULL,
    object_key  VARCHAR(500) NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_student_info_change_request_files_request_id (request_id),
    CONSTRAINT fk_student_info_change_request_files_request
        FOREIGN KEY (request_id) REFERENCES student_info_change_requests (id)
        ON DELETE CASCADE
) ENGINE=InnoDB;
