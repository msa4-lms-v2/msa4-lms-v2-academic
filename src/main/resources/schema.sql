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
) ENGINE=InnoDB;

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
) ENGINE=InnoDB;

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
) ENGINE=InnoDB;

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
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS colleges (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT uk_colleges_code UNIQUE (code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS departments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(20) NOT NULL,
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

CREATE TABLE IF NOT EXISTS majors (
    id BIGINT NOT NULL AUTO_INCREMENT,
    department_id BIGINT NOT NULL,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT uk_majors_code UNIQUE (code),
    CONSTRAINT fk_majors_department
        FOREIGN KEY (department_id) REFERENCES departments (id)
        ON DELETE RESTRICT,
    INDEX idx_majors_department_id (department_id)
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
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS students (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    major_id BIGINT NULL,
    double_major_id BIGINT NULL,
    grade_level TINYINT NOT NULL,
    admission_year SMALLINT NOT NULL,
    academic_status VARCHAR(20) NOT NULL DEFAULT 'ENROLLED',
    advisor_id BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_students_user_id UNIQUE (user_id),
    CONSTRAINT ck_students_distinct_majors
        CHECK (major_id IS NULL OR double_major_id IS NULL OR major_id <> double_major_id),
    CONSTRAINT fk_students_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_students_department
        FOREIGN KEY (department_id) REFERENCES departments (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_students_major
        FOREIGN KEY (major_id) REFERENCES majors (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_students_double_major
        FOREIGN KEY (double_major_id) REFERENCES majors (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_students_advisor
        FOREIGN KEY (advisor_id) REFERENCES professors (id)
        ON DELETE RESTRICT,
    INDEX idx_students_department_id (department_id),
    INDEX idx_students_major_id (major_id),
    INDEX idx_students_double_major_id (double_major_id),
    INDEX idx_students_advisor_id (advisor_id)
) ENGINE=InnoDB;

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
    INDEX idx_withdrawal_requests_student_status (student_id, status),
    INDEX idx_withdrawal_requests_status_created (status, created_at)
) ENGINE=InnoDB;

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
) ENGINE=InnoDB;

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
) ENGINE=InnoDB;

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
) ENGINE=InnoDB;

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
) ENGINE=InnoDB;

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
) ENGINE=InnoDB;

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
) ENGINE=InnoDB;

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
) ENGINE=InnoDB;

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
) ENGINE=InnoDB;

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
) ENGINE=InnoDB;

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
) ENGINE=InnoDB;

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
    CONSTRAINT fk_graduation_requirements_department
        FOREIGN KEY (department_id) REFERENCES departments (id)
        ON DELETE RESTRICT,
    INDEX idx_graduation_requirements_department_year (department_id, admission_year)
) ENGINE=InnoDB;

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
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_student_info_change_requests_student_status (student_id, status),
    CONSTRAINT fk_student_info_change_requests_student
        FOREIGN KEY (student_id) REFERENCES students (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_student_info_change_requests_reviewer
        FOREIGN KEY (reviewed_by) REFERENCES users (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS student_info_change_request_files (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    request_id  BIGINT NOT NULL,
    file_name   VARCHAR(255) NOT NULL,
    object_key  VARCHAR(500) NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_student_info_change_request_files_request_id (request_id),
    CONSTRAINT fk_student_info_change_request_files_request
        FOREIGN KEY (request_id) REFERENCES student_info_change_requests (id)
        ON DELETE CASCADE
) ENGINE=InnoDB;
