-- 2026-09-14 입학 가상계좌 연결. 서비스/worker 중지 및 백업 후 적용. 운영 실행하지 않음.
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='admission_candidates' AND COLUMN_NAME='advisor_professor_id')=0, 'ALTER TABLE admission_candidates ADD COLUMN advisor_professor_id BIGINT NULL', 'SELECT 1');
PREPARE admission_stmt FROM @ddl;
EXECUTE admission_stmt;
DEALLOCATE PREPARE admission_stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='admission_candidates' AND COLUMN_NAME='tuition_bill_id')=0, 'ALTER TABLE admission_candidates ADD COLUMN tuition_bill_id BIGINT NULL', 'SELECT 1');
PREPARE admission_stmt FROM @ddl;
EXECUTE admission_stmt;
DEALLOCATE PREPARE admission_stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='admission_candidates' AND COLUMN_NAME='tuition_paid')=0, 'ALTER TABLE admission_candidates ADD COLUMN tuition_paid BOOLEAN NOT NULL DEFAULT FALSE', 'SELECT 1');
PREPARE admission_stmt FROM @ddl;
EXECUTE admission_stmt;
DEALLOCATE PREPARE admission_stmt;
SET @ddl=IF((SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='admission_candidates' AND CONSTRAINT_NAME='uk_admission_tuition_bill')=0,'ALTER TABLE admission_candidates ADD CONSTRAINT uk_admission_tuition_bill UNIQUE(tuition_bill_id)','SELECT 1');
PREPARE admission_stmt FROM @ddl; EXECUTE admission_stmt; DEALLOCATE PREPARE admission_stmt;

-- 기존 REGISTERED/CONFIRMED/PROVISIONING/PROVISIONED 행은 완납·Auth 활성·학생 연결 및 outbox를 검증한 ID별 전환표가 필요하다.
-- 일괄 상태 치환을 수행하지 않는다. 아래 조회가 0행이 되기 전에는 새 버전을 기동하지 않는다.
SELECT id,status,student_id FROM admission_candidates WHERE status NOT IN ('PENDING','COMPLETED','CANCELLED');
ALTER TABLE admission_candidates ALTER status SET DEFAULT 'PENDING';
