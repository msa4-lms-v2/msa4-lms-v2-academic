-- 2026-09-04: Academic→Payment Kafka 이벤트 연동 - 발행 측 인프라
-- outbox_events 테이블(ERD 확정, DDL 미구현이었음)과, Student/Semester 변경 시점을
-- 이벤트 payload의 sourceVersion으로 쓸 snapshot_version 컬럼을 추가한다.
-- WithdrawalRequest는 이미 JPA @Version(version 컬럼)이 있어 그대로 재사용하므로 여기서 손대지 않는다.

CREATE TABLE IF NOT EXISTS outbox_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id CHAR(36) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload JSON NOT NULL,
    source_version BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME NOT NULL,
    locked_by VARCHAR(100) NULL,
    locked_until DATETIME NULL,
    last_error_code VARCHAR(100) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_outbox_events_event_id UNIQUE (event_id),
    INDEX idx_outbox_events_status_next_attempt (status, next_attempt_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE students  ADD COLUMN snapshot_version BIGINT NOT NULL DEFAULT 0 AFTER id;
ALTER TABLE semesters ADD COLUMN snapshot_version BIGINT NOT NULL DEFAULT 1 AFTER id;
