-- 로컬 Postman 수강신청 테스트 전용. 운영/공유 DB에서는 실행하지 않습니다.
-- 기존 01 더미 또는 실제 로컬 데이터의 활성 교수/학과를 참조할 뿐 계정·학적·성적은 수정하지 않습니다.
-- 전용 학기: 2099 FIRST (is_current=false). 실제 업무 학기의 기간/최대학점은 바꾸지 않습니다.
-- 재실행 시 전용 학기의 신청 기간만 실행 시각 -1일 ~ +7일로 갱신합니다.
-- 규칙·강의의 수동 변경 및 Postman으로 만든 신청/이력/멱등 키는 초기화하지 않습니다.
-- 2099 FIRST 또는 TEST-ENR-*가 다른 용도로 존재하면 생성을 건너뜁니다. 마지막 seed_status를 확인하세요.
-- 최초 스키마가 준비된 Academic 로컬 DB에서 파일 전체를 한 연결로 실행하세요. 오류 시 ROLLBACK 후 원인을 확인하세요.

START TRANSACTION;

SET @enrollment_sample_marker = 'LOCAL_ENROLLMENT_APPLICATION_SAMPLE';
SET @enrollment_sample_professor_id = (
    SELECT professor.id
    FROM professors professor
    JOIN users account ON account.id = professor.user_id
    JOIN departments department ON department.id = professor.department_id
    WHERE account.role = 'PROFESSOR'
      AND account.status = 'ACTIVE'
      AND account.deleted_at IS NULL
      AND department.active = TRUE
    ORDER BY professor.id
    LIMIT 1
);
SET @enrollment_sample_department_id = (
    SELECT department_id FROM professors WHERE id = @enrollment_sample_professor_id
);
SET @enrollment_sample_existing_semester_id = (
    SELECT id FROM semesters WHERE academic_year = 2099 AND term = 'FIRST'
);
SET @enrollment_sample_owned = EXISTS (
    SELECT 1 FROM lectures lecture
    JOIN courses course ON course.id = lecture.course_id
    WHERE lecture.semester_id = @enrollment_sample_existing_semester_id
      AND lecture.section_no = 'LOCALTEST'
      AND lecture.syllabus = @enrollment_sample_marker
      AND course.code IN ('TEST-ENR-A', 'TEST-ENR-B', 'TEST-ENR-C', 'TEST-ENR-D')
);
SET @enrollment_sample_conflict = NOT @enrollment_sample_owned AND (
    @enrollment_sample_existing_semester_id IS NOT NULL OR EXISTS (
        SELECT 1 FROM courses
        WHERE code IN ('TEST-ENR-A', 'TEST-ENR-B', 'TEST-ENR-C', 'TEST-ENR-D', 'TEST-ENR-PRE')
    )
);

INSERT INTO semesters (
    academic_year, term, start_date, end_date, enrollment_start_at, enrollment_end_at, is_current
)
SELECT 2099, 'FIRST', '2099-03-02', '2099-06-19', NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 7 DAY, FALSE
WHERE @enrollment_sample_professor_id IS NOT NULL
  AND NOT @enrollment_sample_conflict
  AND @enrollment_sample_existing_semester_id IS NULL;
SET @enrollment_sample_created = ROW_COUNT();
SET @enrollment_sample_enabled = @enrollment_sample_professor_id IS NOT NULL
    AND NOT @enrollment_sample_conflict
    AND (@enrollment_sample_created = 1 OR @enrollment_sample_owned);
SET @enrollment_sample_semester_id = (
    SELECT id FROM semesters
    WHERE academic_year = 2099 AND term = 'FIRST' AND @enrollment_sample_enabled
);

UPDATE semesters
SET enrollment_start_at = NOW() - INTERVAL 1 DAY, enrollment_end_at = NOW() + INTERVAL 7 DAY
WHERE id = @enrollment_sample_semester_id;

INSERT INTO enrollment_credit_limit_rules (semester_id, max_credits, is_active)
SELECT @enrollment_sample_semester_id, 6, TRUE
WHERE @enrollment_sample_enabled
  AND NOT EXISTS (
      SELECT 1 FROM enrollment_credit_limit_rules WHERE semester_id = @enrollment_sample_semester_id
  );

INSERT INTO courses (department_id, code, name, credits, target_grade, completion_type)
SELECT @enrollment_sample_department_id, sample.code, sample.name, 3, NULL, 'GENERAL_ELECTIVE'
FROM (
    SELECT 'TEST-ENR-A' AS code, '[테스트] 수강신청 성공 A' AS name
    UNION ALL SELECT 'TEST-ENR-B', '[테스트] 최대학점 경계 B'
    UNION ALL SELECT 'TEST-ENR-C', '[테스트] 최대학점 초과 C'
    UNION ALL SELECT 'TEST-ENR-D', '[테스트] 선수과목 미충족 D'
    UNION ALL SELECT 'TEST-ENR-PRE', '[테스트] 미이수 선수과목'
) sample
WHERE @enrollment_sample_enabled
  AND NOT EXISTS (SELECT 1 FROM courses existing WHERE existing.code = sample.code);

INSERT INTO lectures (
    semester_id, course_id, professor_id, section_no, capacity, classroom, status,
    midterm_ratio, final_ratio, assignment_ratio, attendance_ratio, syllabus
)
SELECT @enrollment_sample_semester_id, course.id, @enrollment_sample_professor_id,
       'LOCALTEST', 40, 'LOCAL-TEST', 'OPEN', 30, 30, 30, 10, @enrollment_sample_marker
FROM courses course
WHERE @enrollment_sample_enabled
  AND course.code IN ('TEST-ENR-A', 'TEST-ENR-B', 'TEST-ENR-C', 'TEST-ENR-D')
  AND NOT EXISTS (
      SELECT 1 FROM lectures existing
      WHERE existing.semester_id = @enrollment_sample_semester_id
        AND existing.course_id = course.id AND existing.section_no = 'LOCALTEST'
  );

-- 테스트 강의끼리 시간표 충돌이 나지 않도록 월~목 1~2교시에 나눠 배치합니다.
INSERT INTO lecture_schedules (lecture_id, day_of_week, start_period, end_period)
SELECT lecture.id,
       CASE course.code WHEN 'TEST-ENR-A' THEN 'MON' WHEN 'TEST-ENR-B' THEN 'TUE'
                        WHEN 'TEST-ENR-C' THEN 'WED' WHEN 'TEST-ENR-D' THEN 'THU' END,
       1, 2
FROM lectures lecture
JOIN courses course ON course.id = lecture.course_id
WHERE lecture.semester_id = @enrollment_sample_semester_id
  AND lecture.section_no = 'LOCALTEST' AND lecture.syllabus = @enrollment_sample_marker
  AND course.code IN ('TEST-ENR-A', 'TEST-ENR-B', 'TEST-ENR-C', 'TEST-ENR-D')
  AND NOT EXISTS (SELECT 1 FROM lecture_schedules existing WHERE existing.lecture_id = lecture.id);

-- PRE에는 개설 강의/수강/성적을 만들지 않습니다. D 신청은 선수과목 미충족으로 거절되어야 합니다.
INSERT INTO course_prerequisites (course_id, prerequisite_course_id, is_active)
SELECT target.id, prerequisite.id, TRUE
FROM courses target
JOIN courses prerequisite ON prerequisite.code = 'TEST-ENR-PRE'
WHERE @enrollment_sample_enabled AND target.code = 'TEST-ENR-D'
  AND NOT EXISTS (
      SELECT 1 FROM course_prerequisites existing
      WHERE existing.course_id = target.id AND existing.prerequisite_course_id = prerequisite.id
  );

COMMIT;

SELECT CASE WHEN @enrollment_sample_conflict THEN 'SKIPPED_RESERVED_DATA_CONFLICT'
            WHEN @enrollment_sample_professor_id IS NULL THEN 'SKIPPED_NO_ACTIVE_PROFESSOR'
            ELSE 'READY' END AS seed_status,
       @enrollment_sample_semester_id AS semester_id;

-- Postman에는 course_id가 아니라 아래 lecture_id를 넣습니다. 실제 값은 DB마다 다릅니다.
SELECT semester.id AS semester_id, course.code AS course_code, lecture.id AS lecture_id,
       course.credits, lecture.status, rule.max_credits, rule.is_active AS rule_active,
       semester.enrollment_start_at, semester.enrollment_end_at
FROM lectures lecture
JOIN courses course ON course.id = lecture.course_id
JOIN semesters semester ON semester.id = lecture.semester_id
JOIN enrollment_credit_limit_rules rule ON rule.semester_id = semester.id
WHERE semester.id = @enrollment_sample_semester_id
  AND lecture.section_no = 'LOCALTEST' AND lecture.syllabus = @enrollment_sample_marker
ORDER BY course.code;
