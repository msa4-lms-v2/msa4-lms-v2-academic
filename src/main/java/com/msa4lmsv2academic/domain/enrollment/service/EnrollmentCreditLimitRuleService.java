package com.msa4lmsv2academic.domain.enrollment.service;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentCreditLimitRule;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentCreditLimitRuleQueryRepository;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentCreditLimitRuleRepository;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentCreditLimitRuleSearchCondition;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentCreditLimitRuleSearchResult;
import com.msa4lmsv2academic.domain.enrollment.request.EnrollmentCreditLimitRuleCreateRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.request.EnrollmentCreditLimitRuleSearchRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.request.EnrollmentCreditLimitRuleStatusRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.request.EnrollmentCreditLimitRuleUpdateRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.response.EnrollmentCreditLimitRuleResponseDTO;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.repository.SemesterRepository;
import com.msa4lmsv2academic.global.error.DuplicateEnrollmentCreditLimitRuleException;
import com.msa4lmsv2academic.global.error.EnrollmentCreditLimitRuleAccessDeniedException;
import com.msa4lmsv2academic.global.error.EnrollmentCreditLimitRuleNotFoundException;
import com.msa4lmsv2academic.global.error.EnrollmentCreditLimitRuleReferenceNotFoundException;
import com.msa4lmsv2academic.global.error.EnrollmentCreditLimitRuleStateConflictException;
import com.msa4lmsv2academic.global.error.InvalidEnrollmentCreditLimitRuleRequestException;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentCreditLimitRuleService {

    private static final int MIN_CREDITS = 1;
    private static final int MAX_CREDITS = 30;
    private static final String TARGET_TYPE = "ENROLLMENT_CREDIT_LIMIT_RULE";
    private static final String CREATE_ACTION = "ENROLLMENT_CREDIT_LIMIT_RULE_CREATE";
    private static final String UPDATE_ACTION = "ENROLLMENT_CREDIT_LIMIT_RULE_UPDATE";
    private static final String STATUS_ACTION = "ENROLLMENT_CREDIT_LIMIT_RULE_STATUS_CHANGE";

    private final EnrollmentCreditLimitRuleRepository enrollmentCreditLimitRuleRepository;
    private final EnrollmentCreditLimitRuleQueryRepository enrollmentCreditLimitRuleQueryRepository;
    private final SemesterRepository semesterRepository;
    private final AuditLogService auditLogService;

    public PageResponseDTO<EnrollmentCreditLimitRuleResponseDTO> search(
            EnrollmentCreditLimitRuleSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        validateAdmin(currentUser);
        if (request == null) {
            throw new InvalidEnrollmentCreditLimitRuleRequestException("검색 조건이 필요합니다.");
        }
        int page = request.resolvedPage();
        int size = request.resolvedSize();
        long offset = (page - 1L) * size;
        EnrollmentCreditLimitRuleSearchResult result = enrollmentCreditLimitRuleQueryRepository.search(
                new EnrollmentCreditLimitRuleSearchCondition(
                        offset,
                        size,
                        request.academicYear(),
                        request.term(),
                        request.active(),
                        request.resolvedSortBy(),
                        request.descending()
                )
        );
        return new PageResponseDTO<>(
                result.items().stream().map(EnrollmentCreditLimitRuleResponseDTO::from).toList(),
                result.totalCount(),
                page,
                size,
                offset + result.items().size() < result.totalCount()
        );
    }

    public EnrollmentCreditLimitRuleResponseDTO get(Long ruleId, CurrentUser currentUser) {
        validateAdmin(currentUser);
        return EnrollmentCreditLimitRuleResponseDTO.from(findRule(ruleId));
    }

    @Transactional
    public EnrollmentCreditLimitRuleResponseDTO create(
            EnrollmentCreditLimitRuleCreateRequestDTO request,
            CurrentUser currentUser,
            String requestId,
            String ipAddress
    ) {
        validateAdmin(currentUser);
        validateCreateRequest(request);
        Semester semester = findSemester(request.semesterId());
        validateBeforeEnrollmentStarts(semester);
        if (enrollmentCreditLimitRuleRepository.existsBySemesterId(semester.getId())) {
            throw new DuplicateEnrollmentCreditLimitRuleException();
        }

        EnrollmentCreditLimitRule saved = save(
                EnrollmentCreditLimitRule.create(semester, request.maxCredits())
        );
        auditLogService.record(
                currentUser.id(),
                CREATE_ACTION,
                TARGET_TYPE,
                saved.getId(),
                null,
                snapshot(saved),
                request.reason().strip(),
                normalizeNullable(requestId),
                normalizeNullable(ipAddress)
        );
        return EnrollmentCreditLimitRuleResponseDTO.from(saved);
    }

    @Transactional
    public EnrollmentCreditLimitRuleResponseDTO update(
            Long ruleId,
            EnrollmentCreditLimitRuleUpdateRequestDTO request,
            CurrentUser currentUser,
            String requestId,
            String ipAddress
    ) {
        validateAdmin(currentUser);
        validateUpdateRequest(request);
        EnrollmentCreditLimitRule rule = findRule(ruleId);
        validateBeforeEnrollmentStarts(rule.getSemester());
        if (rule.getMaxCredits() == request.maxCredits()) {
            return EnrollmentCreditLimitRuleResponseDTO.from(rule);
        }

        Map<String, Object> beforeValue = snapshot(rule);
        rule.updateMaxCredits(request.maxCredits());
        EnrollmentCreditLimitRule saved = save(rule);
        auditLogService.record(
                currentUser.id(),
                UPDATE_ACTION,
                TARGET_TYPE,
                saved.getId(),
                beforeValue,
                snapshot(saved),
                request.reason().strip(),
                normalizeNullable(requestId),
                normalizeNullable(ipAddress)
        );
        return EnrollmentCreditLimitRuleResponseDTO.from(saved);
    }

    @Transactional
    public EnrollmentCreditLimitRuleResponseDTO changeStatus(
            Long ruleId,
            EnrollmentCreditLimitRuleStatusRequestDTO request,
            CurrentUser currentUser,
            String requestId,
            String ipAddress
    ) {
        validateAdmin(currentUser);
        validateStatusRequest(request);
        EnrollmentCreditLimitRule rule = findRule(ruleId);
        validateBeforeEnrollmentStarts(rule.getSemester());
        if (rule.isActive() == request.active()) {
            return EnrollmentCreditLimitRuleResponseDTO.from(rule);
        }

        Map<String, Object> beforeValue = snapshot(rule);
        if (request.active()) {
            rule.activate();
        } else {
            rule.deactivate();
        }
        EnrollmentCreditLimitRule saved = save(rule);
        auditLogService.record(
                currentUser.id(),
                STATUS_ACTION,
                TARGET_TYPE,
                saved.getId(),
                beforeValue,
                snapshot(saved),
                request.reason().strip(),
                normalizeNullable(requestId),
                normalizeNullable(ipAddress)
        );
        return EnrollmentCreditLimitRuleResponseDTO.from(saved);
    }

    private EnrollmentCreditLimitRule save(EnrollmentCreditLimitRule rule) {
        try {
            return enrollmentCreditLimitRuleRepository.saveAndFlush(rule);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEnrollmentCreditLimitRuleException();
        }
    }

    private EnrollmentCreditLimitRule findRule(Long ruleId) {
        if (ruleId == null || ruleId <= 0) {
            throw new InvalidEnrollmentCreditLimitRuleRequestException("ruleId는 양수여야 합니다.");
        }
        return enrollmentCreditLimitRuleQueryRepository.findByIdWithSemester(ruleId)
                .orElseThrow(EnrollmentCreditLimitRuleNotFoundException::new);
    }

    private Semester findSemester(Long semesterId) {
        return semesterRepository.findById(semesterId)
                .orElseThrow(EnrollmentCreditLimitRuleReferenceNotFoundException::new);
    }

    private void validateAdmin(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || !currentUser.isAdmin()) {
            throw new EnrollmentCreditLimitRuleAccessDeniedException();
        }
    }

    private void validateCreateRequest(EnrollmentCreditLimitRuleCreateRequestDTO request) {
        if (request == null || request.semesterId() == null || request.semesterId() <= 0
                || request.maxCredits() == null || request.reason() == null || request.reason().isBlank()) {
            throw new InvalidEnrollmentCreditLimitRuleRequestException(
                    "semesterId, maxCredits, reason은 필수입니다."
            );
        }
        validateCredits(request.maxCredits());
    }

    private void validateUpdateRequest(EnrollmentCreditLimitRuleUpdateRequestDTO request) {
        if (request == null || request.maxCredits() == null
                || request.reason() == null || request.reason().isBlank()) {
            throw new InvalidEnrollmentCreditLimitRuleRequestException(
                    "maxCredits와 reason은 필수입니다."
            );
        }
        validateCredits(request.maxCredits());
    }

    private void validateStatusRequest(EnrollmentCreditLimitRuleStatusRequestDTO request) {
        if (request == null || request.active() == null
                || request.reason() == null || request.reason().isBlank()) {
            throw new InvalidEnrollmentCreditLimitRuleRequestException(
                    "active와 reason은 필수입니다."
            );
        }
    }

    private void validateCredits(int maxCredits) {
        if (maxCredits < MIN_CREDITS || maxCredits > MAX_CREDITS) {
            throw new InvalidEnrollmentCreditLimitRuleRequestException(
                    "maxCredits는 1 이상 30 이하여야 합니다."
            );
        }
    }

    private void validateBeforeEnrollmentStarts(Semester semester) {
        if (!LocalDateTime.now().isBefore(semester.getEnrollmentStartAt())) {
            throw new EnrollmentCreditLimitRuleStateConflictException();
        }
    }

    private Map<String, Object> snapshot(EnrollmentCreditLimitRule rule) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("version", rule.getVersion());
        value.put("semesterId", rule.getSemester().getId());
        value.put("academicYear", rule.getSemester().getAcademicYear());
        value.put("term", rule.getSemester().getTerm().name());
        value.put("maxCredits", rule.getMaxCredits());
        value.put("active", rule.isActive());
        return value;
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
