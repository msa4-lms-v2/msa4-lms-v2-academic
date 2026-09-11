package com.msa4lmsv2academic.domain.attendance.service;

import com.msa4lmsv2academic.domain.attendance.entity.ExcuseRequest;
import com.msa4lmsv2academic.domain.attendance.entity.ExcuseRequestStatus;
import com.msa4lmsv2academic.domain.attendance.entity.Attendance;
import com.msa4lmsv2academic.domain.attendance.entity.AttendanceSessionStatus;
import com.msa4lmsv2academic.domain.attendance.entity.AttendanceStatus;
import com.msa4lmsv2academic.domain.attendance.repository.AttendanceRepository;
import com.msa4lmsv2academic.domain.attendance.repository.AttendanceSessionRepository;
import com.msa4lmsv2academic.domain.attendance.repository.ExcuseRequestRepository;
import com.msa4lmsv2academic.domain.attendance.request.ExcuseReviewRequestDTO;
import com.msa4lmsv2academic.domain.attendance.response.ExcuseRequestResponseDTO;
import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.global.error.ExcuseRequestAccessDeniedException;
import com.msa4lmsv2academic.global.error.ExcuseRequestNotFoundException;
import com.msa4lmsv2academic.global.error.ExcuseReviewConflictException;
import com.msa4lmsv2academic.global.error.InvalidExcuseRequestException;
import com.msa4lmsv2academic.global.idempotency.AcademicIdempotencyKey;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExcuseReviewService {

    private static final String AUDIT_TARGET_TYPE = "EXCUSE_REQUEST";
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;
    private static final int MAX_AUDIT_REASON_LENGTH = 255;

    private final ExcuseRequestRepository excuseRequestRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final ExcuseReviewIdempotencyService idempotencyService;
    private final AuditLogService auditLogService;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public GlobalResponseDTO<ExcuseRequestResponseDTO> review(
            Long requestId,
            ExcuseReviewRequestDTO reviewRequest,
            String idempotencyKey,
            CurrentUser currentUser,
            String traceRequestId,
            String ipAddress
    ) {
        validateRequest(requestId, reviewRequest, idempotencyKey, currentUser);
        ExcuseRequest excuseRequest = excuseRequestRepository.findDetailForUpdate(requestId)
                .orElseThrow(ExcuseRequestNotFoundException::new);
        validateProfessorOwner(excuseRequest, currentUser.id());

        String requestHash = idempotencyService.hash(requestId, reviewRequest);
        LocalDateTime now = LocalDateTime.now();
        var replay = idempotencyService.replay(idempotencyKey, currentUser.id(), requestHash, now);
        if (replay.isPresent()) {
            return replay.orElseThrow();
        }
        AcademicIdempotencyKey reserved = idempotencyService.reserve(
                idempotencyKey,
                currentUser.id(),
                requestHash,
                now
        );

        Map<String, Object> beforeValue = auditSnapshot(excuseRequest);
        String action;
        String rejectReason = null;
        Attendance approvedAttendance = null;
        try {
            if (reviewRequest.status() == ExcuseRequestStatus.APPROVED) {
                approvedAttendance = resolveAttendanceForApproval(excuseRequest);
                beforeValue.put("attendanceId", approvedAttendance.getId());
                beforeValue.put("attendanceStatus", approvedAttendance.getStatus().name());
                excuseRequest.approve();
                approvedAttendance.markExcused();
                action = "EXCUSE_APPROVED";
            } else {
                rejectReason = reviewRequest.rejectReason().trim();
                excuseRequest.reject(rejectReason);
                action = "EXCUSE_REJECTED";
            }
        } catch (IllegalStateException exception) {
            throw new ExcuseReviewConflictException(exception.getMessage());
        } catch (IllegalArgumentException exception) {
            throw new InvalidExcuseRequestException(exception.getMessage());
        }

        excuseRequestRepository.saveAndFlush(excuseRequest);
        Map<String, Object> afterValue = auditSnapshot(excuseRequest);
        if (approvedAttendance != null) {
            afterValue.put("attendanceId", approvedAttendance.getId());
            afterValue.put("attendanceStatus", approvedAttendance.getStatus().name());
        }
        auditLogService.record(
                currentUser.id(),
                action,
                AUDIT_TARGET_TYPE,
                excuseRequest.getId(),
                beforeValue,
                afterValue,
                auditReason(rejectReason),
                traceRequestId,
                ipAddress
        );

        GlobalResponseDTO<ExcuseRequestResponseDTO> response = GlobalResponseDTO.success(
                ExcuseRequestResponseDTO.from(excuseRequest)
        );
        idempotencyService.complete(reserved, response);
        return response;
    }

    private void validateRequest(
            Long requestId,
            ExcuseReviewRequestDTO request,
            String idempotencyKey,
            CurrentUser currentUser
    ) {
        if (currentUser == null || currentUser.id() == null || !"PROFESSOR".equals(currentUser.role())) {
            throw new ExcuseRequestAccessDeniedException("교수만 공결 신청을 승인하거나 반려할 수 있습니다.");
        }
        if (requestId == null || requestId <= 0 || request == null
                || (request.status() != ExcuseRequestStatus.APPROVED
                && request.status() != ExcuseRequestStatus.REJECTED)
                || (request.status() == ExcuseRequestStatus.REJECTED
                && (request.rejectReason() == null || request.rejectReason().isBlank()))
                || (request.rejectReason() != null && request.rejectReason().length() > 500)
                || idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH
                || idempotencyKey.chars().anyMatch(Character::isWhitespace)) {
            throw new InvalidExcuseRequestException("공결 처리 상태, 반려 사유와 멱등성 키를 확인해 주세요.");
        }
    }

    private void validateProfessorOwner(ExcuseRequest request, Long professorUserId) {
        Long ownerUserId = request.getEnrollment().getLecture().getProfessor().getUser().getId();
        if (!ownerUserId.equals(professorUserId)) {
            throw new ExcuseRequestAccessDeniedException("본인이 담당하는 강의의 공결 신청만 처리할 수 있습니다.");
        }
    }

    private Attendance resolveAttendanceForApproval(ExcuseRequest request) {
        var session = attendanceSessionRepository.findByLectureIdAndSessionDateAndPeriodForUpdate(
                        request.getEnrollment().getLecture().getId(),
                        request.getLectureDate(),
                        (int) request.getPeriod())
                .orElseThrow(() -> new ExcuseReviewConflictException("공결 승인 대상 출석 세션이 없습니다."));
        if (session.getStatus() != AttendanceSessionStatus.CLOSED) {
            throw new ExcuseReviewConflictException("출석 세션이 종료된 뒤에만 공결을 승인할 수 있습니다.");
        }
        Attendance attendance = attendanceRepository.findBySessionIdAndEnrollmentIdForUpdate(session.getId(), request.getEnrollment().getId())
                .orElseThrow(() -> new ExcuseReviewConflictException("공결 승인 대상 출결 기록이 없습니다."));
        if (attendance.getStatus() == AttendanceStatus.PRESENT) {
            throw new ExcuseReviewConflictException("이미 출석 처리된 학생의 공결은 승인할 수 없습니다.");
        }
        if (attendance.getStatus() == AttendanceStatus.EXCUSED) {
            throw new ExcuseReviewConflictException("이미 공결 처리된 출결 기록입니다.");
        }
        return attendance;
    }

    private Map<String, Object> auditSnapshot(ExcuseRequest request) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("status", request.getStatus().name());
        snapshot.put("rejectReason", request.getRejectReason());
        snapshot.put("enrollmentId", request.getEnrollment().getId());
        snapshot.put("lectureDate", request.getLectureDate().toString());
        snapshot.put("period", request.getPeriod());
        return snapshot;
    }

    private String auditReason(String reason) {
        if (reason == null || reason.length() <= MAX_AUDIT_REASON_LENGTH) {
            return reason;
        }
        return reason.substring(0, MAX_AUDIT_REASON_LENGTH);
    }
}
