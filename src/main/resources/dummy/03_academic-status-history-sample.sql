-- Academic 로컬 Postman 조회 테스트 전용. 운영/공유 DB에 실행하지 마세요.
-- academic_status_histories에만 가상 이력을 추가합니다. 실제 학적·계정·소속·수강·원본 신청은 변경하지 않습니다.
-- 실제 승인 결과가 아닙니다. reason의 LOCAL_STATUS_HISTORY_SAMPLE_V1 표시로 구분하며 source_id는 NULL입니다.
-- 기존 활성 재학 학생과 관리자를 조회합니다. 없으면 계정을 만들지 않고 건너뜁니다.
-- 같은 대상에 대한 순차 재실행은 중복을 만들지 않습니다. 파일 전체를 한 연결에서 실행하세요.
-- local 재시작은 01/02 더미도 실행합니다. 기존 데이터 보존 시 이 03 파일만 수동 실행하세요.

START TRANSACTION;

SET @history_sample_marker = '[LOCAL_STATUS_HISTORY_SAMPLE_V1]';
SET @history_sample_admin_id = (
    SELECT id FROM users
    WHERE role = 'ADMIN' AND status = 'ACTIVE' AND deleted_at IS NULL
    ORDER BY id LIMIT 1
);

-- 이전에 선택한 대상이 있으면 유지합니다. 대상의 상태가 변해도 다른 학생에게 자동으로 갈아타지 않습니다.
SET @history_sample_primary_id = COALESCE(
    (SELECT student_id FROM academic_status_histories
     WHERE reason = CONCAT(@history_sample_marker, ' P1 일반휴학(조회용 가상 이력)')
     ORDER BY id LIMIT 1),
    (SELECT student.id FROM students student
     JOIN users account ON account.id = student.user_id
     WHERE account.role = 'STUDENT' AND account.status = 'ACTIVE' AND account.deleted_at IS NULL
       AND student.academic_status = 'ENROLLED'
     ORDER BY student.id LIMIT 1)
);
SET @history_sample_secondary_id = COALESCE(
    (SELECT student_id FROM academic_status_histories
     WHERE reason = CONCAT(@history_sample_marker, ' S1 일반휴학(조회용 가상 이력)')
     ORDER BY id LIMIT 1),
    (SELECT student.id FROM students student
     JOIN users account ON account.id = student.user_id
     WHERE account.role = 'STUDENT' AND account.status = 'ACTIVE' AND account.deleted_at IS NULL
       AND student.academic_status = 'ENROLLED' AND student.id <> @history_sample_primary_id
     ORDER BY student.id LIMIT 1)
);

INSERT INTO academic_status_histories (
    student_id, previous_status, new_status, reason, changed_by, source_type, source_id, created_at
)
SELECT student.id, sample.previous_status, sample.new_status,
       CONCAT(@history_sample_marker, ' ', sample.reason), @history_sample_admin_id,
       sample.source_type, NULL, sample.created_at
FROM (
    SELECT @history_sample_primary_id AS student_id, 'ENROLLED' AS previous_status, 'ON_LEAVE' AS new_status,
           'P1 일반휴학(조회용 가상 이력)' AS reason, 'LEAVE_REQUEST' AS source_type,
           CAST('2026-08-01 09:00:00' AS DATETIME) AS created_at
    UNION ALL
    SELECT @history_sample_primary_id, 'ON_LEAVE', 'ENROLLED',
           'P2 복학(조회용 가상 이력)', 'LEAVE_REQUEST', CAST('2026-08-05 09:00:00' AS DATETIME)
    UNION ALL
    SELECT @history_sample_primary_id, 'ENROLLED', 'ON_LEAVE',
           'P3 휴학(조회용 가상 이력)', 'LEAVE_REQUEST', CAST('2026-08-10 09:00:00' AS DATETIME)
    UNION ALL
    SELECT @history_sample_primary_id, 'ON_LEAVE', 'ENROLLED',
           'P4 관리자 교정(조회용 가상 이력)', 'ADMIN_CORRECTION', CAST('2026-08-10 09:00:00' AS DATETIME)
    UNION ALL
    SELECT @history_sample_secondary_id, 'ENROLLED', 'ON_LEAVE',
           'S1 일반휴학(조회용 가상 이력)', 'LEAVE_REQUEST', CAST('2026-08-03 09:00:00' AS DATETIME)
    UNION ALL
    SELECT @history_sample_secondary_id, 'ON_LEAVE', 'ENROLLED',
           'S2 복학(조회용 가상 이력)', 'LEAVE_REQUEST', CAST('2026-08-07 09:00:00' AS DATETIME)
) sample
JOIN students student ON student.id = sample.student_id
JOIN users account ON account.id = student.user_id
WHERE @history_sample_admin_id IS NOT NULL
  AND account.role = 'STUDENT' AND account.status = 'ACTIVE' AND account.deleted_at IS NULL
  AND student.academic_status = 'ENROLLED'
  AND NOT EXISTS (
      SELECT 1 FROM academic_status_histories existing
      WHERE existing.student_id = student.id
        AND existing.reason = CONCAT(@history_sample_marker, ' ', sample.reason)
  );
SET @history_sample_inserted_count = ROW_COUNT();

COMMIT;

-- READY_PRIMARY_ONLY는 실패가 아닙니다. 기존 재학 학생이 한 명이라 4건만 준비된 상태입니다.
SELECT CASE
           WHEN @history_sample_admin_id IS NULL THEN 'SKIPPED_NO_ACTIVE_ADMIN'
           WHEN @history_sample_primary_id IS NULL THEN 'SKIPPED_NO_ENROLLED_STUDENT'
           WHEN (SELECT COUNT(*) FROM academic_status_histories
                 WHERE LEFT(reason, CHAR_LENGTH(@history_sample_marker)) = @history_sample_marker) = 6 THEN 'READY'
           WHEN (SELECT COUNT(*) FROM academic_status_histories
                 WHERE LEFT(reason, CHAR_LENGTH(@history_sample_marker)) = @history_sample_marker) = 4 THEN 'READY_PRIMARY_ONLY'
           ELSE 'CHECK_SELECTED_STUDENT_STATE'
       END AS seed_status,
       @history_sample_inserted_count AS inserted_count,
       @history_sample_primary_id AS primary_student_id,
       @history_sample_secondary_id AS secondary_student_id,
       @history_sample_admin_id AS changed_by_user_id;

-- student_id는 검색 조건, student_user_id는 Auth 로그인 계정과 대조할 값입니다. 비밀번호/토큰은 출력하지 않습니다.
SELECT student.id AS student_id, account.id AS student_user_id, account.name AS student_name,
       student.department_id, student.academic_status AS current_academic_status,
       student.advisor_id AS advisor_professor_id, advisor.user_id AS advisor_user_id,
       COUNT(history.id) AS sample_history_count
FROM academic_status_histories history
JOIN students student ON student.id = history.student_id
JOIN users account ON account.id = student.user_id
LEFT JOIN professors advisor ON advisor.id = student.advisor_id
WHERE LEFT(history.reason, CHAR_LENGTH(@history_sample_marker)) = @history_sample_marker
GROUP BY student.id, account.id, account.name, student.department_id, student.academic_status,
         student.advisor_id, advisor.user_id
ORDER BY student.id;

SELECT id AS history_id, student_id, previous_status, new_status, source_type, source_id, reason, created_at
FROM academic_status_histories
WHERE LEFT(reason, CHAR_LENGTH(@history_sample_marker)) = @history_sample_marker
ORDER BY created_at DESC, id DESC;
