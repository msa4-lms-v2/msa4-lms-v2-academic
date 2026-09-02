package com.msa4lmsv2academic.domain.counseling.service;

import com.msa4lmsv2academic.domain.counseling.entity.CounselingAppointment;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingAppointmentStatus;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingNotification;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingNotificationType;
import com.msa4lmsv2academic.domain.counseling.repository.CounselingNotificationRepository;
import com.msa4lmsv2academic.domain.counseling.request.CounselingNotificationSearchRequestDTO;
import com.msa4lmsv2academic.domain.counseling.response.CounselingNotificationResponseDTO;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.global.error.CounselingAccessDeniedException;
import com.msa4lmsv2academic.global.error.CounselingNotificationNotFoundException;
import com.msa4lmsv2academic.global.error.InvalidCounselingRequestException;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CounselingNotificationService {

    private final CounselingNotificationRepository notificationRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void createForStatusChange(
            CounselingAppointment appointment,
            CounselingAppointmentStatus previousStatus,
            String professorNote
    ) {
        NotificationTarget target = notificationTarget(appointment, previousStatus);
        String deduplicationKey = deduplicationKey(
                appointment.getId(),
                target.recipient().getId(),
                target.type(),
                previousStatus,
                appointment.getStatus(),
                professorNote
        );
        if (notificationRepository.existsByDeduplicationKey(deduplicationKey)) {
            return;
        }

        notificationRepository.saveAndFlush(CounselingNotification.create(
                appointment,
                target.recipient(),
                target.type(),
                previousStatus,
                appointment.getStatus(),
                target.message(),
                deduplicationKey
        ));
    }

    public PageResponseDTO<CounselingNotificationResponseDTO> search(
            CounselingNotificationSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        validateParticipant(currentUser);
        int page = request.resolvedPage();
        int size = request.resolvedSize();
        PageRequest pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        Page<CounselingNotification> result = request.resolvedUnreadOnly()
                ? notificationRepository.findByRecipientIdAndReadAtIsNull(currentUser.id(), pageable)
                : notificationRepository.findByRecipientId(currentUser.id(), pageable);
        List<CounselingNotificationResponseDTO> items = result.getContent().stream()
                .map(CounselingNotificationResponseDTO::from)
                .toList();
        return new PageResponseDTO<>(items, result.getTotalElements(), page, size, result.hasNext());
    }

    @Transactional
    public CounselingNotificationResponseDTO markRead(Long notificationId, CurrentUser currentUser) {
        validateParticipant(currentUser);
        if (notificationId == null || notificationId <= 0) {
            throw new InvalidCounselingRequestException("notificationId는 양수여야 합니다.");
        }
        CounselingNotification notification = notificationRepository.findOwnedByIdForUpdate(
                        notificationId,
                        currentUser.id()
                )
                .orElseThrow(CounselingNotificationNotFoundException::new);
        notification.markRead(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        return CounselingNotificationResponseDTO.from(notificationRepository.saveAndFlush(notification));
    }

    private NotificationTarget notificationTarget(
            CounselingAppointment appointment,
            CounselingAppointmentStatus previousStatus
    ) {
        return switch (appointment.getStatus()) {
            case CONFIRMED -> new NotificationTarget(
                    appointment.getStudent().getUser(),
                    CounselingNotificationType.APPOINTMENT_CONFIRMED,
                    "상담 예약이 승인되었습니다."
            );
            case REJECTED -> new NotificationTarget(
                    appointment.getStudent().getUser(),
                    CounselingNotificationType.APPOINTMENT_REJECTED,
                    "상담 예약이 반려되었습니다."
            );
            case CANCELLED -> new NotificationTarget(
                    appointment.getProfessor().getUser(),
                    CounselingNotificationType.APPOINTMENT_CANCELLED,
                    "학생이 상담 예약을 취소했습니다."
            );
            case COMPLETED -> new NotificationTarget(
                    appointment.getStudent().getUser(),
                    previousStatus == CounselingAppointmentStatus.COMPLETED
                            ? CounselingNotificationType.PROFESSOR_RESPONSE_UPDATED
                            : CounselingNotificationType.COUNSELING_COMPLETED,
                    previousStatus == CounselingAppointmentStatus.COMPLETED
                            ? "교수 상담 답변이 변경되었습니다."
                            : "교수 상담 답변이 등록되었습니다."
            );
            case PENDING -> throw new InvalidCounselingRequestException("대기 상태 변경 알림은 생성할 수 없습니다.");
        };
    }

    private String deduplicationKey(
            Long appointmentId,
            Long recipientUserId,
            CounselingNotificationType type,
            CounselingAppointmentStatus previousStatus,
            CounselingAppointmentStatus newStatus,
            String professorNote
    ) {
        String source = String.join(
                "|",
                Objects.toString(appointmentId, ""),
                Objects.toString(recipientUserId, ""),
                type.name(),
                previousStatus.name(),
                newStatus.name(),
                Objects.toString(professorNote, "")
        );
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void validateParticipant(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null
                || !("STUDENT".equals(currentUser.role()) || "PROFESSOR".equals(currentUser.role()))) {
            throw new CounselingAccessDeniedException("상담 참여자만 알림을 사용할 수 있습니다.");
        }
    }

    private record NotificationTarget(
            User recipient,
            CounselingNotificationType type,
            String message
    ) {
    }
}
