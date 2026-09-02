-- 선수과목 기준은 물리 삭제하지 않고 활성 상태와 감사 로그로 변경 이력을 추적한다.
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
) ENGINE=InnoDB;
