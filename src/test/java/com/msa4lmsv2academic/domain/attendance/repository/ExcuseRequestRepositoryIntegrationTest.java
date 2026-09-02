package com.msa4lmsv2academic.domain.attendance.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.msa4lmsv2academic.domain.attendance.entity.ExcuseRequest;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ExcuseRequestRepositoryIntegrationTest extends MySqlIntegrationTest {

    private static final long REQUEST_ID = 99101L;
    private static final long STUDENT_USER_ID = 99103L;
    private static final long PROFESSOR_USER_ID = 99102L;

    @Autowired
    private ExcuseRequestRepository excuseRequestRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO colleges (id, code, name, active) "
                + "VALUES (99101, 'EXCUSE-COL', '공결테스트대학', 1)");
        jdbcTemplate.update("INSERT INTO departments (id, code, college_id, name, active) "
                + "VALUES (99101, 'EXC', 99101, '공결테스트학과', 1)");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) "
                + "VALUES (99102, '공결담당교수', 'PROFESSOR', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) "
                + "VALUES (99103, '공결신청학생', 'STUDENT', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO professors (id, version, user_id, hire_year, department_id) "
                + "VALUES (99101, 0, 99102, 2020, 99101)");
        jdbcTemplate.update("INSERT INTO students "
                + "(id, user_id, department_id, grade_level, admission_year, academic_status, advisor_id) "
                + "VALUES (99101, 99103, 99101, 2, 2025, 'ENROLLED', 99101)");
        jdbcTemplate.update("INSERT INTO semesters "
                + "(id, academic_year, term, start_date, end_date, enrollment_start_at, enrollment_end_at, is_current) "
                + "VALUES (99101, 2026, 'SECOND', '2026-08-31', '2026-12-18', "
                + "'2026-08-01 09:00:00', '2026-08-07 18:00:00', 1)");
        jdbcTemplate.update("INSERT INTO courses "
                + "(id, department_id, code, name, credits, target_grade, completion_type) "
                + "VALUES (99101, 99101, 'EXCUSE-01', '공결테스트강의', 3, 2, 'MAJOR_REQUIRED')");
        jdbcTemplate.update("INSERT INTO lectures "
                + "(id, semester_id, course_id, professor_id, section_no, capacity, classroom, status, "
                + "midterm_ratio, final_ratio, assignment_ratio, attendance_ratio, syllabus) "
                + "VALUES (99101, 99101, 99101, 99101, '01', 40, '공학관 301호', 'OPEN', "
                + "30, 30, 30, 10, '공결테스트 강의계획서')");
        jdbcTemplate.update("INSERT INTO enrollments "
                        + "(id, student_id, lecture_id, status, enrolled_at, grade_status) "
                        + "VALUES (99101, 99101, 99101, 'ACTIVE', ?, 'DRAFT')",
                LocalDateTime.of(2026, 8, 5, 9, 0));
        jdbcTemplate.update("INSERT INTO excuse_requests "
                + "(id, enrollment_id, lecture_date, period, reason, status, attachment_original_name, "
                + "attachment_stored_name, attachment_content_type, attachment_size) "
                + "VALUES (99101, 99101, '2026-09-01', 2, '병원 진료', 'PENDING', "
                + "'진료확인서.pdf', 'excuse-requests/99101/file.pdf', 'application/pdf', 2048)");
    }

    @Test
    void loadsOwnerProfessorAndAttachmentForAuthorization() {
        ExcuseRequest result = excuseRequestRepository.findDetailById(REQUEST_ID).orElseThrow();

        assertThat(result.getEnrollment().getStudent().getUser().getId()).isEqualTo(STUDENT_USER_ID);
        assertThat(result.getEnrollment().getLecture().getProfessor().getUser().getId())
                .isEqualTo(PROFESSOR_USER_ID);
        assertThat(result.getAttachmentOriginalName()).isEqualTo("진료확인서.pdf");
        assertThat(result.getAttachmentStoredName()).isEqualTo("excuse-requests/99101/file.pdf");
    }

    @Test
    void locksAndLoadsRequestBeforeAttachmentReplacement() {
        ExcuseRequest result = excuseRequestRepository.findDetailForUpdate(REQUEST_ID).orElseThrow();

        assertThat(result.getId()).isEqualTo(REQUEST_ID);
        assertThat(result.hasAttachment()).isTrue();
    }
}
