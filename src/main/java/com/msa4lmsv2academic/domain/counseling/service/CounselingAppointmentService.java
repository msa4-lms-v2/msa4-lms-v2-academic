package com.msa4lmsv2academic.domain.counseling.service;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingAppointment;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingAppointmentStatus;
import com.msa4lmsv2academic.domain.counseling.entity.CounselorAvailability;
import com.msa4lmsv2academic.domain.counseling.repository.CounselingAppointmentRepository;
import com.msa4lmsv2academic.domain.counseling.repository.CounselingParticipantQueryRepository;
import com.msa4lmsv2academic.domain.counseling.repository.CounselorAvailabilityRepository;
import com.msa4lmsv2academic.domain.counseling.request.CounselingAppointmentCreateRequestDTO;
import com.msa4lmsv2academic.domain.counseling.request.CounselingAppointmentSearchRequestDTO;
import com.msa4lmsv2academic.domain.counseling.request.CounselingAppointmentStatusRequestDTO;
import com.msa4lmsv2academic.domain.counseling.response.CounselingAppointmentResponseDTO;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.global.error.CounselingAccessDeniedException;
import com.msa4lmsv2academic.global.error.CounselingAppointmentNotFoundException;
import com.msa4lmsv2academic.global.error.CounselingParticipantNotFoundException;
import com.msa4lmsv2academic.global.error.CounselingScheduleConflictException;
import com.msa4lmsv2academic.global.error.CounselingStatusConflictException;
import com.msa4lmsv2academic.global.error.InvalidCounselingRequestException;
import com.msa4lmsv2academic.global.response.PageRes;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CounselingAppointmentService {

    private static final Duration APPOINTMENT_DURATION = Duration.ofMinutes(30);
    private static final int MAX_TOPIC_LENGTH = 255;
    private static final int MAX_NOTE_LENGTH = 5000;
    private static final String AUDIT_TARGET_TYPE = "COUNSELING_APPOINTMENT";

    private final CounselingAppointmentRepository appointmentRepository;
    private final CounselorAvailabilityRepository availabilityRepository;
    private final CounselingParticipantQueryRepository participantQueryRepository;
    private final AuditLogService auditLogService;

    public PageRes<CounselingAppointmentResponseDTO> search(
            CounselingAppointmentSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        validateAuthenticatedRole(currentUser);
        int page = request.resolvedPage();
        int size = request.resolvedSize();
        Set<CounselingAppointmentStatus> statuses = request.status() == null
                ? EnumSet.allOf(CounselingAppointmentStatus.class)
                : EnumSet.of(request.status());
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "appointmentAt"));

        Page<CounselingAppointment> result = switch (currentUser.role()) {
            case "STUDENT" -> appointmentRepository.findByStudentUserIdAndStatusIn(
                    currentUser.id(), statuses, pageable
            );
            case "PROFESSOR" -> appointmentRepository.findByProfessorUserIdAndStatusIn(
                    currentUser.id(), statuses, pageable
            );
            case "ADMIN" -> appointmentRepository.findByStatusIn(statuses, pageable);
            default -> throw new CounselingAccessDeniedException("상담 예약을 조회할 권한이 없습니다.");
        };

        List<CounselingAppointmentResponseDTO> items = result.getContent().stream()
                .map(CounselingAppointmentResponseDTO::from)
                .toList();
        return new PageRes<>(items, result.getTotalElements(), page, size, result.hasNext());
    }

    public CounselingAppointmentResponseDTO get(Long appointmentId, CurrentUser currentUser) {
        validateAuthenticatedRole(currentUser);
        CounselingAppointment appointment = getAppointment(appointmentId);
        validateReadable(appointment, currentUser);
        return CounselingAppointmentResponseDTO.from(appointment);
    }

    @Transactional
    public CounselingAppointmentResponseDTO create(
            CounselingAppointmentCreateRequestDTO request,
            CurrentUser currentUser
    ) {
        validateStudent(currentUser);
        validateAppointmentAt(request.appointmentAt());

        Student student = participantQueryRepository.findStudentByUserIdForUpdate(currentUser.id())
                .orElseThrow(() -> new CounselingParticipantNotFoundException("학생 정보를 찾을 수 없습니다."));
        Professor professor = participantQueryRepository.findProfessorById(request.professorId())
                .orElseThrow(() -> new CounselingParticipantNotFoundException("교수 정보를 찾을 수 없습니다."));

        List<CounselorAvailability> slots = availabilityRepository.findBookableSlotsForUpdate(
                professor.getId(),
                request.appointmentAt().getDayOfWeek(),
                request.appointmentAt().toLocalDate()
        );
        if (slots.stream().noneMatch(slot -> containsAppointment(slot, request.appointmentAt()))) {
            throw new InvalidCounselingRequestException("교수가 공개한 상담 가능 시간에만 예약할 수 있습니다.");
        }

        if (appointmentRepository.existsByProfessorIdAndAppointmentAt(professor.getId(), request.appointmentAt())) {
            throw new CounselingScheduleConflictException("해당 교수의 상담 시간이 이미 예약되었습니다.");
        }
        if (appointmentRepository.existsByStudentIdAndAppointmentAt(student.getId(), request.appointmentAt())) {
            throw new CounselingScheduleConflictException("같은 시간에 다른 상담을 예약할 수 없습니다.");
        }

        CounselingAppointment appointment = CounselingAppointment.create(
                student,
                professor,
                request.appointmentAt(),
                normalizeNullable(request.topic(), MAX_TOPIC_LENGTH, "상담 주제")
        );
        try {
            return CounselingAppointmentResponseDTO.from(appointmentRepository.saveAndFlush(appointment));
        } catch (DataIntegrityViolationException exception) {
            throw new CounselingScheduleConflictException("해당 상담 시간이 이미 예약되었습니다.");
        }
    }

    @Transactional
    public CounselingAppointmentResponseDTO changeStatus(
            Long appointmentId,
            CounselingAppointmentStatusRequestDTO request,
            CurrentUser currentUser
    ) {
        validateAuthenticatedRole(currentUser);
        CounselingAppointment appointment = getAppointment(appointmentId);
        String note = normalizeNullable(request.professorNote(), MAX_NOTE_LENGTH, "교수 메모");

        if ("STUDENT".equals(currentUser.role())) {
            validateStudentStatusChange(appointment, request.status(), note, currentUser.id());
        } else if ("PROFESSOR".equals(currentUser.role())) {
            validateProfessorStatusChange(appointment, request.status(), note, currentUser.id());
        } else {
            throw new CounselingAccessDeniedException("상담 참여자만 예약 상태를 변경할 수 있습니다.");
        }
        Map<String, Object> beforeValue = auditSnapshot(appointment);

        try {
            appointment.changeStatus(request.status(), note);
        } catch (IllegalStateException exception) {
            throw new CounselingStatusConflictException("현재 상태에서 요청한 상담 상태로 변경할 수 없습니다.");
        }
        CounselingAppointment saved = appointmentRepository.saveAndFlush(appointment);
        auditLogService.record(
                currentUser.id(),
                auditAction(request.status()),
                AUDIT_TARGET_TYPE,
                saved.getId(),
                beforeValue,
                auditSnapshot(saved),
                auditReason(request.status(), note),
                null,
                null
        );
        return CounselingAppointmentResponseDTO.from(saved);
    }

    private CounselingAppointment getAppointment(Long appointmentId) {
        if (appointmentId == null || appointmentId <= 0) {
            throw new InvalidCounselingRequestException("appointmentId는 양수여야 합니다.");
        }
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(CounselingAppointmentNotFoundException::new);
    }

    private boolean containsAppointment(CounselorAvailability slot, LocalDateTime appointmentAt) {
        LocalTime start = LocalTime.parse(slot.getStartTime());
        LocalTime end = LocalTime.parse(slot.getEndTime());
        LocalTime appointmentStart = appointmentAt.toLocalTime();
        LocalTime appointmentEnd = appointmentStart.plus(APPOINTMENT_DURATION);
        return !appointmentStart.isBefore(start) && !appointmentEnd.isAfter(end);
    }

    private void validateAppointmentAt(LocalDateTime appointmentAt) {
        if (appointmentAt == null || !appointmentAt.isAfter(LocalDateTime.now())) {
            throw new InvalidCounselingRequestException("상담 예약 시각은 현재보다 미래여야 합니다.");
        }
        if (appointmentAt.getSecond() != 0 || appointmentAt.getNano() != 0
                || (appointmentAt.getMinute() != 0 && appointmentAt.getMinute() != 30)) {
            throw new InvalidCounselingRequestException("상담 예약은 정시 또는 30분에 시작해야 합니다.");
        }
    }

    private void validateReadable(CounselingAppointment appointment, CurrentUser currentUser) {
        boolean readable = switch (currentUser.role()) {
            case "STUDENT" -> appointment.getStudent().getUser().getId().equals(currentUser.id());
            case "PROFESSOR" -> appointment.getProfessor().getUser().getId().equals(currentUser.id());
            case "ADMIN" -> true;
            default -> false;
        };
        if (!readable) {
            throw new CounselingAccessDeniedException("본인의 상담 예약만 조회할 수 있습니다.");
        }
    }

    private void validateStudentStatusChange(
            CounselingAppointment appointment,
            CounselingAppointmentStatus requestedStatus,
            String professorNote,
            Long studentUserId
    ) {
        if (!appointment.getStudent().getUser().getId().equals(studentUserId)) {
            throw new CounselingAccessDeniedException("본인의 상담 예약만 취소할 수 있습니다.");
        }
        if (requestedStatus != CounselingAppointmentStatus.CANCELLED || professorNote != null) {
            throw new CounselingAccessDeniedException("학생은 본인 예약 취소만 요청할 수 있습니다.");
        }
    }

    private void validateProfessorStatusChange(
            CounselingAppointment appointment,
            CounselingAppointmentStatus requestedStatus,
            String professorNote,
            Long professorUserId
    ) {
        if (!appointment.getProfessor().getUser().getId().equals(professorUserId)) {
            throw new CounselingAccessDeniedException("본인에게 예약된 상담만 변경할 수 있습니다.");
        }
        if (requestedStatus == CounselingAppointmentStatus.CANCELLED) {
            throw new CounselingAccessDeniedException("상담 예약 취소는 신청 학생만 할 수 있습니다.");
        }
        if (!(requestedStatus == CounselingAppointmentStatus.CONFIRMED
                || requestedStatus == CounselingAppointmentStatus.REJECTED
                || requestedStatus == CounselingAppointmentStatus.COMPLETED)) {
            throw new CounselingAccessDeniedException("교수는 상담 승인·반려·완료만 처리할 수 있습니다.");
        }
        if (requestedStatus == CounselingAppointmentStatus.CONFIRMED && professorNote == null) {
            throw new InvalidCounselingRequestException("상담 승인 사유는 필수입니다.");
        }
        if (requestedStatus == CounselingAppointmentStatus.REJECTED && professorNote == null) {
            throw new InvalidCounselingRequestException("상담 반려 사유는 필수입니다.");
        }
        if (requestedStatus == CounselingAppointmentStatus.COMPLETED && professorNote == null) {
            throw new InvalidCounselingRequestException("온라인 상담 완료 시 교수 답변은 필수입니다.");
        }
    }

    private String auditAction(CounselingAppointmentStatus status) {
        return switch (status) {
            case CONFIRMED -> "COUNSELING_APPOINTMENT_CONFIRMED";
            case REJECTED -> "COUNSELING_APPOINTMENT_REJECTED";
            case CANCELLED -> "COUNSELING_APPOINTMENT_CANCELLED";
            case COMPLETED -> "COUNSELING_APPOINTMENT_COMPLETED";
            case PENDING -> throw new InvalidCounselingRequestException("대기 상태로 되돌릴 수 없습니다.");
        };
    }

    private String auditReason(CounselingAppointmentStatus status, String professorNote) {
        return status == CounselingAppointmentStatus.CONFIRMED
                || status == CounselingAppointmentStatus.REJECTED
                || status == CounselingAppointmentStatus.COMPLETED
                ? professorNote
                : null;
    }

    private Map<String, Object> auditSnapshot(CounselingAppointment appointment) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("status", appointment.getStatus().name());
        snapshot.put("studentId", appointment.getStudent().getId());
        snapshot.put("professorId", appointment.getProfessor().getId());
        snapshot.put("appointmentAt", appointment.getAppointmentAt().toString());
        snapshot.put("topic", appointment.getTopic());
        snapshot.put("professorNote", appointment.getProfessorNote());
        return snapshot;
    }

    private String normalizeNullable(String value, int maxLength, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new InvalidCounselingRequestException(fieldName + "는 " + maxLength + "자 이하여야 합니다.");
        }
        return normalized;
    }

    private void validateAuthenticatedRole(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null
                || !("STUDENT".equals(currentUser.role())
                || "PROFESSOR".equals(currentUser.role())
                || "ADMIN".equals(currentUser.role()))) {
            throw new CounselingAccessDeniedException("상담 예약을 사용할 권한이 없습니다.");
        }
    }

    private void validateStudent(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || !"STUDENT".equals(currentUser.role())) {
            throw new CounselingAccessDeniedException("학생만 상담을 예약할 수 있습니다.");
        }
    }
}
