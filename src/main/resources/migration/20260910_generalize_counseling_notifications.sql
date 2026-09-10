-- 20260908_replace_appointment_counseling_with_online_counseling.sql 적용 후 한 번만 실행합니다.
-- 기존 상담 알림의 제목·내용·읽음 상태·중복 키와 상태 전이 이력을 보존하면서 공통 알림으로 전환합니다.

RENAME TABLE counseling_notifications TO notifications;

ALTER TABLE notifications
    ADD COLUMN category VARCHAR(30) NOT NULL DEFAULT 'COUNSELING' AFTER recipient_user_id,
    ADD COLUMN resource_type VARCHAR(40) NULL AFTER notification_type,
    ADD COLUMN resource_id BIGINT NULL AFTER resource_type,
    ADD COLUMN title VARCHAR(200) NULL AFTER resource_id,
    ADD COLUMN context JSON NULL AFTER title;

UPDATE notifications n
JOIN counselings c ON c.id = n.counseling_id
SET n.resource_type = 'COUNSELING',
    n.resource_id = n.counseling_id,
    n.title = c.title,
    n.context = JSON_OBJECT('previousStatus', n.previous_status, 'newStatus', n.new_status);

ALTER TABLE notifications
    DROP FOREIGN KEY fk_counseling_notifications_counseling,
    DROP INDEX idx_counseling_notifications_counseling,
    DROP COLUMN counseling_id,
    DROP COLUMN previous_status,
    DROP COLUMN new_status,
    MODIFY COLUMN resource_type VARCHAR(40) NOT NULL,
    MODIFY COLUMN resource_id BIGINT NOT NULL,
    MODIFY COLUMN title VARCHAR(200) NOT NULL,
    RENAME INDEX uk_counseling_notifications_deduplication_key TO uk_notifications_deduplication_key,
    RENAME INDEX idx_counseling_notifications_recipient_read_created TO idx_notifications_recipient_read_created;
