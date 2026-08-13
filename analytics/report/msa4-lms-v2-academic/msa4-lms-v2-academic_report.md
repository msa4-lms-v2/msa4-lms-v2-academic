# msa4-lms-v2-academic 작업 리포트

최종 수정일: 2026-08-14

## 2026-08-14 현재학기 생성 유일성

- `is_current = 1`인 행에만 값이 생성되는 `current_semester_guard` 컬럼을 추가했다.
- 생성 컬럼에 UNIQUE 제약을 적용해 현재학기가 동시에 두 건 생성되지 않게 했다.
- 기존 현재학기 해제를 먼저 flush한 뒤 새 현재학기를 저장해 교체 시 UNIQUE 충돌을 방지했다.
- 재구축용 `schema.sql`과 기존 DB 적용용 마이그레이션 스크립트를 함께 갱신했다.
- MySQL에서 두 번째 현재학기 저장을 거부하는 통합 테스트를 추가했다.

### 검증

- `compileJava`, `compileTestJava`가 통과했다.
- Testcontainers 기반 통합 테스트는 로컬 Docker 데몬을 찾지 못해 실행하지 못했다.
