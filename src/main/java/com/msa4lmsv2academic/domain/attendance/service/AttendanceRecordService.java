package com.msa4lmsv2academic.domain.attendance.service;

import com.msa4lmsv2academic.domain.attendance.entity.Attendance;
import com.msa4lmsv2academic.domain.attendance.repository.AttendanceRecordQueryRepository;
import com.msa4lmsv2academic.domain.attendance.repository.AttendanceRecordSearchResult;
import com.msa4lmsv2academic.domain.attendance.repository.AttendanceRepository;
import com.msa4lmsv2academic.domain.attendance.request.AttendanceRecordSearchRequestDTO;
import com.msa4lmsv2academic.domain.attendance.request.AttendanceRecordUpdateRequestDTO;
import com.msa4lmsv2academic.domain.attendance.response.AttendanceRecordResponseDTO;
import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.msa4lmsv2academic.global.error.AttendanceRecordAccessDeniedException;
import com.msa4lmsv2academic.global.error.AttendanceRecordNotFoundException;
import com.msa4lmsv2academic.global.error.AttendanceStateConflictException;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceRecordService {

    private static final String AUDIT_ACTION = "ATTENDANCE_RECORD_UPDATED";
    private static final String AUDIT_TARGET = "ATTENDANCE";

    private final AttendanceRecordQueryRepository attendanceRecordQueryRepository;
    private final AttendanceRepository attendanceRepository;
    private final AuditLogService auditLogService;

    public PageResponseDTO<AttendanceRecordResponseDTO> search(
            AttendanceRecordSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        UserRole role = validateAndResolveRole(currentUser, "출결 기록을 조회할 권한이 없습니다.");
        int page = request.resolvedPage();
        int size = request.resolvedSize();
        long offset = (long) (page - 1) * size;

        AttendanceRecordSearchResult result = attendanceRecordQueryRepository.search(
                currentUser.id(),
                role,
                request.classId(),
                request.enrollmentId(),
                request.fromDate(),
                request.toDate(),
                request.status(),
                offset,
                size
        );
        List<AttendanceRecordResponseDTO> items = result.items().stream()
                .map(AttendanceRecordResponseDTO::from)
                .toList();
        boolean hasNext = offset + items.size() < result.totalCount();
        return new PageResponseDTO<>(items, result.totalCount(), page, size, hasNext);
    }

    @Transactional
    public AttendanceRecordResponseDTO update(
            Long attendanceId,
            AttendanceRecordUpdateRequestDTO request,
            CurrentUser currentUser,
            String requestId,
            String ipAddress
    ) {
        UserRole role = validateAndResolveRole(currentUser, "출결 기록을 수정할 권한이 없습니다.");
        if (role == UserRole.STUDENT) {
            throw new AttendanceRecordAccessDeniedException("교수와 관리자만 출결 기록을 수정할 수 있습니다.");
        }

        Attendance attendance = attendanceRecordQueryRepository.findByIdForUpdate(attendanceId)
                .orElseThrow(AttendanceRecordNotFoundException::new);
        validateUpdateOwnership(attendance, currentUser, role);

        String normalizedRemarks = normalizeNullable(request.remarks());
        if (attendance.getStatus() == request.status()
                && Objects.equals(attendance.getRemarks(), normalizedRemarks)) {
            throw new AttendanceStateConflictException("변경할 출결 정보가 기존 값과 같습니다.");
        }

        Map<String, Object> beforeValue = snapshot(attendance);
        attendance.modify(request.status(), normalizedRemarks);
        Attendance saved = attendanceRepository.saveAndFlush(attendance);
        Map<String, Object> afterValue = snapshot(saved);

        auditLogService.record(
                currentUser.id(),
                AUDIT_ACTION,
                AUDIT_TARGET,
                saved.getId(),
                beforeValue,
                afterValue,
                request.reason().trim(),
                normalizeNullable(requestId),
                normalizeNullable(ipAddress)
        );
        return AttendanceRecordResponseDTO.from(saved);
    }

    private UserRole validateAndResolveRole(CurrentUser currentUser, String message) {
        if (currentUser == null || currentUser.id() == null || currentUser.role() == null) {
            throw new AttendanceRecordAccessDeniedException(message);
        }
        try {
            return UserRole.valueOf(currentUser.role());
        } catch (IllegalArgumentException exception) {
            throw new AttendanceRecordAccessDeniedException(message);
        }
    }

    private void validateUpdateOwnership(Attendance attendance, CurrentUser currentUser, UserRole role) {
        if (role == UserRole.PROFESSOR
                && !Objects.equals(
                        attendance.getEnrollment().getLecture().getProfessor().getUser().getId(),
                        currentUser.id()
                )) {
            throw new AttendanceRecordAccessDeniedException("본인이 담당하는 강의의 출결만 수정할 수 있습니다.");
        }
    }

    private Map<String, Object> snapshot(Attendance attendance) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("status", attendance.getStatus());
        value.put("remarks", attendance.getRemarks());
        value.put("modified", attendance.isModified());
        return value;
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
