-- 휴·복학 신청의 단일 증빙 메타데이터를 신청별 최대 5개 증빙 구조로 확장합니다.
-- 실제 파일은 기존 비공개 MinIO object를 그대로 사용하며 DB에는 메타데이터만 저장합니다.
-- 실행 전 SELECT DATABASE(), SHOW CREATE TABLE academic_requests를 확인하고 백업하십시오.

CREATE TABLE IF NOT EXISTS leave_request_files (
    id BIGINT NOT NULL AUTO_INCREMENT,
    request_id BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_leave_request_files_request (request_id),
    CONSTRAINT fk_leave_request_files_request FOREIGN KEY (request_id)
        REFERENCES academic_requests (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO leave_request_files (
    request_id,
    original_name,
    stored_name,
    content_type,
    size,
    created_at
)
SELECT
    ar.id,
    ar.attachment_original_name,
    ar.attachment_stored_name,
    ar.attachment_content_type,
    ar.attachment_size,
    ar.created_at
FROM academic_requests ar
WHERE ar.attachment_stored_name IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM leave_request_files file
      WHERE file.request_id = ar.id
        AND file.stored_name = ar.attachment_stored_name
  );

-- 기존 attachment_* 컬럼은 구버전 조회·다운로드 호환을 위해 유지하며 신규 신청에는 쓰지 않습니다.
