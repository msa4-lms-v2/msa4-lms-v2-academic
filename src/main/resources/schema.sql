CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NULL,
    phone_number VARCHAR(20) NULL,
    address VARCHAR(255) NULL,
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
    version BIGINT NOT NULL DEFAULT 0,
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
    PRIMARY KEY (id),
    CONSTRAINT fk_lectures_semester
        FOREIGN KEY (semester_id) REFERENCES semesters (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_lectures_course
        FOREIGN KEY (course_id) REFERENCES courses (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_lectures_professor
        FOREIGN KEY (professor_id) REFERENCES professors (id)
        ON DELETE RESTRICT,
    INDEX idx_lectures_semester_id (semester_id),
    INDEX idx_lectures_course_id (course_id),
    INDEX idx_lectures_professor_id (professor_id)
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
