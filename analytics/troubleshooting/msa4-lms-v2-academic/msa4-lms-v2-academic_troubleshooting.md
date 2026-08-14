# msa4-lms-v2-academic 트러블슈팅

최종 수정일: 2026-08-14

## 2026-08-14 교수 및 공지 동시 수정 유실

### 원인

교수와 공지 엔티티에 버전 필드가 없어 같은 행을 동시에 읽은 요청들이 서로의 변경을 덮어쓸 수 있었다.

### 해결

- 두 엔티티에 JPA `@Version` 필드를 추가했다.
- DB 테이블에 `version BIGINT NOT NULL DEFAULT 0` 컬럼을 추가했다.
- 낙관적 락 충돌을 HTTP 409로 응답하도록 예외 처리를 추가했다.

### 적용 순서

기존 DB에는 애플리케이션 배포 전에 `src/main/resources/migration/20260814_add_professor_notice_version.sql`을 실행한다.
