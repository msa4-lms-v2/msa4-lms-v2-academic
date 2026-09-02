-- 기존 JSON 자퇴 신청 계약은 유지하고 선택 PDF 증빙 메타데이터만 추가합니다.
-- 실제 파일은 비공개 MinIO에 저장하며 attachment_stored_name에는 object key만 보관합니다.
ALTER TABLE withdrawal_requests
    ADD COLUMN attachment_original_name VARCHAR(255) NULL AFTER cancelled_at,
    ADD COLUMN attachment_stored_name VARCHAR(255) NULL AFTER attachment_original_name,
    ADD COLUMN attachment_content_type VARCHAR(100) NULL AFTER attachment_stored_name,
    ADD COLUMN attachment_size BIGINT NULL AFTER attachment_content_type;
