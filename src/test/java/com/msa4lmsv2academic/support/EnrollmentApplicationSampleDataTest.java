package com.msa4lmsv2academic.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.domain.enrollment.request.StudentEnrollmentCreateRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.response.EnrollmentApplicationReasonResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.response.StudentEnrollmentCreateResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.service.StudentEnrollmentApplicationService;
import com.msa4lmsv2academic.global.error.EnrollmentApplicationRejectedException;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest(properties = "academic.enrollment.idempotency-cleanup.cron=-")
class EnrollmentApplicationSampleDataTest {
    // 더미 01이 프로필/현재 학기를 갱신하므로 다른 통합 테스트의 DB와 분리합니다.
    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withDatabaseName("lms_enrollment_sample_test")
            .withUsername("academic")
            .withPassword("academic");
    private static final String SAMPLE = "dummy/02_enrollment-application-sample.sql";

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.data-locations", () -> "classpath*:dummy/*.sql");
        registry.add("springdoc.api-docs.enabled", () -> "false");
        registry.add("springdoc.swagger-ui.enabled", () -> "false");
    }

    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private StudentEnrollmentApplicationService service;

    @BeforeAll
    static void verifiesLocalWildcardInitialization(@Autowired JdbcTemplate jdbc) {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lectures "
                + "WHERE syllabus = 'LOCAL_ENROLLMENT_APPLICATION_SAMPLE'", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM semesters WHERE is_current = TRUE", Integer.class))
                .isEqualTo(1);
    }

    @BeforeEach
    void setUp() {
        // 이 클래스 전용 Testcontainers DB의 예약 테스트 데이터만 정리합니다.
        jdbc.update("DELETE FROM idempotency_keys WHERE endpoint = 'POST /api/academic/enrollments'");
        jdbc.update("DELETE h FROM enrollment_histories h JOIN lectures l ON l.id = h.lecture_id "
                + "JOIN semesters s ON s.id = l.semester_id WHERE s.academic_year = 2099 AND s.term = 'FIRST'");
        jdbc.update("DELETE e FROM enrollments e JOIN lectures l ON l.id = e.lecture_id "
                + "JOIN semesters s ON s.id = l.semester_id WHERE s.academic_year = 2099 AND s.term = 'FIRST'");
        jdbc.update("DELETE l FROM lectures l JOIN semesters s ON s.id = l.semester_id "
                + "WHERE s.academic_year = 2099 AND s.term = 'FIRST'");
        jdbc.update("DELETE r FROM enrollment_credit_limit_rules r JOIN semesters s ON s.id = r.semester_id "
                + "WHERE s.academic_year = 2099 AND s.term = 'FIRST'");
        jdbc.update("DELETE FROM semesters WHERE academic_year = 2099 AND term = 'FIRST'");
        jdbc.update("DELETE p FROM course_prerequisites p JOIN courses c "
                + "ON c.id = p.course_id OR c.id = p.prerequisite_course_id WHERE c.code LIKE 'TEST-ENR-%'");
        jdbc.update("DELETE FROM courses WHERE code LIKE 'TEST-ENR-%'");
        runScript("dummy/01_msa4-lms-v2-academic-sample.sql");
    }

    @Test
    void repeatedExecutionCreatesOnlyIsolatedFixturesAndPreservesExistingData() {
        var users = jdbc.queryForList("SELECT * FROM users ORDER BY id");
        var students = jdbc.queryForList("SELECT * FROM students ORDER BY id");
        var professors = jdbc.queryForList("SELECT * FROM professors ORDER BY id");
        var enrollments = jdbc.queryForList("SELECT * FROM enrollments ORDER BY id");
        var semesters = jdbc.queryForList("SELECT * FROM semesters ORDER BY id");
        var rules = jdbc.queryForList("SELECT * FROM enrollment_credit_limit_rules ORDER BY id");
        runScript(SAMPLE);
        runScript(SAMPLE);
        assertThat(jdbc.queryForList("SELECT * FROM users ORDER BY id")).isEqualTo(users);
        assertThat(jdbc.queryForList("SELECT * FROM students ORDER BY id")).isEqualTo(students);
        assertThat(jdbc.queryForList("SELECT * FROM professors ORDER BY id")).isEqualTo(professors);
        assertThat(jdbc.queryForList("SELECT * FROM enrollments ORDER BY id")).isEqualTo(enrollments);
        assertThat(jdbc.queryForList("SELECT * FROM semesters WHERE academic_year <> 2099 ORDER BY id"))
                .isEqualTo(semesters);
        assertThat(jdbc.queryForList("SELECT * FROM enrollment_credit_limit_rules WHERE semester_id <> ? ORDER BY id",
                semesterId())).isEqualTo(rules);
        assertThat(count("SELECT COUNT(*) FROM courses WHERE code LIKE 'TEST-ENR-%'")).isEqualTo(5);
        assertThat(count("SELECT COUNT(*) FROM lectures WHERE syllabus = 'LOCAL_ENROLLMENT_APPLICATION_SAMPLE' "
                + "AND status = 'OPEN' AND capacity = 40")).isEqualTo(4);
        assertThat(count("SELECT COUNT(*) FROM semesters WHERE academic_year = 2099 AND term = 'FIRST' "
                + "AND is_current = FALSE AND enrollment_start_at < NOW() AND enrollment_end_at > NOW()"))
                .isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM enrollment_credit_limit_rules WHERE semester_id = ? "
                + "AND max_credits = 6 AND is_active = TRUE", semesterId())).isEqualTo(1);
        assertThat(count("SELECT COUNT(DISTINCT ls.day_of_week) FROM lecture_schedules ls "
                + "JOIN lectures l ON l.id = ls.lecture_id WHERE l.semester_id = ?", semesterId())).isEqualTo(4);
        assertThat(count("SELECT COUNT(*) FROM course_prerequisites p JOIN courses c ON c.id = p.course_id "
                + "JOIN courses pre ON pre.id = p.prerequisite_course_id "
                + "WHERE c.code = 'TEST-ENR-D' AND pre.code = 'TEST-ENR-PRE' AND p.is_active = TRUE")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM lectures l JOIN courses c ON c.id = l.course_id "
                + "WHERE c.code = 'TEST-ENR-PRE'")).isZero();
        assertThat(count("SELECT COUNT(*) FROM enrollment_histories")).isZero();
        assertThat(count("SELECT COUNT(*) FROM idempotency_keys")).isZero();
    }

    @Test
    void fixtureSupportsPrerequisiteRejectionSuccessReplayDuplicateAndCreditLimit() {
        runScript(SAMPLE);
        assertRejected("TEST-ENR-D", "sample-d", "PREREQUISITE_NOT_COMPLETED");
        var first = apply("TEST-ENR-A", "sample-a");
        assertThat(first.code()).isEqualTo("00");
        assertThat(first.data().status().name()).isEqualTo("ACTIVE");
        assertThat(apply("TEST-ENR-A", "sample-a")).isEqualTo(first);
        assertRejected("TEST-ENR-A", "sample-duplicate", "DUPLICATE_ENROLLMENT");
        assertThat(apply("TEST-ENR-B", "sample-b").code()).isEqualTo("00");
        assertRejected("TEST-ENR-C", "sample-c", "CREDIT_LIMIT_EXCEEDED");
        var enrollments = jdbc.queryForList("SELECT * FROM enrollments ORDER BY id");
        var histories = jdbc.queryForList("SELECT * FROM enrollment_histories ORDER BY id");
        var keys = jdbc.queryForList("SELECT * FROM idempotency_keys ORDER BY id");
        runScript(SAMPLE);
        assertThat(jdbc.queryForList("SELECT * FROM enrollments ORDER BY id")).isEqualTo(enrollments);
        assertThat(jdbc.queryForList("SELECT * FROM enrollment_histories ORDER BY id")).isEqualTo(histories);
        assertThat(jdbc.queryForList("SELECT * FROM idempotency_keys ORDER BY id")).isEqualTo(keys);
        assertThat(histories).hasSize(2);
        assertThat(keys).hasSize(2);
        assertThat(count("SELECT COUNT(*) FROM enrollments e JOIN lectures l ON l.id = e.lecture_id "
                + "WHERE l.semester_id = ?", semesterId())).isEqualTo(2);
    }

    @Test
    void rerunRefreshesOnlyFixturePeriodWithoutResettingManualChanges() {
        runScript(SAMPLE);
        jdbc.update("UPDATE semesters SET enrollment_start_at = '2000-01-01', enrollment_end_at = '2000-01-02' WHERE id = ?",
                semesterId());
        jdbc.update("UPDATE enrollment_credit_limit_rules SET max_credits = 9, is_active = FALSE WHERE semester_id = ?",
                semesterId());
        jdbc.update("UPDATE lectures SET capacity = 1, status = 'CLOSED' WHERE id = ?", lectureId("TEST-ENR-A"));
        jdbc.update("UPDATE students SET academic_status = 'ON_LEAVE' WHERE user_id = 1");
        var students = jdbc.queryForList("SELECT * FROM students ORDER BY id");
        var rules = jdbc.queryForList("SELECT * FROM enrollment_credit_limit_rules ORDER BY id");
        var lectures = jdbc.queryForList("SELECT * FROM lectures ORDER BY id");
        runScript(SAMPLE);
        assertThat(jdbc.queryForList("SELECT * FROM students ORDER BY id")).isEqualTo(students);
        assertThat(jdbc.queryForList("SELECT * FROM enrollment_credit_limit_rules ORDER BY id")).isEqualTo(rules);
        assertThat(jdbc.queryForList("SELECT * FROM lectures ORDER BY id")).isEqualTo(lectures);
        assertThat(jdbc.queryForObject("SELECT enrollment_end_at FROM semesters WHERE id = ?",
                LocalDateTime.class, semesterId())).isAfter(LocalDateTime.now().plusDays(6));
    }

    @Test
    void skipsExistingUnownedSemesterWithoutModifyingIt() {
        jdbc.update("INSERT INTO semesters (academic_year, term, start_date, end_date, "
                + "enrollment_start_at, enrollment_end_at, is_current) VALUES "
                + "(2099, 'FIRST', '2099-04-01', '2099-07-01', '2099-02-01', '2099-02-02', FALSE)");
        var semesters = jdbc.queryForList("SELECT * FROM semesters ORDER BY id");
        runScript(SAMPLE);
        assertThat(jdbc.queryForList("SELECT * FROM semesters ORDER BY id")).isEqualTo(semesters);
        assertThat(count("SELECT COUNT(*) FROM courses WHERE code LIKE 'TEST-ENR-%'")).isZero();
        assertThat(count("SELECT COUNT(*) FROM lectures WHERE semester_id = ?", semesterId())).isZero();
    }

    @Test
    void skipsExistingReservedCourseCodeWithoutReusingIt() {
        jdbc.update("INSERT INTO courses (department_id, code, name, credits, completion_type) "
                + "SELECT id, 'TEST-ENR-A', '기존 교과목', 1, 'GENERAL_REQUIRED' FROM departments ORDER BY id LIMIT 1");
        var courses = jdbc.queryForList("SELECT * FROM courses ORDER BY id");
        runScript(SAMPLE);
        assertThat(jdbc.queryForList("SELECT * FROM courses ORDER BY id")).isEqualTo(courses);
        assertThat(count("SELECT COUNT(*) FROM semesters WHERE academic_year = 2099")).isZero();
    }

    @Test
    void missingActiveProfessorDoesNotCreatePartialFixtureOrAccounts() {
        jdbc.update("UPDATE users SET status = 'INACTIVE' WHERE role = 'PROFESSOR'");
        var users = jdbc.queryForList("SELECT * FROM users ORDER BY id");
        runScript(SAMPLE);
        assertThat(jdbc.queryForList("SELECT * FROM users ORDER BY id")).isEqualTo(users);
        assertThat(count("SELECT COUNT(*) FROM courses WHERE code LIKE 'TEST-ENR-%'")).isZero();
        assertThat(count("SELECT COUNT(*) FROM semesters WHERE academic_year = 2099")).isZero();
    }

    private void runScript(String path) {
        var populator = new ResourceDatabasePopulator(new ClassPathResource(path));
        populator.setSqlScriptEncoding("UTF-8");
        populator.execute(dataSource);
    }

    private long semesterId() {
        return jdbc.queryForObject("SELECT id FROM semesters WHERE academic_year = 2099 AND term = 'FIRST'", Long.class);
    }

    private long lectureId(String code) {
        return jdbc.queryForObject("SELECT l.id FROM lectures l JOIN courses c ON c.id = l.course_id "
                + "WHERE l.semester_id = ? AND l.section_no = 'LOCALTEST' AND c.code = ?", Long.class, semesterId(), code);
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    private GlobalResponseDTO<StudentEnrollmentCreateResponseDTO> apply(String course, String key) {
        return service.create(new StudentEnrollmentCreateRequestDTO(lectureId(course)), key, new CurrentUser(1L, "STUDENT"));
    }

    private void assertRejected(String course, String key, String reason) {
        assertThatThrownBy(() -> apply(course, key)).isInstanceOfSatisfying(EnrollmentApplicationRejectedException.class,
                exception -> assertThat(exception.getReasons()).extracting(EnrollmentApplicationReasonResponseDTO::code)
                        .contains(reason));
    }
}
