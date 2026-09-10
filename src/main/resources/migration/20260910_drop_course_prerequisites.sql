-- 선수과목 정책을 폐지합니다. 실행 전 대상 DB를 백업하고 테이블 삭제 범위를 확인하세요.
-- 기존 20260824 생성 마이그레이션은 이미 적용된 배포 이력 보존을 위해 삭제하지 않습니다.
DROP TABLE IF EXISTS course_prerequisites;
