CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NULL,
    phone_number VARCHAR(20) NULL,
    address VARCHAR(255) NULL,
    profile_image_key VARCHAR(500) NULL,
    role VARCHAR(20) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT NOT NULL,
    before_value JSON NULL,
    after_value JSON NULL,
    reason VARCHAR(255) NULL,
    request_id VARCHAR(50) NULL,
    ip_address VARCHAR(45) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_audit_logs_actor
        FOREIGN KEY (actor_id) REFERENCES users (id)
        ON DELETE RESTRICT,
    INDEX idx_audit_logs_actor_id (actor_id),
    INDEX idx_audit_logs_target (target_type, target_id),
    INDEX idx_audit_logs_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notices (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    content TEXT NULL,
    target_role VARCHAR(20) NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    author_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notices_author
        FOREIGN KEY (author_id) REFERENCES users (id)
        ON DELETE RESTRICT,
    INDEX idx_notices_target_active_created (target_role, is_active, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS academic_schedules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    content TEXT NULL,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    target_role VARCHAR(20) NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    author_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_academic_schedules_author
        FOREIGN KEY (author_id) REFERENCES users (id)
        ON DELETE RESTRICT,
    INDEX idx_academic_schedules_target_active_start (target_role, is_active, start_date),
    INDEX idx_academic_schedules_end_date (end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS colleges (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT uk_colleges_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS departments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(3) NOT NULL,
    college_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT uk_departments_code UNIQUE (code),
    CONSTRAINT fk_departments_college
        FOREIGN KEY (college_id) REFERENCES colleges (id)
        ON DELETE RESTRICT,
    INDEX idx_departments_college_id (college_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS professors (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    user_id BIGINT NOT NULL,
    hire_year SMALLINT NULL,
    department_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_professors_user_id UNIQUE (user_id),
    CONSTRAINT fk_professors_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_professors_department
        FOREIGN KEY (department_id) REFERENCES departments (id)
        ON DELETE RESTRICT,
    INDEX idx_professors_department_id (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS students (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    double_major_id BIGINT NULL,
    grade_level TINYINT NOT NULL,
    admission_year SMALLINT NOT NULL,
    academic_status VARCHAR(20) NOT NULL DEFAULT 'ENROLLED',
    advisor_id BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_students_user_id UNIQUE (user_id),
    CONSTRAINT ck_students_distinct_departments
        CHECK (double_major_id IS NULL OR department_id <> double_major_id),
    CONSTRAINT fk_students_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_students_department
        FOREIGN KEY (department_id) REFERENCES departments (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_students_double_major
        FOREIGN KEY (double_major_id) REFERENCES departments (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_students_advisor
        FOREIGN KEY (advisor_id) REFERENCES professors (id)
        ON DELETE RESTRICT,
    INDEX idx_students_department_id (department_id),
    INDEX idx_students_double_major_id (double_major_id),
    INDEX idx_students_advisor_id (advisor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS withdrawal_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    student_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    requested_effective_date DATE NULL,
    effective_date DATE NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_by BIGINT NOT NULL,
    advisor_reviewed_by BIGINT NULL,
    advisor_reviewed_at DATETIME NULL,
    advisor_reject_reason VARCHAR(500) NULL,
    processed_by BIGINT NULL,
    reject_reason VARCHAR(500) NULL,
    processed_at DATETIME NULL,
    cancel_reason VARCHAR(255) NULL DEFAULT NULL,
    cancelled_by BIGINT NULL DEFAULT NULL,
    cancelled_at DATETIME NULL DEFAULT NULL,
    attachment_original_name VARCHAR(255) NULL,
    attachment_stored_name VARCHAR(255) NULL,
    attachment_content_type VARCHAR(100) NULL,
    attachment_size BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active_student_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN status IN ('PENDING', 'ADVISOR_APPROVED') THEN student_id ELSE NULL END
    ) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_withdrawal_requests_active_student UNIQUE (active_student_id),
    CONSTRAINT fk_withdrawal_requests_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
    CONSTRAINT fk_withdrawal_requests_requested_by FOREIGN KEY (requested_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_withdrawal_requests_advisor_reviewer FOREIGN KEY (advisor_reviewed_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_withdrawal_requests_processor FOREIGN KEY (processed_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_withdrawal_requests_canceller FOREIGN KEY (cancelled_by) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_withdrawal_requests_student_status (student_id, status),
    INDEX idx_withdrawal_requests_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS academic_status_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    previous_status VARCHAR(20) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    reason VARCHAR(500) NULL,
    changed_by BIGINT NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    source_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_academic_status_histories_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
    CONSTRAINT fk_academic_status_histories_changed_by FOREIGN KEY (changed_by) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_academic_status_histories_student_created (student_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS counselor_availabilities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    professor_id BIGINT NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    start_time VARCHAR(5) NOT NULL,
    end_time VARCHAR(5) NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_counselor_availabilities_professor
        FOREIGN KEY (professor_id) REFERENCES professors (id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_counselor_availabilities_time
        CHECK (start_time < end_time),
    CONSTRAINT ck_counselor_availabilities_validity
        CHECK (valid_to IS NULL OR valid_to >= valid_from),
    INDEX idx_counselor_availabilities_professor_day (professor_id, day_of_week),
    INDEX idx_counselor_availabilities_validity (valid_from, valid_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS counseling_appointments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    professor_id BIGINT NOT NULL,
    appointment_at DATETIME NOT NULL,
    topic VARCHAR(255) NULL,
    professor_note TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_counseling_appointments_professor_time
        UNIQUE (professor_id, appointment_at),
    CONSTRAINT uk_counseling_appointments_student_time
        UNIQUE (student_id, appointment_at),
    CONSTRAINT fk_counseling_appointments_student
        FOREIGN KEY (student_id) REFERENCES students (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_counseling_appointments_professor
        FOREIGN KEY (professor_id) REFERENCES professors (id)
        ON DELETE RESTRICT,
    INDEX idx_counseling_appointments_student_status (student_id, status),
    INDEX idx_counseling_appointments_professor_status (professor_id, status),
    INDEX idx_counseling_appointments_at (appointment_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS counseling_notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    appointment_id BIGINT NOT NULL,
    recipient_user_id BIGINT NOT NULL,
    notification_type VARCHAR(40) NOT NULL,
    previous_status VARCHAR(20) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    message VARCHAR(500) NOT NULL,
    deduplication_key CHAR(64) NOT NULL,
    read_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_counseling_notifications_deduplication_key UNIQUE (deduplication_key),
    CONSTRAINT fk_counseling_notifications_appointment
        FOREIGN KEY (appointment_id) REFERENCES counseling_appointments (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_counseling_notifications_recipient
        FOREIGN KEY (recipient_user_id) REFERENCES users (id)
        ON DELETE RESTRICT,
    INDEX idx_counseling_notifications_recipient_read_created (recipient_user_id, read_at, created_at),
    INDEX idx_counseling_notifications_appointment (appointment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS semesters (
    id BIGINT NOT NULL AUTO_INCREMENT,
    academic_year SMALLINT NOT NULL,
    term VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    enrollment_start_at DATETIME NOT NULL,
    enrollment_end_at DATETIME NOT NULL,
    is_current TINYINT(1) NOT NULL DEFAULT 0,
    current_semester_guard TINYINT
        GENERATED ALWAYS AS (CASE WHEN is_current = 1 THEN 1 ELSE NULL END) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_semesters_academic_year_term UNIQUE (academic_year, term),
    CONSTRAINT uk_semesters_single_current UNIQUE (current_semester_guard)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS enrollment_credit_limit_rules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    semester_id BIGINT NOT NULL,
    max_credits TINYINT NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_enrollment_credit_limit_rules_semester UNIQUE (semester_id),
    CONSTRAINT ck_enrollment_credit_limit_rules_max_credits
        CHECK (max_credits BETWEEN 1 AND 30),
    CONSTRAINT fk_enrollment_credit_limit_rules_semester
        FOREIGN KEY (semester_id) REFERENCES semesters (id)
        ON DELETE RESTRICT,
    INDEX idx_enrollment_credit_limit_rules_active_semester (is_active, semester_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS courses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    department_id BIGINT NOT NULL,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    credits TINYINT NOT NULL,
    target_grade TINYINT NULL,
    completion_type VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_courses_code UNIQUE (code),
    CONSTRAINT fk_courses_department
        FOREIGN KEY (department_id) REFERENCES departments (id)
        ON DELETE RESTRICT,
    INDEX idx_courses_department_id (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS course_prerequisites (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    prerequisite_course_id BIGINT NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_course_prerequisites_course_prerequisite
        UNIQUE (course_id, prerequisite_course_id),
    CONSTRAINT ck_course_prerequisites_distinct_courses
        CHECK (course_id <> prerequisite_course_id),
    CONSTRAINT fk_course_prerequisites_course
        FOREIGN KEY (course_id) REFERENCES courses (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_course_prerequisites_prerequisite_course
        FOREIGN KEY (prerequisite_course_id) REFERENCES courses (id)
        ON DELETE RESTRICT,
    INDEX idx_course_prerequisites_course_active (course_id, is_active),
    INDEX idx_course_prerequisites_prerequisite_active (prerequisite_course_id, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS lecture_opening_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    course_id BIGINT NOT NULL,
    professor_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL,
    section_no VARCHAR(10) NOT NULL,
    requested_capacity INT NOT NULL,
    classroom VARCHAR(50) NOT NULL,
    midterm_ratio INT NOT NULL DEFAULT 30,
    final_ratio INT NOT NULL DEFAULT 30,
    assignment_ratio INT NOT NULL DEFAULT 30,
    attendance_ratio INT NOT NULL DEFAULT 10,
    syllabus TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reject_reason VARCHAR(500) NULL,
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active_request_guard TINYINT
        GENERATED ALWAYS AS (CASE WHEN status = 'PENDING' THEN 1 ELSE NULL END) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_lecture_opening_requests_pending
        UNIQUE (course_id, professor_id, semester_id, section_no, active_request_guard),
    CONSTRAINT ck_lecture_opening_requests_capacity CHECK (requested_capacity > 0),
    CONSTRAINT ck_lecture_opening_requests_ratios CHECK (
        midterm_ratio + final_ratio + assignment_ratio + attendance_ratio = 100
    ),
    CONSTRAINT fk_lecture_opening_requests_course
        FOREIGN KEY (course_id) REFERENCES courses (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_lecture_opening_requests_professor
        FOREIGN KEY (professor_id) REFERENCES professors (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_lecture_opening_requests_semester
        FOREIGN KEY (semester_id) REFERENCES semesters (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_lecture_opening_requests_reviewer
        FOREIGN KEY (reviewed_by) REFERENCES users (id)
        ON DELETE RESTRICT,
    INDEX idx_lecture_opening_requests_professor_status (professor_id, status),
    INDEX idx_lecture_opening_requests_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS lecture_opening_request_schedules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    request_id BIGINT NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    start_period TINYINT NOT NULL,
    end_period TINYINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_lecture_opening_request_schedules_slot
        UNIQUE (request_id, day_of_week, start_period, end_period),
    CONSTRAINT ck_lecture_opening_request_schedules_period
        CHECK (start_period > 0 AND end_period >= start_period),
    CONSTRAINT fk_lecture_opening_request_schedules_request
        FOREIGN KEY (request_id) REFERENCES lecture_opening_requests (id)
        ON DELETE CASCADE,
    INDEX idx_lecture_opening_request_schedules_request_id (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS lectures (
    id BIGINT NOT NULL AUTO_INCREMENT,
    semester_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    professor_id BIGINT NOT NULL,
    section_no VARCHAR(10) NOT NULL DEFAULT '01',
    capacity INT NOT NULL DEFAULT 40,
    classroom VARCHAR(50) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    midterm_ratio INT NOT NULL DEFAULT 30,
    final_ratio INT NOT NULL DEFAULT 30,
    assignment_ratio INT NOT NULL DEFAULT 30,
    attendance_ratio INT NOT NULL DEFAULT 10,
    syllabus TEXT NULL,
    approved_request_id BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_lectures_semester_course_section UNIQUE (semester_id, course_id, section_no),
    CONSTRAINT uk_lectures_approved_request UNIQUE (approved_request_id),
    CONSTRAINT fk_lectures_semester
        FOREIGN KEY (semester_id) REFERENCES semesters (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_lectures_course
        FOREIGN KEY (course_id) REFERENCES courses (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_lectures_professor
        FOREIGN KEY (professor_id) REFERENCES professors (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_lectures_approved_request
        FOREIGN KEY (approved_request_id) REFERENCES lecture_opening_requests (id)
        ON DELETE RESTRICT,
    INDEX idx_lectures_semester_id (semester_id),
    INDEX idx_lectures_course_id (course_id),
    INDEX idx_lectures_professor_id (professor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS lecture_schedules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lecture_id BIGINT NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    start_period TINYINT NOT NULL,
    end_period TINYINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_lecture_schedules_slot
        UNIQUE (lecture_id, day_of_week, start_period),
    CONSTRAINT ck_lecture_schedules_period CHECK (start_period > 0 AND end_period >= start_period),
    CONSTRAINT fk_lecture_schedules_lecture
        FOREIGN KEY (lecture_id) REFERENCES lectures (id)
        ON DELETE CASCADE,
    INDEX idx_lecture_schedules_lecture_id (lecture_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS syllabus_files (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lecture_id BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    uploaded_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_syllabus_files_lecture
        FOREIGN KEY (lecture_id) REFERENCES lectures (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_syllabus_files_uploader
        FOREIGN KEY (uploaded_by) REFERENCES users (id)
        ON DELETE RESTRICT,
    INDEX idx_syllabus_files_lecture_id (lecture_id),
    INDEX idx_syllabus_files_lecture_duplicate (lecture_id, original_name, size)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS enrollment_carts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    lecture_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_enrollment_carts_student_lecture UNIQUE (student_id, lecture_id),
    CONSTRAINT fk_enrollment_carts_student
        FOREIGN KEY (student_id) REFERENCES students (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_enrollment_carts_lecture
        FOREIGN KEY (lecture_id) REFERENCES lectures (id)
        ON DELETE RESTRICT,
    INDEX idx_enrollment_carts_student_id (student_id),
    INDEX idx_enrollment_carts_lecture_id (lecture_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS enrollments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    lecture_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    enrolled_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    midterm_score DECIMAL(5,2) NULL,
    final_score DECIMAL(5,2) NULL,
    assignment_score DECIMAL(5,2) NULL,
    attendance_score DECIMAL(5,2) NULL,
    total_score DECIMAL(5,2) NULL,
    letter_grade VARCHAR(5) NULL,
    grade_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    PRIMARY KEY (id),
    CONSTRAINT fk_enrollments_student
        FOREIGN KEY (student_id) REFERENCES students (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_enrollments_lecture
        FOREIGN KEY (lecture_id) REFERENCES lectures (id)
        ON DELETE RESTRICT,
    INDEX idx_enrollments_student_id (student_id),
    INDEX idx_enrollments_lecture_id (lecture_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS excuse_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    enrollment_id BIGINT NOT NULL,
    lecture_date DATE NOT NULL,
    period TINYINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reject_reason VARCHAR(500) NULL,
    attachment_original_name VARCHAR(255) NULL,
    attachment_stored_name VARCHAR(255) NULL,
    attachment_content_type VARCHAR(100) NULL,
    attachment_size BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_excuse_requests_enrollment_date_period
        UNIQUE (enrollment_id, lecture_date, period),
    CONSTRAINT ck_excuse_requests_period CHECK (period BETWEEN 1 AND 20),
    CONSTRAINT fk_excuse_requests_enrollment
        FOREIGN KEY (enrollment_id) REFERENCES enrollments (id)
        ON DELETE RESTRICT,
    INDEX idx_excuse_requests_enrollment_id (enrollment_id),
    INDEX idx_excuse_requests_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS student_grade_summaries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL,
    total_credits TINYINT UNSIGNED NOT NULL DEFAULT 0,
    gpa DECIMAL(4,2) NOT NULL DEFAULT 0.00,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_student_grade_summaries_student_semester UNIQUE (student_id, semester_id),
    CONSTRAINT fk_student_grade_summaries_student
        FOREIGN KEY (student_id) REFERENCES students (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_student_grade_summaries_semester
        FOREIGN KEY (semester_id) REFERENCES semesters (id)
        ON DELETE RESTRICT,
    INDEX idx_student_grade_summaries_student_id (student_id),
    INDEX idx_student_grade_summaries_semester_id (semester_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS grade_correction_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    enrollment_id BIGINT NOT NULL,
    field_changed VARCHAR(30) NOT NULL,
    previous_value VARCHAR(50) NULL,
    new_value VARCHAR(50) NULL,
    changed_by BIGINT NOT NULL,
    reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_grade_correction_histories_enrollment
        FOREIGN KEY (enrollment_id) REFERENCES enrollments (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_grade_correction_histories_changed_by
        FOREIGN KEY (changed_by) REFERENCES users (id)
        ON DELETE RESTRICT,
    INDEX idx_grade_correction_histories_enrollment_id (enrollment_id),
    INDEX idx_grade_correction_histories_changed_by (changed_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS graduation_requirements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    department_id BIGINT NOT NULL,
    admission_year SMALLINT NOT NULL,
    required_major_credits INT NOT NULL,
    required_general_credits INT NOT NULL,
    required_total_credits INT NOT NULL,
    required_courses JSON NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_graduation_requirements_department_year
        UNIQUE (department_id, admission_year),
    CONSTRAINT fk_graduation_requirements_department
        FOREIGN KEY (department_id) REFERENCES departments (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2026-08-17: 학생 본인 학적 정보(연락처/주소/사진) 변경 신청 + 관리자 승인 워크플로우
CREATE TABLE IF NOT EXISTS student_info_change_requests (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    student_id          BIGINT NOT NULL,
    new_name            VARCHAR(50) NULL,
    new_phone_number    VARCHAR(20) NULL,
    new_email           VARCHAR(100) NULL,
    new_address         VARCHAR(255) NULL,
    new_profile_image_key VARCHAR(500) NULL,
    reason              VARCHAR(500) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    reviewed_by         BIGINT NULL,
    reviewed_at         DATETIME NULL,
    reject_reason       VARCHAR(500) NULL,
    cancelled_at        DATETIME NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_student_info_change_requests_student_status (student_id, status),
    INDEX idx_student_info_change_requests_status_created (status, created_at),
    CONSTRAINT fk_student_info_change_requests_student
        FOREIGN KEY (student_id) REFERENCES students (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_student_info_change_requests_reviewer
        FOREIGN KEY (reviewed_by) REFERENCES users (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS student_info_change_request_files (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    request_id  BIGINT NOT NULL,
    file_name   VARCHAR(255) NOT NULL,
    object_key  VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size   BIGINT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_student_info_change_request_files_request_id (request_id),
    CONSTRAINT fk_student_info_change_request_files_request
        FOREIGN KEY (request_id) REFERENCES student_info_change_requests (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2026-08-20: 교수 본인 프로필 변경 신청 + 관리자 승인·반려 + 본인 취소 워크플로우
CREATE TABLE IF NOT EXISTS professor_info_change_requests (
    id                    BIGINT NOT NULL AUTO_INCREMENT,
    professor_id          BIGINT NOT NULL,
    new_name              VARCHAR(50) NULL,
    new_phone_number      VARCHAR(20) NULL,
    new_email             VARCHAR(100) NULL,
    new_address           VARCHAR(255) NULL,
    new_profile_image_key VARCHAR(500) NULL,
    reason                VARCHAR(500) NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    reviewed_by           BIGINT NULL,
    reviewed_at           DATETIME NULL,
    reject_reason         VARCHAR(500) NULL,
    cancelled_at          DATETIME NULL,
    created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_professor_info_change_requests_professor_status (professor_id, status),
    INDEX idx_professor_info_change_requests_status_created (status, created_at),
    CONSTRAINT fk_professor_info_change_requests_professor
        FOREIGN KEY (professor_id) REFERENCES professors (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_professor_info_change_requests_reviewer
        FOREIGN KEY (reviewed_by) REFERENCES users (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS professor_info_change_request_files (
    id           BIGINT NOT NULL AUTO_INCREMENT,
    request_id   BIGINT NOT NULL,
    file_name    VARCHAR(255) NOT NULL,
    object_key   VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size    BIGINT NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_professor_info_change_request_files_request_id (request_id),
    CONSTRAINT fk_professor_info_change_request_files_request
        FOREIGN KEY (request_id) REFERENCES professor_info_change_requests (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 2026-08-21: 관리자 입학 예정자 등록·조회·수정 및 확정 상태 관리
CREATE TABLE IF NOT EXISTS admission_candidates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    application_number VARCHAR(50) NOT NULL,
    name VARCHAR(50) NOT NULL,
    birth_date DATE NOT NULL,
    email VARCHAR(100) NULL,
    phone_number VARCHAR(20) NULL,
    address VARCHAR(255) NULL,
    department_id BIGINT NOT NULL,
    admission_year SMALLINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'REGISTERED',
    student_id BIGINT NULL,
    created_by BIGINT NOT NULL,
    status_changed_by BIGINT NULL,
    status_changed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_admission_candidates_application_number UNIQUE (application_number),
    CONSTRAINT uk_admission_candidates_student_id UNIQUE (student_id),
    CONSTRAINT fk_admission_candidates_department
        FOREIGN KEY (department_id) REFERENCES departments (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_admission_candidates_student
        FOREIGN KEY (student_id) REFERENCES students (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_admission_candidates_created_by
        FOREIGN KEY (created_by) REFERENCES users (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_admission_candidates_status_changed_by
        FOREIGN KEY (status_changed_by) REFERENCES users (id)
        ON DELETE RESTRICT,
    INDEX idx_admission_candidates_department_year_status (department_id, admission_year, status),
    INDEX idx_admission_candidates_name (name),
    INDEX idx_admission_candidates_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 수강신청과 성공 이력·멱등 응답의 원자적 저장. ERD 컬럼 그대로 사용합니다.
CREATE TABLE IF NOT EXISTS enrollment_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    lecture_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_enrollment_histories_student_id (student_id),
    INDEX idx_enrollment_histories_lecture_id (lecture_id),
    CONSTRAINT fk_enrollment_histories_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
    CONSTRAINT fk_enrollment_histories_lecture FOREIGN KEY (lecture_id) REFERENCES lectures (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS idempotency_keys (
    id BIGINT NOT NULL AUTO_INCREMENT,
    idempotency_key VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    requester_user_id BIGINT NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_snapshot JSON NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_idempotency_keys_key UNIQUE (idempotency_key),
    INDEX idx_idempotency_keys_requester (requester_user_id),
    INDEX idx_idempotency_keys_expiry (status, expires_at),
    CONSTRAINT fk_idempotency_keys_user FOREIGN KEY (requester_user_id) REFERENCES users (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 기존 사용자 academic_requests 테이블과 동일한 구조를 재사용합니다. 기존 테이블을 ALTER/DROP하지 않습니다.
CREATE TABLE IF NOT EXISTS academic_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    request_type VARCHAR(20) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    target_year SMALLINT NOT NULL,
    target_semester TINYINT NOT NULL,
    return_year SMALLINT NULL,
    return_semester TINYINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reject_reason VARCHAR(500) NULL,
    cancel_reason VARCHAR(500) NULL,
    attachment_original_name VARCHAR(255) NULL,
    attachment_stored_name VARCHAR(255) NULL,
    attachment_content_type VARCHAR(100) NULL,
    attachment_size BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active_student_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN status = 'PENDING' THEN student_id ELSE NULL END
    ) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_academic_requests_active_student UNIQUE (active_student_id),
    INDEX idx_academic_requests_student_status (student_id, status),
    INDEX idx_academic_requests_status_created (status, created_at),
    CONSTRAINT fk_academic_requests_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
    CONSTRAINT ck_academic_requests_target_semester CHECK (target_semester IN (1, 2)),
    CONSTRAINT ck_academic_requests_return_semester CHECK (return_semester IS NULL OR return_semester IN (1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 휴·복학 신청별 최대 5개 PDF 증빙 메타데이터. 기존 academic_requests.attachment_*는 호환용으로 유지합니다.
CREATE TABLE IF NOT EXISTS leave_request_files (
    id BIGINT NOT NULL AUTO_INCREMENT,
    request_id BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_leave_request_files_request (request_id),
    CONSTRAINT fk_leave_request_files_request FOREIGN KEY (request_id)
        REFERENCES academic_requests (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 학기별 수강신청 기간과 분리한 휴·복학 접수/승인 기간. semester_id는 적용 학기입니다.
CREATE TABLE IF NOT EXISTS leave_request_periods (
    id BIGINT NOT NULL AUTO_INCREMENT,
    semester_id BIGINT NOT NULL,
    request_type VARCHAR(20) NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    approval_start_at DATETIME NOT NULL,
    approval_end_at DATETIME NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_leave_request_periods_semester_type UNIQUE (semester_id, request_type),
    CONSTRAINT fk_leave_request_periods_semester FOREIGN KEY (semester_id) REFERENCES semesters (id) ON DELETE RESTRICT,
    CONSTRAINT ck_leave_request_periods_request_type CHECK (
        request_type IN ('GENERAL_LEAVE', 'GENERAL_RETURN', 'MILITARY_LEAVE', 'MILITARY_RETURN')
    ),
    CONSTRAINT ck_leave_request_periods_receipt CHECK (start_at < end_at),
    CONSTRAINT ck_leave_request_periods_approval CHECK (approval_start_at < approval_end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 전과와 향후 복수전공이 공유하는 적용 학기별 접수 기간. 유형·학기별 한 설정만 허용합니다.
CREATE TABLE IF NOT EXISTS academic_change_request_periods (
    id BIGINT NOT NULL AUTO_INCREMENT,
    semester_id BIGINT NOT NULL,
    request_type VARCHAR(20) NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_academic_change_periods_semester_type UNIQUE (semester_id, request_type),
    CONSTRAINT fk_academic_change_periods_semester FOREIGN KEY (semester_id) REFERENCES semesters (id) ON DELETE RESTRICT,
    CONSTRAINT ck_academic_change_periods_request_type CHECK (
        request_type IN ('TRANSFER_DEPARTMENT', 'DOUBLE_MAJOR')
    ),
    CONSTRAINT ck_academic_change_periods_range CHECK (start_at < end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 전과/복수전공 공용 신청 원본. 전과는 적용 학기를, 복수전공은 접수 모집 회차를 저장합니다.
CREATE TABLE IF NOT EXISTS academic_change_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    request_type VARCHAR(20) NOT NULL,
    source_department_id BIGINT NOT NULL,
    target_department_id BIGINT NOT NULL,
    target_semester_id BIGINT NULL,
    request_period_id BIGINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reject_reason VARCHAR(500) NULL,
    processed_by BIGINT NULL,
    processed_at DATETIME NULL,
    cancel_reason VARCHAR(500) NULL,
    cancelled_by BIGINT NULL,
    cancelled_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active_student_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN status = 'PENDING' THEN student_id ELSE NULL END
    ) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_academic_change_requests_active_type UNIQUE (request_type, active_student_id),
    INDEX idx_academic_change_requests_student_status (student_id, status),
    INDEX idx_academic_change_requests_status_created (status, created_at),
    INDEX idx_academic_change_requests_target_semester (target_semester_id),
    INDEX idx_academic_change_requests_period (request_period_id),
    CONSTRAINT fk_academic_change_requests_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
    CONSTRAINT fk_academic_change_requests_source_department FOREIGN KEY (source_department_id) REFERENCES departments (id) ON DELETE RESTRICT,
    CONSTRAINT fk_academic_change_requests_target_department FOREIGN KEY (target_department_id) REFERENCES departments (id) ON DELETE RESTRICT,
    CONSTRAINT fk_academic_change_requests_target_semester FOREIGN KEY (target_semester_id) REFERENCES semesters (id) ON DELETE RESTRICT,
    CONSTRAINT fk_academic_change_requests_period FOREIGN KEY (request_period_id) REFERENCES academic_change_request_periods (id) ON DELETE RESTRICT,
    CONSTRAINT fk_academic_change_requests_processor FOREIGN KEY (processed_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_academic_change_requests_canceller FOREIGN KEY (cancelled_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_academic_change_requests_type CHECK (
        request_type IN ('TRANSFER_DEPARTMENT', 'DOUBLE_MAJOR')
    ),
    CONSTRAINT ck_academic_change_requests_target_scope CHECK (
        (request_type = 'TRANSFER_DEPARTMENT' AND target_semester_id IS NOT NULL)
        OR (request_type = 'DOUBLE_MAJOR'
            AND target_semester_id IS NULL AND request_period_id IS NOT NULL)
    ),
    CONSTRAINT ck_academic_change_requests_status CHECK (
        status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')
    ),
    CONSTRAINT ck_academic_change_requests_processing CHECK (
        (status = 'PENDING' AND processed_by IS NULL AND processed_at IS NULL
            AND reject_reason IS NULL AND cancelled_by IS NULL AND cancelled_at IS NULL AND cancel_reason IS NULL)
        OR (status = 'APPROVED' AND processed_by IS NOT NULL AND processed_at IS NOT NULL
            AND reject_reason IS NULL AND cancelled_by IS NULL AND cancelled_at IS NULL AND cancel_reason IS NULL)
        OR (status = 'REJECTED' AND processed_by IS NOT NULL AND processed_at IS NOT NULL
            AND reject_reason IS NOT NULL AND CHAR_LENGTH(TRIM(reject_reason)) > 0
            AND cancelled_by IS NULL AND cancelled_at IS NULL AND cancel_reason IS NULL)
        OR (status = 'CANCELLED' AND processed_by IS NULL AND processed_at IS NULL AND reject_reason IS NULL
            AND cancelled_by IS NOT NULL AND cancelled_at IS NOT NULL
            AND cancel_reason IS NOT NULL AND CHAR_LENGTH(TRIM(cancel_reason)) > 0)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 전과 PDF 3종과 복수전공 PDF 2종 메타데이터. 실제 파일은 비공개 MinIO에 저장합니다.
CREATE TABLE IF NOT EXISTS academic_change_request_files (
    id BIGINT NOT NULL AUTO_INCREMENT,
    request_id BIGINT NOT NULL,
    document_type VARCHAR(30) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_academic_change_request_files_type UNIQUE (request_id, document_type),
    INDEX idx_academic_change_request_files_request (request_id),
    CONSTRAINT fk_academic_change_request_files_request FOREIGN KEY (request_id)
        REFERENCES academic_change_requests (id) ON DELETE RESTRICT,
    CONSTRAINT ck_academic_change_request_files_type CHECK (
        document_type IN ('SELF_INTRODUCTION', 'STUDY_PLAN', 'TRANSCRIPT')
    ),
    CONSTRAINT ck_academic_change_request_files_size CHECK (size > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 관리자 제적. 학생 자퇴와 분리하며 확정 시 학적 이력/감사/진행 중 신청 취소를 함께 저장합니다.
CREATE TABLE IF NOT EXISTS dismissal_candidates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    student_id BIGINT NOT NULL,
    reason_type VARCHAR(30) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    registered_by BIGINT NOT NULL,
    processed_by BIGINT NULL DEFAULT NULL,
    processed_at DATETIME NULL DEFAULT NULL,
    cancel_reason VARCHAR(500) NULL DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active_student_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN status = 'PENDING' THEN student_id ELSE NULL END
    ) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_dismissal_candidates_active_student UNIQUE (active_student_id),
    INDEX idx_dismissal_candidates_student_status (student_id, status),
    INDEX idx_dismissal_candidates_status_created (status, created_at),
    CONSTRAINT fk_dismissal_candidates_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
    CONSTRAINT fk_dismissal_candidates_registrant FOREIGN KEY (registered_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_dismissal_candidates_processor FOREIGN KEY (processed_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_dismissal_candidates_version CHECK (version >= 0),
    CONSTRAINT ck_dismissal_candidates_reason_type CHECK (
        reason_type IN ('LEAVE_EXPIRED', 'NON_REGISTRATION', 'DISCIPLINARY', 'ACADEMIC_WARNING', 'ACADEMIC_WARNING_REPEAT')
    ),
    CONSTRAINT ck_dismissal_candidates_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED')),
    CONSTRAINT ck_dismissal_candidates_reason CHECK (CHAR_LENGTH(TRIM(reason)) > 0),
    CONSTRAINT ck_dismissal_candidates_processing CHECK (
        (status = 'PENDING' AND processed_by IS NULL AND processed_at IS NULL AND cancel_reason IS NULL)
        OR (status = 'CONFIRMED' AND processed_by IS NOT NULL AND processed_at IS NOT NULL AND cancel_reason IS NULL)
        OR (status = 'CANCELLED' AND processed_by IS NOT NULL AND processed_at IS NOT NULL
            AND cancel_reason IS NOT NULL AND CHAR_LENGTH(TRIM(cancel_reason)) > 0)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
