-- 관리자 공지사항 관리에 필요한 중요 공지 전환일·첨부파일 메타데이터를 추가한다.
ALTER TABLE notices
    ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT 'NORMAL' AFTER content,
    ADD COLUMN normal_transition_date DATE NULL AFTER category,
    ADD INDEX idx_notices_category_transition (category, normal_transition_date),
    ADD INDEX idx_notices_author_created (author_id, created_at);

CREATE TABLE notice_attachments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    notice_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    object_key VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_notice_attachments_notice_id (notice_id),
    CONSTRAINT fk_notice_attachments_notice
        FOREIGN KEY (notice_id) REFERENCES notices (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
