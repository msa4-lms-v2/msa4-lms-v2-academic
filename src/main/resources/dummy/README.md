# 로컬 수강신청 테스트 더미

`02_enrollment-application-sample.sql`은 기존 스키마에 **데이터만** 넣는다. 테이블·컬럼은 변경하지 않는다.
운영/공유 DB에 적용하지 않는다. Auth·SCG·Payment 데이터나 코드는 변경하지 않는다.

## 전과 신청 희망 학과·전공 더미

`04_department-transfer-target-sample.sql`은 전과 신청 Postman 테스트에서 사용할 활성 단과대·학과·전공을 추가한다. 기존 학생 소속, 전과 신청, 접수 기간과 다른 서비스 데이터는 변경하지 않는다.

- 단과대: 코드 `DTR`, `전과테스트경영대학`
- 학과: 코드 `DTR`, `전과테스트경영학과`
- 반복 실행해도 코드별 한 행만 유지하며 활성 상태를 테스트 기준으로 복원한다.
- 파일 전체를 Academic 로컬 DB의 한 연결에서 실행하면 마지막 조회로 `target_department_id`가 출력된다.
- local 프로필로 Academic을 재시작해도 `dummy/*.sql` 순서에 따라 자동 실행된다. 기존 Postman 결과를 보존하려면 재시작 대신 이 파일만 수동으로 실행한다.

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

## 학적 변경 이력 조회용 더미

`03_academic-status-history-sample.sql`은 `academic_status_histories`에 조회용 가상 이력만 추가한다.
학생의 현재 학적, 계정, 소속, 지도교수, 수강, 원본 신청은 수정하지 않으며 Auth·SCG·Payment는 변경하지 않는다.
테이블·컬럼 추가도 없다. **실제 승인 결과가 아니며 운영/공유 DB에는 적용하지 않는다.**

### 적용 방법

1. 로컬 Academic DB를 선택한다. 현재 `academic_status_histories` 스키마가 먼저 준비돼 있어야 한다.
2. SQL 편집기에서 `03_academic-status-history-sample.sql` 전체만 한 연결로 실행한다. 오류 시 같은 연결에서 `ROLLBACK`한다.
3. `seed_status`, 실제 `student_id`와 `student_user_id`, 생성 이력 결과를 확인한다.
4. DB에 직접 추가한 이력은 다음 GET부터 조회되므로 Academic 재시작은 필요 없다.

local 프로필은 `dummy/*.sql`을 읽으므로 재시작 때도 03이 실행된다. 단, **01은 학생 상태·성적·현재 학기 등을 다시 설정하고 02는 테스트 수강신청 기간을 갱신한다. 기존 결과를 보존하려면 03만 수동 실행한다.** 설정 파일과 기존 01/02 SQL은 이번 작업에서 변경하지 않는다.

### 대상과 데이터

- 최초에는 삭제되지 않은 `ACTIVE` 학생 계정 중 `ENROLLED`인 학생을 ID 순으로 최대 2명 선택한다. 관리자는 삭제되지 않은 `ACTIVE` ADMIN 중 ID가 가장 작은 계정을 `changed_by`로 사용한다.
- 학생이나 관리자가 없으면 임의의 계정·학생을 만들지 않고 건너뛴다.
- 처음 기록한 기본·추가 학생은 마커로 기억한다. 재실행 전에 그 학생이 비활성·논리 삭제·휴학 등으로 변경되면 해당 학생에게 누락 이력을 추가하지 않는다. 기존 이력도 덮어쓰지 않는다.
- 사유의 `[LOCAL_STATUS_HISTORY_SAMPLE_V1]`로 실제 이력과 구분한다. 실제 원본 신청을 만들지 않으므로 `source_id=NULL`이다.
- 동일 대상·마커 이력이 있으면 추가하지 않는다. 단일 연결 순차 실행용이며 동시 실행 중복 방지까지 보장하지는 않는다.

| 대상 | 가상 이력 | 기록 시각(KST) | source_type |
|---|---|---|---|
| 기본 학생 P1 | ENROLLED → ON_LEAVE | 2026-08-01 09:00:00 | LEAVE_REQUEST |
| 기본 학생 P2 | ON_LEAVE → ENROLLED | 2026-08-05 09:00:00 | LEAVE_REQUEST |
| 기본 학생 P3 | ENROLLED → ON_LEAVE | 2026-08-10 09:00:00 | LEAVE_REQUEST |
| 기본 학생 P4 | ON_LEAVE → ENROLLED | 2026-08-10 09:00:00 | ADMIN_CORRECTION |
| 추가 학생 S1(있을 때만) | ENROLLED → ON_LEAVE | 2026-08-03 09:00:00 | LEAVE_REQUEST |
| 추가 학생 S2(있을 때만) | ON_LEAVE → ENROLLED | 2026-08-07 09:00:00 | LEAVE_REQUEST |

`READY`는 6건, `READY_PRIMARY_ONLY`는 기본 학생 4건이 준비됐다는 뜻이다.
`SKIPPED_NO_ACTIVE_ADMIN`, `SKIPPED_NO_ENROLLED_STUDENT`는 선행 데이터 부족을 뜻한다.
`CHECK_SELECTED_STUDENT_STATE`는 기존 대상 상태 또는 부분 데이터를 확인해야 한다는 뜻이다.
재실행 시 `inserted_count=0`이고 기존 4/6건이 남아 있는 것도 정상이다. READY는 현재 교수의 조회 권한까지 보장하는 값은 아니다.

### Postman 핵심 테스트

공통 URL은 `GET http://localhost:8080/api/academic/status-histories`다.
Authorization은 각 역할의 **accessToken**을 쓰는 Bearer Token이며 Body와 Idempotency-Key는 없다.
아래 `{primaryStudentId}`·`{secondaryStudentId}`는 실행 결과로 바꾼다. `student_user_id`와 혼동하지 않는다.

| Request 이름 | 역할 / 쿼리 | 예상 |
|---|---|---|
| Admin - Get Academic Status Histories | ADMIN, `?page=1&size=20&sortDirection=desc` | 00, 테스트 이력 4건 또는 6건 포함. 실제 이력이 있으면 totalCount는 더 클 수 있음 |
| Admin - Filter Academic Status Histories | ADMIN, `?studentId={primaryStudentId}&newStatus=ON_LEAVE&fromDate=2026-08-01&toDate=2026-08-10` | 기본 학생 P1/P3 포함, 조건 밖 이력 제외 |
| Admin - Page Academic Status Histories | ADMIN, `?studentId={primaryStudentId}&page=1&size=2&sortDirection=desc` 후 page=2 | 같은 시각은 historyId 내림차순. 페이지 간 중복 없음 |
| Student - Get My Academic Status Histories | 기본 학생의 STUDENT 토큰, 필터 없음 | 본인 P1~P4만 포함하고 추가 학생 S1/S2는 제외 |
| Student - Hide Other Student Histories | 기본 학생 토큰, `?studentId={secondaryStudentId}` | 추가 학생이 실제로 있을 때 HTTP 200, 빈 items, totalCount=0 |
| Professor - Get Scoped Academic Status Histories | PROFESSOR, 선택 학생의 `studentId` | 현재 지도학생·같은 학과·현재 학기 ACTIVE 수강생이면 해당 학생 이력, 관계가 없으면 빈 목록 |

추가 학생이 없거나 범위 밖 교수가 없으면 해당 권한 경계 테스트를 완료했다고 기록하지 않는다.
이 SQL은 테스트 편의를 위해 계정이나 소속 관계를 조작하지 않으므로 필요한 추가 계정은 별도로 준비해야 한다.
