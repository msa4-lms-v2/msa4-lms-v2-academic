package com.msa4lmsv2academic.domain.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.attendance.entity.ExcuseRequest;
import com.msa4lmsv2academic.domain.attendance.entity.ExcuseRequestStatus;
import com.msa4lmsv2academic.domain.attendance.repository.ExcuseRequestRepository;
import com.msa4lmsv2academic.domain.attendance.request.ExcuseReviewRequestDTO;
import com.msa4lmsv2academic.domain.attendance.response.ExcuseRequestResponseDTO;
import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.global.error.ExcuseRequestAccessDeniedException;
import com.msa4lmsv2academic.global.error.ExcuseReviewConflictException;
import com.msa4lmsv2academic.global.idempotency.AcademicIdempotencyKey;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ExcuseReviewServiceTest {

    private static final long PROFESSOR_USER_ID = 11L;
    private static final long REQUEST_ID = 301L;
    private static final String IDEMPOTENCY_KEY = "excuse-review-301";

    private ExcuseRequestRepository excuseRequestRepository;
    private ExcuseReviewIdempotencyService idempotencyService;
    private AuditLogService auditLogService;
    private ExcuseReviewService service;
    private ExcuseRequest excuseRequest;
    private CurrentUser professor;

    @BeforeEach
    void setUp() {
        excuseRequestRepository = mock(ExcuseRequestRepository.class);
        idempotencyService = mock(ExcuseReviewIdempotencyService.class);
        auditLogService = mock(AuditLogService.class);
        service = new ExcuseReviewService(excuseRequestRepository, idempotencyService, auditLogService);

        User professorUser = mock(User.class);
        Professor lectureProfessor = mock(Professor.class);
        Lecture lecture = mock(Lecture.class);
        Enrollment enrollment = mock(Enrollment.class);
        when(professorUser.getId()).thenReturn(PROFESSOR_USER_ID);
        when(lectureProfessor.getUser()).thenReturn(professorUser);
        when(lecture.getProfessor()).thenReturn(lectureProfessor);
        when(enrollment.getId()).thenReturn(201L);
        when(enrollment.getLecture()).thenReturn(lecture);

        excuseRequest = ExcuseRequest.create(
                enrollment,
                LocalDate.of(2026, 9, 1),
                (byte) 2,
                "병원 진료"
        );
        ReflectionTestUtils.setField(excuseRequest, "id", REQUEST_ID);
        ReflectionTestUtils.setField(excuseRequest, "createdAt", LocalDateTime.of(2026, 9, 2, 10, 0));
        ReflectionTestUtils.setField(excuseRequest, "updatedAt", LocalDateTime.of(2026, 9, 2, 10, 0));

        professor = new CurrentUser(PROFESSOR_USER_ID, "PROFESSOR");
        when(excuseRequestRepository.findDetailForUpdate(REQUEST_ID)).thenReturn(Optional.of(excuseRequest));
        when(idempotencyService.hash(eq(REQUEST_ID), any(ExcuseReviewRequestDTO.class))).thenReturn("request-hash");
        when(idempotencyService.replay(eq(IDEMPOTENCY_KEY), eq(PROFESSOR_USER_ID), eq("request-hash"), any()))
                .thenReturn(Optional.empty());
        when(idempotencyService.reserve(eq(IDEMPOTENCY_KEY), eq(PROFESSOR_USER_ID), eq("request-hash"), any()))
                .thenReturn(mock(AcademicIdempotencyKey.class));
    }

    @Test
    void approvesPendingRequestAndRecordsAudit() {
        var response = service.review(
                REQUEST_ID,
                new ExcuseReviewRequestDTO(ExcuseRequestStatus.APPROVED, null),
                IDEMPOTENCY_KEY,
                professor,
                "request-trace",
                "127.0.0.1"
        );

        assertThat(response.data().status()).isEqualTo(ExcuseRequestStatus.APPROVED);
        assertThat(excuseRequest.getStatus()).isEqualTo(ExcuseRequestStatus.APPROVED);
        verify(excuseRequestRepository).saveAndFlush(excuseRequest);
        verify(auditLogService).record(
                eq(PROFESSOR_USER_ID),
                eq("EXCUSE_APPROVED"),
                eq("EXCUSE_REQUEST"),
                eq(REQUEST_ID),
                any(),
                any(),
                eq(null),
                eq("request-trace"),
                eq("127.0.0.1")
        );
        verify(idempotencyService).complete(any(AcademicIdempotencyKey.class), eq(response));
    }

    @Test
    void rejectsPendingRequestWithReasonAndRecordsAudit() {
        var response = service.review(
                REQUEST_ID,
                new ExcuseReviewRequestDTO(ExcuseRequestStatus.REJECTED, "  증빙 불충분  "),
                IDEMPOTENCY_KEY,
                professor,
                null,
                null
        );

        assertThat(response.data().status()).isEqualTo(ExcuseRequestStatus.REJECTED);
        assertThat(response.data().rejectReason()).isEqualTo("증빙 불충분");
        verify(auditLogService).record(
                eq(PROFESSOR_USER_ID),
                eq("EXCUSE_REJECTED"),
                eq("EXCUSE_REQUEST"),
                eq(REQUEST_ID),
                any(),
                any(),
                eq("증빙 불충분"),
                eq(null),
                eq(null)
        );
    }

    @Test
    void rejectsProfessorWhoDoesNotOwnLecture() {
        assertThatThrownBy(() -> service.review(
                REQUEST_ID,
                new ExcuseReviewRequestDTO(ExcuseRequestStatus.APPROVED, null),
                IDEMPOTENCY_KEY,
                new CurrentUser(99L, "PROFESSOR"),
                null,
                null
        )).isInstanceOf(ExcuseRequestAccessDeniedException.class)
                .hasMessageContaining("담당하는 강의");

        verify(idempotencyService, never()).reserve(any(), any(), any(), any());
        verify(excuseRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsRepeatedReviewWithDifferentLogicalRequest() {
        excuseRequest.approve();

        assertThatThrownBy(() -> service.review(
                REQUEST_ID,
                new ExcuseReviewRequestDTO(ExcuseRequestStatus.REJECTED, "재검토"),
                IDEMPOTENCY_KEY,
                professor,
                null,
                null
        )).isInstanceOf(ExcuseReviewConflictException.class)
                .hasMessageContaining("처리 대기");

        verify(auditLogService, never()).record(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void replaysCompletedResponseWithoutChangingStateAgain() {
        ExcuseRequestResponseDTO replayData = new ExcuseRequestResponseDTO(
                REQUEST_ID,
                201L,
                LocalDate.of(2026, 9, 1),
                (byte) 2,
                "병원 진료",
                ExcuseRequestStatus.APPROVED,
                null,
                LocalDateTime.of(2026, 9, 2, 10, 0),
                LocalDateTime.of(2026, 9, 2, 11, 0)
        );
        GlobalResponseDTO<ExcuseRequestResponseDTO> replayResponse = GlobalResponseDTO.success(replayData);
        when(idempotencyService.replay(eq(IDEMPOTENCY_KEY), eq(PROFESSOR_USER_ID), eq("request-hash"), any()))
                .thenReturn(Optional.of(replayResponse));

        var response = service.review(
                REQUEST_ID,
                new ExcuseReviewRequestDTO(ExcuseRequestStatus.APPROVED, null),
                IDEMPOTENCY_KEY,
                professor,
                null,
                null
        );

        assertThat(response).isSameAs(replayResponse);
        verify(idempotencyService, never()).reserve(any(), any(), any(), any());
        verify(excuseRequestRepository, never()).saveAndFlush(any());
        verify(auditLogService, never()).record(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
