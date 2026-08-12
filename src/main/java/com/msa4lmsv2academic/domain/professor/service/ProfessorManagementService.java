package com.msa4lmsv2academic.domain.professor.service;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.organization.repository.DepartmentRepository;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.professor.repository.ProfessorQueryRepository;
import com.msa4lmsv2academic.domain.professor.repository.ProfessorRepository;
import com.msa4lmsv2academic.domain.professor.repository.ProfessorSearchCondition;
import com.msa4lmsv2academic.domain.professor.repository.ProfessorSearchResult;
import com.msa4lmsv2academic.domain.professor.request.ProfessorSearchRequestDTO;
import com.msa4lmsv2academic.domain.professor.request.ProfessorUpdateRequestDTO;
import com.msa4lmsv2academic.domain.professor.response.ProfessorDetailResponseDTO;
import com.msa4lmsv2academic.domain.professor.response.ProfessorSummaryResponseDTO;
import com.msa4lmsv2academic.global.error.InvalidProfessorRequestException;
import com.msa4lmsv2academic.global.error.ProfessorAccessDeniedException;
import com.msa4lmsv2academic.global.error.ProfessorDepartmentNotFoundException;
import com.msa4lmsv2academic.global.error.ProfessorNotFoundException;
import com.msa4lmsv2academic.global.response.PageRes;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.Year;
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
public class ProfessorManagementService {

    private static final int MIN_HIRE_YEAR = 1900;
    private static final String UPDATE_ACTION = "PROFESSOR_EMPLOYMENT_UPDATE";
    private static final String TARGET_TYPE = "PROFESSOR";

    private final ProfessorRepository professorRepository;
    private final ProfessorQueryRepository professorQueryRepository;
    private final DepartmentRepository departmentRepository;
    private final AuditLogService auditLogService;

    public PageRes<ProfessorSummaryResponseDTO> searchProfessors(ProfessorSearchRequestDTO request) {
        validateHireYear(request.hireYear());

        int page = request.resolvedPage();
        int size = request.resolvedSize();
        long offset = (page - 1L) * size;
        ProfessorSearchCondition condition = new ProfessorSearchCondition(
                offset,
                size,
                request.departmentId(),
                toShort(request.hireYear()),
                request.status(),
                request.keyword()
        );

        ProfessorSearchResult result = professorQueryRepository.search(condition);
        List<ProfessorSummaryResponseDTO> items = result.items().stream()
                .map(ProfessorSummaryResponseDTO::from)
                .toList();
        boolean hasNext = offset + items.size() < result.totalCount();
        return new PageRes<>(items, result.totalCount(), page, size, hasNext);
    }

    public ProfessorDetailResponseDTO getProfessor(Long professorId) {
        return ProfessorDetailResponseDTO.from(findProfessor(professorId));
    }

    @Transactional
    public ProfessorDetailResponseDTO updateProfessor(Long professorId, ProfessorUpdateRequestDTO request,
                                                       CurrentUser currentUser, String requestId, String ipAddress) {
        validateAdmin(currentUser);
        validateUpdateRequest(request);

        Professor professor = findProfessor(professorId);
        Department targetDepartment = resolveDepartment(request.departmentId(), professor.getDepartment());
        Short targetHireYear = request.hireYear() == null
                ? professor.getHireYear()
                : toShort(request.hireYear());

        if (isSameEmployment(professor, targetDepartment, targetHireYear)) {
            return ProfessorDetailResponseDTO.from(professor);
        }

        Map<String, Object> beforeValue = employmentSnapshot(professor);
        professor.updateEmployment(targetDepartment, targetHireYear);
        Professor savedProfessor = professorRepository.saveAndFlush(professor);

        auditLogService.record(
                currentUser.id(),
                UPDATE_ACTION,
                TARGET_TYPE,
                savedProfessor.getId(),
                beforeValue,
                employmentSnapshot(savedProfessor),
                request.reason().trim(),
                normalizeNullable(requestId),
                normalizeNullable(ipAddress)
        );
        return ProfessorDetailResponseDTO.from(savedProfessor);
    }

    private Professor findProfessor(Long professorId) {
        return professorQueryRepository.findByIdWithDetails(professorId)
                .orElseThrow(ProfessorNotFoundException::new);
    }

    private Department resolveDepartment(Long departmentId, Department currentDepartment) {
        if (departmentId == null || departmentId.equals(currentDepartment.getId())) {
            return currentDepartment;
        }
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(ProfessorDepartmentNotFoundException::new);
        if (!department.isActive()) {
            throw new InvalidProfessorRequestException("비활성 학과로 교수를 배정할 수 없습니다.");
        }
        return department;
    }

    private void validateAdmin(CurrentUser currentUser) {
        if (currentUser == null || !currentUser.isAdmin()) {
            throw new ProfessorAccessDeniedException();
        }
    }

    private void validateUpdateRequest(ProfessorUpdateRequestDTO request) {
        if (request == null || !request.hasAnyUpdateField()) {
            throw new InvalidProfessorRequestException("departmentId, hireYear 중 최소 한 필드가 필요합니다.");
        }
        if (request.reason() == null || request.reason().isBlank() || request.reason().length() > 255) {
            throw new InvalidProfessorRequestException("reason은 공백이 아닌 255자 이하의 값이어야 합니다.");
        }
        validateHireYear(request.hireYear());
    }

    private void validateHireYear(Integer hireYear) {
        int currentYear = Year.now().getValue();
        if (hireYear != null && (hireYear < MIN_HIRE_YEAR || hireYear > currentYear)) {
            throw new InvalidProfessorRequestException(
                    "hireYear는 " + MIN_HIRE_YEAR + "년부터 " + currentYear + "년까지 허용됩니다."
            );
        }
    }

    private Short toShort(Integer hireYear) {
        return hireYear == null ? null : hireYear.shortValue();
    }

    private boolean isSameEmployment(Professor professor, Department department, Short hireYear) {
        return professor.getDepartment().getId().equals(department.getId())
                && Objects.equals(professor.getHireYear(), hireYear);
    }

    private Map<String, Object> employmentSnapshot(Professor professor) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("departmentId", professor.getDepartment().getId());
        snapshot.put("departmentName", professor.getDepartment().getName());
        snapshot.put("hireYear", professor.getHireYear());
        return snapshot;
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
