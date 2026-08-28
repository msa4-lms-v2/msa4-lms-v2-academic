package com.msa4lmsv2academic.domain.counseling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.counseling.entity.CounselingAppointment;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingAppointmentStatus;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingNotification;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingNotificationType;
import com.msa4lmsv2academic.domain.counseling.repository.CounselingNotificationRepository;
import com.msa4lmsv2academic.domain.counseling.request.CounselingNotificationSearchRequestDTO;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.global.error.CounselingNotificationNotFoundException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CounselingNotificationServiceTest {

    @Mock
    private CounselingNotificationRepository notificationRepository;

    private CounselingNotificationService service;

    @BeforeEach
    void setUp() {
        service = new CounselingNotificationService(notificationRepository);
    }

    @Test
    void professorConfirmationCreatesNotificationForStudent() {
        CounselingAppointment appointment = appointment();
        appointment.changeStatus(CounselingAppointmentStatus.CONFIRMED, "상담을 진행하겠습니다.");
        when(notificationRepository.saveAndFlush(any(CounselingNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createForStatusChange(
                appointment,
                CounselingAppointmentStatus.PENDING,
                "상담을 진행하겠습니다."
        );

        ArgumentCaptor<CounselingNotification> captor = ArgumentCaptor.forClass(CounselingNotification.class);
        verify(notificationRepository).saveAndFlush(captor.capture());
        CounselingNotification saved = captor.getValue();
        assertThat(saved.getRecipient().getId()).isEqualTo(21L);
        assertThat(saved.getType()).isEqualTo(CounselingNotificationType.APPOINTMENT_CONFIRMED);
        assertThat(saved.getPreviousStatus()).isEqualTo(CounselingAppointmentStatus.PENDING);
        assertThat(saved.getNewStatus()).isEqualTo(CounselingAppointmentStatus.CONFIRMED);
        assertThat(saved.getDeduplicationKey()).hasSize(64);
    }

    @Test
    void studentCancellationCreatesNotificationForProfessor() {
        CounselingAppointment appointment = appointment();
        appointment.changeStatus(CounselingAppointmentStatus.CANCELLED, null);
        when(notificationRepository.saveAndFlush(any(CounselingNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createForStatusChange(appointment, CounselingAppointmentStatus.PENDING, null);

        ArgumentCaptor<CounselingNotification> captor = ArgumentCaptor.forClass(CounselingNotification.class);
        verify(notificationRepository).saveAndFlush(captor.capture());
        CounselingNotification saved = captor.getValue();
        assertThat(saved.getRecipient().getId()).isEqualTo(11L);
        assertThat(saved.getType()).isEqualTo(CounselingNotificationType.APPOINTMENT_CANCELLED);
    }

    @Test
    void sameStatusEventDoesNotCreateDuplicateNotification() {
        CounselingAppointment appointment = appointment();
        appointment.changeStatus(CounselingAppointmentStatus.REJECTED, "시간 조정이 필요합니다.");
        when(notificationRepository.existsByDeduplicationKey(any())).thenReturn(true);

        service.createForStatusChange(
                appointment,
                CounselingAppointmentStatus.PENDING,
                "시간 조정이 필요합니다."
        );

        verify(notificationRepository, never()).saveAndFlush(any());
    }

    @Test
    void recipientCanMarkNotificationReadRepeatedly() {
        CounselingNotification notification = notification();
        when(notificationRepository.findOwnedByIdForUpdate(71L, 21L)).thenReturn(Optional.of(notification));
        when(notificationRepository.saveAndFlush(notification)).thenReturn(notification);

        var first = service.markRead(71L, new CurrentUser(21L, "STUDENT"));
        LocalDateTime firstReadAt = first.readAt();
        var second = service.markRead(71L, new CurrentUser(21L, "STUDENT"));

        assertThat(first.read()).isTrue();
        assertThat(firstReadAt).isNotNull();
        assertThat(second.readAt()).isEqualTo(firstReadAt);
    }

    @Test
    void studentCanSearchOnlyOwnUnreadNotifications() {
        CounselingNotification notification = notification();
        when(notificationRepository.findByRecipientIdAndReadAtIsNull(eq(21L), any()))
                .thenReturn(new PageImpl<>(List.of(notification)));

        var result = service.search(
                new CounselingNotificationSearchRequestDTO(1, 20, true),
                new CurrentUser(21L, "STUDENT")
        );

        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.items()).singleElement()
                .satisfies(item -> {
                    assertThat(item.notificationId()).isEqualTo(71L);
                    assertThat(item.read()).isFalse();
                });
    }

    @Test
    void anotherUserCannotReadNotification() {
        when(notificationRepository.findOwnedByIdForUpdate(71L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(71L, new CurrentUser(99L, "STUDENT")))
                .isInstanceOf(CounselingNotificationNotFoundException.class);
    }

    private CounselingAppointment appointment() {
        User studentUser = user(21L, "상담 신청 학생");
        User professorUser = user(11L, "상담 담당 교수");
        Student student = mock(Student.class);
        Professor professor = mock(Professor.class);
        lenient().when(student.getUser()).thenReturn(studentUser);
        lenient().when(professor.getUser()).thenReturn(professorUser);

        CounselingAppointment appointment = CounselingAppointment.create(
                student,
                professor,
                LocalDateTime.of(2026, 9, 4, 14, 0),
                "진로 상담"
        );
        ReflectionTestUtils.setField(appointment, "id", 51L);
        return appointment;
    }

    private CounselingNotification notification() {
        CounselingAppointment appointment = appointment();
        appointment.changeStatus(CounselingAppointmentStatus.CONFIRMED, "상담을 진행하겠습니다.");
        CounselingNotification notification = CounselingNotification.create(
                appointment,
                appointment.getStudent().getUser(),
                CounselingNotificationType.APPOINTMENT_CONFIRMED,
                CounselingAppointmentStatus.PENDING,
                CounselingAppointmentStatus.CONFIRMED,
                "상담 예약이 승인되었습니다.",
                "a".repeat(64)
        );
        ReflectionTestUtils.setField(notification, "id", 71L);
        return notification;
    }

    private User user(Long id, String name) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(id);
        lenient().when(user.getName()).thenReturn(name);
        return user;
    }
}
