package com.msa4lmsv2academic.domain.lecture.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.domain.audit.repository.AuditLogRepository;
import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import com.msa4lmsv2academic.domain.course.entity.Course;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.lecture.entity.LectureStatus;
import com.msa4lmsv2academic.domain.lecture.repository.LectureRepository;
import com.msa4lmsv2academic.domain.lecture.request.LectureSyllabusUpdateRequestDTO;
import com.msa4lmsv2academic.domain.lecture.response.LectureSyllabusResponseDTO;
import com.msa4lmsv2academic.domain.organization.entity.College;
import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.msa4lmsv2academic.domain.user.entity.UserStatus;
import com.msa4lmsv2academic.global.error.LectureSyllabusAccessDeniedException;
import com.msa4lmsv2academic.global.error.LectureSyllabusConflictException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class LectureSyllabusServiceIntegrationTest extends MySqlIntegrationTest {

    private static final Long PROFESSOR_USER_ID = 99101L;
    private static final Long ADMIN_USER_ID = 99102L;
    private static final CurrentUser PROFESSOR = new CurrentUser(PROFESSOR_USER_ID, "PROFESSOR");
    private static final CurrentUser ADMIN = new CurrentUser(ADMIN_USER_ID, "ADMIN");

    @Autowired
    private LectureSyllabusService lectureSyllabusService;

    @Autowired
    private LectureRepository lectureRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private EntityManager entityManager;

    private Lecture openLecture;
    private Lecture closedLecture;

    @BeforeEach
    void setUp() {
        User professorUser = User.synchronize(
                PROFESSOR_USER_ID,
                "강의계획 담당교수",
                "syllabus-professor@test.com",
                null,
                null,
                UserRole.PROFESSOR,
                UserStatus.ACTIVE
        );
        User adminUser = User.synchronize(
                ADMIN_USER_ID,
                "강의계획 관리자",
                "syllabus-admin@test.com",
                null,
                null,
                UserRole.ADMIN,
                UserStatus.ACTIVE
        );
        College college = College.create("SYL-COL", "강의계획대학", true);
        Department department = Department.create("211", college, "강의계획학과", true);
        Professor professor = Professor.create(professorUser, (short) 2020, department);
        Semester semester = Semester.create(
                (short) 2026,
                SemesterTerm.FIRST,
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 6, 19),
                LocalDateTime.of(2026, 2, 1, 9, 0),
                LocalDateTime.of(2026, 2, 7, 18, 0),
                false
        );
        Course course = Course.create(
                department,
                "SYL-COURSE",
                "강의계획서 작성",
                (byte) 3,
                (byte) 3,
                CompletionType.MAJOR_REQUIRED
        );
        openLecture = Lecture.create(
                semester, course, professor, "01", 40, "공학관 301호", LectureStatus.OPEN,
                30, 30, 30, 10, null
        );
        closedLecture = Lecture.create(
                semester, course, professor, "02", 40, "공학관 302호", LectureStatus.CLOSED,
                30, 30, 30, 10, "마감된 계획서"
        );

        entityManager.persist(professorUser);
        entityManager.persist(adminUser);
        entityManager.persist(college);
        entityManager.persist(department);
        entityManager.persist(professor);
        entityManager.persist(semester);
        entityManager.persist(course);
        entityManager.persist(openLecture);
        entityManager.persist(closedLecture);
        entityManager.flush();
    }

    @Test
    void professorWritesSyllabusAndAdminReadsIt() {
        LectureSyllabusResponseDTO updated = lectureSyllabusService.update(
                openLecture.getId(),
                new LectureSyllabusUpdateRequestDTO("  주차별 강의 목표와 실습 계획  "),
                PROFESSOR,
                "integration-request",
                "127.0.0.1"
        );
        entityManager.clear();

        assertThat(updated.syllabus()).isEqualTo("주차별 강의 목표와 실습 계획");
        assertThat(lectureRepository.findById(openLecture.getId()).orElseThrow().getSyllabus())
                .isEqualTo("주차별 강의 목표와 실습 계획");
        assertThat(lectureSyllabusService.get(openLecture.getId(), ADMIN).syllabus())
                .isEqualTo("주차별 강의 목표와 실습 계획");
        assertThat(auditLogRepository.findAll()).extracting("action")
                .contains("LECTURE_SYLLABUS_CREATED");
    }

    @Test
    void repeatedSamePutDoesNotCreateDuplicateAudit() {
        LectureSyllabusUpdateRequestDTO request = new LectureSyllabusUpdateRequestDTO("동일한 강의계획서");

        lectureSyllabusService.update(openLecture.getId(), request, PROFESSOR, null, null);
        lectureSyllabusService.update(openLecture.getId(), request, PROFESSOR, null, null);

        assertThat(auditLogRepository.findAll()).extracting("action")
                .containsExactly("LECTURE_SYLLABUS_CREATED");
    }

    @Test
    void adminCannotUpdateAndClosedLectureRejectsProfessorUpdate() {
        LectureSyllabusUpdateRequestDTO request = new LectureSyllabusUpdateRequestDTO("수정 시도");

        assertThatThrownBy(() -> lectureSyllabusService.update(
                openLecture.getId(), request, ADMIN, null, null
        )).isInstanceOf(LectureSyllabusAccessDeniedException.class);
        assertThatThrownBy(() -> lectureSyllabusService.update(
                closedLecture.getId(), request, PROFESSOR, null, null
        )).isInstanceOf(LectureSyllabusConflictException.class);
    }
}
