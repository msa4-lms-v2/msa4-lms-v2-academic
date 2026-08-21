package com.msa4lmsv2academic.domain.graduation.service;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.graduation.entity.GraduationRequirement;
import com.msa4lmsv2academic.domain.graduation.repository.GraduationCreditQueryRepository;
import com.msa4lmsv2academic.domain.graduation.repository.GraduationRequirementQueryRepository;
import com.msa4lmsv2academic.domain.graduation.repository.GraduationRequirementRepository;
import com.msa4lmsv2academic.domain.graduation.repository.GraduationRequirementSearchCondition;
import com.msa4lmsv2academic.domain.graduation.repository.GraduationRequirementSearchResult;
import com.msa4lmsv2academic.domain.graduation.request.GraduationRequirementCreateRequestDTO;
import com.msa4lmsv2academic.domain.graduation.request.GraduationRequirementSearchRequestDTO;
import com.msa4lmsv2academic.domain.graduation.request.GraduationRequirementUpdateRequestDTO;
import com.msa4lmsv2academic.domain.graduation.response.GraduationRequirementResponseDTO;
import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.organization.repository.DepartmentQueryRepository;
import com.msa4lmsv2academic.global.error.DuplicateGraduationRequirementException;
import com.msa4lmsv2academic.global.error.GraduationCreditAccessDeniedException;
import com.msa4lmsv2academic.global.error.GraduationRequirementNotFoundException;
import com.msa4lmsv2academic.global.error.InvalidGraduationRequirementRequestException;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraduationRequirementService {

    private static final int MIN_ADMISSION_YEAR = 1900;
    private static final int MAX_CREDITS = 300;
    private static final String TARGET_TYPE = "GRADUATION_REQUIREMENT";
    private static final String CREATE_ACTION = "GRADUATION_REQUIREMENT_CREATE";
    private static final String UPDATE_ACTION = "GRADUATION_REQUIREMENT_UPDATE";

    private final GraduationRequirementRepository graduationRequirementRepository;
    private final GraduationRequirementQueryRepository graduationRequirementQueryRepository;
    private final GraduationCreditQueryRepository graduationCreditQueryRepository;
    private final DepartmentQueryRepository departmentQueryRepository;
    private final AuditLogService auditLogService;

    public PageResponseDTO<GraduationRequirementResponseDTO> search(
            GraduationRequirementSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        validateAdmin(currentUser);
        validateSearchRequest(request);
        int page = request.resolvedPage();
        int size = request.resolvedSize();
        long offset = (page - 1L) * size;
        GraduationRequirementSearchResult result = graduationRequirementQueryRepository.search(
                new GraduationRequirementSearchCondition(
                        offset,
                        size,
                        request.normalizedKeyword(),
                        request.departmentId(),
                        request.admissionYear() == null ? null : request.admissionYear().shortValue(),
                        request.resolvedSortBy(),
                        request.descending()
                )
        );
        return new PageResponseDTO<>(
                result.items().stream().map(GraduationRequirementResponseDTO::from).toList(),
                result.totalCount(),
                page,
                size,
                offset + result.items().size() < result.totalCount()
        );
    }

    public GraduationRequirementResponseDTO get(Long requirementId, CurrentUser currentUser) {
        validateAdmin(currentUser);
        return GraduationRequirementResponseDTO.from(findRequirement(requirementId));
    }

    @Transactional
    public GraduationRequirementResponseDTO create(
            GraduationRequirementCreateRequestDTO request,
            CurrentUser currentUser,
            String requestId,
            String ipAddress
    ) {
        validateAdmin(currentUser);
        validateCreateRequest(request);
        short admissionYear = request.admissionYear().shortValue();
        Department department = findDepartmentForRequirement(request.departmentId(), admissionYear);
        validateUnique(department.getId(), admissionYear, null);

        GraduationRequirement requirement = GraduationRequirement.create(
                department,
                admissionYear,
                request.requiredMajorCredits(),
                request.requiredGeneralCredits(),
                request.requiredTotalCredits(),
                null
        );
        GraduationRequirement saved = save(requirement);
        auditLogService.record(
                currentUser.id(),
                CREATE_ACTION,
                TARGET_TYPE,
                saved.getId(),
                null,
                snapshot(saved),
                null,
                normalizeNullable(requestId),
                normalizeNullable(ipAddress)
        );
        return GraduationRequirementResponseDTO.from(saved);
    }

    @Transactional
    public GraduationRequirementResponseDTO update(
            Long requirementId,
            GraduationRequirementUpdateRequestDTO request,
            CurrentUser currentUser,
            String requestId,
            String ipAddress
    ) {
        validateAdmin(currentUser);
        validateUpdateRequest(request);
        GraduationRequirement requirement = findRequirement(requirementId);

        Long departmentId = request.departmentId() == null
                ? requirement.getDepartment().getId()
                : request.departmentId();
        short admissionYear = request.admissionYear() == null
                ? requirement.getAdmissionYear()
                : request.admissionYear().shortValue();
        int majorCredits = request.requiredMajorCredits() == null
                ? requirement.getRequiredMajorCredits()
                : request.requiredMajorCredits();
        int generalCredits = request.requiredGeneralCredits() == null
                ? requirement.getRequiredGeneralCredits()
                : request.requiredGeneralCredits();
        int totalCredits = request.requiredTotalCredits() == null
                ? requirement.getRequiredTotalCredits()
                : request.requiredTotalCredits();
        validateCredits(majorCredits, generalCredits, totalCredits);

        Department department = departmentId.equals(requirement.getDepartment().getId())
                ? requirement.getDepartment()
                : findDepartmentForRequirement(departmentId, admissionYear);
        if (departmentId.equals(requirement.getDepartment().getId())) {
            validateDepartmentPolicy(department, admissionYear);
        }
        validateUnique(departmentId, admissionYear, requirementId);

        if (isSame(requirement, departmentId, admissionYear, majorCredits, generalCredits, totalCredits)) {
            return GraduationRequirementResponseDTO.from(requirement);
        }

        Map<String, Object> beforeValue = snapshot(requirement);
        requirement.update(department, admissionYear, majorCredits, generalCredits, totalCredits);
        GraduationRequirement saved = save(requirement);
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
        return GraduationRequirementResponseDTO.from(saved);
    }

    private GraduationRequirement save(GraduationRequirement requirement) {
        try {
            return graduationRequirementRepository.saveAndFlush(requirement);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateGraduationRequirementException();
        }
    }

    private GraduationRequirement findRequirement(Long requirementId) {
        if (requirementId == null || requirementId <= 0) {
            throw new InvalidGraduationRequirementRequestException("requirementId는 양수여야 합니다.");
        }
        return graduationRequirementQueryRepository.findByIdWithDepartment(requirementId)
                .orElseThrow(GraduationRequirementNotFoundException::new);
    }

    private Department findDepartmentForRequirement(Long departmentId, short admissionYear) {
        Department department = departmentQueryRepository.findByIdWithCollege(departmentId)
                .orElseThrow(() -> new InvalidGraduationRequirementRequestException("존재하는 학과를 지정해야 합니다."));
        validateDepartmentPolicy(department, admissionYear);
        return department;
    }

    private void validateDepartmentPolicy(Department department, short admissionYear) {
        if (!department.isActive()
                && !graduationCreditQueryRepository.existsStudentInDepartmentAndAdmissionYear(
                department.getId(), admissionYear)) {
            throw new InvalidGraduationRequirementRequestException(
                    "비활성 학과는 해당 입학연도의 기존 학생이 있을 때만 졸업요건을 관리할 수 있습니다."
            );
        }
    }

    private void validateAdmin(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || !currentUser.isAdmin()) {
            throw new GraduationCreditAccessDeniedException("졸업 학점요건은 관리자만 관리할 수 있습니다.");
        }
    }

    private void validateCreateRequest(GraduationRequirementCreateRequestDTO request) {
        if (request == null || request.departmentId() == null || request.admissionYear() == null
                || request.requiredMajorCredits() == null || request.requiredGeneralCredits() == null
                || request.requiredTotalCredits() == null) {
            throw new InvalidGraduationRequirementRequestException("졸업 학점요건 등록 필수값이 누락되었습니다.");
        }
        validateAdmissionYear(request.admissionYear());
        validateCredits(
                request.requiredMajorCredits(),
                request.requiredGeneralCredits(),
                request.requiredTotalCredits()
        );
    }

    private void validateUpdateRequest(GraduationRequirementUpdateRequestDTO request) {
        if (request == null || !request.hasAnyUpdateField()) {
            throw new InvalidGraduationRequirementRequestException("수정할 필드를 한 개 이상 입력해야 합니다.");
        }
        if (request.reason() == null || request.reason().isBlank() || request.reason().strip().length() > 255) {
            throw new InvalidGraduationRequirementRequestException("reason은 공백이 아닌 255자 이하의 값이어야 합니다.");
        }
        if (request.admissionYear() != null) {
            validateAdmissionYear(request.admissionYear());
        }
    }

    private void validateSearchRequest(GraduationRequirementSearchRequestDTO request) {
        if (request == null) {
            throw new InvalidGraduationRequirementRequestException("검색 조건이 필요합니다.");
        }
        if (request.admissionYear() != null) {
            validateAdmissionYear(request.admissionYear());
        }
    }

    private void validateAdmissionYear(int admissionYear) {
        int maximumYear = Year.now().getValue() + 1;
        if (admissionYear < MIN_ADMISSION_YEAR || admissionYear > maximumYear) {
            throw new InvalidGraduationRequirementRequestException(
                    "admissionYear는 " + MIN_ADMISSION_YEAR + "년부터 " + maximumYear + "년까지 허용됩니다."
            );
        }
    }

    private void validateCredits(int majorCredits, int generalCredits, int totalCredits) {
        if (majorCredits < 0 || majorCredits > MAX_CREDITS
                || generalCredits < 0 || generalCredits > MAX_CREDITS
                || totalCredits < 0 || totalCredits > MAX_CREDITS) {
            throw new InvalidGraduationRequirementRequestException("학점 기준은 0 이상 300 이하여야 합니다.");
        }
        if (majorCredits + generalCredits > totalCredits) {
            throw new InvalidGraduationRequirementRequestException("전공학점과 교양학점의 합은 총학점을 초과할 수 없습니다.");
        }
    }

    private void validateUnique(Long departmentId, short admissionYear, Long excludedId) {
        boolean duplicate = excludedId == null
                ? graduationRequirementRepository.existsByDepartmentIdAndAdmissionYear(departmentId, admissionYear)
                : graduationRequirementRepository.existsByDepartmentIdAndAdmissionYearAndIdNot(
                departmentId, admissionYear, excludedId);
        if (duplicate) {
            throw new DuplicateGraduationRequirementException();
        }
    }

    private boolean isSame(
            GraduationRequirement requirement,
            Long departmentId,
            short admissionYear,
            int majorCredits,
            int generalCredits,
            int totalCredits
    ) {
        return Objects.equals(requirement.getDepartment().getId(), departmentId)
                && requirement.getAdmissionYear() == admissionYear
                && requirement.getRequiredMajorCredits() == majorCredits
                && requirement.getRequiredGeneralCredits() == generalCredits
                && requirement.getRequiredTotalCredits() == totalCredits;
    }

    private Map<String, Object> snapshot(GraduationRequirement requirement) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("departmentId", requirement.getDepartment().getId());
        snapshot.put("departmentName", requirement.getDepartment().getName());
        snapshot.put("admissionYear", requirement.getAdmissionYear());
        snapshot.put("requiredMajorCredits", requirement.getRequiredMajorCredits());
        snapshot.put("requiredGeneralCredits", requirement.getRequiredGeneralCredits());
        snapshot.put("requiredTotalCredits", requirement.getRequiredTotalCredits());
        return snapshot;
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
