package com.msa4lmsv2academic.domain.grade.service;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.enrollment.entity.GradeStatus;
import com.msa4lmsv2academic.domain.grade.entity.GradeCorrectionHistory;
import com.msa4lmsv2academic.domain.grade.repository.GradeCorrectionHistoryRepository;
import com.msa4lmsv2academic.domain.grade.repository.GradeManagementRepository;
import com.msa4lmsv2academic.domain.grade.request.GradeCorrectionHistorySearchRequestDTO;
import com.msa4lmsv2academic.domain.grade.request.GradeCorrectionItemRequestDTO;
import com.msa4lmsv2academic.domain.grade.request.GradeCorrectionRequestDTO;
import com.msa4lmsv2academic.domain.grade.response.GradeClassResponseDTO;
import com.msa4lmsv2academic.domain.grade.response.GradeCorrectionHistoryResponseDTO;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.lecture.repository.LectureRepository;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.repository.UserRepository;
import com.msa4lmsv2academic.global.error.GradeManagementAccessDeniedException;
import com.msa4lmsv2academic.global.error.GradeManagementConflictException;
import com.msa4lmsv2academic.global.error.GradeManagementNotFoundException;
import com.msa4lmsv2academic.global.error.InvalidGradeManagementRequestException;
import com.msa4lmsv2academic.global.idempotency.AcademicIdempotencyKey;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradeCorrectionService {

    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;
    private static final String AUDIT_TARGET_TYPE = "ENROLLMENT_GRADE";
    private static final String MIDTERM_SCORE = "MIDTERM_SCORE";
    private static final String FINAL_SCORE = "FINAL_SCORE";
    private static final String ASSIGNMENT_SCORE = "ASSIGNMENT_SCORE";
    private static final String ATTENDANCE_SCORE = "ATTENDANCE_SCORE";
    private static final String TOTAL_SCORE = "TOTAL_SCORE";
    private static final String LETTER_GRADE = "LETTER_GRADE";

    private final LectureRepository lectureRepository;
    private final GradeManagementRepository gradeRepository;
    private final GradeCorrectionHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final GradeCalculationPolicy calculationPolicy;
    private final GradeIdempotencyService idempotencyService;
    private final AuditLogService auditLogService;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public GlobalResponseDTO<GradeClassResponseDTO> correct(
            GradeCorrectionRequestDTO request,
            String idempotencyKey,
            CurrentUser currentUser,
            String traceRequestId,
            String ipAddress
    ) {
        validateCorrectionRequest(request, idempotencyKey, currentUser);
        String hash = idempotencyService.hash(request);
        LocalDateTime now = LocalDateTime.now();
        var replay = idempotencyService.replay(
                idempotencyKey,
                currentUser.id(),
                GradeIdempotencyService.CORRECTION_ENDPOINT,
                hash,
                now
        );
        if (replay.isPresent()) {
            return replay.orElseThrow();
        }

        Lecture lecture = lectureRepository.findSyllabusByIdForUpdate(request.classId())
                .orElseThrow(() -> new GradeManagementNotFoundException("강의를 찾을 수 없습니다."));
        validateOwnerOrAdmin(lecture, currentUser);

        List<Enrollment> enrollments = gradeRepository.findActiveGradesForUpdate(request.classId());
        Map<Long, Enrollment> enrollmentById = new HashMap<>();
        enrollments.forEach(enrollment -> enrollmentById.put(enrollment.getId(), enrollment));
        List<Enrollment> targets = request.corrections().stream()
                .map(correction -> findTargetEnrollment(correction.enrollmentId(), enrollmentById))
                .toList();
        if (targets.stream().anyMatch(enrollment -> enrollment.getGradeStatus() != GradeStatus.OPENED)) {
            throw new GradeManagementConflictException("공개된 성적만 정정할 수 있습니다.");
        }

        User changedBy = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new GradeManagementNotFoundException("처리자 정보를 찾을 수 없습니다."));
        List<CorrectionPlan> plans = buildPlans(lecture, request.corrections(), targets);
        if (plans.stream().allMatch(plan -> plan.histories().isEmpty())) {
            throw new GradeManagementConflictException("기존 성적과 동일하여 정정할 내용이 없습니다.");
        }

        AcademicIdempotencyKey reserved = idempotencyService.reserve(
                idempotencyKey,
                currentUser.id(),
                GradeIdempotencyService.CORRECTION_ENDPOINT,
                hash,
                now
        );
        for (CorrectionPlan plan : plans) {
            if (plan.histories().isEmpty()) {
                continue;
            }
            Map<String, Object> before = auditSnapshot(plan.enrollment());
            GradeCorrectionItemRequestDTO correction = plan.correction();
            try {
                plan.enrollment().correctOpenedGrade(
                        correction.midtermScore(), correction.finalScore(),
                        correction.assignmentScore(), correction.attendanceScore(),
                        plan.totalScore(), plan.letterGrade()
                );
            } catch (IllegalStateException exception) {
                throw new GradeManagementConflictException(exception.getMessage());
            } catch (IllegalArgumentException exception) {
                throw new InvalidGradeManagementRequestException(exception.getMessage());
            }
            historyRepository.saveAll(plan.histories().stream()
                    .map(change -> GradeCorrectionHistory.recordCorrection(
                            plan.enrollment(), change.field(), change.before(), change.after(),
                            changedBy, correction.normalizedReason(), now
                    ))
                    .toList());
            auditLogService.record(
                    currentUser.id(), "GRADE_CORRECTED", AUDIT_TARGET_TYPE, plan.enrollment().getId(),
                    before, auditSnapshot(plan.enrollment()), correction.normalizedReason(),
                    traceRequestId, ipAddress
            );
        }

        gradeRepository.flush();
        historyRepository.flush();
        GlobalResponseDTO<GradeClassResponseDTO> response = GlobalResponseDTO.success(
                GradeClassResponseDTO.from(lecture, enrollments)
        );
        idempotencyService.complete(reserved, response);
        return response;
    }

    public Page<GradeCorrectionHistoryResponseDTO> getHistories(
            GradeCorrectionHistorySearchRequestDTO request,
            Pageable pageable,
            CurrentUser currentUser
    ) {
        validateClassIdAndUser(request == null ? null : request.classId(), currentUser);
        Lecture lecture = lectureRepository.findSyllabusById(request.classId())
                .orElseThrow(() -> new GradeManagementNotFoundException("강의를 찾을 수 없습니다."));
        validateOwnerOrAdmin(lecture, currentUser);
        return historyRepository.findByClassId(request.classId(), pageable)
                .map(GradeCorrectionHistoryResponseDTO::from);
    }

    private List<CorrectionPlan> buildPlans(
            Lecture lecture,
            List<GradeCorrectionItemRequestDTO> corrections,
            List<Enrollment> targets
    ) {
        List<CorrectionPlan> plans = new ArrayList<>();
        for (int index = 0; index < corrections.size(); index++) {
            GradeCorrectionItemRequestDTO correction = corrections.get(index);
            Enrollment enrollment = targets.get(index);
            var calculated = calculationPolicy.calculate(lecture, correction.toGradeScoreRequest());
            List<FieldChange> changes = changes(enrollment, correction,
                    calculated.totalScore(), calculated.letterGrade());
            plans.add(new CorrectionPlan(
                    enrollment, correction, calculated.totalScore(), calculated.letterGrade(), changes
            ));
        }
        return plans;
    }

    private List<FieldChange> changes(
            Enrollment enrollment,
            GradeCorrectionItemRequestDTO correction,
            BigDecimal totalScore,
            String letterGrade
    ) {
        List<FieldChange> changes = new ArrayList<>();
        addNumberChange(changes, MIDTERM_SCORE, enrollment.getMidtermScore(), correction.midtermScore());
        addNumberChange(changes, FINAL_SCORE, enrollment.getFinalScore(), correction.finalScore());
        addNumberChange(changes, ASSIGNMENT_SCORE, enrollment.getAssignmentScore(), correction.assignmentScore());
        addNumberChange(changes, ATTENDANCE_SCORE, enrollment.getAttendanceScore(), correction.attendanceScore());
        addNumberChange(changes, TOTAL_SCORE, enrollment.getTotalScore(), totalScore);
        addTextChange(changes, LETTER_GRADE, enrollment.getLetterGrade(), letterGrade);
        return changes;
    }

    private void addNumberChange(
            List<FieldChange> changes,
            String field,
            BigDecimal before,
            BigDecimal after
    ) {
        if (before == null || after == null || before.compareTo(after) != 0) {
            changes.add(new FieldChange(field, number(before), number(after)));
        }
    }

    private void addTextChange(List<FieldChange> changes, String field, String before, String after) {
        if (!Objects.equals(before, after)) {
            changes.add(new FieldChange(field, before, after));
        }
    }

    private void validateCorrectionRequest(
            GradeCorrectionRequestDTO request,
            String idempotencyKey,
            CurrentUser currentUser
    ) {
        if (request == null || request.corrections() == null || request.corrections().isEmpty()
                || request.corrections().stream().anyMatch(correction -> correction == null
                || !correction.hasCompleteScoresAndReason())) {
            throw new InvalidGradeManagementRequestException("정정할 성적을 한 건 이상 입력해 주세요.");
        }
        validateClassIdAndUser(request.classId(), currentUser);
        validateIdempotencyKey(idempotencyKey);
        Set<Long> enrollmentIds = new HashSet<>();
        if (request.corrections().stream()
                .anyMatch(correction -> !enrollmentIds.add(correction.enrollmentId()))) {
            throw new GradeManagementConflictException("한 요청에 같은 수강 ID가 중복되어 있습니다.");
        }
    }

    private void validateClassIdAndUser(Long classId, CurrentUser currentUser) {
        if (classId == null || classId <= 0) {
            throw new InvalidGradeManagementRequestException("classId는 양수여야 합니다.");
        }
        if (currentUser == null || currentUser.id() == null
                || !("PROFESSOR".equals(currentUser.role()) || "ADMIN".equals(currentUser.role()))) {
            throw new GradeManagementAccessDeniedException("교수 또는 관리자만 성적 정정 이력을 관리할 수 있습니다.");
        }
    }

    private void validateOwnerOrAdmin(Lecture lecture, CurrentUser currentUser) {
        if (!currentUser.isAdmin()
                && !lecture.getProfessor().getUser().getId().equals(currentUser.id())) {
            throw new GradeManagementAccessDeniedException("본인이 담당하는 강의의 성적만 정정할 수 있습니다.");
        }
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.length() > MAX_IDEMPOTENCY_KEY_LENGTH
                || key.chars().anyMatch(Character::isWhitespace)) {
            throw new InvalidGradeManagementRequestException("멱등성 키는 공백 없는 1~100자여야 합니다.");
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
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private record FieldChange(String field, String before, String after) {
    }

    private record CorrectionPlan(
            Enrollment enrollment,
            GradeCorrectionItemRequestDTO correction,
            BigDecimal totalScore,
            String letterGrade,
            List<FieldChange> histories
    ) {
    }
}
