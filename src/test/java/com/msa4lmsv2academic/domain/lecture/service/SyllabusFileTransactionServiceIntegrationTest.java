package com.msa4lmsv2academic.domain.lecture.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.domain.audit.repository.AuditLogRepository;
import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import com.msa4lmsv2academic.domain.course.entity.Course;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.lecture.entity.LectureStatus;
import com.msa4lmsv2academic.domain.lecture.response.SyllabusFileDownloadTarget;
import com.msa4lmsv2academic.domain.lecture.response.SyllabusFileResponseDTO;
import com.msa4lmsv2academic.domain.organization.entity.College;
import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.msa4lmsv2academic.domain.user.entity.UserStatus;
import com.msa4lmsv2academic.global.error.DuplicateSyllabusFileException;
import com.msa4lmsv2academic.global.error.SyllabusFileAccessDeniedException;
import com.msa4lmsv2academic.global.error.SyllabusFileConflictException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class SyllabusFileTransactionServiceIntegrationTest extends MySqlIntegrationTest {

    private static final Long PROFESSOR_USER_ID = 99201L;
    private static final Long OTHER_PROFESSOR_USER_ID = 99202L;
    private static final Long ADMIN_USER_ID = 99203L;
    private static final CurrentUser PROFESSOR = new CurrentUser(PROFESSOR_USER_ID, "PROFESSOR");
    private static final CurrentUser OTHER_PROFESSOR = new CurrentUser(OTHER_PROFESSOR_USER_ID, "PROFESSOR");
    private static final CurrentUser ADMIN = new CurrentUser(ADMIN_USER_ID, "ADMIN");

    @Autowired
    private SyllabusFileTransactionService transactionService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private EntityManager entityManager;

    private Lecture openLecture;
    private Lecture closedLecture;

    @BeforeEach
    void setUp() {
        User professorUser = user(PROFESSOR_USER_ID, "강의계획서 교수", UserRole.PROFESSOR);
        User otherProfessorUser = user(OTHER_PROFESSOR_USER_ID, "다른 교수", UserRole.PROFESSOR);
        User adminUser = user(ADMIN_USER_ID, "강의계획서 관리자", UserRole.ADMIN);
        College college = College.create("SYF-COL", "강의계획서대학", true);
        Department department = Department.create("213", college, "강의계획서학과", true);
        Professor professor = Professor.create(professorUser, (short) 2020, department);
        Professor otherProfessor = Professor.create(otherProfessorUser, (short) 2021, department);
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
                "SYF-COURSE",
                "강의계획서 파일",
                (byte) 3,
                (byte) 3,
                CompletionType.MAJOR_REQUIRED
        );
        openLecture = lecture(semester, course, professor, "01", LectureStatus.OPEN);
        closedLecture = lecture(semester, course, professor, "02", LectureStatus.CLOSED);

        entityManager.persist(professorUser);
        entityManager.persist(otherProfessorUser);
        entityManager.persist(adminUser);
        entityManager.persist(college);
        entityManager.persist(department);
        entityManager.persist(professor);
        entityManager.persist(otherProfessor);
        entityManager.persist(semester);
        entityManager.persist(course);
        entityManager.persist(openLecture);
        entityManager.persist(closedLecture);
        entityManager.flush();
    }

    @Test
    void professorRegistersFileAndAdminCanListAndDownloadMetadata() {
        SyllabusFileResponseDTO uploaded = transactionService.register(
                openLecture.getId(),
                "강의계획서.pdf",
                "syllabus-files/file.pdf",
                "application/pdf",
                2048L,
                PROFESSOR,
                "request-1",
                "127.0.0.1"
        );

        List<SyllabusFileResponseDTO> files = transactionService.list(openLecture.getId(), ADMIN);
        SyllabusFileDownloadTarget target = transactionService.getDownloadTarget(uploaded.fileId(), ADMIN);

        assertThat(files).extracting(SyllabusFileResponseDTO::fileId)
                .containsExactly(uploaded.fileId());
        assertThat(target.originalName()).isEqualTo("강의계획서.pdf");
        assertThat(target.storedName()).isEqualTo("syllabus-files/file.pdf");
        assertThat(auditLogRepository.findAll()).extracting("action")
                .contains("SYLLABUS_FILE_UPLOADED");
    }

    @Test
    void duplicateNameAndSizeInSameLectureAreRejected() {
        transactionService.register(
                openLecture.getId(),
                "강의계획서.pdf",
                "syllabus-files/first.pdf",
                "application/pdf",
                2048L,
                PROFESSOR,
                null,
                null
        );

        assertThatThrownBy(() -> transactionService.validateUploadTarget(
                openLecture.getId(), "강의계획서.pdf", 2048L, PROFESSOR
        )).isInstanceOf(DuplicateSyllabusFileException.class);
    }

    @Test
    void otherProfessorAndClosedLectureAreRejected() {
        assertThatThrownBy(() -> transactionService.list(openLecture.getId(), OTHER_PROFESSOR))
                .isInstanceOf(SyllabusFileAccessDeniedException.class);
        assertThatThrownBy(() -> transactionService.validateUploadTarget(
                closedLecture.getId(), "강의계획서.pdf", 2048L, PROFESSOR
        )).isInstanceOf(SyllabusFileConflictException.class);
    }

    private User user(Long id, String name, UserRole role) {
        return User.synchronize(id, name, null, null, null, role, UserStatus.ACTIVE);
    }

    private Lecture lecture(
            Semester semester,
            Course course,
            Professor professor,
            String sectionNo,
            LectureStatus status
    ) {
        return Lecture.create(
                semester,
                course,
                professor,
                sectionNo,
                40,
                "공학관 301호",
                status,
                30,
                30,
                30,
                10,
                "강의계획서 본문"
        );
    }
}
