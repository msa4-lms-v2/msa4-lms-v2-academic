package com.msa4lmsv2academic.domain.grade.service;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.enrollment.entity.GradeStatus;
import com.msa4lmsv2academic.domain.grade.repository.GradeManagementRepository;
import com.msa4lmsv2academic.domain.grade.request.GradeFinalizeRequestDTO;
import com.msa4lmsv2academic.domain.grade.request.GradeSaveRequestDTO;
import com.msa4lmsv2academic.domain.grade.request.GradeScoreRequestDTO;
import com.msa4lmsv2academic.domain.grade.response.GradeClassResponseDTO;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.lecture.repository.LectureRepository;
import com.msa4lmsv2academic.global.error.GradeManagementAccessDeniedException;
import com.msa4lmsv2academic.global.error.GradeManagementConflictException;
import com.msa4lmsv2academic.global.error.GradeManagementNotFoundException;
import com.msa4lmsv2academic.global.error.InvalidGradeManagementRequestException;
import com.msa4lmsv2academic.global.idempotency.AcademicIdempotencyKey;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradeManagementService {

    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;
    private static final String AUDIT_TARGET_TYPE = "ENROLLMENT_GRADE";

    private final LectureRepository lectureRepository;
    private final GradeManagementRepository gradeRepository;
    private final GradeCalculationPolicy calculationPolicy;
    private final GradeIdempotencyService idempotencyService;
    private final AuditLogService auditLogService;

    public GradeClassResponseDTO getGrades(Long classId, CurrentUser currentUser) {
        validateClassIdAndUser(classId, currentUser);
        Lecture lecture = lectureRepository.findSyllabusById(classId)
                .orElseThrow(() -> new GradeManagementNotFoundException("강의를 찾을 수 없습니다."));
        validateOwnerOrAdmin(lecture, currentUser);
        return GradeClassResponseDTO.from(lecture, gradeRepository.findActiveGrades(classId));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public GlobalResponseDTO<GradeClassResponseDTO> createDraft(
            GradeSaveRequestDTO request, String idempotencyKey, CurrentUser currentUser,
            String traceRequestId, String ipAddress
    ) {
        return save(request, idempotencyKey, currentUser, traceRequestId, ipAddress, true);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public GlobalResponseDTO<GradeClassResponseDTO> updateDraft(
            GradeSaveRequestDTO request, String idempotencyKey, CurrentUser currentUser,
            String traceRequestId, String ipAddress
    ) {
        return save(request, idempotencyKey, currentUser, traceRequestId, ipAddress, false);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public GlobalResponseDTO<GradeClassResponseDTO> finalizeGrades(
            Long classId, GradeFinalizeRequestDTO request, String idempotencyKey,
            CurrentUser currentUser, String traceRequestId, String ipAddress
    ) {
        validateClassIdAndUser(classId, currentUser);
        validateIdempotencyKey(idempotencyKey);
        if (request == null || request.status() != GradeStatus.OPENED) {
            throw new InvalidGradeManagementRequestException("성적 확정 상태는 OPENED만 사용할 수 있습니다.");
        }

        String hash = idempotencyService.hash(new FinalizeFingerprint(classId, request));
        LocalDateTime now = LocalDateTime.now();
        var replay = idempotencyService.replay(
                idempotencyKey, currentUser.id(), GradeIdempotencyService.FINALIZE_ENDPOINT, hash, now
        );
        if (replay.isPresent()) {
            return replay.orElseThrow();
        }

        Lecture lecture = lectureRepository.findSyllabusByIdForUpdate(classId)
                .orElseThrow(() -> new GradeManagementNotFoundException("강의를 찾을 수 없습니다."));
        validateOwnerOrAdmin(lecture, currentUser);
        List<Enrollment> enrollments = gradeRepository.findActiveGradesForUpdate(classId);
        if (enrollments.isEmpty()) {
            throw new InvalidGradeManagementRequestException("확정할 수강생 성적이 없습니다.");
        }
        if (enrollments.stream().anyMatch(enrollment -> enrollment.getGradeStatus() != GradeStatus.DRAFT)) {
            throw new GradeManagementConflictException("이미 확정된 성적이 포함되어 있습니다.");
        }
        if (enrollments.stream().anyMatch(enrollment -> !enrollment.hasCompleteGradeScores()
                || enrollment.getTotalScore() == null || enrollment.getLetterGrade() == null)) {
            throw new InvalidGradeManagementRequestException("모든 활성 수강생의 네 가지 점수를 입력해야 확정할 수 있습니다.");
        }

        AcademicIdempotencyKey reserved = idempotencyService.reserve(
                idempotencyKey, currentUser.id(), GradeIdempotencyService.FINALIZE_ENDPOINT, hash, now
        );
        for (Enrollment enrollment : enrollments) {
            Map<String, Object> before = auditSnapshot(enrollment);
            try {
                enrollment.openGrade();
            } catch (IllegalStateException exception) {
                throw new GradeManagementConflictException(exception.getMessage());
            }
            auditLogService.record(
                    currentUser.id(), "GRADE_FINALIZED", AUDIT_TARGET_TYPE, enrollment.getId(),
                    before, auditSnapshot(enrollment), "성적 확정", traceRequestId, ipAddress
            );
        }
        gradeRepository.flush();
        GlobalResponseDTO<GradeClassResponseDTO> response = GlobalResponseDTO.success(
                GradeClassResponseDTO.from(lecture, enrollments)
        );
        idempotencyService.complete(reserved, response);
        return response;
    }

    private GlobalResponseDTO<GradeClassResponseDTO> save(
            GradeSaveRequestDTO request, String idempotencyKey, CurrentUser currentUser,
            String traceRequestId, String ipAddress, boolean initialInput
    ) {
        validateSaveRequest(request, idempotencyKey, currentUser);
        String endpoint = initialInput
                ? GradeIdempotencyService.CREATE_ENDPOINT : GradeIdempotencyService.UPDATE_ENDPOINT;
        String hash = idempotencyService.hash(request);
        LocalDateTime now = LocalDateTime.now();
        var replay = idempotencyService.replay(idempotencyKey, currentUser.id(), endpoint, hash, now);
        if (replay.isPresent()) {
            return replay.orElseThrow();
        }

        Lecture lecture = lectureRepository.findSyllabusByIdForUpdate(request.classId())
                .orElseThrow(() -> new GradeManagementNotFoundException("강의를 찾을 수 없습니다."));
        validateOwnerOrAdmin(lecture, currentUser);
        List<Enrollment> enrollments = gradeRepository.findActiveGradesForUpdate(request.classId());
        Map<Long, Enrollment> enrollmentById = new HashMap<>();
        enrollments.forEach(enrollment -> enrollmentById.put(enrollment.getId(), enrollment));

        List<Enrollment> targets = request.grades().stream()
                .map(grade -> findTargetEnrollment(grade.enrollmentId(), enrollmentById))
                .toList();
        if (targets.stream().anyMatch(enrollment -> enrollment.getGradeStatus() != GradeStatus.DRAFT)) {
            throw new GradeManagementConflictException("확정된 성적은 임시저장 방식으로 수정할 수 없습니다.");
        }
        if (initialInput && targets.stream().anyMatch(Enrollment::hasGradeInput)) {
            throw new GradeManagementConflictException("이미 입력된 성적은 수정 API를 이용해 주세요.");
        }
        if (!initialInput && targets.stream().anyMatch(enrollment -> !enrollment.hasGradeInput())) {
            throw new GradeManagementConflictException("최초 성적 입력은 임시저장 API를 이용해 주세요.");
        }

        AcademicIdempotencyKey reserved = idempotencyService.reserve(
                idempotencyKey, currentUser.id(), endpoint, hash, now
        );
        boolean changed = false;
        for (int index = 0; index < request.grades().size(); index++) {
            GradeScoreRequestDTO grade = request.grades().get(index);
            Enrollment enrollment = targets.get(index);
            Map<String, Object> before = auditSnapshot(enrollment);
            var calculated = calculationPolicy.calculate(lecture, grade);
            try {
                enrollment.saveDraftGrade(
                        grade.midtermScore(), grade.finalScore(), grade.assignmentScore(),
                        grade.attendanceScore(), calculated.totalScore(), calculated.letterGrade()
                );
            } catch (IllegalStateException exception) {
                throw new GradeManagementConflictException(exception.getMessage());
            }
            Map<String, Object> after = auditSnapshot(enrollment);
            if (!before.equals(after)) {
                changed = true;
                auditLogService.record(
                        currentUser.id(), initialInput ? "GRADE_DRAFT_CREATED" : "GRADE_DRAFT_UPDATED",
                        AUDIT_TARGET_TYPE, enrollment.getId(), before, after,
                        initialInput ? "성적 임시저장" : "임시저장 성적 수정", traceRequestId, ipAddress
                );
            }
        }
        if (!changed) {
            throw new GradeManagementConflictException("저장된 성적과 동일한 중복 요청입니다.");
        }

        gradeRepository.flush();
        GlobalResponseDTO<GradeClassResponseDTO> response = GlobalResponseDTO.success(
                GradeClassResponseDTO.from(lecture, enrollments)
        );
        idempotencyService.complete(reserved, response);
        return response;
    }

    private void validateSaveRequest(
            GradeSaveRequestDTO request, String idempotencyKey, CurrentUser currentUser
    ) {
        if (request == null) {
            throw new InvalidGradeManagementRequestException("성적 입력값을 확인해 주세요.");
        }
        validateClassIdAndUser(request.classId(), currentUser);
        validateIdempotencyKey(idempotencyKey);
        if (request.grades() == null || request.grades().isEmpty()
                || request.grades().stream().anyMatch(grade -> grade == null || !grade.hasAnyScore())) {
            throw new InvalidGradeManagementRequestException("수강생별로 한 개 이상의 점수를 입력해 주세요.");
        }
        Set<Long> ids = new HashSet<>();
        if (request.grades().stream().anyMatch(grade -> !ids.add(grade.enrollmentId()))) {
            throw new GradeManagementConflictException("한 요청에 같은 수강 ID가 중복되어 있습니다.");
        }
    }

    private void validateClassIdAndUser(Long classId, CurrentUser currentUser) {
        if (classId == null || classId <= 0) {
            throw new InvalidGradeManagementRequestException("classId는 양수여야 합니다.");
        }
        if (currentUser == null || currentUser.id() == null
                || !("PROFESSOR".equals(currentUser.role()) || "ADMIN".equals(currentUser.role()))) {
            throw new GradeManagementAccessDeniedException("교수 또는 관리자만 성적을 관리할 수 있습니다.");
        }
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.length() > MAX_IDEMPOTENCY_KEY_LENGTH
                || key.chars().anyMatch(Character::isWhitespace)) {
            throw new InvalidGradeManagementRequestException("멱등성 키는 공백 없는 1~100자여야 합니다.");
        }
    }

    private void validateOwnerOrAdmin(Lecture lecture, CurrentUser currentUser) {
        if (!currentUser.isAdmin()
                && !lecture.getProfessor().getUser().getId().equals(currentUser.id())) {
            throw new GradeManagementAccessDeniedException("본인이 담당하는 강의의 성적만 관리할 수 있습니다.");
        }
    }

    private Enrollment findTargetEnrollment(Long enrollmentId, Map<Long, Enrollment> enrollmentById) {
        Enrollment enrollment = enrollmentById.get(enrollmentId);
        if (enrollment == null) {
            throw new GradeManagementNotFoundException("해당 강의의 활성 수강 정보를 찾을 수 없습니다.");
        }
        return enrollment;
    }

    private Map<String, Object> auditSnapshot(Enrollment enrollment) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("gradeStatus", enrollment.getGradeStatus().name());
        snapshot.put("midtermScore", number(enrollment.getMidtermScore()));
        snapshot.put("finalScore", number(enrollment.getFinalScore()));
        snapshot.put("assignmentScore", number(enrollment.getAssignmentScore()));
        snapshot.put("attendanceScore", number(enrollment.getAttendanceScore()));
        snapshot.put("totalScore", number(enrollment.getTotalScore()));
        snapshot.put("letterGrade", enrollment.getLetterGrade());
        return snapshot;
    }

    private String number(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    private record FinalizeFingerprint(Long classId, GradeFinalizeRequestDTO request) {
    }
}
