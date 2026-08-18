package com.msa4lmsv2academic.domain.lecture.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import com.msa4lmsv2academic.domain.course.entity.Course;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.lecture.entity.LectureDayOfWeek;
import com.msa4lmsv2academic.domain.lecture.entity.LectureOpeningRequest;
import com.msa4lmsv2academic.domain.lecture.entity.LectureOpeningRequestStatus;
import com.msa4lmsv2academic.domain.lecture.repository.LectureOpeningReferenceQueryRepository;
import com.msa4lmsv2academic.domain.lecture.repository.LectureOpeningRequestRepository;
import com.msa4lmsv2academic.domain.lecture.repository.LectureRepository;
import com.msa4lmsv2academic.domain.lecture.repository.LectureScheduleRepository;
import com.msa4lmsv2academic.domain.lecture.request.LectureOpeningCreateRequestDTO;
import com.msa4lmsv2academic.domain.lecture.request.LectureOpeningReviewRequestDTO;
import com.msa4lmsv2academic.domain.lecture.request.LectureOpeningScheduleRequestDTO;
import com.msa4lmsv2academic.domain.lecture.response.LectureOpeningResponseDTO;
import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.msa4lmsv2academic.domain.user.entity.UserStatus;
import com.msa4lmsv2academic.global.error.DuplicateLectureOpeningRequestException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LectureOpeningServiceTest {

    private LectureOpeningRequestRepository openingRequestRepository;
    private LectureOpeningReferenceQueryRepository referenceQueryRepository;
    private LectureRepository lectureRepository;
    private LectureScheduleRepository lectureScheduleRepository;
    private AuditLogService auditLogService;
    private LectureOpeningService service;
    private Department department;
    private Professor professor;
    private Course course;
    private Semester semester;
    private User admin;

    @BeforeEach
    void setUp() {
        openingRequestRepository = mock(LectureOpeningRequestRepository.class);
        referenceQueryRepository = mock(LectureOpeningReferenceQueryRepository.class);
        lectureRepository = mock(LectureRepository.class);
        lectureScheduleRepository = mock(LectureScheduleRepository.class);
        auditLogService = mock(AuditLogService.class);
        service = new LectureOpeningService(
                openingRequestRepository,
                referenceQueryRepository,
                lectureRepository,
                lectureScheduleRepository,
                auditLogService
        );

        department = Department.create("CSE", null, "컴퓨터공학과", true);
        ReflectionTestUtils.setField(department, "id", 11L);
        User professorUser = User.synchronize(
                9001L, "담당교수", null, null, null, UserRole.PROFESSOR, UserStatus.ACTIVE
        );
        professor = Professor.create(professorUser, (short) 2020, department);
        ReflectionTestUtils.setField(professor, "id", 21L);
        course = Course.create(
                department, "CSE301", "운영체제", (byte) 3, (byte) 3, CompletionType.MAJOR_REQUIRED
        );
        ReflectionTestUtils.setField(course, "id", 31L);
        semester = Semester.create(
                (short) 2026,
                SemesterTerm.FIRST,
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 6, 19),
                LocalDateTime.of(2026, 2, 1, 9, 0),
                LocalDateTime.of(2026, 2, 7, 18, 0),
                false
        );
        ReflectionTestUtils.setField(semester, "id", 41L);
        admin = User.synchronize(9002L, "관리자", null, null, null, UserRole.ADMIN, UserStatus.ACTIVE);
    }

    @Test
    void createsPendingOpeningRequestAndRecordsAudit() {
        when(referenceQueryRepository.findProfessorByUserId(9001L)).thenReturn(Optional.of(professor));
        when(referenceQueryRepository.findCourseById(31L)).thenReturn(Optional.of(course));
        when(referenceQueryRepository.findSemesterById(41L)).thenReturn(Optional.of(semester));
        when(openingRequestRepository.saveAndFlush(any(LectureOpeningRequest.class)))
                .thenAnswer(invocation -> {
                    LectureOpeningRequest request = invocation.getArgument(0);
                    ReflectionTestUtils.setField(request, "id", 101L);
                    ReflectionTestUtils.setField(request, "version", 0L);
                    return request;
                });

        LectureOpeningResponseDTO response = service.create(
                createRequest(),
                new CurrentUser(9001L, "PROFESSOR")
        );

        assertThat(response.openingRequestId()).isEqualTo(101L);
        assertThat(response.status()).isEqualTo(LectureOpeningRequestStatus.PENDING);
        assertThat(response.schedules()).hasSize(1);
        verify(auditLogService).record(
                eq(9001L),
                eq("LECTURE_OPENING_REQUESTED"),
                eq("LECTURE_OPENING_REQUEST"),
                eq(101L),
                isNull(),
                any(),
                isNull(),
                isNull(),
                isNull()
        );
    }

    @Test
    void approvalCreatesLectureAndSchedule() {
        LectureOpeningRequest request = pendingRequest();
        when(openingRequestRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(request));
        when(referenceQueryRepository.findUserById(9002L)).thenReturn(Optional.of(admin));
        when(referenceQueryRepository.lockProfessor(21L)).thenReturn(Optional.of(professor));
        when(lectureRepository.saveAndFlush(any(Lecture.class))).thenAnswer(invocation -> {
            Lecture lecture = invocation.getArgument(0);
            ReflectionTestUtils.setField(lecture, "id", 201L);
            return lecture;
        });
        when(openingRequestRepository.saveAndFlush(request)).thenReturn(request);

        LectureOpeningResponseDTO response = service.review(
                new LectureOpeningReviewRequestDTO(101L, true, null, null),
                new CurrentUser(9002L, "ADMIN")
        );

        assertThat(response.status()).isEqualTo(LectureOpeningRequestStatus.APPROVED);
        assertThat(response.lectureId()).isEqualTo(201L);
        verify(lectureScheduleRepository).saveAll(any());
        verify(auditLogService).record(
                eq(9002L),
                eq("LECTURE_OPENING_APPROVED"),
                eq("LECTURE_OPENING_REQUEST"),
                eq(101L),
                any(),
                any(),
                isNull(),
                isNull(),
                isNull()
        );
    }

    @Test
    void rejectsAlreadyProcessedRequest() {
        LectureOpeningRequest request = pendingRequest();
        request.reject(admin, "시간표 재검토", LocalDateTime.now());
        when(openingRequestRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(request));
        when(referenceQueryRepository.findUserById(9002L)).thenReturn(Optional.of(admin));
        when(referenceQueryRepository.lockProfessor(21L)).thenReturn(Optional.of(professor));

        assertThatThrownBy(() -> service.review(
                new LectureOpeningReviewRequestDTO(101L, true, null, null),
                new CurrentUser(9002L, "ADMIN")
        )).isInstanceOf(DuplicateLectureOpeningRequestException.class);
    }

    private LectureOpeningRequest pendingRequest() {
        LectureOpeningRequest request = LectureOpeningRequest.create(
                course, professor, semester, "01", 40, "공학관 301호", 30, 30, 30, 10, "강의계획서"
        );
        request.addSchedule(LectureDayOfWeek.MON, (byte) 1, (byte) 2);
        ReflectionTestUtils.setField(request, "id", 101L);
        ReflectionTestUtils.setField(request, "version", 0L);
        return request;
    }

    private LectureOpeningCreateRequestDTO createRequest() {
        return new LectureOpeningCreateRequestDTO(
                31L,
                41L,
                "01",
                40,
                "공학관 301호",
                30,
                30,
                30,
                10,
                "운영체제 강의계획서",
                List.of(new LectureOpeningScheduleRequestDTO(
                        LectureDayOfWeek.MON,
                        (byte) 1,
                        (byte) 2
                ))
        );
    }
}
