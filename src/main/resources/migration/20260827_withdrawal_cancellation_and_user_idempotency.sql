-- Academic 전용. 모든 Academic 인스턴스/worker를 중지하고 DB를 백업한 뒤 실행하세요.
-- MySQL DDL은 implicit commit이므로 이 파일 전체가 하나의 rollback 가능한 transaction은 아닙니다.
-- 학생 ID를 단순 rename하지 않고 students.user_id로 변환합니다. 키/해시/응답/만료일/collation은 보존합니다.
-- 오류 시 원인을 확인하고 같은 파일을 재실행합니다. schema.sql 재실행으로 이관을 대체할 수 없습니다.
DELIMITER $$
DROP PROCEDURE IF EXISTS migrate_academic_withdrawal_20260827$$
CREATE PROCEDURE migrate_academic_withdrawal_20260827()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'idempotency_keys' AND COLUMN_NAME = 'requester_student_id') THEN
        IF EXISTS (SELECT 1 FROM idempotency_keys k
                   LEFT JOIN students s ON s.id = k.requester_student_id
                   LEFT JOIN users u ON u.id = s.user_id WHERE u.id IS NULL) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unmapped student requester: stop and repair before migration';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE()
                       AND TABLE_NAME = 'idempotency_keys' AND CONSTRAINT_NAME = 'fk_idempotency_keys_requester'
                       AND COLUMN_NAME = 'requester_student_id' AND REFERENCED_TABLE_NAME = 'students') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unexpected requester FK: inspect SHOW CREATE TABLE before migration';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                       AND TABLE_NAME = 'idempotency_keys' AND COLUMN_NAME = 'requester_user_id') THEN
            ALTER TABLE idempotency_keys ADD COLUMN requester_user_id BIGINT NULL AFTER requester_student_id;
        END IF;
        IF EXISTS (SELECT 1 FROM idempotency_keys k JOIN students s ON s.id = k.requester_student_id
                   WHERE k.requester_user_id IS NOT NULL AND k.requester_user_id <> s.user_id) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Requester mapping mismatch: existing user values were not overwritten';
        END IF;
        UPDATE idempotency_keys k JOIN students s ON s.id = k.requester_student_id
        SET k.requester_user_id = s.user_id WHERE k.requester_user_id IS NULL;
        IF EXISTS (SELECT 1 FROM idempotency_keys WHERE requester_user_id IS NULL) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Incomplete requester mapping: old column preserved';
        END IF;
        ALTER TABLE idempotency_keys
            DROP FOREIGN KEY fk_idempotency_keys_requester,
            DROP INDEX idx_idempotency_keys_requester,
            DROP COLUMN requester_student_id,
            MODIFY COLUMN requester_user_id BIGINT NOT NULL,
            ADD INDEX idx_idempotency_keys_requester (requester_user_id),
            ADD CONSTRAINT fk_idempotency_keys_user FOREIGN KEY (requester_user_id)
                REFERENCES users (id) ON DELETE RESTRICT;
    ELSE
        IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                       AND TABLE_NAME = 'idempotency_keys' AND COLUMN_NAME = 'requester_user_id'
                       AND IS_NULLABLE = 'NO' AND DATA_TYPE = 'bigint')
           OR NOT EXISTS (SELECT 1 FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'idempotency_keys' AND COLUMN_NAME = 'requester_user_id'
                          AND REFERENCED_TABLE_NAME = 'users' AND REFERENCED_COLUMN_NAME = 'id') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing or unexpected idempotency schema: stop and inspect';
        END IF;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'withdrawal_requests' AND COLUMN_NAME = 'cancel_reason') THEN
        ALTER TABLE withdrawal_requests ADD COLUMN cancel_reason VARCHAR(255) NULL DEFAULT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'withdrawal_requests' AND COLUMN_NAME = 'cancelled_by') THEN
        ALTER TABLE withdrawal_requests ADD COLUMN cancelled_by BIGINT NULL DEFAULT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'withdrawal_requests' AND COLUMN_NAME = 'cancelled_at') THEN
        ALTER TABLE withdrawal_requests ADD COLUMN cancelled_at DATETIME NULL DEFAULT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'withdrawal_requests' AND CONSTRAINT_NAME = 'fk_withdrawal_requests_canceller') THEN
        ALTER TABLE withdrawal_requests ADD CONSTRAINT fk_withdrawal_requests_canceller
            FOREIGN KEY (cancelled_by) REFERENCES users (id) ON DELETE RESTRICT;
    END IF;
END$$
CALL migrate_academic_withdrawal_20260827()$$
DROP PROCEDURE migrate_academic_withdrawal_20260827$$
DELIMITER ;
