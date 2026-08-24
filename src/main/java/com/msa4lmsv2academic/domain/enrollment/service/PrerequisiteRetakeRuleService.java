package com.msa4lmsv2academic.domain.enrollment.service;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.course.entity.Course;
import com.msa4lmsv2academic.domain.enrollment.entity.CoursePrerequisite;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.enrollment.entity.GradeStatus;
import com.msa4lmsv2academic.domain.enrollment.entity.PrerequisiteRetakeRuleRejectionReason;
import com.msa4lmsv2academic.domain.enrollment.entity.RetakeGradePolicy;
import com.msa4lmsv2academic.domain.enrollment.entity.RetakeStatus;
import com.msa4lmsv2academic.domain.enrollment.repository.CourseGradeAttemptQueryResult;
import com.msa4lmsv2academic.domain.enrollment.repository.CoursePrerequisiteRepository;
import com.msa4lmsv2academic.domain.enrollment.repository.PrerequisiteRetakeRuleQueryRepository;
import com.msa4lmsv2academic.domain.enrollment.repository.PrerequisiteRetakeRuleSearchCondition;
import com.msa4lmsv2academic.domain.enrollment.repository.PrerequisiteRetakeRuleSearchResult;
import com.msa4lmsv2academic.domain.enrollment.request.PrerequisiteRetakeRuleCreateRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.request.PrerequisiteRetakeRuleSearchRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.request.PrerequisiteRetakeRuleStatusRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.request.PrerequisiteRetakeRuleUpdateRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.response.PrerequisiteCompletionResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.response.PrerequisiteRetakeEvaluationResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.response.PrerequisiteRetakeReasonResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.response.PrerequisiteRetakeRuleCriteriaResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.response.PrerequisiteRetakeRuleQueryResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.response.RetakeConditionResponseDTO;
import com.msa4lmsv2academic.domain.student.repository.ProfessorStudentScope;
import com.msa4lmsv2academic.domain.student.repository.StudentQueryRepository;
import com.msa4lmsv2academic.global.error.DuplicatePrerequisiteRetakeRuleException;
import com.msa4lmsv2academic.global.error.InvalidPrerequisiteRetakeRuleRequestException;
import com.msa4lmsv2academic.global.error.PrerequisiteRetakeRuleAccessDeniedException;
import com.msa4lmsv2academic.global.error.PrerequisiteRetakeRuleNotFoundException;
import com.msa4lmsv2academic.global.error.PrerequisiteRetakeRuleReferenceNotFoundException;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrerequisiteRetakeRuleService {

    private static final String TARGET_TYPE = "COURSE_PREREQUISITE";
    private static final String CREATE_ACTION = "COURSE_PREREQUISITE_CREATE";
    private static final String REACTIVATE_ACTION = "COURSE_PREREQUISITE_REACTIVATE";
    private static final String UPDATE_ACTION = "COURSE_PREREQUISITE_UPDATE";
    private static final String STATUS_ACTION = "COURSE_PREREQUISITE_STATUS_CHANGE";

    private final CoursePrerequisiteRepository coursePrerequisiteRepository;
    private final PrerequisiteRetakeRuleQueryRepository prerequisiteRetakeRuleQueryRepository;
    private final StudentQueryRepository studentQueryRepository;
    private final AuditLogService auditLogService;

    public PrerequisiteRetakeRuleQueryResponseDTO search(
            PrerequisiteRetakeRuleSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        validateSearchRequest(request);
        SearchScope scope = resolveSearchScope(request, currentUser);
        if (request.courseId() != null) {
            findCourse(request.courseId());
        }

        int page = request.resolvedPage();
        int size = request.resolvedSize();
        PrerequisiteRetakeRuleSearchResult result = prerequisiteRetakeRuleQueryRepository.search(
                new PrerequisiteRetakeRuleSearchCondition(
                        (page - 1L) * size,
                        size,
                        request.normalizedKeyword(),
                        request.courseId(),
                        scope.criteriaActive(),
                        request.resolvedSortBy(),
                        request.descending()
                )
        );
        PageResponseDTO<PrerequisiteRetakeRuleCriteriaResponseDTO> criteria = new PageResponseDTO<>(
                result.items().stream()
                        .map(PrerequisiteRetakeRuleCriteriaResponseDTO::from)
                        .toList(),
                result.totalCount(),
                page,
                size,
                (long) page * size < result.totalCount()
        );
        PrerequisiteRetakeEvaluationResponseDTO evaluation = scope.evaluate()
                ? evaluate(scope.studentId(), findCourse(request.courseId()))
                : null;
        return new PrerequisiteRetakeRuleQueryResponseDTO(criteria, evaluation);
    }

    public PrerequisiteRetakeRuleCriteriaResponseDTO get(Long ruleId, CurrentUser currentUser) {
        validateAdmin(currentUser);
        return PrerequisiteRetakeRuleCriteriaResponseDTO.from(findRule(ruleId));
    }

    @Transactional
    public PrerequisiteRetakeRuleCreateResult create(
            PrerequisiteRetakeRuleCreateRequestDTO request,
            CurrentUser currentUser,
            String requestId,
            String ipAddress
    ) {
        validateAdmin(currentUser);
        Course course = findCourse(request.courseId());
        Course prerequisiteCourse = findCourse(request.prerequisiteCourseId());
        validateDistinctCourses(course.getId(), prerequisiteCourse.getId());

        CoursePrerequisite existing = coursePrerequisiteRepository
                .findByCourseIdAndPrerequisiteCourseId(course.getId(), prerequisiteCourse.getId())
                .orElse(null);
        if (existing != null) {
            if (existing.isActive()) {
                throw new DuplicatePrerequisiteRetakeRuleException();
            }
            validateNoCycle(existing.getId(), course.getId(), prerequisiteCourse.getId());
            Map<String, Object> beforeValue = ruleValues(existing);
            existing.activate();
            CoursePrerequisite reactivated = coursePrerequisiteRepository.saveAndFlush(existing);
            auditLogService.record(
                    currentUser.id(),
                    REACTIVATE_ACTION,
                    TARGET_TYPE,
                    reactivated.getId(),
                    beforeValue,
                    ruleValues(reactivated),
                    request.reason().strip(),
                    requestId,
                    ipAddress
            );
            return new PrerequisiteRetakeRuleCreateResult(
                    PrerequisiteRetakeRuleCriteriaResponseDTO.from(reactivated),
                    false
            );
        }

        validateNoCycle(null, course.getId(), prerequisiteCourse.getId());
        try {
            CoursePrerequisite saved = coursePrerequisiteRepository.saveAndFlush(
                    CoursePrerequisite.create(course, prerequisiteCourse)
            );
            auditLogService.record(
                    currentUser.id(),
                    CREATE_ACTION,
                    TARGET_TYPE,
                    saved.getId(),
                    null,
                    ruleValues(saved),
                    request.reason().strip(),
                    requestId,
                    ipAddress
            );
            return new PrerequisiteRetakeRuleCreateResult(
                    PrerequisiteRetakeRuleCriteriaResponseDTO.from(saved),
                    true
            );
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicatePrerequisiteRetakeRuleException();
        }
    }

    @Transactional
    public PrerequisiteRetakeRuleCriteriaResponseDTO update(
            Long ruleId,
            PrerequisiteRetakeRuleUpdateRequestDTO request,
            CurrentUser currentUser,
            String requestId,
            String ipAddress
    ) {
        validateAdmin(currentUser);
        CoursePrerequisite rule = findRule(ruleId);
        Course course = findCourse(request.courseId());
        Course prerequisiteCourse = findCourse(request.prerequisiteCourseId());
        validateDistinctCourses(course.getId(), prerequisiteCourse.getId());
        boolean unchanged = Objects.equals(rule.getCourse().getId(), course.getId())
                && Objects.equals(rule.getPrerequisiteCourse().getId(), prerequisiteCourse.getId());
        if (unchanged) {
            return PrerequisiteRetakeRuleCriteriaResponseDTO.from(rule);
        }
        if (coursePrerequisiteRepository.existsByCourseIdAndPrerequisiteCourseIdAndIdNot(
                course.getId(), prerequisiteCourse.getId(), ruleId
        )) {
            throw new DuplicatePrerequisiteRetakeRuleException();
        }
        if (rule.isActive()) {
            validateNoCycle(ruleId, course.getId(), prerequisiteCourse.getId());
        }

        Map<String, Object> beforeValue = ruleValues(rule);
        try {
            rule.changeCourses(course, prerequisiteCourse);
            CoursePrerequisite saved = coursePrerequisiteRepository.saveAndFlush(rule);
            auditLogService.record(
                    currentUser.id(),
                    UPDATE_ACTION,
                    TARGET_TYPE,
                    saved.getId(),
                    beforeValue,
                    ruleValues(saved),
                    request.reason().strip(),
                    requestId,
                    ipAddress
            );
            return PrerequisiteRetakeRuleCriteriaResponseDTO.from(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicatePrerequisiteRetakeRuleException();
        }
    }

    @Transactional
    public PrerequisiteRetakeRuleCriteriaResponseDTO changeStatus(
            Long ruleId,
            PrerequisiteRetakeRuleStatusRequestDTO request,
            CurrentUser currentUser,
            String requestId,
            String ipAddress
    ) {
        validateAdmin(currentUser);
        CoursePrerequisite rule = findRule(ruleId);
        if (rule.isActive() == request.active()) {
            return PrerequisiteRetakeRuleCriteriaResponseDTO.from(rule);
        }
        if (request.active()) {
            validateNoCycle(
                    ruleId,
                    rule.getCourse().getId(),
                    rule.getPrerequisiteCourse().getId()
            );
        }

        Map<String, Object> beforeValue = ruleValues(rule);
        if (request.active()) {
            rule.activate();
        } else {
            rule.deactivate();
        }
        CoursePrerequisite saved = coursePrerequisiteRepository.saveAndFlush(rule);
        auditLogService.record(
                currentUser.id(),
                STATUS_ACTION,
                TARGET_TYPE,
                saved.getId(),
                beforeValue,
                ruleValues(saved),
                request.reason().strip(),
                requestId,
                ipAddress
        );
        return PrerequisiteRetakeRuleCriteriaResponseDTO.from(saved);
    }

    private PrerequisiteRetakeEvaluationResponseDTO evaluate(Long studentId, Course course) {
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

    private SearchScope resolveSearchScope(
            PrerequisiteRetakeRuleSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        validateAuthenticated(currentUser);
        return switch (currentUser.role()) {
            case "STUDENT" -> resolveStudentSearchScope(request, currentUser);
            case "PROFESSOR" -> resolveProfessorSearchScope(request, currentUser);
            case "ADMIN" -> resolveAdminSearchScope(request);
            default -> throw new PrerequisiteRetakeRuleAccessDeniedException(
                    "선수과목·재수강 조건 조회 권한이 없습니다."
            );
        };
    }

    private SearchScope resolveStudentSearchScope(
            PrerequisiteRetakeRuleSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        requireCourseId(request.courseId());
        if (request.studentId() != null) {
            throw new PrerequisiteRetakeRuleAccessDeniedException(
                    "학생은 다른 학생의 조건을 조회할 수 없습니다."
            );
        }
        validateActiveFilterForEvaluation(request.active());
        Long studentId = prerequisiteRetakeRuleQueryRepository.findStudentIdByUserId(currentUser.id())
                .orElseThrow(() -> new PrerequisiteRetakeRuleReferenceNotFoundException(
                        "Academic에 연결된 학생 정보가 없습니다."
                ));
        return new SearchScope(studentId, true, true);
    }

    private SearchScope resolveProfessorSearchScope(
            PrerequisiteRetakeRuleSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        requireCourseId(request.courseId());
        if (request.studentId() == null) {
            throw new InvalidPrerequisiteRetakeRuleRequestException(
                    "교수 조회에는 studentId가 필요합니다."
            );
        }
        validateActiveFilterForEvaluation(request.active());
        ProfessorStudentScope professorScope = studentQueryRepository
                .findProfessorScopeByUserId(currentUser.id())
                .orElseThrow(() -> new PrerequisiteRetakeRuleAccessDeniedException(
                        "Academic에 연결된 교수 정보가 없습니다."
                ));
        if (!studentQueryRepository.isStudentInProfessorScope(request.studentId(), professorScope)) {
            throw new PrerequisiteRetakeRuleAccessDeniedException(
                    "해당 학생의 선수과목·재수강 조건 조회 범위가 아닙니다."
            );
        }
        return new SearchScope(request.studentId(), true, true);
    }

    private SearchScope resolveAdminSearchScope(PrerequisiteRetakeRuleSearchRequestDTO request) {
        if (request.studentId() == null) {
            return new SearchScope(null, false, request.active());
        }
        requireCourseId(request.courseId());
        validateActiveFilterForEvaluation(request.active());
        if (!prerequisiteRetakeRuleQueryRepository.existsStudentById(request.studentId())) {
            throw new PrerequisiteRetakeRuleReferenceNotFoundException("학생 정보를 찾을 수 없습니다.");
        }
        return new SearchScope(request.studentId(), true, true);
    }

    private void validateSearchRequest(PrerequisiteRetakeRuleSearchRequestDTO request) {
        if (request == null) {
            throw new InvalidPrerequisiteRetakeRuleRequestException("조회 조건이 필요합니다.");
        }
    }

    private void validateActiveFilterForEvaluation(Boolean active) {
        if (Boolean.FALSE.equals(active)) {
            throw new PrerequisiteRetakeRuleAccessDeniedException(
                    "개인별 판정에서는 비활성 선수과목 기준을 조회할 수 없습니다."
            );
        }
    }

    private void requireCourseId(Long courseId) {
        if (courseId == null) {
            throw new InvalidPrerequisiteRetakeRuleRequestException(
                    "개인별 조건 판정에는 courseId가 필요합니다."
            );
        }
    }

    private void validateNoCycle(Long excludedRuleId, Long courseId, Long prerequisiteCourseId) {
        Map<Long, List<Long>> graph = new HashMap<>();
        prerequisiteRetakeRuleQueryRepository.findActiveEdges().stream()
                .filter(edge -> !Objects.equals(edge.ruleId(), excludedRuleId))
                .forEach(edge -> graph.computeIfAbsent(edge.courseId(), ignored -> new ArrayList<>())
                        .add(edge.prerequisiteCourseId()));
        graph.computeIfAbsent(courseId, ignored -> new ArrayList<>()).add(prerequisiteCourseId);

        ArrayDeque<Long> pending = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        pending.push(prerequisiteCourseId);
        while (!pending.isEmpty()) {
            Long current = pending.pop();
            if (Objects.equals(current, courseId)) {
                throw new InvalidPrerequisiteRetakeRuleRequestException(
                        "선수과목 관계가 순환하도록 등록할 수 없습니다."
                );
            }
            if (visited.add(current)) {
                graph.getOrDefault(current, List.of()).forEach(pending::push);
            }
        }
    }

    private void validateDistinctCourses(Long courseId, Long prerequisiteCourseId) {
        if (Objects.equals(courseId, prerequisiteCourseId)) {
            throw new InvalidPrerequisiteRetakeRuleRequestException(
                    "교과목 자체를 선수과목으로 등록할 수 없습니다."
            );
        }
    }

    private Course findCourse(Long courseId) {
        return prerequisiteRetakeRuleQueryRepository.findCourseById(courseId)
                .orElseThrow(() -> new PrerequisiteRetakeRuleReferenceNotFoundException(
                        "교과목 정보를 찾을 수 없습니다."
                ));
    }

    private CoursePrerequisite findRule(Long ruleId) {
        if (ruleId == null || ruleId <= 0) {
            throw new InvalidPrerequisiteRetakeRuleRequestException("ruleId는 양수여야 합니다.");
        }
        return prerequisiteRetakeRuleQueryRepository.findByIdWithCourses(ruleId)
                .orElseThrow(PrerequisiteRetakeRuleNotFoundException::new);
    }

    private void validateAdmin(CurrentUser currentUser) {
        validateAuthenticated(currentUser);
        if (!"ADMIN".equals(currentUser.role())) {
            throw new PrerequisiteRetakeRuleAccessDeniedException(
                    "선수과목 기준정보는 관리자만 변경할 수 있습니다."
            );
        }
    }

    private void validateAuthenticated(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || currentUser.role() == null) {
            throw new PrerequisiteRetakeRuleAccessDeniedException("인증 사용자 정보가 없습니다.");
        }
    }

    private Map<String, Object> ruleValues(CoursePrerequisite rule) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("courseId", rule.getCourse().getId());
        values.put("prerequisiteCourseId", rule.getPrerequisiteCourse().getId());
        values.put("active", rule.isActive());
        return values;
    }

    private record SearchScope(
            Long studentId,
            boolean evaluate,
            Boolean criteriaActive
    ) {
    }
}
