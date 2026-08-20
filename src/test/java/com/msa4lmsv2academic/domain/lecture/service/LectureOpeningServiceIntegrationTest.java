package com.msa4lmsv2academic.domain.lecture.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.domain.audit.repository.AuditLogRepository;
import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import com.msa4lmsv2academic.domain.course.entity.Course;
import com.msa4lmsv2academic.domain.lecture.entity.LectureDayOfWeek;
import com.msa4lmsv2academic.domain.lecture.entity.LectureOpeningRequestStatus;
import com.msa4lmsv2academic.domain.lecture.repository.LectureOpeningRequestRepository;
import com.msa4lmsv2academic.domain.lecture.repository.LectureRepository;
import com.msa4lmsv2academic.domain.lecture.repository.LectureScheduleRepository;
import com.msa4lmsv2academic.domain.lecture.request.LectureOpeningCorrectionRequestDTO;
import com.msa4lmsv2academic.domain.lecture.request.LectureOpeningCreateRequestDTO;
import com.msa4lmsv2academic.domain.lecture.request.LectureOpeningReviewRequestDTO;
import com.msa4lmsv2academic.domain.lecture.request.LectureOpeningScheduleRequestDTO;
import com.msa4lmsv2academic.domain.lecture.response.LectureOpeningResponseDTO;
import com.msa4lmsv2academic.domain.organization.entity.College;
import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.msa4lmsv2academic.domain.user.entity.UserStatus;
import com.msa4lmsv2academic.global.error.DuplicateLectureOpeningRequestException;
import com.msa4lmsv2academic.global.error.LectureOpeningAccessDeniedException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class LectureOpeningServiceIntegrationTest extends MySqlIntegrationTest {

    private static final Long PROFESSOR_USER_ID = 98001L;
    private static final Long ADMIN_USER_ID = 98002L;
    private static final CurrentUser PROFESSOR = new CurrentUser(PROFESSOR_USER_ID, "PROFESSOR");
    private static final CurrentUser ADMIN = new CurrentUser(ADMIN_USER_ID, "ADMIN");

    @Autowired
    private LectureOpeningService lectureOpeningService;

    @Autowired
    private LectureOpeningRequestRepository openingRequestRepository;

    @Autowired
    private LectureRepository lectureRepository;

    @Autowired
    private LectureScheduleRepository lectureScheduleRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private EntityManager entityManager;

    private Course course;
    private Semester semester;

    @BeforeEach
    void setUp() {
        User professorUser = User.synchronize(
                PROFESSOR_USER_ID,
                "개설신청 교수",
                "lecture-opening-professor@test.com",
                null,
                null,
                UserRole.PROFESSOR,
                UserStatus.ACTIVE
        );
        User adminUser = User.synchronize(
                ADMIN_USER_ID,
                "개설승인 관리자",
                "lecture-opening-admin@test.com",
                null,
                null,
                UserRole.ADMIN,
                UserStatus.ACTIVE
        );
        College college = College.create("OPEN-COL", "개설신청대학", true);
        Department department = Department.create("OPEN-DEPT", college, "개설신청학과", true);
        Professor professor = Professor.create(professorUser, (short) 2020, department);
        semester = Semester.create(
                (short) 2026,
                SemesterTerm.FIRST,
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 6, 19),
                LocalDateTime.of(2026, 2, 1, 9, 0),
                LocalDateTime.of(2026, 2, 7, 18, 0),
                false
        );
        course = Course.create(
                department,
                "OPEN-COURSE",
                "분산시스템",
                (byte) 3,
                (byte) 3,
                CompletionType.MAJOR_REQUIRED
        );

        entityManager.persist(professorUser);
        entityManager.persist(adminUser);
        entityManager.persist(college);
        entityManager.persist(department);
        entityManager.persist(professor);
        entityManager.persist(semester);
        entityManager.persist(course);
        entityManager.flush();
    }

    @Test
    void professorCreatesPendingRequestAndCanSearchOwnStatus() {
        LectureOpeningResponseDTO created = lectureOpeningService.create(createRequest(), PROFESSOR);

        assertThat(created.openingRequestId()).isNotNull();
        assertThat(created.status()).isEqualTo(LectureOpeningRequestStatus.PENDING);
        assertThat(created.schedules()).hasSize(1);
        assertThat(created.lectureId()).isNull();

        Page<LectureOpeningResponseDTO> result = lectureOpeningService.search(
                LectureOpeningRequestStatus.PENDING,
                PageRequest.of(0, 20),
                PROFESSOR
        );
        assertThat(result.getContent()).extracting(LectureOpeningResponseDTO::openingRequestId)
                .containsExactly(created.openingRequestId());
        assertThat(auditLogRepository.findAll()).extracting("action")
                .contains("LECTURE_OPENING_REQUESTED");
    }

    @Test
    void professorUpdatesOwnPendingRequestAndAuditKeepsBeforeAndAfterValues() {
        LectureOpeningResponseDTO created = lectureOpeningService.create(createRequest(), PROFESSOR);

        LectureOpeningResponseDTO updated = lectureOpeningService.update(
                created.openingRequestId(),
                correctionRequest(),
                PROFESSOR
        );

        assertThat(updated.status()).isEqualTo(LectureOpeningRequestStatus.PENDING);
        assertThat(updated.sectionNo()).isEqualTo("02");
        assertThat(updated.requestedCapacity()).isEqualTo(45);
        assertThat(updated.classroom()).isEqualTo("공학관 401호");
        assertThat(updated.schedules()).hasSize(1);
        assertThat(updated.schedules().getFirst().dayOfWeek()).isEqualTo(LectureDayOfWeek.TUE);
        assertThat(auditLogRepository.findAll()).filteredOn(
                audit -> "LECTURE_OPENING_UPDATED".equals(audit.getAction())
        ).singleElement().satisfies(audit -> {
            assertThat(audit.getBeforeValue()).containsEntry("sectionNo", "01");
            assertThat(audit.getAfterValue()).containsEntry("sectionNo", "02");
            assertThat(audit.getBeforeValue().get("schedules").toString()).contains("MON");
            assertThat(audit.getAfterValue().get("schedules").toString()).contains("TUE");
        });
    }

    @Test
    void adminCorrectionAndApprovalCreateLectureAndSchedulesOnce() {
        LectureOpeningResponseDTO created = lectureOpeningService.create(createRequest(), PROFESSOR);
        LectureOpeningCorrectionRequestDTO correction = new LectureOpeningCorrectionRequestDTO(
                course.getId(),
                semester.getId(),
                "02",
                45,
                "공학관 401호",
                20,
                40,
                30,
                10,
                "관리자 확인 후 보정된 강의계획서",
                List.of(new LectureOpeningScheduleRequestDTO(
                        LectureDayOfWeek.TUE,
                        (byte) 3,
                        (byte) 4
                ))
        );

        LectureOpeningResponseDTO approved = lectureOpeningService.review(
                new LectureOpeningReviewRequestDTO(created.openingRequestId(), true, null, correction),
                ADMIN
        );

        assertThat(approved.status()).isEqualTo(LectureOpeningRequestStatus.APPROVED);
        assertThat(approved.sectionNo()).isEqualTo("02");
        assertThat(approved.requestedCapacity()).isEqualTo(45);
        assertThat(approved.lectureId()).isNotNull();
        assertThat(lectureRepository.findById(approved.lectureId()).orElseThrow().getClassroom())
                .isEqualTo("공학관 401호");
        assertThat(lectureScheduleRepository.count()).isEqualTo(1);
        assertThat(auditLogRepository.findAll()).extracting("action")
                .contains("LECTURE_OPENING_APPROVED");

        assertThatThrownBy(() -> lectureOpeningService.review(
                new LectureOpeningReviewRequestDTO(created.openingRequestId(), true, null, null),
                ADMIN
        )).isInstanceOf(DuplicateLectureOpeningRequestException.class);
        assertThatThrownBy(() -> lectureOpeningService.update(
                created.openingRequestId(),
                correctionRequest(),
                PROFESSOR
        )).isInstanceOf(DuplicateLectureOpeningRequestException.class);
    }

    @Test
    void duplicatePendingRequestAndStudentAccessAreRejected() {
        LectureOpeningResponseDTO created = lectureOpeningService.create(createRequest(), PROFESSOR);

        assertThatThrownBy(() -> lectureOpeningService.create(createRequest(), PROFESSOR))
                .isInstanceOf(DuplicateLectureOpeningRequestException.class);
        assertThatThrownBy(() -> lectureOpeningService.get(
                created.openingRequestId(),
                new CurrentUser(98003L, "STUDENT")
        )).isInstanceOf(LectureOpeningAccessDeniedException.class);
    }

    @Test
    void adminRejectsPendingRequestWithoutCreatingLecture() {
        LectureOpeningResponseDTO created = lectureOpeningService.create(createRequest(), PROFESSOR);

        LectureOpeningResponseDTO rejected = lectureOpeningService.review(
                new LectureOpeningReviewRequestDTO(
                        created.openingRequestId(),
                        false,
                        "강의 시간 재조정이 필요합니다.",
                        null
                ),
                ADMIN
        );

        assertThat(rejected.status()).isEqualTo(LectureOpeningRequestStatus.REJECTED);
        assertThat(rejected.rejectReason()).isEqualTo("강의 시간 재조정이 필요합니다.");
        assertThat(lectureRepository.count()).isZero();
        assertThat(openingRequestRepository.findById(created.openingRequestId()).orElseThrow().getReviewedAt())
                .isNotNull();
    }

    private LectureOpeningCreateRequestDTO createRequest() {
        return new LectureOpeningCreateRequestDTO(
                course.getId(),
                semester.getId(),
                "01",
                40,
                "공학관 301호",
                30,
                30,
                30,
                10,
                "분산시스템의 목표, 교재, 주차별 계획",
                List.of(new LectureOpeningScheduleRequestDTO(
                        LectureDayOfWeek.MON,
                        (byte) 1,
                        (byte) 2
                ))
        );
    }

    private LectureOpeningCorrectionRequestDTO correctionRequest() {
        return new LectureOpeningCorrectionRequestDTO(
                course.getId(),
                semester.getId(),
                "02",
                45,
                "공학관 401호",
                20,
                40,
                30,
                10,
                "교수가 보완한 강의계획서",
                List.of(new LectureOpeningScheduleRequestDTO(
                        LectureDayOfWeek.TUE,
                        (byte) 3,
                        (byte) 4
                ))
        );
    }
}
