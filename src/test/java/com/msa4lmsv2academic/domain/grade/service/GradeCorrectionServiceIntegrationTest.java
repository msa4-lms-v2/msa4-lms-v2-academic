package com.msa4lmsv2academic.domain.grade.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.domain.grade.request.GradeCorrectionHistorySearchRequestDTO;
import com.msa4lmsv2academic.domain.grade.request.GradeCorrectionItemRequestDTO;
import com.msa4lmsv2academic.domain.grade.request.GradeCorrectionRequestDTO;
import com.msa4lmsv2academic.global.error.GradeManagementAccessDeniedException;
import com.msa4lmsv2academic.global.error.GradeManagementConflictException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class GradeCorrectionServiceIntegrationTest extends MySqlIntegrationTest {

    private static final long CLASS_ID = 99701L;
    private static final long ENROLLMENT_ID = 99701L;
    private static final long PROFESSOR_USER_ID = 99701L;

    @Autowired
    private GradeCorrectionService gradeCorrectionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO colleges (id, code, name, active) "
                + "VALUES (99701, 'COR-COL', '성적정정테스트대학', 1)");
        jdbcTemplate.update("INSERT INTO departments (id, code, college_id, name, active) "
                + "VALUES (99701, 'COR', 99701, '성적정정테스트학과', 1)");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) "
                + "VALUES (99701, '성적정정교수', 'PROFESSOR', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) "
                + "VALUES (99702, '성적정정학생', 'STUDENT', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) "
                + "VALUES (99703, '다른성적교수', 'PROFESSOR', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO professors (id, version, user_id, hire_year, department_id) "
                + "VALUES (99701, 0, 99701, 2020, 99701)");
        jdbcTemplate.update("INSERT INTO professors (id, version, user_id, hire_year, department_id) "
                + "VALUES (99702, 0, 99703, 2020, 99701)");
        jdbcTemplate.update("INSERT INTO students "
                + "(id, user_id, department_id, grade_level, admission_year, academic_status, advisor_id) "
                + "VALUES (99701, 99702, 99701, 2, 2025, 'ENROLLED', 99701)");
        jdbcTemplate.update("INSERT INTO semesters "
                + "(id, academic_year, term, start_date, end_date, enrollment_start_at, enrollment_end_at, is_current) "
                + "VALUES (99701, 2026, 'SECOND', '2026-08-31', '2026-12-18', "
                + "'2026-08-01 09:00:00', '2026-08-07 18:00:00', 0)");
        jdbcTemplate.update("INSERT INTO courses "
                + "(id, department_id, code, name, credits, target_grade, completion_type) "
                + "VALUES (99701, 99701, 'CORRECT-01', '성적정정테스트', 3, 2, 'MAJOR_REQUIRED')");
        jdbcTemplate.update("INSERT INTO lectures "
                + "(id, semester_id, course_id, professor_id, section_no, capacity, classroom, status, "
                + "midterm_ratio, final_ratio, assignment_ratio, attendance_ratio, syllabus) "
                + "VALUES (99701, 99701, 99701, 99701, '01', 40, '공학관 302호', 'OPEN', "
                + "30, 30, 20, 20, '성적정정 테스트 강의계획서')");
        jdbcTemplate.update("INSERT INTO enrollments "
                        + "(id, student_id, lecture_id, status, enrolled_at, midterm_score, final_score, "
                        + "assignment_score, attendance_score, total_score, letter_grade, grade_status) "
                        + "VALUES (99701, 99701, 99701, 'ACTIVE', ?, 80, 80, 80, 80, 80, 'B', 'OPENED')",
                LocalDateTime.of(2026, 8, 5, 9, 0));
    }

    @Test
    void correctsOpenedGradeAndRecordsOnlyChangedFields() {
        CurrentUser professor = new CurrentUser(PROFESSOR_USER_ID, "PROFESSOR");
        GradeCorrectionRequestDTO request = correctionRequest("  중간고사 채점 오류 정정  ");

        var first = gradeCorrectionService.correct(
                request, "grade-correction-replay", professor, "trace-correction", "127.0.0.1"
        );
        var replay = gradeCorrectionService.correct(
                request, "grade-correction-replay", professor, "trace-replay", "127.0.0.1"
        );

        assertThat(replay).isEqualTo(first);
        assertThat(first.data().grades().getFirst().midtermScore()).isEqualByComparingTo("100.00");
        assertThat(first.data().grades().getFirst().totalScore()).isEqualByComparingTo("86.00");
        assertThat(first.data().grades().getFirst().letterGrade()).isEqualTo("B+");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM grade_correction_histories WHERE enrollment_id = ?",
                Long.class, ENROLLMENT_ID
        )).isEqualTo(3L);
        assertThat(jdbcTemplate.queryForList(
                "SELECT field_changed FROM grade_correction_histories WHERE enrollment_id = ?",
                String.class, ENROLLMENT_ID
        )).containsExactlyInAnyOrder("MIDTERM_SCORE", "TOTAL_SCORE", "LETTER_GRADE");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT reason FROM grade_correction_histories WHERE enrollment_id = ? LIMIT 1",
                String.class, ENROLLMENT_ID
        )).isEqualTo("중간고사 채점 오류 정정");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE target_type = 'ENROLLMENT_GRADE' "
                        + "AND target_id = ? AND action = 'GRADE_CORRECTED'",
                Long.class, ENROLLMENT_ID
        )).isEqualTo(1L);
    }

    @Test
    void returnsPagedCorrectionHistoriesForOwner() {
        CurrentUser professor = new CurrentUser(PROFESSOR_USER_ID, "PROFESSOR");
        gradeCorrectionService.correct(
                correctionRequest("점수 정정"), "grade-correction-history", professor,
                "trace-history", "127.0.0.1"
        );

        var page = gradeCorrectionService.getHistories(
                new GradeCorrectionHistorySearchRequestDTO(CLASS_ID, 1, 20),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id")),
                professor
        );

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).allSatisfy(history -> {
            assertThat(history.enrollmentId()).isEqualTo(ENROLLMENT_ID);
            assertThat(history.studentId()).isEqualTo(99701L);
            assertThat(history.studentName()).isEqualTo("성적정정학생");
            assertThat(history.changedBy()).isEqualTo(PROFESSOR_USER_ID);
            assertThat(history.changedByName()).isEqualTo("성적정정교수");
            assertThat(history.reason()).isEqualTo("점수 정정");
        });
    }

    @Test
    void blocksCorrectionBeforeGradeIsOpened() {
        jdbcTemplate.update("UPDATE enrollments SET grade_status = 'DRAFT' WHERE id = ?", ENROLLMENT_ID);

        assertThatThrownBy(() -> gradeCorrectionService.correct(
                correctionRequest("확정 전 정정 시도"), "grade-correction-draft",
                new CurrentUser(PROFESSOR_USER_ID, "PROFESSOR"), "trace-draft", "127.0.0.1"
        )).isInstanceOf(GradeManagementConflictException.class)
                .hasMessageContaining("공개된 성적");
    }

    @Test
    void blocksProfessorWhoDoesNotOwnLecture() {
        assertThatThrownBy(() -> gradeCorrectionService.getHistories(
                new GradeCorrectionHistorySearchRequestDTO(CLASS_ID, 1, 20),
                PageRequest.of(0, 20),
                new CurrentUser(99703L, "PROFESSOR")
        )).isInstanceOf(GradeManagementAccessDeniedException.class);
    }

    private GradeCorrectionRequestDTO correctionRequest(String reason) {
        return new GradeCorrectionRequestDTO(CLASS_ID, List.of(
                new GradeCorrectionItemRequestDTO(
                        ENROLLMENT_ID,
                        new BigDecimal("100.00"),
                        new BigDecimal("80.00"),
                        new BigDecimal("80.00"),
                        new BigDecimal("80.00"),
                        reason
                )
        ));
    }
}
