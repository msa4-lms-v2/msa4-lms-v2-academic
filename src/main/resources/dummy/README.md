# 로컬 수강신청 테스트 더미

`02_enrollment-application-sample.sql`은 기존 스키마에 **데이터만** 넣는다. 테이블·컬럼은 변경하지 않는다.
운영/공유 DB에 적용하지 않는다. Auth·SCG·Payment 데이터나 코드는 변경하지 않는다.

## 적용

- 현재 `local` 설정은 `dummy/*.sql`을 실행한다. Academic을 local 프로필로 재시작하면 기존 `01`과 새 `02`가 실행된다.
- **기존 `01`은 사용자·학적·성적·현재 학기·기본 최대학점 등의 샘플 값을 다시 설정한다.** 기존 테스트 결과를 보존하려면 재시작 대신 Academic 로컬 DB의 SQL 편집기에서 `02` 파일 전체만 한 연결로 실행한다.
- `schema.sql` 및 필요한 migration 적용이 선행되어야 한다. `02`는 누락 컬럼이나 테이블을 보정하지 않는다.
- `02` 실행 결과의 `seed_status=READY`와 강의 4행을 확인한다. 활성 교수/학과가 없으면 `SKIPPED_NO_ACTIVE_PROFESSOR`, 예약 학기/코드가 다른 데이터와 겹치면 `SKIPPED_RESERVED_DATA_CONFLICT`로 건너뛴다. 교수/학생 ID를 가정하거나 계정을 자동 생성하지 않는다.
- 스크립트는 transaction 단위다. 수동 실행 도중 오류가 나면 같은 연결에서 `ROLLBACK` 후 원인을 확인한다.

## 생성 데이터

| 대상 | 최초 생성 | 목적 |
|---|---|---|
| semesters | 2099년 FIRST 1행, `is_current=false` | 기존 실제 학기와 분리. 신청 기간은 실행 시각 -1일 ~ +7일 |
| enrollment_credit_limit_rules | 활성 규칙 1행, 최대 6학점 | 3학점 두 강의 허용, 세 번째 강의 거절 |
| courses | `TEST-ENR-A/B/C/D/PRE` 5행, 각각 3학점 | 기존 수강·성적 이력과 분리 |
| lectures | A/B/C/D 4행, OPEN, 정원 40, 분반 LOCALTEST | Postman 신청 대상. PRE 강의는 만들지 않음 |
| lecture_schedules | 월/화/수/목 1~2교시 각 1행 | 테스트 강의끼리 시간표 중복 방지 |
| course_prerequisites | D → PRE 활성 규칙 1행 | 선수과목 미이수 거절 |

이 파일은 기존 사용자·학생·교수·학적·성적을 수정하지 않으며, 수강신청·신청 이력·멱등 키도 미리 생성하지 않는다.
재실행 시 전용 학기의 신청 기간만 새로 열고, 나머지 데이터는 누락된 항목만 추가한다.
수동 변경한 최대학점·규칙 활성 상태·강의 상태/정원 및 Postman 신청 결과는 보존하므로, 재실행이 테스트 초기화는 아니다.
2099년 학기라는 표시는 로컬 데이터 구분용이며 실제 수업 학기를 뜻하지 않는다. `is_current`에 의존하는 다른 화면의 테스트용도 아니다.

## 생성된 ID 확인

```sql
SELECT s.id AS semester_id, c.code AS course_code, l.id AS lecture_id,
       c.credits, l.status, r.max_credits, r.is_active AS rule_active,
       s.enrollment_start_at, s.enrollment_end_at
FROM lectures l
JOIN courses c ON c.id = l.course_id
JOIN semesters s ON s.id = l.semester_id
JOIN enrollment_credit_limit_rules r ON r.semester_id = s.id
WHERE s.academic_year = 2099 AND s.term = 'FIRST'
  AND l.section_no = 'LOCALTEST'
  AND l.syllabus = 'LOCAL_ENROLLMENT_APPLICATION_SAMPLE'
ORDER BY c.code;
```

## Postman 핵심 테스트 순서

기존 **재학(ENROLLED) 학생**으로 로그인한다. 이 학생이 전용 학기에 아직 신청하지 않았고 PRE 이수 이력이 없는 초기 상태를 기준으로 한다.
모든 요청은 `POST http://localhost:8080/api/academic/enrollments`, Authorization은 학생 Bearer Token,
Body는 raw JSON `{"lectureId": 실제_강의_ID}`다. ID는 위 조회의 `lecture_id`이며 `course_id`가 아니다.

| 순서 / 영문 request 이름 | 대상 | Idempotency-Key 예시 | 예상 |
|---|---|---|---|
| 1. Reject Missing Prerequisite | D | local-enroll-d-001 | HTTP 409 / E11, `data.reasons[].code=PREREQUISITE_NOT_COMPLETED` |
| 2. Create Enrollment | A | local-enroll-a-001 | HTTP 200 / 00, ACTIVE 신청 생성 |
| 3. Replay Enrollment | A, 2와 본문 동일 | local-enroll-a-001 (2와 동일) | 2와 동일 응답·enrollmentId, 추가 저장 없음 |
| 4. Reject Duplicate Enrollment | A | local-enroll-a-duplicate-001 | HTTP 409 / E11, DUPLICATE_ENROLLMENT |
| 5. Reach Credit Limit | B | local-enroll-b-001 | HTTP 200 / 00, 합계 6학점 허용 |
| 6. Reject Credit Limit Exceeded | C | local-enroll-c-001 | HTTP 409 / E11, CREDIT_LIMIT_EXCEEDED |

최대학점 검증이 선수과목보다 먼저이므로 D는 A/B로 6학점을 채우기 **전에** 확인한다.
키는 사용자/테스트 회차별로 구별하고, 재생 테스트에서만 동일 키를 그대로 복사한다. 대소문자만 바꿔 새 키로 사용하지 않는다.
성공 POST가 실제로 저장한 `enrollments`, `enrollment_histories`, `idempotency_keys`를 확인하며, 실패 요청은 이 세 테이블에 새 행을 남기지 않는다.
이미 신청된 학생으로 다시 진행하면 중복/최대학점 오류가 날 수 있다. 이 SQL은 기존 신청을 삭제하지 않는다.
