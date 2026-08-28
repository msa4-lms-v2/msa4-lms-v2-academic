package com.msa4lmsv2academic.domain.graduation.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.msa4lmsv2academic.domain.student.repository.ProfessorStudentScope;
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
class GraduationCreditQueryRepositoryIntegrationTest extends MySqlIntegrationTest {

    private static final long STUDENT_USER_ID = 91001L;
    private static final long PROFESSOR_USER_ID = 91002L;
    private static final long STUDENT_ID = 91001L;
    private static final long PROFESSOR_ID = 91001L;

    @Autowired
    private GraduationCreditQueryRepository graduationCreditQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO colleges (id, code, name, active) VALUES (91001, 'GRAD-COL', '졸업진단대학', 1)");
        jdbcTemplate.update("INSERT INTO departments (id, code, college_id, name, active) "
                + "VALUES (91001, '221', 91001, '졸업진단학과', 1)");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) VALUES (?, '진단학생', 'STUDENT', 'ACTIVE')",
                STUDENT_USER_ID);
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) VALUES (?, '지도교수', 'PROFESSOR', 'ACTIVE')",
                PROFESSOR_USER_ID);
        jdbcTemplate.update("INSERT INTO professors (id, version, user_id, hire_year, department_id) "
                + "VALUES (?, 0, ?, 2020, 91001)", PROFESSOR_ID, PROFESSOR_USER_ID);
        jdbcTemplate.update("INSERT INTO students "
                        + "(id, user_id, department_id, grade_level, admission_year, academic_status, advisor_id) "
                        + "VALUES (?, ?, 91001, 4, 2023, 'ENROLLED', ?)",
                STUDENT_ID, STUDENT_USER_ID, PROFESSOR_ID);
        jdbcTemplate.update("INSERT INTO semesters "
                + "(id, academic_year, term, start_date, end_date, enrollment_start_at, enrollment_end_at, is_current) "
                + "VALUES (91001, 2026, 'FIRST', '2026-03-02', '2026-06-19', "
                + "'2026-02-01 09:00:00', '2026-02-07 18:00:00', 0)");
        jdbcTemplate.update("INSERT INTO graduation_requirements "
                + "(department_id, admission_year, required_major_credits, required_general_credits, "
                + "required_total_credits, required_courses) VALUES (91001, 2023, 60, 30, 130, NULL)");

        insertCourse(91001L, "GRAD-MR", "전공필수", 3, "MAJOR_REQUIRED");
        insertCourse(91002L, "GRAD-ME", "전공선택", 3, "MAJOR_ELECTIVE");
        insertCourse(91003L, "GRAD-GR", "교양필수", 2, "GENERAL_REQUIRED");
        insertCourse(91004L, "GRAD-GE-CANCEL", "취소교양선택", 2, "GENERAL_ELECTIVE");
        insertCourse(91005L, "GRAD-GE-DRAFT", "미공개교양선택", 1, "GENERAL_ELECTIVE");
        insertCourse(91006L, "GRAD-INVALID", "비정상성적교과", 3, "MAJOR_ELECTIVE");

        insertLecture(91001L, 91001L, "01");
        insertLecture(91002L, 91001L, "02");
        insertLecture(91003L, 91002L, "01");
        insertLecture(91004L, 91003L, "01");
        insertLecture(91005L, 91004L, "01");
        insertLecture(91006L, 91005L, "01");
        insertLecture(91007L, 91006L, "01");

        insertEnrollment(91001L, 91001L, "ACTIVE", "OPENED", "A+");
        insertEnrollment(91002L, 91002L, "ACTIVE", "OPENED", "B+");
        insertEnrollment(91003L, 91003L, "ACTIVE", "OPENED", "F");
        insertEnrollment(91004L, 91004L, "ACTIVE", "OPENED", "A");
        insertEnrollment(91005L, 91005L, "CANCELLED", "OPENED", "A");
        insertEnrollment(91006L, 91006L, "ACTIVE", "DRAFT", "A");
        insertEnrollment(91007L, 91007L, "ACTIVE", "OPENED", "P");
    }

    @Test
    void diagnosesCreditsFromActiveOpenedPassingCoursesWithoutCountingRetakesTwice() {
        GraduationCreditDiagnosisQueryResult result = graduationCreditQueryRepository
                .findCreditDiagnosisByStudentId(STUDENT_ID)
                .orElseThrow();

        assertThat(result.requiredMajorCredits()).isEqualTo(60);
        assertThat(result.requiredGeneralCredits()).isEqualTo(30);
        assertThat(result.requiredTotalCredits()).isEqualTo(130);
        assertThat(result.earnedMajorCredits()).isEqualTo(3);
        assertThat(result.earnedGeneralCredits()).isEqualTo(2);
        assertThat(result.earnedRequiredCredits()).isEqualTo(5);
        assertThat(result.earnedElectiveCredits()).isZero();
        assertThat(result.earnedTotalCredits()).isEqualTo(5);
        assertThat(graduationCreditQueryRepository.sumTotalCreditsByStudentId(STUDENT_ID)).isEqualTo(5);
    }

    @Test
    void latestOpenedRetakeGradeReplacesOlderPassingGrade() {
        jdbcTemplate.update("INSERT INTO semesters "
                + "(id, academic_year, term, start_date, end_date, enrollment_start_at, enrollment_end_at, is_current) "
                + "VALUES (91002, 2026, 'SECOND', '2026-09-01', '2026-12-18', "
                + "'2026-08-01 09:00:00', '2026-08-07 18:00:00', 1)");
        jdbcTemplate.update("INSERT INTO lectures "
                        + "(id, semester_id, course_id, professor_id, section_no, capacity, classroom, status, "
                        + "midterm_ratio, final_ratio, assignment_ratio, attendance_ratio, syllabus) "
                        + "VALUES (91008, 91002, 91001, ?, '01', 40, 'A101', 'CLOSED', 30, 30, 30, 10, NULL)",
                PROFESSOR_ID);
        insertEnrollment(91008L, 91008L, "ACTIVE", "OPENED", "F");

        GraduationCreditDiagnosisQueryResult result = graduationCreditQueryRepository
                .findCreditDiagnosisByStudentId(STUDENT_ID)
                .orElseThrow();

        assertThat(result.earnedMajorCredits()).isZero();
        assertThat(result.earnedGeneralCredits()).isEqualTo(2);
        assertThat(result.earnedTotalCredits()).isEqualTo(2);
    }

    @Test
    void checksStudentOwnershipAndAdvisorRelationshipFromPersistedData() {
        assertThat(graduationCreditQueryRepository.isStudentOwnedByUser(STUDENT_ID, STUDENT_USER_ID)).isTrue();
        assertThat(graduationCreditQueryRepository.isStudentOwnedByUser(STUDENT_ID, 99999L)).isFalse();
        assertThat(graduationCreditQueryRepository.isStudentInProfessorScope(
                STUDENT_ID,
                new ProfessorStudentScope(PROFESSOR_ID, 91001L)
        )).isTrue();
        assertThat(graduationCreditQueryRepository.isStudentInProfessorScope(
                STUDENT_ID,
                new ProfessorStudentScope(99999L, 99999L)
        )).isFalse();
    }

    @Test
    void loadsAllCourseRecordsIncludingExcludedGradeCandidates() {
        var records = graduationCreditQueryRepository.findCreditRecordsByStudentId(STUDENT_ID);

        assertThat(records).hasSize(7);
        assertThat(records).anySatisfy(record -> {
            assertThat(record.enrollmentId()).isEqualTo(91007L);
            assertThat(record.courseCode()).isEqualTo("GRAD-INVALID");
            assertThat(record.letterGrade()).isEqualTo("P");
        });
        assertThat(records).anySatisfy(record -> {
            assertThat(record.enrollmentId()).isEqualTo(91005L);
            assertThat(record.enrollmentStatus().name()).isEqualTo("CANCELLED");
        });
    }

    private void insertCourse(long id, String code, String name, int credits, String completionType) {
        jdbcTemplate.update("INSERT INTO courses "
                        + "(id, department_id, code, name, credits, target_grade, completion_type) "
                        + "VALUES (?, 91001, ?, ?, ?, NULL, ?)",
                id, code, name, credits, completionType);
    }

    private void insertLecture(long id, long courseId, String sectionNo) {
        jdbcTemplate.update("INSERT INTO lectures "
                        + "(id, semester_id, course_id, professor_id, section_no, capacity, classroom, status, "
                        + "midterm_ratio, final_ratio, assignment_ratio, attendance_ratio, syllabus) "
                        + "VALUES (?, 91001, ?, ?, ?, 40, 'A101', 'CLOSED', 30, 30, 30, 10, NULL)",
                id, courseId, PROFESSOR_ID, sectionNo);
    }

    private void insertEnrollment(long id, long lectureId, String status, String gradeStatus, String letterGrade) {
        jdbcTemplate.update("INSERT INTO enrollments "
                        + "(id, student_id, lecture_id, status, enrolled_at, letter_grade, grade_status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                id, STUDENT_ID, lectureId, status, LocalDateTime.of(2026, 2, 2, 9, 0), letterGrade, gradeStatus);
    }
}
