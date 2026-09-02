package com.msa4lmsv2academic.domain.academicstatus.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import com.msa4lmsv2academic.domain.course.entity.Course;
import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.lecture.entity.LectureStatus;
import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.msa4lmsv2academic.domain.user.entity.UserStatus;
import com.msa4lmsv2academic.domain.withdrawal.entity.AcademicStatusHistory;
import com.msa4lmsv2academic.global.security.CurrentUser;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AcademicStatusHistoryControllerTest extends MySqlIntegrationTest {

    private static final String PATH = "/api/academic/status-histories";
    private static final long PROFESSOR_USER_ID = 16801L;
    private static final long ADMIN_USER_ID = 16802L;
    private static final long STUDENT_USER_ID = 16811L;

    @Autowired private MockMvc mvc;
    @Autowired private EntityManager entityManager;

    private Department department;
    private Department otherDepartment;
    private Student sameDepartment;
    private Student advisee;
    private Student lectureStudent;
    private Student unrelated;
    private Student cancelled;
    private Student pastStudent;
    private Student otherProfessorStudent;
    private long firstHistoryId;

    @BeforeEach
    void setUp() {
        department = Department.create("H01", null, "이력컴퓨터학과", true);
        otherDepartment = Department.create("H02", null, "이력경영학과", true);
        entityManager.persist(department);
        entityManager.persist(otherDepartment);
        user(ADMIN_USER_ID, "이력관리자", UserRole.ADMIN);
        Professor professor = Professor.create(user(PROFESSOR_USER_ID, "이력교수", UserRole.PROFESSOR),
                (short) 2020, department);
        Professor otherProfessor = Professor.create(user(16803L, "다른교수", UserRole.PROFESSOR),
                (short) 2020, otherDepartment);
        entityManager.persist(professor);
        entityManager.persist(otherProfessor);
        sameDepartment = student(STUDENT_USER_ID, "가학과학생", department, null, AcademicStatus.WITHDRAWN);
        advisee = student(16812L, "나지도학생", otherDepartment, professor, AcademicStatus.GRADUATED);
        lectureStudent = student(16813L, "다수강학생", otherDepartment, null, AcademicStatus.DISMISSED);
        unrelated = student(16814L, "라무관학생", otherDepartment, null, AcademicStatus.ENROLLED);
        cancelled = student(16815L, "마취소학생", otherDepartment, null, AcademicStatus.ON_LEAVE);
        pastStudent = student(16816L, "바과거학생", otherDepartment, null, AcademicStatus.ENROLLED);
        otherProfessorStudent = student(16817L, "사다른교수학생", otherDepartment, null, AcademicStatus.ENROLLED);
        Semester currentSemester = semester((short) 2088, true);
        Semester pastSemester = semester((short) 2087, false);
        Course course = Course.create(department, "HISTORY-COURSE", "이력검증강의", (byte) 3,
                null, CompletionType.MAJOR_REQUIRED);
        entityManager.persist(course);
        Lecture lecture = lecture(currentSemester, course, professor, "01");
        Lecture secondLecture = lecture(currentSemester, course, professor, "02");
        Lecture pastLecture = lecture(pastSemester, course, professor, "01");
        Lecture otherLecture = lecture(currentSemester, course, otherProfessor, "03");
        entityManager.persist(Enrollment.create(lectureStudent, lecture, LocalDateTime.now()));
        entityManager.persist(Enrollment.create(lectureStudent, secondLecture, LocalDateTime.now()));
        entityManager.persist(Enrollment.create(advisee, lecture, LocalDateTime.now()));
        Enrollment cancelledEnrollment = Enrollment.create(cancelled, lecture, LocalDateTime.now());
        cancelledEnrollment.cancel();
        entityManager.persist(cancelledEnrollment);
        entityManager.persist(Enrollment.create(pastStudent, pastLecture, LocalDateTime.now()));
        entityManager.persist(Enrollment.create(otherProfessorStudent, otherLecture, LocalDateTime.now()));
        entityManager.flush();
        firstHistoryId = history(sameDepartment, "ENROLLED", "ON_LEAVE", "LEAVE_REQUEST", "휴학 사유", 10L,
                "2026-08-27T00:00:00");
        history(advisee, "ENROLLED", "GRADUATED", "ADMIN_CORRECTION", null, null, "2026-08-27T23:59:59");
        history(lectureStudent, "ENROLLED", "DISMISSED", "DISMISSAL", "퇴학 사유", 20L,
                "2026-08-28T00:00:00");
        for (Student target : List.of(unrelated, cancelled, pastStudent, otherProfessorStudent)) {
            history(target, "ON_LEAVE", "ENROLLED", "READMISSION", "복구 사유", null,
                    "2026-08-28T00:00:00");
        }
        entityManager.clear();
    }

    @Test
    void studentGetsOnlyOwnHistoryUsingUserIdNotStudentId() throws Exception {
        assertThat(sameDepartment.getId()).isNotEqualTo(STUDENT_USER_ID);
        mvc.perform(get(PATH).headers(headers(STUDENT_USER_ID, "STUDENT")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].historyId").value(firstHistoryId))
                .andExpect(jsonPath("$.data.items[0].studentId").value(sameDepartment.getId()))
                .andExpect(jsonPath("$.data.items[0].changedBy").value(ADMIN_USER_ID))
                .andExpect(jsonPath("$.data.items[0].sourceType").value("LEAVE_REQUEST"))
                .andExpect(jsonPath("$.data.items[0].email").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].changedByName").doesNotExist())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void studentCannotWidenScopeWithAnotherStudentId() throws Exception {
        mvc.perform(get(PATH).headers(headers(STUDENT_USER_ID, "STUDENT"))
                        .param("studentId", unrelated.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.totalCount").value(0));
    }

    @Test
    void professorUnionIncludesTerminalStudentsWithoutDuplicates() throws Exception {
        mvc.perform(get(PATH).headers(headers(PROFESSOR_USER_ID, "PROFESSOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.items[*].studentName", containsInAnyOrder(
                        "가학과학생", "나지도학생", "다수강학생")));
    }

    @Test
    void professorCannotSeeUnrelatedCancelledPastOrOtherProfessorsStudents() throws Exception {
        for (Student target : List.of(unrelated, cancelled, pastStudent, otherProfessorStudent)) {
            mvc.perform(get(PATH).headers(headers(PROFESSOR_USER_ID, "PROFESSOR"))
                            .param("studentId", target.getId().toString()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty())
                    .andExpect(jsonPath("$.data.totalCount").value(0));
        }
    }

    @Test
    void professorScopeAlsoAppliesToCountAndPagination() throws Exception {
        mvc.perform(get(PATH).headers(headers(PROFESSOR_USER_ID, "PROFESSOR"))
                        .param("page", "2").param("size", "1").param("sortDirection", "asc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.items[0].studentName").value("나지도학생"))
                .andExpect(jsonPath("$.data.page").value(2)).andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }

    @Test
    void adminSeesAllAndSortIsStableAtIdenticalTimestamps() throws Exception {
        mvc.perform(get(PATH).headers(headers(ADMIN_USER_ID, "ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCount").value(7))
                .andExpect(jsonPath("$.data.items[*].studentName", contains(
                        "사다른교수학생", "바과거학생", "마취소학생", "라무관학생", "다수강학생", "나지도학생", "가학과학생")));
        mvc.perform(get(PATH).headers(headers(ADMIN_USER_ID, "ADMIN"))
                        .param("sortDirection", "asc").param("size", "2").param("page", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCount").value(7))
                .andExpect(jsonPath("$.data.items[*].studentName", contains("다수강학생", "라무관학생")));
    }

    @Test
    void filtersAreCombinedAndSearchUsesOnlyStudentName() throws Exception {
        mvc.perform(get(PATH).headers(headers(ADMIN_USER_ID, "ADMIN"))
                        .param("keyword", "  학과학생  ").param("studentId", sameDepartment.getId().toString())
                        .param("departmentId", department.getId().toString())
                        .param("previousStatus", "ENROLLED").param("newStatus", "ON_LEAVE")
                        .param("sourceType", "LEAVE_REQUEST").param("fromDate", "2026-08-27")
                        .param("toDate", "2026-08-27"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.items[0].historyId").value(firstHistoryId));
        for (String keyword : List.of("휴학 사유", "history.test", "이력컴퓨터학과", "%", "_")) {
            mvc.perform(get(PATH).headers(headers(ADMIN_USER_ID, "ADMIN")).param("keyword", keyword))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCount").value(0));
        }
    }

    @ParameterizedTest
    @CsvSource({"previousStatus,ON_LEAVE,4", "newStatus,DISMISSED,1", "sourceType,READMISSION,4"})
    void eachStatusAndSourceFilterIsApplied(String key, String value, int count) throws Exception {
        mvc.perform(get(PATH).headers(headers(ADMIN_USER_ID, "ADMIN")).param(key, value))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCount").value(count));
    }

    @Test
    void inclusiveDatesIncludeBothMidnightAndLastSecondButNotNextDay() throws Exception {
        mvc.perform(get(PATH).headers(headers(ADMIN_USER_ID, "ADMIN"))
                        .param("fromDate", "2026-08-27").param("toDate", "2026-08-27"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCount").value(2));
        mvc.perform(get(PATH).headers(headers(ADMIN_USER_ID, "ADMIN")).param("fromDate", "2026-08-28"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCount").value(5));
        mvc.perform(get(PATH).headers(headers(ADMIN_USER_ID, "ADMIN")).param("toDate", "9999-12-31"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCount").value(7));
    }

    @Test
    void nullableReasonAndSourceIdRemainPresent() throws Exception {
        mvc.perform(get(PATH).headers(headers(PROFESSOR_USER_ID, "PROFESSOR"))
                        .param("studentId", advisee.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].reason", nullValue()))
                .andExpect(jsonPath("$.data.items[0].sourceId", nullValue()));
    }

    @Test
    void noDataAndOutOfRangePagesUseStandardEmptyPage() throws Exception {
        mvc.perform(get(PATH).headers(headers(ADMIN_USER_ID, "ADMIN")).param("keyword", "없는이름"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.totalCount").value(0)).andExpect(jsonPath("$.data.hasNext").value(false));
        mvc.perform(get(PATH).headers(headers(ADMIN_USER_ID, "ADMIN")).param("page", "2147483647"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.totalCount").value(7)).andExpect(jsonPath("$.data.hasNext").value(false));
        mvc.perform(get(PATH).headers(headers(999999L, "STUDENT")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void sizeIsClampedAndBlankKeywordIsIgnored() throws Exception {
        mvc.perform(get(PATH).headers(headers(ADMIN_USER_ID, "ADMIN")).param("size", "1000").param("keyword", "  "))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.size").value(100))
                .andExpect(jsonPath("$.data.totalCount").value(7));
    }

    @ParameterizedTest
    @CsvSource({"page,0", "size,0", "studentId,-1", "departmentId,0", "previousStatus,INVALID",
            "newStatus,INVALID", "sourceType,CANCELLED", "sortDirection,name", "fromDate,2026-02-30",
            "toDate,99999-01-01", "fromDate,0999-01-01", "page,not-a-number"})
    void invalidInputReturnsStandardError(String key, String value) throws Exception {
        mvc.perform(get(PATH).headers(headers(ADMIN_USER_ID, "ADMIN")).param(key, value))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E21"));
    }

    @Test
    void reversedDatesAndOversizedKeywordReturnBadRequest() throws Exception {
        mvc.perform(get(PATH).headers(headers(ADMIN_USER_ID, "ADMIN"))
                        .param("fromDate", "2026-08-28").param("toDate", "2026-08-27"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E21"));
        mvc.perform(get(PATH).headers(headers(ADMIN_USER_ID, "ADMIN")).param("keyword", "가".repeat(101)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E21"));
    }

    @Test
    void missingAuthenticationAndMissingProfessorAreRejected() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("E02"));
        mvc.perform(get(PATH).headers(headers(999999L, "PROFESSOR")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("E10"));
    }

    @Test
    void unsupportedAuthenticatedRoleIsForbiddenAndMalformedGatewayIdentityIsUnauthorized() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken(new CurrentUser(ADMIN_USER_ID, "SYSTEM"),
                null, List.of(new SimpleGrantedAuthority("ROLE_SYSTEM")));
        mvc.perform(get(PATH).with(authentication(authentication)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("E03"));
        mvc.perform(get(PATH).headers(headers(0L, "STUDENT")))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("E04"));
    }

    @Test
    void currentAffiliationControlsScopeAndDisplayWithoutRewritingHistory() throws Exception {
        Student target = entityManager.find(Student.class, sameDepartment.getId());
        target.changeAffiliation(entityManager.find(Department.class, otherDepartment.getId()));
        entityManager.flush();
        entityManager.clear();
        mvc.perform(get(PATH).headers(headers(PROFESSOR_USER_ID, "PROFESSOR"))
                        .param("studentId", target.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty());
        mvc.perform(get(PATH).headers(headers(STUDENT_USER_ID, "STUDENT")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].departmentId").value(otherDepartment.getId()))
                .andExpect(jsonPath("$.data.items[0].departmentName").value("이력경영학과"))
                .andExpect(jsonPath("$.data.items[0].previousStatus").value("ENROLLED"))
                .andExpect(jsonPath("$.data.items[0].newStatus").value("ON_LEAVE"));
    }

    @Test
    void existingWithdrawalHistoryIsReturnedAndRepeatedReadsDoNotWriteHistoryOrAudit() throws Exception {
        AcademicStatusHistory withdrawalHistory = AcademicStatusHistory.withdrawalApproved(
                entityManager.getReference(Student.class, sameDepartment.getId()), AcademicStatus.ENROLLED,
                entityManager.getReference(User.class, ADMIN_USER_ID), 123L);
        entityManager.persist(withdrawalHistory);
        entityManager.flush();
        entityManager.clear();
        long auditBefore = count("audit_logs");
        for (int request = 0; request < 2; request++) {
            mvc.perform(get(PATH).headers(headers(STUDENT_USER_ID, "STUDENT")).param("sourceType", "WITHDRAWAL_REQUEST"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCount").value(1))
                    .andExpect(jsonPath("$.data.items[0].reason").value("자퇴 최종 승인"))
                    .andExpect(jsonPath("$.data.items[0].sourceId").value(123));
        }
        assertThat(count("academic_status_histories")).isEqualTo(8);
        assertThat(count("audit_logs")).isEqualTo(auditBefore);
        assertThat(entityManager.find(Student.class, sameDepartment.getId()).getAcademicStatus())
                .isEqualTo(AcademicStatus.WITHDRAWN);
    }

    @Test
    void existingStudentDirectoryStillRejectsTerminalStatusForProfessor() throws Exception {
        mvc.perform(get("/api/academic/students").headers(headers(PROFESSOR_USER_ID, "PROFESSOR"))
                        .param("academicStatus", "WITHDRAWN"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("E03"));
    }

    @Test
    void softDeletedStudentAccountIsExcludedButStoredHistoryIsPreserved() throws Exception {
        entityManager.remove(entityManager.getReference(User.class, STUDENT_USER_ID));
        entityManager.flush();
        entityManager.clear();
        mvc.perform(get(PATH).headers(headers(ADMIN_USER_ID, "ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCount").value(6));
        mvc.perform(get(PATH).headers(headers(ADMIN_USER_ID, "ADMIN"))
                        .param("studentId", sameDepartment.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty());
        assertThat(count("academic_status_histories")).isEqualTo(7);
    }

    private long count(String table) {
        return ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM " + table).getSingleResult()).longValue();
    }

    private long history(Student target, String previous, String next, String source, String reason,
                         Long sourceId, String createdAt) {
        entityManager.createNativeQuery("""
                INSERT INTO academic_status_histories
                    (student_id, previous_status, new_status, reason, changed_by, source_type, source_id, created_at)
                VALUES (:student, :previous, :next, :reason, :actor, :source, :sourceId, :createdAt)
                """)
                .setParameter("student", target.getId()).setParameter("previous", previous).setParameter("next", next)
                .setParameter("reason", reason).setParameter("actor", ADMIN_USER_ID).setParameter("source", source)
                .setParameter("sourceId", sourceId).setParameter("createdAt", LocalDateTime.parse(createdAt)).executeUpdate();
        return ((Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
    }

    private User user(long id, String name, UserRole role) {
        User user = User.synchronize(id, name, id + "@history.test", null, null, role, UserStatus.ACTIVE);
        entityManager.persist(user);
        return user;
    }

    private Student student(long userId, String name, Department affiliation, Professor advisor, AcademicStatus status) {
        Student student = Student.create(user(userId, name, UserRole.STUDENT), affiliation, (byte) 2,
                (short) 2025, advisor);
        student.changeAcademicStatus(status);
        entityManager.persist(student);
        return student;
    }

    private Semester semester(short year, boolean current) {
        Semester semester = Semester.create(year, SemesterTerm.FIRST, LocalDate.of(year, 3, 1),
                LocalDate.of(year, 6, 30), LocalDateTime.of(year, 2, 1, 9, 0), LocalDateTime.of(year, 2, 20, 18, 0), current);
        entityManager.persist(semester);
        return semester;
    }

    private Lecture lecture(Semester semester, Course course, Professor professor, String section) {
        Lecture lecture = Lecture.create(semester, course, professor, section, 30, "H101", LectureStatus.OPEN,
                25, 25, 25, 25, null);
        entityManager.persist(lecture);
        return lecture;
    }

    private HttpHeaders headers(long userId, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", String.valueOf(userId));
        headers.set("X-User-Role", role);
        return headers;
    }
}
