package com.msa4lmsv2academic.domain.dashboard;

import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.semester.repository.SemesterRepository;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.mysql.MySQLContainer;
import java.time.LocalDate;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminDashboardQueryRepositoryTest {
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");
    static JdbcTemplate jdbc;
    AdminDashboardQueryRepository queries;

    @BeforeAll static void start() {
        MYSQL.start();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()));
        jdbc.execute("create table users(id bigint primary key, name varchar(100))");
        jdbc.execute("create table students(id bigint primary key, user_id bigint)");
        jdbc.execute("create table professors(id bigint primary key, user_id bigint)");
        jdbc.execute("create table lecture_opening_requests(id bigint primary key, professor_id bigint, status varchar(30), created_at datetime)");
        jdbc.execute("create table academic_requests(id bigint primary key, student_id bigint, request_type varchar(30), status varchar(30), target_year int, target_semester int, created_at datetime)");
        jdbc.execute("create table withdrawal_requests(id bigint primary key, student_id bigint, status varchar(30), effective_date date, requested_effective_date date, created_at datetime)");
        jdbc.execute("create table dismissal_candidates(id bigint primary key, student_id bigint, status varchar(30), processed_at datetime, created_at datetime)");
        jdbc.execute("create table academic_change_request_periods(id bigint primary key, semester_id bigint)");
        jdbc.execute("create table academic_change_requests(id bigint primary key, student_id bigint, request_type varchar(30), status varchar(30), target_semester_id bigint, request_period_id bigint, created_at datetime)");
        jdbc.update("insert into users values (1,'학생'),(2,'교수')");
        jdbc.update("insert into students values (1,1)");
        jdbc.update("insert into professors values (1,2)");
    }
    @AfterAll static void stop() { MYSQL.stop(); }
    @BeforeEach void reset() {
        for (String table : new String[]{"lecture_opening_requests", "academic_requests", "withdrawal_requests", "dismissal_candidates", "academic_change_requests", "academic_change_request_periods"}) jdbc.update("delete from " + table);
        queries = new AdminDashboardQueryRepository(new NamedParameterJdbcTemplate(jdbc));
    }
    private Semester current() {
        var semester = Semester.create((short)2026, SemesterTerm.SECOND, LocalDate.parse("2026-09-01"), LocalDate.parse("2026-12-31"),
                LocalDate.parse("2026-08-01").atStartOfDay(), LocalDate.parse("2026-08-30").atStartOfDay(), true);
        ReflectionTestUtils.setField(semester, "id", 20L);
        return semester;
    }
    @Test void queueIsCumulativeBoundedAndOldestFirstButCountsAreNotLimited() {
        for (int i=1; i<=35; i++) jdbc.update("insert into lecture_opening_requests values (?,1,'PENDING','2025-01-01')", i);
        jdbc.update("insert into lecture_opening_requests values (36,1,'APPROVED','2025-01-01')");
        jdbc.update("insert into academic_requests values (1,1,'GENERAL_LEAVE','PENDING',2025,1,'2024-12-01')");
        jdbc.update("insert into withdrawal_requests values (1,1,'PENDING',null,null,'2025-01-01'),(2,1,'ADVISOR_APPROVED',null,null,'2025-02-01')");
        assertThat(queries.summary().lecturePending()).isEqualTo(35);
        assertThat(queries.summary().academicChangePending()).isEqualTo(2);
        assertThat(queries.tasks()).hasSize(30);
        assertThat(queries.tasks().getFirst().type()).isEqualTo("LEAVE");
        assertThat(queries.tasks()).noneMatch(task -> task.type().equals("WITHDRAWAL") && task.id()==1);
    }
    @Test void statsUseTargetSemesterAndIncludeRejectionsButNotCancellations() {
        jdbc.update("insert into academic_requests values (1,1,'GENERAL_RETURN','APPROVED',2026,2,'2026-08-01'),(2,1,'MILITARY_RETURN','REJECTED',2026,2,'2026-08-02'),(3,1,'GENERAL_RETURN','CANCELLED',2026,2,'2026-08-03'),(4,1,'GENERAL_RETURN','PENDING',2025,2,'2026-09-01')");
        jdbc.update("insert into academic_change_request_periods values (1,20),(2,19)");
        jdbc.update("insert into academic_change_requests values (1,1,'DOUBLE_MAJOR','APPLIED',null,1,'2026-08-01'),(2,1,'DOUBLE_MAJOR','ADVISOR_REJECTED',null,1,'2026-08-01'),(3,1,'DOUBLE_MAJOR','ADVISOR_APPROVED',null,1,'2026-08-01'),(4,1,'DOUBLE_MAJOR','PENDING',null,2,'2026-09-01')");
        var stats = queries.stats(current());
        assertThat(stats).contains(new AdminDashboardResponse.AcademicStat("RETURN",2,0), new AdminDashboardResponse.AcademicStat("DOUBLE_MAJOR",2,1));
        assertThat(queries.summary().academicChangePending()).isEqualTo(2); // old return + advisor-approved double major
    }
    @Test void withdrawalAndDismissalUseEffectiveDatesAndMissingTypesAreZeroFilled() {
        jdbc.update("insert into withdrawal_requests values (1,1,'APPROVED','2026-09-02','2026-08-01','2026-08-01'),(2,1,'REJECTED',null,'2026-09-03','2026-08-01'),(3,1,'PENDING',null,null,'2026-09-04')");
        jdbc.update("insert into dismissal_candidates values (1,1,'CONFIRMED','2026-09-02','2026-08-01'),(2,1,'CANCELLED','2026-09-02','2026-08-01')");
        var semesters = mock(SemesterRepository.class);
        when(semesters.findFirstByCurrentTrue()).thenReturn(Optional.of(current()));
        var response = new AdminDashboardService(semesters, queries).getDashboard();
        assertThat(response.academicStats()).hasSize(6).contains(
                new AdminDashboardResponse.AcademicStat("WITHDRAWAL",2,1),
                new AdminDashboardResponse.AcademicStat("DISMISSAL",1,0),
                new AdminDashboardResponse.AcademicStat("LEAVE",0,0));
        assertThat(response.currentSemester().label()).isEqualTo("2학기");
    }
    @Test void noCurrentSemesterStillReturnsPendingWork() {
        jdbc.update("insert into lecture_opening_requests values (1,1,'PENDING','2025-01-01')");
        var semesters = mock(SemesterRepository.class);
        when(semesters.findFirstByCurrentTrue()).thenReturn(Optional.empty());
        var response = new AdminDashboardService(semesters, queries).getDashboard();
        assertThat(response.currentSemester()).isNull();
        assertThat(response.academicStats()).isNull();
        assertThat(response.summary().lecturePending()).isEqualTo(1);
    }
}
