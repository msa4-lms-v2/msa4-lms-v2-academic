package com.msa4lmsv2academic.domain.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.attendance.entity.ExcuseRequest;
import com.msa4lmsv2academic.domain.attendance.entity.ExcuseRequestStatus;
import com.msa4lmsv2academic.domain.attendance.repository.ExcuseRequestRepository;
import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.global.error.ExcuseAttachmentConflictException;
import com.msa4lmsv2academic.global.error.ExcuseAttachmentNotFoundException;
import com.msa4lmsv2academic.global.error.ExcuseRequestAccessDeniedException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExcuseAttachmentTransactionServiceTest {

    private static final CurrentUser STUDENT = new CurrentUser(1001L, "STUDENT");
    private static final CurrentUser PROFESSOR = new CurrentUser(2001L, "PROFESSOR");
    private static final CurrentUser ADMIN = new CurrentUser(3001L, "ADMIN");

    private ExcuseRequestRepository repository;
    private AuditLogService auditLogService;
    private ExcuseAttachmentTransactionService service;

    @BeforeEach
    void setUp() {
        repository = mock(ExcuseRequestRepository.class);
        auditLogService = mock(AuditLogService.class);
        service = new ExcuseAttachmentTransactionService(repository, auditLogService);
    }

    @Test
    void studentOwnerRegistersAttachmentAndAudit() {
        ExcuseRequest excuseRequest = request(STUDENT.id(), PROFESSOR.id(), ExcuseRequestStatus.PENDING, false);
        when(repository.findDetailForUpdate(31L)).thenReturn(Optional.of(excuseRequest));

        var result = service.register(
                31L,
                "진료확인서.pdf",
                "excuse-requests/31/file.pdf",
                "application/pdf",
                2048L,
                STUDENT,
                "request-1",
                "127.0.0.1"
        );

        assertThat(result.originalName()).isEqualTo("진료확인서.pdf");
        verify(repository).flush();
        verify(auditLogService).record(
                eq(STUDENT.id()),
                eq("EXCUSE_ATTACHMENT_UPLOADED"),
                eq("EXCUSE_REQUEST"),
                eq(31L),
                eq(null),
                any(Map.class),
                eq("공결 증빙 등록"),
                eq("request-1"),
                eq("127.0.0.1")
        );
    }

    @Test
    void rejectsUploadByAnotherStudentOrAfterProcessing() {
        ExcuseRequest anotherStudentsRequest = request(9999L, PROFESSOR.id(), ExcuseRequestStatus.PENDING, false);
        when(repository.findDetailById(31L)).thenReturn(Optional.of(anotherStudentsRequest));

        assertThatThrownBy(() -> service.validateUploadTarget(31L, STUDENT))
                .isInstanceOf(ExcuseRequestAccessDeniedException.class);

        ExcuseRequest approvedRequest = request(STUDENT.id(), PROFESSOR.id(), ExcuseRequestStatus.APPROVED, false);
        when(repository.findDetailById(32L)).thenReturn(Optional.of(approvedRequest));

        assertThatThrownBy(() -> service.validateUploadTarget(32L, STUDENT))
                .isInstanceOf(ExcuseAttachmentConflictException.class);
    }

    @Test
    void ownerProfessorAndAdminCanDownloadButUnrelatedProfessorCannot() {
        ExcuseRequest excuseRequest = request(STUDENT.id(), PROFESSOR.id(), ExcuseRequestStatus.PENDING, true);
        when(repository.findDetailById(31L)).thenReturn(Optional.of(excuseRequest));

        assertThat(service.getDownloadTarget(31L, STUDENT).storedName())
                .isEqualTo("excuse-requests/31/file.pdf");
        assertThat(service.getDownloadTarget(31L, PROFESSOR).storedName())
                .isEqualTo("excuse-requests/31/file.pdf");
        assertThat(service.getDownloadTarget(31L, ADMIN).storedName())
                .isEqualTo("excuse-requests/31/file.pdf");

        assertThatThrownBy(() -> service.getDownloadTarget(31L, new CurrentUser(2999L, "PROFESSOR")))
                .isInstanceOf(ExcuseRequestAccessDeniedException.class);
    }

    @Test
    void reportsMissingAttachmentAfterAuthorization() {
        ExcuseRequest excuseRequest = request(STUDENT.id(), PROFESSOR.id(), ExcuseRequestStatus.PENDING, false);
        when(repository.findDetailById(31L)).thenReturn(Optional.of(excuseRequest));

        assertThatThrownBy(() -> service.getDownloadTarget(31L, STUDENT))
                .isInstanceOf(ExcuseAttachmentNotFoundException.class);
    }

    private ExcuseRequest request(
            Long studentUserId,
            Long professorUserId,
            ExcuseRequestStatus status,
            boolean attachment
    ) {
        User studentUser = mock(User.class);
        when(studentUser.getId()).thenReturn(studentUserId);
        Student student = mock(Student.class);
        when(student.getUser()).thenReturn(studentUser);

        User professorUser = mock(User.class);
        when(professorUser.getId()).thenReturn(professorUserId);
        Professor professor = mock(Professor.class);
        when(professor.getUser()).thenReturn(professorUser);

        Lecture lecture = mock(Lecture.class);
        when(lecture.getProfessor()).thenReturn(professor);
        Enrollment enrollment = mock(Enrollment.class);
        when(enrollment.getStudent()).thenReturn(student);
        when(enrollment.getLecture()).thenReturn(lecture);

        ExcuseRequest excuseRequest = mock(ExcuseRequest.class);
        when(excuseRequest.getId()).thenReturn(31L);
        when(excuseRequest.getEnrollment()).thenReturn(enrollment);
        when(excuseRequest.getStatus()).thenReturn(status);
        when(excuseRequest.hasAttachment()).thenReturn(attachment);
        when(excuseRequest.getUpdatedAt()).thenReturn(LocalDateTime.of(2026, 9, 2, 10, 30));
        if (attachment) {
            when(excuseRequest.getAttachmentOriginalName()).thenReturn("진료확인서.pdf");
            when(excuseRequest.getAttachmentStoredName()).thenReturn("excuse-requests/31/file.pdf");
            when(excuseRequest.getAttachmentContentType()).thenReturn("application/pdf");
            when(excuseRequest.getAttachmentSize()).thenReturn(2048L);
        } else {
            when(excuseRequest.hasAttachment())
                    .thenReturn(false)
                    .thenReturn(true);
            when(excuseRequest.getAttachmentOriginalName()).thenReturn("진료확인서.pdf");
            when(excuseRequest.getAttachmentStoredName()).thenReturn("excuse-requests/31/file.pdf");
            when(excuseRequest.getAttachmentContentType()).thenReturn("application/pdf");
            when(excuseRequest.getAttachmentSize()).thenReturn(2048L);
        }
        return excuseRequest;
    }
}
