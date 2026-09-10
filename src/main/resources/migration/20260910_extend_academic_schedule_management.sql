-- 관리자 학사일정의 분류·학기 자동 판정과 실제 수강정정 기간 제어를 추가합니다.
ALTER TABLE academic_schedules
    ADD COLUMN category VARCHAR(30) NULL AFTER content,
    ADD COLUMN academic_year SMALLINT NULL AFTER end_date,
    ADD COLUMN term VARCHAR(20) NULL AFTER academic_year,
    MODIFY COLUMN start_date DATE NOT NULL,
    MODIFY COLUMN end_date DATE NULL;

UPDATE academic_schedules
SET category = COALESCE(category, 'OTHER'),
    academic_year = COALESCE(academic_year, YEAR(start_date)),
    term = COALESCE(term, CASE WHEN MONTH(start_date) <= 7 THEN 'FIRST' ELSE 'SECOND' END);

ALTER TABLE academic_schedules
    MODIFY COLUMN category VARCHAR(30) NOT NULL,
    MODIFY COLUMN academic_year SMALLINT NOT NULL,
    MODIFY COLUMN term VARCHAR(20) NOT NULL;

CREATE INDEX idx_academic_schedules_category_term
    ON academic_schedules (category, academic_year, term);

CREATE TABLE IF NOT EXISTS course_correction_periods (
    id BIGINT NOT NULL AUTO_INCREMENT,
    semester_id BIGINT NOT NULL,
    start_at DATE NOT NULL,
    end_at DATE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_course_correction_periods_semester UNIQUE (semester_id),
    CONSTRAINT fk_course_correction_periods_semester
        FOREIGN KEY (semester_id) REFERENCES semesters (id) ON DELETE RESTRICT,
    CONSTRAINT ck_course_correction_periods_range CHECK (start_at <= end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
