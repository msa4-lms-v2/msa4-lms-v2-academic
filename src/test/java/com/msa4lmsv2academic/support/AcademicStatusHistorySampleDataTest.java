package com.msa4lmsv2academic.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.mysql.MySQLContainer;

class AcademicStatusHistorySampleDataTest {

    // 사용자 로컬 DB나 다른 통합 테스트의 데이터와 완전히 분리합니다.
    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withDatabaseName("lms_status_history_sample_test")
            .withUsername("academic")
            .withPassword("academic");
    private static final String SAMPLE = "dummy/03_academic-status-history-sample.sql";
    private static final String MARKER = "[LOCAL_STATUS_HISTORY_SAMPLE_V1]";

    private static DataSource dataSource;
    private static JdbcTemplate jdbc;

    @BeforeAll
    static void startDatabase() {
        MYSQL.start();
        dataSource = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        runScript("schema.sql");
    }

    @AfterAll
    static void stopDatabase() {
        MYSQL.stop();
    }

    @BeforeEach
    void setUp() {
        // 이 클래스 전용 임시 DB만 정리합니다.
        jdbc.update("DELETE FROM academic_status_histories");
        jdbc.update("DELETE FROM students");
        jdbc.update("DELETE FROM professors");
        jdbc.update("DELETE FROM departments");
        jdbc.update("DELETE FROM users");
        jdbc.update("""
                INSERT INTO users (id, name, role, status) VALUES
                (9001, '기본학생', 'STUDENT', 'ACTIVE'),
                (9002, '추가학생', 'STUDENT', 'ACTIVE'),
                (9003, '관리자', 'ADMIN', 'ACTIVE'),
                (9004, '교수', 'PROFESSOR', 'ACTIVE')
                """);
        jdbc.update("INSERT INTO departments (id, code, name, active) VALUES "
                + "(301, '301', '기본학과', TRUE), (302, '302', '다른학과', TRUE)");
        jdbc.update("INSERT INTO professors (id, user_id, hire_year, department_id) VALUES (41, 9004, 2020, 301)");
        jdbc.update("""
                INSERT INTO students (id, user_id, department_id, grade_level, admission_year, academic_status, advisor_id)
                VALUES (51, 9001, 301, 2, 2025, 'ENROLLED', 41), (52, 9002, 302, 2, 2025, 'ENROLLED', NULL)
                """);
    }

    @Test
    void repeatedExecutionAddsSixHistoriesWithoutChangingExistingBusinessData() {
        jdbc.update("""
                INSERT INTO academic_status_histories
                (student_id, previous_status, new_status, reason, changed_by, source_type, source_id, created_at)
                VALUES (51, 'ON_LEAVE', 'ENROLLED', '기존 승인 이력', 9003, 'LEAVE_REQUEST', 42, '2026-07-01 09:00:00')
                """);
        var users = rows("users");
        var students = rows("students");
        var professors = rows("professors");
        var departments = rows("departments");
        var originalHistory = rows("academic_status_histories").getFirst();

        runScript(SAMPLE);
        var firstRun = rows("academic_status_histories");
        runScript(SAMPLE);

        assertThat(rows("academic_status_histories")).isEqualTo(firstRun).hasSize(7).contains(originalHistory);
        assertThat(rows("users")).isEqualTo(users);
        assertThat(rows("students")).isEqualTo(students);
        assertThat(rows("professors")).isEqualTo(professors);
        assertThat(rows("departments")).isEqualTo(departments);
        assertThat(count("SELECT COUNT(*) FROM withdrawal_requests")).isZero();
        assertThat(count("SELECT COUNT(*) FROM enrollments")).isZero();
        assertThat(count("SELECT COUNT(*) FROM audit_logs")).isZero();
        assertThat(count("SELECT COUNT(*) FROM academic_status_histories WHERE LOCATE(?, reason) = 1 "
                + "AND source_id IS NULL AND changed_by = 9003", MARKER)).isEqualTo(6);
    }

    @Test
    void fixturesSupportStudentDateStatusSourceFiltersAndStablePagination() {
        runScript(SAMPLE);
        assertThat(count("SELECT COUNT(*) FROM academic_status_histories WHERE student_id = 51")).isEqualTo(4);
        assertThat(count("SELECT COUNT(*) FROM academic_status_histories WHERE student_id = 52")).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM academic_status_histories WHERE student_id = 51 "
                + "AND new_status = 'ON_LEAVE' AND created_at >= '2026-08-01' AND created_at < '2026-08-11'"))
                .isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM academic_status_histories WHERE source_type = 'ADMIN_CORRECTION'"))
                .isEqualTo(1);
        List<Long> firstPage = jdbc.queryForList("SELECT id FROM academic_status_histories WHERE student_id = 51 "
                + "ORDER BY created_at DESC, id DESC LIMIT 2", Long.class);
        List<Long> secondPage = jdbc.queryForList("SELECT id FROM academic_status_histories WHERE student_id = 51 "
                + "ORDER BY created_at DESC, id DESC LIMIT 2 OFFSET 2", Long.class);
        assertThat(firstPage).hasSize(2).doesNotContainAnyElementsOf(secondPage);
        assertThat(firstPage.getFirst()).isGreaterThan(firstPage.getLast());
        assertThat(secondPage).hasSize(2);
    }

    @Test
    void oneStudentCreatesFourHistoriesWithoutCreatingAnotherAccount() {
        jdbc.update("DELETE FROM students WHERE id = 52");
        jdbc.update("DELETE FROM users WHERE id = 9002");
        var users = rows("users");
        runScript(SAMPLE);
        runScript(SAMPLE);
        assertThat(rows("academic_status_histories")).hasSize(4);
        assertThat(rows("users")).isEqualTo(users);
        assertThat(rows("students")).hasSize(1);
    }

    @Test
    void missingActiveAdminSkipsSeedWithoutTouchingStudentStatuses() {
        jdbc.update("UPDATE users SET status = 'INACTIVE' WHERE id = 9003");
        var students = rows("students");
        runScript(SAMPLE);
        assertThat(rows("academic_status_histories")).isEmpty();
        assertThat(rows("students")).isEqualTo(students);
    }

    @Test
    void missingEnrolledStudentSkipsSeedWithoutInventingStudents() {
        jdbc.update("UPDATE students SET academic_status = 'ON_LEAVE'");
        var students = rows("students");
        runScript(SAMPLE);
        assertThat(rows("academic_status_histories")).isEmpty();
        assertThat(rows("students")).isEqualTo(students);
    }

    @Test
    void deletedStudentIsNotSelected() {
        jdbc.update("UPDATE users SET deleted_at = NOW() WHERE id = 9001");
        runScript(SAMPLE);
        assertThat(count("SELECT COUNT(*) FROM academic_status_histories WHERE student_id = 51")).isZero();
        assertThat(count("SELECT COUNT(*) FROM academic_status_histories WHERE student_id = 52")).isEqualTo(4);
    }

    @Test
    void rerunDoesNotSwitchExistingTargetsAfterTheirStatusChanges() {
        runScript(SAMPLE);
        var histories = rows("academic_status_histories");
        jdbc.update("UPDATE students SET academic_status = 'ON_LEAVE'");
        jdbc.update("INSERT INTO users (id, name, role, status) VALUES (9005, '신규학생', 'STUDENT', 'ACTIVE')");
        jdbc.update("INSERT INTO students (id, user_id, department_id, grade_level, admission_year, academic_status) "
                + "VALUES (53, 9005, 301, 1, 2026, 'ENROLLED')");
        var students = rows("students");
        runScript(SAMPLE);
        assertThat(rows("academic_status_histories")).isEqualTo(histories);
        assertThat(rows("students")).isEqualTo(students);
    }

    private static void runScript(String path) {
        var populator = new ResourceDatabasePopulator(new ClassPathResource(path));
        populator.setSqlScriptEncoding("UTF-8");
        populator.execute(dataSource);
    }

    private List<Map<String, Object>> rows(String table) {
        return jdbc.queryForList("SELECT * FROM " + table + " ORDER BY id");
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }
}
