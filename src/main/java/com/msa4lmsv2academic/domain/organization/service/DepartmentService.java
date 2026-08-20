package com.msa4lmsv2academic.domain.organization.service;

import com.msa4lmsv2academic.domain.organization.entity.College;
import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.organization.repository.CollegeRepository;
import com.msa4lmsv2academic.domain.organization.repository.DepartmentQueryRepository;
import com.msa4lmsv2academic.domain.organization.repository.DepartmentRepository;
import com.msa4lmsv2academic.domain.organization.repository.DepartmentSearchCondition;
import com.msa4lmsv2academic.domain.organization.repository.DepartmentSearchResult;
import com.msa4lmsv2academic.domain.organization.request.DepartmentCreateRequestDTO;
import com.msa4lmsv2academic.domain.organization.request.DepartmentSearchRequestDTO;
import com.msa4lmsv2academic.domain.organization.request.DepartmentUpdateRequestDTO;
import com.msa4lmsv2academic.domain.organization.response.DepartmentResponseDTO;
import com.msa4lmsv2academic.global.error.CollegeNotFoundException;
import com.msa4lmsv2academic.global.error.DepartmentNotFoundException;
import com.msa4lmsv2academic.global.error.DuplicateDepartmentException;
import com.msa4lmsv2academic.global.error.InvalidDepartmentRequestException;
import com.msa4lmsv2academic.global.response.PageRes;
import com.msa4lmsv2academic.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private static final int MAX_CODE_LENGTH = 20;
    private static final int MAX_NAME_LENGTH = 100;

    private final DepartmentRepository departmentRepository;
    private final DepartmentQueryRepository departmentQueryRepository;
    private final CollegeRepository collegeRepository;

    public PageRes<DepartmentResponseDTO> searchDepartments(
            DepartmentSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        int page = request.resolvedPage();
        int size = request.resolvedSize();
        long offset = (page - 1L) * size;
        DepartmentSearchCondition condition = new DepartmentSearchCondition(
                offset,
                size,
                request.collegeId(),
                request.active(),
                request.keyword(),
                currentUser.isAdmin()
        );

        DepartmentSearchResult result = departmentQueryRepository.search(condition);
        List<DepartmentResponseDTO> items = result.items().stream()
                .map(DepartmentResponseDTO::from)
                .toList();
        boolean hasNext = offset + items.size() < result.totalCount();

        return new PageRes<>(items, result.totalCount(), page, size, hasNext);
    }

    public DepartmentResponseDTO getDepartment(Long departmentId, CurrentUser currentUser) {
        Department department = departmentQueryRepository.findByIdWithCollege(departmentId)
                .orElseThrow(DepartmentNotFoundException::new);

        if (!currentUser.isAdmin() && (!department.isActive()
                || department.getCollege() != null && !department.getCollege().isActive())) {
            throw new DepartmentNotFoundException();
        }

        return DepartmentResponseDTO.from(department);
    }

    @Transactional
    public DepartmentResponseDTO createDepartment(DepartmentCreateRequestDTO request) {
        String code = validateCode(request.code());
        String name = normalizeAndValidateName(request.name());
        College college = getActiveCollege(request.collegeId());

        validateUniqueCode(code);
        Department department = Department.create(
                code,
                college,
                name,
                request.active() == null || request.active()
        );

        try {
            return DepartmentResponseDTO.from(departmentRepository.saveAndFlush(department));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateDepartmentException("이미 등록된 학과 코드입니다.");
        }
    }

    @Transactional
    public DepartmentResponseDTO updateDepartment(Long departmentId, DepartmentUpdateRequestDTO request) {
        if (!request.hasAnyField()) {
            throw new InvalidDepartmentRequestException("name, active 중 최소 한 필드가 필요합니다.");
        }

        Department department = departmentQueryRepository.findByIdWithCollege(departmentId)
                .orElseThrow(DepartmentNotFoundException::new);

        String targetName = request.name() == null
                ? department.getName()
                : normalizeAndValidateName(request.name());
        boolean targetActive = request.active() == null ? department.isActive() : request.active();

        College college = department.getCollege();
        if (targetActive && college != null && !college.isActive()) {
            throw new InvalidDepartmentRequestException("비활성 단과대 소속 학과는 활성화할 수 없습니다.");
        }

        department.update(targetName, targetActive);
        return DepartmentResponseDTO.from(departmentRepository.saveAndFlush(department));
    }

    private College getActiveCollege(Long collegeId) {
        if (collegeId == null) {
            return null;
        }
        College college = collegeRepository.findById(collegeId)
                .orElseThrow(CollegeNotFoundException::new);
        if (!college.isActive()) {
            throw new InvalidDepartmentRequestException("비활성 단과대에는 학과를 등록할 수 없습니다.");
        }
        return college;
    }

    private void validateUniqueCode(String code) {
        if (departmentRepository.existsByCode(code)) {
            throw new DuplicateDepartmentException("이미 등록된 학과 코드입니다.");
        }
    }

    private String validateCode(String rawCode) {
        if (rawCode == null || rawCode.isBlank() || rawCode.length() > MAX_CODE_LENGTH) {
            throw new InvalidDepartmentRequestException("code는 공백이 아닌 20자 이하의 값이어야 합니다.");
        }
        return rawCode;
    }

    private String normalizeAndValidateName(String rawName) {
        String name = rawName.trim();
        if (name.isBlank() || name.length() > MAX_NAME_LENGTH) {
            throw new InvalidDepartmentRequestException("name은 공백이 아닌 100자 이하의 값이어야 합니다.");
        }
        return name;
    }
}
