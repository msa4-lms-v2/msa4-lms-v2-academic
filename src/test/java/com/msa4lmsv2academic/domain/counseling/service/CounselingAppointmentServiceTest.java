package com.msa4lmsv2academic.domain.counseling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingAppointment;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingAppointmentStatus;
import com.msa4lmsv2academic.domain.counseling.entity.CounselorAvailability;
import com.msa4lmsv2academic.domain.counseling.repository.CounselingAppointmentRepository;
import com.msa4lmsv2academic.domain.counseling.repository.CounselingParticipantQueryRepository;
import com.msa4lmsv2academic.domain.counseling.repository.CounselorAvailabilityRepository;
import com.msa4lmsv2academic.domain.counseling.request.CounselingAppointmentCreateRequestDTO;
import com.msa4lmsv2academic.domain.counseling.request.CounselingAppointmentStatusRequestDTO;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.global.error.CounselingAccessDeniedException;
import com.msa4lmsv2academic.global.error.CounselingScheduleConflictException;
import com.msa4lmsv2academic.global.error.InvalidCounselingRequestException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CounselingAppointmentServiceTest {

    @Mock
    private CounselingAppointmentRepository appointmentRepository;

    @Mock
    private CounselorAvailabilityRepository availabilityRepository;

    @Mock
    private CounselingParticipantQueryRepository participantQueryRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private CounselingNotificationService notificationService;

    private CounselingAppointmentService service;

    @BeforeEach
    void setUp() {
        service = new CounselingAppointmentService(
                appointmentRepository,
                availabilityRepository,
                participantQueryRepository,
                auditLogService,
                notificationService,
                new CounselingPolicy()
        );
    }

    @Test
    void studentCanBookAnyProfessorWhoPublishedAvailability() {
        CurrentUser currentUser = new CurrentUser(21L, "STUDENT");
        LocalDateTime appointmentAt = nextMondayAtNineThirty();
        Student student = student(41L, 21L, "학생");
        Professor professor = professor(31L, 11L, "교수");
        CounselorAvailability availability = CounselorAvailability.create(
                professor,
                DayOfWeek.MONDAY,
                "09:00",
                "12:00",
                LocalDate.now(),
                null
        );
        when(participantQueryRepository.findStudentByUserIdForUpdate(21L)).thenReturn(Optional.of(student));
        when(participantQueryRepository.findProfessorById(31L)).thenReturn(Optional.of(professor));
        when(availabilityRepository.findBookableSlotsForUpdate(31L, DayOfWeek.MONDAY,
                appointmentAt.toLocalDate())).thenReturn(List.of(availability));
        when(appointmentRepository.saveAndFlush(any(CounselingAppointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(
                new CounselingAppointmentCreateRequestDTO(31L, appointmentAt, "전공 상담"), currentUser
        );

        assertThat(result.professorId()).isEqualTo(31L);
        assertThat(result.studentId()).isEqualTo(41L);
        assertThat(result.status()).isEqualTo(CounselingAppointmentStatus.PENDING);
    }

    @Test
    void professorDoubleBookingIsRejected() {
        CurrentUser currentUser = new CurrentUser(21L, "STUDENT");
        LocalDateTime appointmentAt = nextMondayAtNineThirty();
        Student student = student(41L, 21L, "학생");
        Professor professor = professor(31L, 11L, "교수");
        when(participantQueryRepository.findStudentByUserIdForUpdate(21L)).thenReturn(Optional.of(student));
        when(participantQueryRepository.findProfessorById(31L)).thenReturn(Optional.of(professor));
        when(availabilityRepository.findBookableSlotsForUpdate(any(), any(), any())).thenReturn(List.of(
                CounselorAvailability.create(
                        professor, DayOfWeek.MONDAY, "09:00", "12:00", LocalDate.now(), null
                )
        ));
        when(appointmentRepository.existsByProfessorIdAndAppointmentAt(31L, appointmentAt)).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new CounselingAppointmentCreateRequestDTO(31L, appointmentAt, null), currentUser
        )).isInstanceOf(CounselingScheduleConflictException.class);
    }

    @Test
    void onlyAssignedProfessorCanCompleteAppointment() {
        CounselingAppointment appointment = appointment();
        when(appointmentRepository.findByIdForUpdate(51L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.changeStatus(
                51L,
                new CounselingAppointmentStatusRequestDTO(CounselingAppointmentStatus.CONFIRMED, null),
                new CurrentUser(12L, "PROFESSOR")
        )).isInstanceOf(CounselingAccessDeniedException.class);
    }

    @Test
    void assignedProfessorConfirmsAppointmentAndRecordsAudit() {
        CounselingAppointment appointment = appointment();
        when(appointmentRepository.findByIdForUpdate(51L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.saveAndFlush(appointment)).thenReturn(appointment);

        var result = service.changeStatus(
                51L,
                new CounselingAppointmentStatusRequestDTO(
                        CounselingAppointmentStatus.CONFIRMED,
                        "요청한 시간에 온라인 상담을 진행하겠습니다."
                ),
                new CurrentUser(11L, "PROFESSOR")
        );

        assertThat(result.status()).isEqualTo(CounselingAppointmentStatus.CONFIRMED);
        verify(auditLogService).record(
                eq(11L),
                eq("COUNSELING_APPOINTMENT_CONFIRMED"),
                eq("COUNSELING_APPOINTMENT"),
                eq(51L),
                any(),
                any(),
                eq("요청한 시간에 온라인 상담을 진행하겠습니다."),
                isNull(),
                isNull()
        );
        verify(notificationService).createForStatusChange(
                appointment,
                CounselingAppointmentStatus.PENDING,
                "요청한 시간에 온라인 상담을 진행하겠습니다."
        );
    }

    @Test
    void professorMustProvideReasonWhenConfirmingAppointment() {
        CounselingAppointment appointment = appointment();
        when(appointmentRepository.findByIdForUpdate(51L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.changeStatus(
                51L,
                new CounselingAppointmentStatusRequestDTO(CounselingAppointmentStatus.CONFIRMED, null),
                new CurrentUser(11L, "PROFESSOR")
        )).isInstanceOf(InvalidCounselingRequestException.class)
                .hasMessage("상담 승인 사유는 필수입니다.");
    }

    @Test
    void professorMustProvideReasonWhenRejectingAppointment() {
        CounselingAppointment appointment = appointment();
        when(appointmentRepository.findByIdForUpdate(51L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.changeStatus(
                51L,
                new CounselingAppointmentStatusRequestDTO(CounselingAppointmentStatus.REJECTED, null),
                new CurrentUser(11L, "PROFESSOR")
        )).isInstanceOf(InvalidCounselingRequestException.class)
                .hasMessage("상담 반려 사유는 필수입니다.");
    }

    @Test
    void professorCannotCancelStudentAppointment() {
        CounselingAppointment appointment = appointment();
        when(appointmentRepository.findByIdForUpdate(51L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.changeStatus(
                51L,
                new CounselingAppointmentStatusRequestDTO(CounselingAppointmentStatus.CANCELLED, null),
                new CurrentUser(11L, "PROFESSOR")
        )).isInstanceOf(CounselingAccessDeniedException.class)
                .hasMessage("상담 예약 취소는 신청 학생만 할 수 있습니다.");
    }

    @Test
    void studentCancelsOwnAppointmentAndRecordsAudit() {
        CounselingAppointment appointment = appointment();
        when(appointmentRepository.findByIdForUpdate(51L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.saveAndFlush(appointment)).thenReturn(appointment);

        var result = service.changeStatus(
                51L,
                new CounselingAppointmentStatusRequestDTO(CounselingAppointmentStatus.CANCELLED, null),
                new CurrentUser(21L, "STUDENT")
        );

        assertThat(result.status()).isEqualTo(CounselingAppointmentStatus.CANCELLED);
        verify(auditLogService).record(
                eq(21L),
                eq("COUNSELING_APPOINTMENT_CANCELLED"),
                eq("COUNSELING_APPOINTMENT"),
                eq(51L),
                any(),
                any(),
                isNull(),
                isNull(),
                isNull()
        );
        verify(notificationService).createForStatusChange(
                appointment,
                CounselingAppointmentStatus.PENDING,
                null
        );
    }

    @Test
    void professorCompletesConfirmedAppointmentWithOnlineAnswer() {
        CounselingAppointment appointment = appointment();
        appointment.changeStatus(CounselingAppointmentStatus.CONFIRMED, null);
        when(appointmentRepository.findByIdForUpdate(51L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.saveAndFlush(appointment)).thenReturn(appointment);

        var result = service.changeStatus(
                51L,
                new CounselingAppointmentStatusRequestDTO(
                        CounselingAppointmentStatus.COMPLETED,
                        "수강 계획을 확인한 뒤 전공필수 과목부터 신청하세요."
                ),
                new CurrentUser(11L, "PROFESSOR")
        );

        assertThat(result.status()).isEqualTo(CounselingAppointmentStatus.COMPLETED);
        assertThat(result.professorNote()).contains("전공필수 과목부터");
        verify(auditLogService).record(
                eq(11L),
                eq("COUNSELING_APPOINTMENT_COMPLETED"),
                eq("COUNSELING_APPOINTMENT"),
                eq(51L),
                any(),
                any(),
                eq("수강 계획을 확인한 뒤 전공필수 과목부터 신청하세요."),
                isNull(),
                isNull()
        );
        verify(notificationService).createForStatusChange(
                appointment,
                CounselingAppointmentStatus.CONFIRMED,
                "수강 계획을 확인한 뒤 전공필수 과목부터 신청하세요."
        );
    }

    private LocalDateTime nextMondayAtNineThirty() {
        LocalDate date = LocalDate.now().plusWeeks(2);
        while (date.getDayOfWeek() != DayOfWeek.MONDAY) {
            date = date.plusDays(1);
        }
        return date.atTime(9, 30);
    }

    private CounselingAppointment appointment() {
        CounselingAppointment appointment = CounselingAppointment.create(
                student(41L, 21L, "학생"),
                professor(31L, 11L, "교수"),
                nextMondayAtNineThirty(),
                "수강 계획 상담"
        );
        ReflectionTestUtils.setField(appointment, "id", 51L);
        return appointment;
    }

    private Student student(Long studentId, Long userId, String name) {
        Student student = mock(Student.class);
        User user = mock(User.class);
        lenient().when(student.getId()).thenReturn(studentId);
        lenient().when(student.getUser()).thenReturn(user);
        lenient().when(student.getAcademicStatus()).thenReturn(AcademicStatus.ENROLLED);
        lenient().when(user.getId()).thenReturn(userId);
        lenient().when(user.getName()).thenReturn(name);
        return student;
    }

    private Professor professor(Long professorId, Long userId, String name) {
        Professor professor = mock(Professor.class);
        User user = mock(User.class);
        lenient().when(professor.getId()).thenReturn(professorId);
        lenient().when(professor.getUser()).thenReturn(user);
        lenient().when(user.getId()).thenReturn(userId);
        lenient().when(user.getName()).thenReturn(name);
        return professor;
    }
}
