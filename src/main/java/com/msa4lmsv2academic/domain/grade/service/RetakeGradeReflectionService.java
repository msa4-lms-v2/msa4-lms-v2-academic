package com.msa4lmsv2academic.domain.grade.service;

import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.enrollment.entity.GradeStatus;
import com.msa4lmsv2academic.domain.enrollment.entity.RetakeGradePolicy;
import com.msa4lmsv2academic.domain.grade.entity.GradeCorrectionHistory;
import com.msa4lmsv2academic.domain.grade.entity.GradePointPolicy;
import com.msa4lmsv2academic.domain.grade.entity.StudentGradeSummary;
import com.msa4lmsv2academic.domain.grade.repository.GradeCorrectionHistoryRepository;
import com.msa4lmsv2academic.domain.grade.repository.RetakeGradeReflectionQueryRepository;
import com.msa4lmsv2academic.domain.grade.repository.StudentGradeSummaryRepository;
import com.msa4lmsv2academic.domain.grade.request.RetakeGradeReflectionRequestDTO;
import com.msa4lmsv2academic.domain.grade.response.RetakeGradeReflectionResponseDTO;
import com.msa4lmsv2academic.domain.grade.response.StudentGradeSummaryResponseDTO;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.msa4lmsv2academic.domain.user.entity.UserStatus;
import com.msa4lmsv2academic.domain.user.repository.UserRepository;
import com.msa4lmsv2academic.global.error.InvalidRetakeGradeReflectionRequestException;
import com.msa4lmsv2academic.global.error.RetakeGradeReflectionAccessDeniedException;
import com.msa4lmsv2academic.global.error.RetakeGradeReflectionConflictException;
import com.msa4lmsv2academic.global.error.RetakeGradeReflectionNotFoundException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RetakeGradeReflectionService {

    private static final short MAX_SUMMARY_CREDITS = 255;

    private final RetakeGradeReflectionQueryRepository queryRepository;
    private final StudentGradeSummaryRepository summaryRepository;
    private final GradeCorrectionHistoryRepository historyRepository;
    private final UserRepository userRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RetakeGradeReflectionResponseDTO reflect(
            Long enrollmentId,
            RetakeGradeReflectionRequestDTO request,
            CurrentUser currentUser
    ) {
        validateRequest(enrollmentId, request, currentUser);
        User administrator = findActiveAdministrator(currentUser.id());
        Enrollment target = queryRepository.findEnrollmentForUpdate(enrollmentId)
                .orElseThrow(() -> new RetakeGradeReflectionNotFoundException(
                        "재수강 성적을 반영할 수강 정보를 찾을 수 없습니다."
                ));

        queryRepository.lockStudent(target.getStudent().getId());
        List<Enrollment> attempts = queryRepository.findActiveAttempts(target.getStudent().getId());
        Enrollment previous = validateAndFindPrevious(target, attempts);
        String reflectedValue = GradeCorrectionHistory.gradeValue(target);
        if (historyRepository.existsByEnrollmentIdAndFieldChangedAndNewValue(
                target.getId(), GradeCorrectionHistory.RETAKE_REFLECTION_FIELD, reflectedValue)) {
            throw new RetakeGradeReflectionConflictException("이미 반영된 재수강 성적입니다.");
        }

        List<StudentGradeSummary> summaries = recalculateSummaries(target, attempts);
        LocalDateTime processedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        historyRepository.saveAndFlush(GradeCorrectionHistory.recordRetakeReflection(
                target,
                previous,
                administrator,
                request.reason(),
                processedAt
        ));

        return new RetakeGradeReflectionResponseDTO(
                target.getId(),
                target.getStudent().getId(),
                target.getLecture().getCourse().getId(),
                previous.getId(),
                previous.getLetterGrade(),
                target.getLetterGrade(),
                administrator.getId(),
                processedAt,
                summaries.stream()
                        .sorted(summaryComparator())
                        .map(StudentGradeSummaryResponseDTO::from)
                        .toList()
        );
    }

    private void validateRequest(
            Long enrollmentId,
            RetakeGradeReflectionRequestDTO request,
            CurrentUser currentUser
    ) {
        if (currentUser == null || currentUser.id() == null || !currentUser.isAdmin()) {
            throw new RetakeGradeReflectionAccessDeniedException();
        }
        if (enrollmentId == null || enrollmentId <= 0 || request == null
                || request.reason() == null || request.reason().isBlank()
                || request.reason().length() > 500) {
            throw new InvalidRetakeGradeReflectionRequestException(
                    "양수 enrollmentId와 공백이 아닌 500자 이하의 reason이 필요합니다."
            );
        }
    }

    private User findActiveAdministrator(Long userId) {
        User administrator = userRepository.findById(userId)
                .orElseThrow(() -> new RetakeGradeReflectionNotFoundException(
                        "Academic에 동기화된 관리자 정보를 찾을 수 없습니다."
                ));
        if (administrator.getRole() != UserRole.ADMIN || administrator.getStatus() != UserStatus.ACTIVE) {
            throw new RetakeGradeReflectionAccessDeniedException();
        }
        return administrator;
    }

    private Enrollment validateAndFindPrevious(Enrollment target, List<Enrollment> attempts) {
        if (target.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new RetakeGradeReflectionConflictException("취소된 수강의 성적은 반영할 수 없습니다.");
        }
        if (target.getGradeStatus() != GradeStatus.OPENED) {
            throw new RetakeGradeReflectionConflictException("공개되지 않은 성적은 반영할 수 없습니다.");
        }
        if (!GradePointPolicy.isRecognized(target.getLetterGrade())) {
            throw new RetakeGradeReflectionConflictException("재수강 반영에 사용할 수 없는 성적값입니다.");
        }

        Long targetCourseId = target.getLecture().getCourse().getId();
        List<Enrollment> sameCourseAttempts = attempts.stream()
                .filter(attempt -> attempt.getLecture().getCourse().getId().equals(targetCourseId))
                .sorted(attemptComparator())
                .toList();
        if (sameCourseAttempts.isEmpty() || !sameCourseAttempts.getLast().getId().equals(target.getId())) {
            throw new RetakeGradeReflectionConflictException("가장 최근 수강 성적만 재수강 성적으로 반영할 수 있습니다.");
        }

        List<Enrollment> previousOpenedAttempts = sameCourseAttempts.stream()
                .filter(attempt -> compareAttempt(attempt, target) < 0)
                .filter(attempt -> attempt.getGradeStatus() == GradeStatus.OPENED)
                .filter(attempt -> GradePointPolicy.isRecognized(attempt.getLetterGrade()))
                .toList();
        if (previousOpenedAttempts.isEmpty()) {
            throw new RetakeGradeReflectionConflictException("이전 공개 성적이 없어 재수강 성적으로 반영할 수 없습니다.");
        }
        if (previousOpenedAttempts.stream()
                .map(Enrollment::getLetterGrade)
                .anyMatch(RetakeGradePolicy::blocksRetake)) {
            throw new RetakeGradeReflectionConflictException("B 이상 성적 이력이 있는 과목은 재수강 반영할 수 없습니다.");
        }
        return previousOpenedAttempts.getLast();
    }

    private List<StudentGradeSummary> recalculateSummaries(
            Enrollment target,
            List<Enrollment> attempts
    ) {
        Map<Long, Enrollment> latestOpenedByCourse = new LinkedHashMap<>();
        attempts.stream()
                .filter(attempt -> attempt.getGradeStatus() == GradeStatus.OPENED)
                .sorted(attemptComparator())
                .forEach(attempt -> latestOpenedByCourse.put(
                        attempt.getLecture().getCourse().getId(),
                        attempt
                ));
        List<Enrollment> reflectedAttempts = new ArrayList<>(latestOpenedByCourse.values());
        reflectedAttempts.stream()
                .filter(attempt -> !GradePointPolicy.isRecognized(attempt.getLetterGrade()))
                .findFirst()
                .ifPresent(attempt -> {
                    throw new RetakeGradeReflectionConflictException(
                            "학생의 공개 성적 중 GPA에 반영할 수 없는 값이 있습니다. enrollmentId=" + attempt.getId()
                    );
                });

        Map<Long, StudentGradeSummary> existingBySemesterId = summaryRepository
                .findAllByStudentId(target.getStudent().getId())
                .stream()
                .collect(Collectors.toMap(
                        summary -> summary.getSemester().getId(),
                        Function.identity()
                ));
        Map<Long, Semester> semesters = attempts.stream()
                .filter(attempt -> attempt.getGradeStatus() == GradeStatus.OPENED)
                .map(attempt -> attempt.getLecture().getSemester())
                .collect(Collectors.toMap(Semester::getId, Function.identity(), (left, right) -> left));
        existingBySemesterId.values().forEach(summary ->
                semesters.putIfAbsent(summary.getSemester().getId(), summary.getSemester()));

        Map<Long, List<Enrollment>> reflectedBySemesterId = reflectedAttempts.stream()
                .collect(Collectors.groupingBy(attempt -> attempt.getLecture().getSemester().getId()));
        List<StudentGradeSummary> updated = semesters.values().stream()
                .map(semester -> updateSummary(
                        target,
                        semester,
                        reflectedBySemesterId.getOrDefault(semester.getId(), List.of()),
                        existingBySemesterId.get(semester.getId())
                ))
                .toList();
        return summaryRepository.saveAllAndFlush(updated);
    }

    private StudentGradeSummary updateSummary(
            Enrollment target,
            Semester semester,
            List<Enrollment> reflectedAttempts,
            StudentGradeSummary existing
    ) {
        int credits = reflectedAttempts.stream()
                .mapToInt(attempt -> attempt.getLecture().getCourse().getCredits())
                .sum();
        if (credits > MAX_SUMMARY_CREDITS) {
            throw new RetakeGradeReflectionConflictException("학기별 반영 학점이 저장 가능한 범위를 초과합니다.");
        }
        BigDecimal weightedPoints = reflectedAttempts.stream()
                .map(attempt -> GradePointPolicy.pointOf(attempt.getLetterGrade())
                        .multiply(BigDecimal.valueOf(attempt.getLecture().getCourse().getCredits())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gpa = credits == 0
                ? BigDecimal.ZERO.setScale(2)
                : weightedPoints.divide(BigDecimal.valueOf(credits), 2, RoundingMode.HALF_UP);
        if (existing == null) {
            return StudentGradeSummary.create(target.getStudent(), semester, (short) credits, gpa);
        }
        existing.update((short) credits, gpa);
        return existing;
    }

    private Comparator<Enrollment> attemptComparator() {
        return Comparator
                .comparingInt((Enrollment attempt) -> attempt.getLecture().getSemester().getAcademicYear())
                .thenComparingInt(attempt -> termOrder(attempt.getLecture().getSemester().getTerm()))
                .thenComparing(Enrollment::getId);
    }

    private int compareAttempt(Enrollment left, Enrollment right) {
        return attemptComparator().compare(left, right);
    }

    private Comparator<StudentGradeSummary> summaryComparator() {
        return Comparator
                .comparingInt((StudentGradeSummary summary) -> summary.getSemester().getAcademicYear())
                .thenComparingInt(summary -> termOrder(summary.getSemester().getTerm()));
    }

    private int termOrder(SemesterTerm term) {
        return term == SemesterTerm.SECOND ? 2 : 1;
    }
}
