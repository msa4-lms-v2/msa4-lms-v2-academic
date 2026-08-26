package com.msa4lmsv2academic.domain.enrollment.service;

import com.msa4lmsv2academic.domain.course.entity.Course;
import com.msa4lmsv2academic.domain.enrollment.entity.CoursePrerequisite;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.enrollment.entity.GradeStatus;
import com.msa4lmsv2academic.domain.enrollment.entity.PrerequisiteRetakeRuleRejectionReason;
import com.msa4lmsv2academic.domain.enrollment.entity.RetakeGradePolicy;
import com.msa4lmsv2academic.domain.enrollment.entity.RetakeStatus;
import com.msa4lmsv2academic.domain.enrollment.repository.CourseGradeAttemptQueryResult;
import com.msa4lmsv2academic.domain.enrollment.repository.PrerequisiteRetakeRuleQueryRepository;
import com.msa4lmsv2academic.domain.enrollment.response.PrerequisiteCompletionResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.response.PrerequisiteRetakeEvaluationResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.response.PrerequisiteRetakeReasonResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.response.RetakeConditionResponseDTO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 조회와 수강신청 검증이 공유하는 선수과목·재수강 판정.
 * 호출 서비스가 권한 확인과 트랜잭션 경계를 소유하며, 확인된 학생과 교과목을 전달한다.
 * 이 컴포넌트는 신청 저장·감사 로그·잠금을 수행하지 않고 전체 수강 가능 여부도 판단하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class PrerequisiteRetakeEvaluator {

    private final PrerequisiteRetakeRuleQueryRepository prerequisiteRetakeRuleQueryRepository;

    public PrerequisiteRetakeEvaluationResponseDTO evaluate(Long studentId, Course course) {
        List<CoursePrerequisite> rules = prerequisiteRetakeRuleQueryRepository
                .findActiveRulesByCourseId(course.getId());
        List<Long> courseIds = new ArrayList<>();
        courseIds.add(course.getId());
        rules.stream()
                .map(rule -> rule.getPrerequisiteCourse().getId())
                .forEach(courseIds::add);
        Map<Long, List<CourseGradeAttemptQueryResult>> attemptsByCourseId =
                prerequisiteRetakeRuleQueryRepository.findGradeAttempts(studentId, courseIds)
                        .stream()
                        .collect(Collectors.groupingBy(CourseGradeAttemptQueryResult::courseId));

        List<PrerequisiteCompletionResponseDTO> prerequisites = rules.stream()
                .map(rule -> prerequisiteResult(
                        rule,
                        attemptsByCourseId.getOrDefault(rule.getPrerequisiteCourse().getId(), List.of())
                ))
                .toList();
        boolean prerequisiteSatisfied = prerequisites.stream()
                .allMatch(PrerequisiteCompletionResponseDTO::satisfied);
        RetakeConditionResponseDTO retakeCondition = retakeResult(
                attemptsByCourseId.getOrDefault(course.getId(), List.of())
        );

        LinkedHashSet<PrerequisiteRetakeRuleRejectionReason> rejectionReasons = new LinkedHashSet<>();
        prerequisites.stream()
                .filter(result -> !result.satisfied())
                .map(result -> result.reason().code())
                .forEach(rejectionReasons::add);
        if (!retakeCondition.satisfied()) {
            rejectionReasons.add(retakeCondition.reason().code());
        }
        return new PrerequisiteRetakeEvaluationResponseDTO(
                studentId,
                course.getId(),
                course.getCode(),
                course.getName(),
                prerequisiteSatisfied,
                prerequisites,
                retakeCondition,
                prerequisiteSatisfied && retakeCondition.satisfied(),
                rejectionReasons.stream()
                        .map(PrerequisiteRetakeReasonResponseDTO::from)
                        .toList()
        );
    }

    private PrerequisiteCompletionResponseDTO prerequisiteResult(
            CoursePrerequisite rule,
            List<CourseGradeAttemptQueryResult> attempts
    ) {
        String completedGrade = attempts.stream()
                .filter(nonCancelled())
                .filter(attempt -> attempt.gradeStatus() == GradeStatus.OPENED)
                .map(CourseGradeAttemptQueryResult::letterGrade)
                .filter(Objects::nonNull)
                .filter(RetakeGradePolicy::completesPrerequisite)
                .findFirst()
                .orElse(null);
        boolean satisfied = completedGrade != null;
        return new PrerequisiteCompletionResponseDTO(
                rule.getId(),
                rule.getPrerequisiteCourse().getId(),
                rule.getPrerequisiteCourse().getCode(),
                rule.getPrerequisiteCourse().getName(),
                satisfied,
                completedGrade,
                satisfied ? null : PrerequisiteRetakeReasonResponseDTO.from(
                        PrerequisiteRetakeRuleRejectionReason.PREREQUISITE_NOT_COMPLETED
                )
        );
    }

    private RetakeConditionResponseDTO retakeResult(List<CourseGradeAttemptQueryResult> attempts) {
        List<CourseGradeAttemptQueryResult> validEnrollments = attempts.stream()
                .filter(nonCancelled())
                .toList();
        if (validEnrollments.stream().anyMatch(attempt ->
                attempt.currentSemester() && attempt.gradeStatus() == GradeStatus.DRAFT)) {
            return rejectedRetakeResult(
                    RetakeStatus.ACTIVE_ENROLLMENT_EXISTS,
                    null,
                    PrerequisiteRetakeRuleRejectionReason.ACTIVE_ENROLLMENT_EXISTS
            );
        }

        String blockingGrade = validEnrollments.stream()
                .filter(attempt -> attempt.gradeStatus() == GradeStatus.OPENED)
                .map(CourseGradeAttemptQueryResult::letterGrade)
                .filter(Objects::nonNull)
                .filter(RetakeGradePolicy::blocksRetake)
                .max(Comparator.comparingInt(RetakeGradePolicy::rank))
                .orElse(null);
        if (blockingGrade != null) {
            return rejectedRetakeResult(
                    RetakeStatus.RETAKE_BLOCKED,
                    blockingGrade,
                    PrerequisiteRetakeRuleRejectionReason.RETAKE_BLOCKED_HIGH_GRADE
            );
        }

        if (validEnrollments.stream().anyMatch(attempt -> attempt.gradeStatus() == GradeStatus.DRAFT)) {
            return rejectedRetakeResult(
                    RetakeStatus.GRADE_PENDING,
                    null,
                    PrerequisiteRetakeRuleRejectionReason.GRADE_NOT_OPENED
            );
        }
        if (validEnrollments.stream().anyMatch(attempt ->
                attempt.gradeStatus() == GradeStatus.OPENED && attempt.letterGrade() == null)) {
            return rejectedRetakeResult(
                    RetakeStatus.GRADE_PENDING,
                    null,
                    PrerequisiteRetakeRuleRejectionReason.GRADE_NOT_ENTERED
            );
        }
        String invalidGrade = validEnrollments.stream()
                .filter(attempt -> attempt.gradeStatus() == GradeStatus.OPENED)
                .map(CourseGradeAttemptQueryResult::letterGrade)
                .filter(Objects::nonNull)
                .filter(grade -> !RetakeGradePolicy.isRecognized(grade))
                .findFirst()
                .orElse(null);
        if (invalidGrade != null) {
            return rejectedRetakeResult(
                    RetakeStatus.INVALID_GRADE_DATA,
                    invalidGrade,
                    PrerequisiteRetakeRuleRejectionReason.INVALID_GRADE_DATA
            );
        }

        String allowedGrade = validEnrollments.stream()
                .filter(attempt -> attempt.gradeStatus() == GradeStatus.OPENED)
                .map(CourseGradeAttemptQueryResult::letterGrade)
                .filter(Objects::nonNull)
                .filter(RetakeGradePolicy::isAllowedForRetake)
                .max(Comparator.comparingInt(RetakeGradePolicy::rank))
                .orElse(null);
        if (allowedGrade != null) {
            return new RetakeConditionResponseDTO(
                    RetakeStatus.RETAKE_ALLOWED,
                    true,
                    allowedGrade,
                    null
            );
        }
        return new RetakeConditionResponseDTO(RetakeStatus.FIRST_ENROLLMENT, true, null, null);
    }

    private RetakeConditionResponseDTO rejectedRetakeResult(
            RetakeStatus status,
            String referenceGrade,
            PrerequisiteRetakeRuleRejectionReason reason
    ) {
        return new RetakeConditionResponseDTO(
                status,
                false,
                referenceGrade,
                PrerequisiteRetakeReasonResponseDTO.from(reason)
        );
    }

    private Predicate<CourseGradeAttemptQueryResult> nonCancelled() {
        return attempt -> attempt.enrollmentStatus() != EnrollmentStatus.CANCELLED;
    }
}
