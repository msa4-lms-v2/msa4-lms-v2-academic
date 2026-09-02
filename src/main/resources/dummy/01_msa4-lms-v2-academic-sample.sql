-- Auth의 accounts.id와 Academic의 users.id는 동일해야 합니다.
-- Auth 계정 ID가 다르면 아래 세 값만 실제 accounts.id에 맞게 변경합니다.
SET @student_user_id = 1;
SET @professor_user_id = 2;
SET @admin_user_id = 3;

INSERT INTO users (
    id, name, email, phone_number, address, role, status, created_at, updated_at, deleted_at
) VALUES
    (@student_user_id, '김학생', NULL, '010-1000-1000', '서울특별시', 'STUDENT', 'ACTIVE', NOW(), NOW(), NULL),
    (@professor_user_id, '박교수', NULL, '010-2000-2000', '서울특별시', 'PROFESSOR', 'ACTIVE', NOW(), NOW(), NULL),
    (@admin_user_id, '이관리자', NULL, '010-3000-3000', '서울특별시', 'ADMIN', 'ACTIVE', NOW(), NOW(), NULL)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    phone_number = VALUES(phone_number),
    address = VALUES(address),
    role = VALUES(role),
    status = VALUES(status),
    updated_at = NOW(),
    deleted_at = NULL;

INSERT INTO colleges (code, name, active) VALUES
    ('ENG', '공과대학', TRUE),
    ('HUM', '인문대학', TRUE)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    active = VALUES(active);

SET @engineering_college_id = (SELECT id FROM colleges WHERE code = 'ENG' LIMIT 1);
SET @humanities_college_id = (SELECT id FROM colleges WHERE code = 'HUM' LIMIT 1);

INSERT INTO departments (code, college_id, name, active) VALUES
    ('001', @engineering_college_id, '컴퓨터공학과', TRUE),
    ('002', @engineering_college_id, '전자공학과', TRUE),
    ('003', @humanities_college_id, '국어국문학과', FALSE),
    ('004', NULL, '자유전공학과', TRUE)
ON DUPLICATE KEY UPDATE
    college_id = VALUES(college_id),
    name = VALUES(name),
    active = VALUES(active);

SET @cse_department_id = (SELECT id FROM departments WHERE code = '001' LIMIT 1);

INSERT INTO professors (user_id, hire_year, department_id)
VALUES (@professor_user_id, 2020, @cse_department_id)
ON DUPLICATE KEY UPDATE
    hire_year = VALUES(hire_year),
    department_id = VALUES(department_id);

SET @sample_professor_id = (
    SELECT id FROM professors WHERE user_id = @professor_user_id LIMIT 1
);

INSERT INTO students (
    user_id,
    department_id,
    double_major_id,
    grade_level,
    admission_year,
    academic_status,
    advisor_id
)
VALUES (
    @student_user_id,
    @cse_department_id,
    NULL,
    3,
    2024,
    'ENROLLED',
    @sample_professor_id
)
ON DUPLICATE KEY UPDATE
    department_id = VALUES(department_id),
    double_major_id = VALUES(double_major_id),
    grade_level = VALUES(grade_level),
    admission_year = VALUES(admission_year),
    academic_status = VALUES(academic_status),
    advisor_id = VALUES(advisor_id);

SET @sample_student_id = (
    SELECT id FROM students WHERE user_id = @student_user_id LIMIT 1
);

INSERT INTO semesters (
    academic_year,
    term,
    start_date,
    end_date,
    enrollment_start_at,
    enrollment_end_at,
    is_current
) VALUES
    (2025, 'FIRST', '2025-03-04', '2025-06-20', '2025-02-10 09:00:00', '2025-02-14 18:00:00', FALSE),
    (2025, 'SECOND', '2025-09-01', '2025-12-19', '2025-08-11 09:00:00', '2025-08-15 18:00:00', FALSE),
    (2026, 'FIRST', '2026-03-02', '2026-06-19', '2026-02-09 09:00:00', '2026-02-13 18:00:00', FALSE),
    (2026, 'SECOND', '2026-09-01', '2026-12-18', '2026-08-10 09:00:00', '2026-08-14 18:00:00', FALSE)
ON DUPLICATE KEY UPDATE
    start_date = VALUES(start_date),
    end_date = VALUES(end_date),
    enrollment_start_at = VALUES(enrollment_start_at),
    enrollment_end_at = VALUES(enrollment_end_at);

-- 현재 학기는 서비스 정책과 동일하게 하나만 유지합니다.
UPDATE semesters SET is_current = FALSE WHERE is_current = TRUE;
UPDATE semesters
SET is_current = TRUE
WHERE academic_year = 2026 AND term = 'SECOND';

SET @completed_semester_id = (
    SELECT id FROM semesters WHERE academic_year = 2026 AND term = 'FIRST' LIMIT 1
);

SET @current_semester_id = (
    SELECT id FROM semesters WHERE academic_year = 2026 AND term = 'SECOND' LIMIT 1
);

INSERT INTO enrollment_credit_limit_rules (
    semester_id,
    max_credits,
    is_active,
    created_at,
    updated_at
) VALUES (
    @current_semester_id,
    18,
    TRUE,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE
    max_credits = VALUES(max_credits),
    is_active = VALUES(is_active),
    updated_at = NOW();

INSERT INTO courses (department_id, code, name, credits, target_grade, completion_type) VALUES
    (@cse_department_id, 'CSE1001', '컴퓨터공학개론', 3, 1, 'MAJOR_REQUIRED'),
    (@cse_department_id, 'CSE2001', '자료구조', 3, 2, 'MAJOR_ELECTIVE'),
    (@cse_department_id, 'GEN1001', '대학글쓰기', 3, 1, 'GENERAL_REQUIRED'),
    (@cse_department_id, 'GEN2001', '인문학의이해', 3, 2, 'GENERAL_ELECTIVE'),
    (@cse_department_id, 'CSE3001', '운영체제', 3, 3, 'MAJOR_REQUIRED'),
    (@cse_department_id, 'CSE3002', '컴퓨터네트워크', 3, 3, 'MAJOR_ELECTIVE')
ON DUPLICATE KEY UPDATE
    department_id = VALUES(department_id),
    name = VALUES(name),
    credits = VALUES(credits),
    target_grade = VALUES(target_grade),
    completion_type = VALUES(completion_type);

SET @major_required_course_id = (SELECT id FROM courses WHERE code = 'CSE1001' LIMIT 1);
SET @major_elective_course_id = (SELECT id FROM courses WHERE code = 'CSE2001' LIMIT 1);
SET @general_required_course_id = (SELECT id FROM courses WHERE code = 'GEN1001' LIMIT 1);
SET @general_elective_course_id = (SELECT id FROM courses WHERE code = 'GEN2001' LIMIT 1);
SET @failed_course_id = (SELECT id FROM courses WHERE code = 'CSE3001' LIMIT 1);
SET @draft_course_id = (SELECT id FROM courses WHERE code = 'CSE3002' LIMIT 1);

INSERT INTO course_prerequisites (
    course_id,
    prerequisite_course_id,
    is_active,
    created_at,
    updated_at
) VALUES
    (@major_elective_course_id, @major_required_course_id, TRUE, NOW(), NOW()),
    (@failed_course_id, @major_elective_course_id, TRUE, NOW(), NOW()),
    (@draft_course_id, @major_elective_course_id, TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    is_active = VALUES(is_active),
    updated_at = NOW();

INSERT INTO lectures (
    semester_id, course_id, professor_id, section_no, capacity, classroom, status,
    midterm_ratio, final_ratio, assignment_ratio, attendance_ratio, syllabus
)
SELECT @completed_semester_id, course.id, @sample_professor_id, '01', 40, '공학관 101호', 'CLOSED',
       30, 30, 30, 10, CONCAT(course.name, ' 강의계획서')
FROM courses course
WHERE course.id IN (
    @major_required_course_id,
    @major_elective_course_id,
    @general_required_course_id,
    @general_elective_course_id,
    @failed_course_id,
    @draft_course_id
)
AND NOT EXISTS (
    SELECT 1
    FROM lectures lecture
    WHERE lecture.semester_id = @completed_semester_id
      AND lecture.course_id = course.id
      AND lecture.section_no = '01'
);

INSERT INTO enrollments (
    student_id,
    lecture_id,
    status,
    enrolled_at,
    midterm_score,
    final_score,
    assignment_score,
    attendance_score,
    total_score,
    letter_grade,
    grade_status
)
SELECT
    @sample_student_id,
    lecture.id,
    'ACTIVE',
    '2026-02-10 10:00:00',
    85.00,
    88.00,
    90.00,
    95.00,
    88.90,
    CASE
        WHEN lecture.course_id = @failed_course_id THEN 'F'
        ELSE 'A'
    END,
    CASE
        WHEN lecture.course_id = @draft_course_id THEN 'DRAFT'
        ELSE 'OPENED'
    END
FROM lectures lecture
WHERE lecture.semester_id = @completed_semester_id
  AND lecture.course_id IN (
      @major_required_course_id,
      @major_elective_course_id,
      @general_required_course_id,
      @general_elective_course_id,
      @failed_course_id,
      @draft_course_id
  )
  AND NOT EXISTS (
      SELECT 1
      FROM enrollments enrollment
      WHERE enrollment.student_id = @sample_student_id
        AND enrollment.lecture_id = lecture.id
  );

UPDATE enrollments enrollment
JOIN lectures lecture ON lecture.id = enrollment.lecture_id
SET enrollment.status = 'ACTIVE',
    enrollment.letter_grade = CASE
        WHEN lecture.course_id = @failed_course_id THEN 'F'
        ELSE 'A'
    END,
    enrollment.grade_status = CASE
        WHEN lecture.course_id = @draft_course_id THEN 'DRAFT'
        ELSE 'OPENED'
    END
WHERE enrollment.student_id = @sample_student_id
  AND lecture.semester_id = @completed_semester_id
  AND lecture.course_id IN (
      @major_required_course_id,
      @major_elective_course_id,
      @general_required_course_id,
      @general_elective_course_id,
      @failed_course_id,
      @draft_course_id
  );

INSERT INTO graduation_requirements (
    department_id,
    admission_year,
    required_major_credits,
    required_general_credits,
    required_total_credits,
    required_courses,
    created_at,
    updated_at
)
SELECT
    @cse_department_id,
    2024,
    60,
    30,
    130,
    JSON_ARRAY('CSE1001', 'GEN1001'),
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM graduation_requirements requirement
    WHERE requirement.department_id = @cse_department_id
      AND requirement.admission_year = 2024
);

UPDATE graduation_requirements
SET required_major_credits = 60,
    required_general_credits = 30,
    required_total_credits = 130,
    required_courses = JSON_ARRAY('CSE1001', 'GEN1001'),
    updated_at = NOW()
WHERE department_id = @cse_department_id
  AND admission_year = 2024;
