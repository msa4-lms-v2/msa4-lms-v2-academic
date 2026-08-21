package com.msa4lmsv2academic.domain.admission.service;

import com.msa4lmsv2academic.domain.admission.entity.AdmissionCandidate;
import com.msa4lmsv2academic.domain.admission.entity.AdmissionCandidateStatus;
import com.msa4lmsv2academic.domain.admission.repository.AdmissionCandidateQueryRepository;
import com.msa4lmsv2academic.domain.admission.repository.AdmissionCandidateRepository;
import com.msa4lmsv2academic.domain.admission.repository.AdmissionCandidateSearchCondition;
import com.msa4lmsv2academic.domain.admission.repository.AdmissionCandidateSearchResult;
import com.msa4lmsv2academic.domain.admission.request.AdmissionCandidateCreateRequestDTO;
import com.msa4lmsv2academic.domain.admission.request.AdmissionCandidateSearchRequestDTO;
import com.msa4lmsv2academic.domain.admission.request.AdmissionCandidateStatusRequestDTO;
import com.msa4lmsv2academic.domain.admission.request.AdmissionCandidateUpdateRequestDTO;
import com.msa4lmsv2academic.domain.admission.response.AdmissionCandidateDetailResponseDTO;
import com.msa4lmsv2academic.domain.admission.response.AdmissionCandidateSummaryResponseDTO;
import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.organization.repository.DepartmentQueryRepository;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.service.UserQueryService;
import com.msa4lmsv2academic.global.error.AdmissionCandidateAccessDeniedException;
import com.msa4lmsv2academic.global.error.AdmissionCandidateAdministratorNotFoundException;
import com.msa4lmsv2academic.global.error.AdmissionCandidateNotFoundException;
import com.msa4lmsv2academic.global.error.AdmissionCandidateStateConflictException;
import com.msa4lmsv2academic.global.error.DuplicateAdmissionCandidateException;
import com.msa4lmsv2academic.global.error.InvalidAdmissionCandidateRequestException;
import com.msa4lmsv2academic.global.response.PageRes;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdmissionCandidateService {

    private static final int MIN_SEARCH_YEAR = 1900;
    private static final String TARGET_TYPE = "ADMISSION_CANDIDATE";
    private static final String CREATE_ACTION = "ADMISSION_CANDIDATE_CREATE";
    private static final String UPDATE_ACTION = "ADMISSION_CANDIDATE_UPDATE";
    private static final String STATUS_ACTION = "ADMISSION_CANDIDATE_STATUS_CHANGE";

    private final AdmissionCandidateRepository admissionCandidateRepository;
    private final AdmissionCandidateQueryRepository admissionCandidateQueryRepository;
    private final DepartmentQueryRepository departmentQueryRepository;
    private final UserQueryService userQueryService;
    private final AuditLogService auditLogService;

    public PageRes<AdmissionCandidateSummaryResponseDTO> searchCandidates(
            AdmissionCandidateSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        validateAdmin(currentUser);
        validateSearchRequest(request);

        int page = request.resolvedPage();
        int size = request.resolvedSize();
        long offset = (page - 1L) * size;
        AdmissionCandidateSearchCondition condition = new AdmissionCandidateSearchCondition(
                offset,
                size,
                request.normalizedKeyword(),
                request.departmentId(),
                request.admissionYear() == null ? null : request.admissionYear().shortValue(),
                request.status(),
                request.resolvedSortBy(),
                request.descending()
        );

        AdmissionCandidateSearchResult result = admissionCandidateQueryRepository.search(condition);
        List<AdmissionCandidateSummaryResponseDTO> items = result.items().stream()
                .map(AdmissionCandidateSummaryResponseDTO::from)
                .toList();
        boolean hasNext = offset + items.size() < result.totalCount();
        return new PageRes<>(items, result.totalCount(), page, size, hasNext);
    }

    public AdmissionCandidateDetailResponseDTO getCandidate(Long candidateId, CurrentUser currentUser) {
        validateAdmin(currentUser);
        return AdmissionCandidateDetailResponseDTO.from(findCandidate(candidateId));
    }

    @Transactional
    public AdmissionCandidateDetailResponseDTO createCandidate(
            AdmissionCandidateCreateRequestDTO request,
            CurrentUser currentUser,
            String requestId,
            String ipAddress
    ) {
        validateAdmin(currentUser);
        ValidatedCandidateValues values = validateCreateRequest(request);
        validateUniqueApplicationNumber(values.applicationNumber());
        validateEmailAvailable(values.email());

        Department department = findActiveDepartment(request.departmentId());
        User administrator = findAdministrator(currentUser.id());
        AdmissionCandidate candidate = AdmissionCandidate.create(
                values.applicationNumber(),
                values.name(),
                request.birthDate(),
                values.email(),
                values.phoneNumber(),
                values.address(),
                department,
                request.admissionYear().shortValue(),
                administrator
        );

        AdmissionCandidate saved;
        try {
            saved = admissionCandidateRepository.saveAndFlush(candidate);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateAdmissionCandidateException();
        }

        List<String> createdFields = createdFields(saved);
        auditLogService.record(
                currentUser.id(),
                CREATE_ACTION,
                TARGET_TYPE,
                saved.getId(),
                null,
                auditSnapshot(saved, createdFields),
                null,
                normalizeNullable(requestId),
                normalizeNullable(ipAddress)
        );
        return AdmissionCandidateDetailResponseDTO.from(saved);
    }

    @Transactional
    public AdmissionCandidateDetailResponseDTO updateCandidate(
            Long candidateId,
            AdmissionCandidateUpdateRequestDTO request,
            CurrentUser currentUser,
            String requestId,
            String ipAddress
    ) {
        validateAdmin(currentUser);
        validateUpdateRequest(request);

        AdmissionCandidate candidate = findCandidate(candidateId);
        if (candidate.getStatus() != AdmissionCandidateStatus.REGISTERED) {
            throw new AdmissionCandidateStateConflictException(
                    "REGISTERED 상태의 입학 예정자만 인적사항을 수정할 수 있습니다."
            );
        }

        String targetName = request.name() == null ? candidate.getName() : normalizeRequiredName(request.name());
        LocalDate targetBirthDate = request.birthDate() == null ? candidate.getBirthDate() : request.birthDate();
        String targetEmail = resolveNullablePatch(request.email(), candidate.getEmail());
        String targetPhoneNumber = resolveNullablePatch(request.phoneNumber(), candidate.getPhoneNumber());
        String targetAddress = resolveNullablePatch(request.address(), candidate.getAddress());
        Department targetDepartment = request.departmentId() == null
                || request.departmentId().equals(candidate.getDepartment().getId())
                ? candidate.getDepartment()
                : findActiveDepartment(request.departmentId());
        short targetAdmissionYear = request.admissionYear() == null
                ? candidate.getAdmissionYear()
                : validateAdmissionYear(request.admissionYear()).shortValue();

        List<String> changedFields = changedFields(
                candidate, targetName, targetBirthDate, targetEmail, targetPhoneNumber,
                targetAddress, targetDepartment, targetAdmissionYear
        );
        if (changedFields.isEmpty()) {
            return AdmissionCandidateDetailResponseDTO.from(candidate);
        }
        if (!Objects.equals(candidate.getEmail(), targetEmail)) {
            validateEmailAvailable(targetEmail);
        }

        Map<String, Object> beforeValue = auditSnapshot(candidate, changedFields);
        candidate.update(
                targetName,
                targetBirthDate,
                targetEmail,
                targetPhoneNumber,
                targetAddress,
                targetDepartment,
                targetAdmissionYear
        );
        AdmissionCandidate saved = admissionCandidateRepository.saveAndFlush(candidate);
        auditLogService.record(
                currentUser.id(),
                UPDATE_ACTION,
                TARGET_TYPE,
                saved.getId(),
                beforeValue,
                auditSnapshot(saved, changedFields),
                null,
                normalizeNullable(requestId),
                normalizeNullable(ipAddress)
        );
        return AdmissionCandidateDetailResponseDTO.from(saved);
    }

    @Transactional
    public AdmissionCandidateDetailResponseDTO changeStatus(
            Long candidateId,
            AdmissionCandidateStatusRequestDTO request,
            CurrentUser currentUser,
            String requestId,
            String ipAddress
    ) {
        validateAdmin(currentUser);
        validateStatusRequest(request);

        AdmissionCandidate candidate = findCandidate(candidateId);
        AdmissionCandidateStatus targetStatus = request.status();
        if (targetStatus != AdmissionCandidateStatus.CONFIRMED
                && targetStatus != AdmissionCandidateStatus.CANCELLED) {
            throw new AdmissionCandidateStateConflictException(
                    "관리자는 CONFIRMED 또는 CANCELLED 상태만 요청할 수 있습니다."
            );
        }
        if (candidate.getStatus() == targetStatus) {
            return AdmissionCandidateDetailResponseDTO.from(candidate);
        }
        if (targetStatus == AdmissionCandidateStatus.CONFIRMED) {
            validateCandidateForConfirmation(candidate);
        }

        User administrator = findAdministrator(currentUser.id());
        Map<String, Object> beforeValue = statusSnapshot(candidate);
        candidate.changeStatus(targetStatus, administrator, LocalDateTime.now());
        AdmissionCandidate saved = admissionCandidateRepository.saveAndFlush(candidate);
        auditLogService.record(
                currentUser.id(),
                STATUS_ACTION,
                TARGET_TYPE,
                saved.getId(),
                beforeValue,
                statusSnapshot(saved),
                request.reason().strip(),
                normalizeNullable(requestId),
                normalizeNullable(ipAddress)
        );
        return AdmissionCandidateDetailResponseDTO.from(saved);
    }

    private AdmissionCandidate findCandidate(Long candidateId) {
        return admissionCandidateQueryRepository.findByIdWithDetails(candidateId)
                .orElseThrow(AdmissionCandidateNotFoundException::new);
    }

    private Department findActiveDepartment(Long departmentId) {
        Department department = departmentQueryRepository.findByIdWithCollege(departmentId)
                .orElseThrow(() -> new InvalidAdmissionCandidateRequestException(
                        "존재하는 활성 학과를 지정해야 합니다."
                ));
        if (!department.isActive()) {
            throw new InvalidAdmissionCandidateRequestException("비활성 학과에는 입학 예정자를 배정할 수 없습니다.");
        }
        return department;
    }

    private User findAdministrator(Long administratorId) {
        return userQueryService.findById(administratorId)
                .orElseThrow(AdmissionCandidateAdministratorNotFoundException::new);
    }

    private void validateAdmin(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || !currentUser.isAdmin()) {
            throw new AdmissionCandidateAccessDeniedException();
        }
    }

    private ValidatedCandidateValues validateCreateRequest(AdmissionCandidateCreateRequestDTO request) {
        if (request == null || request.applicationNumber() == null || request.name() == null
                || request.birthDate() == null || request.departmentId() == null || request.admissionYear() == null) {
            throw new InvalidAdmissionCandidateRequestException(
                    "applicationNumber, name, birthDate, departmentId, admissionYear는 필수입니다."
            );
        }
        validateBirthDate(request.birthDate());
        validateAdmissionYear(request.admissionYear());
        return new ValidatedCandidateValues(
                normalizeApplicationNumber(request.applicationNumber()),
                normalizeRequiredName(request.name()),
                normalizeNullable(request.email()),
                normalizeNullable(request.phoneNumber()),
                normalizeNullable(request.address())
        );
    }

    private void validateUpdateRequest(AdmissionCandidateUpdateRequestDTO request) {
        if (request == null || !request.hasAnyUpdateField()) {
            throw new InvalidAdmissionCandidateRequestException(
                    "name, birthDate, email, phoneNumber, address, departmentId, admissionYear 중 최소 한 필드가 필요합니다."
            );
        }
        if (request.name() != null) {
            normalizeRequiredName(request.name());
        }
        if (request.birthDate() != null) {
            validateBirthDate(request.birthDate());
        }
        if (request.admissionYear() != null) {
            validateAdmissionYear(request.admissionYear());
        }
    }

    private void validateSearchRequest(AdmissionCandidateSearchRequestDTO request) {
        if (request == null) {
            throw new InvalidAdmissionCandidateRequestException("검색 조건이 필요합니다.");
        }
        int maxYear = Year.now().getValue() + 1;
        if (request.admissionYear() != null
                && (request.admissionYear() < MIN_SEARCH_YEAR || request.admissionYear() > maxYear)) {
            throw new InvalidAdmissionCandidateRequestException(
                    "검색 admissionYear는 " + MIN_SEARCH_YEAR + "년부터 " + maxYear + "년까지 허용됩니다."
            );
        }
    }

    private void validateStatusRequest(AdmissionCandidateStatusRequestDTO request) {
        if (request == null || request.status() == null || request.reason() == null
                || request.reason().isBlank() || request.reason().strip().length() > 255) {
            throw new InvalidAdmissionCandidateRequestException(
                    "status와 공백이 아닌 255자 이하의 reason은 필수입니다."
            );
        }
    }

    private Integer validateAdmissionYear(Integer admissionYear) {
        int currentYear = Year.now().getValue();
        if (admissionYear == null || admissionYear < currentYear || admissionYear > currentYear + 1) {
            throw new InvalidAdmissionCandidateRequestException(
                    "admissionYear는 " + currentYear + "년 또는 " + (currentYear + 1) + "년이어야 합니다."
            );
        }
        return admissionYear;
    }

    private void validateBirthDate(LocalDate birthDate) {
        if (birthDate == null || !birthDate.isBefore(LocalDate.now())) {
            throw new InvalidAdmissionCandidateRequestException("birthDate는 과거 날짜여야 합니다.");
        }
    }

    private void validateUniqueApplicationNumber(String applicationNumber) {
        if (admissionCandidateRepository.existsByApplicationNumber(applicationNumber)) {
            throw new DuplicateAdmissionCandidateException();
        }
    }

    private void validateEmailAvailable(String email) {
        if (email != null && userQueryService.existsByEmailIgnoreCase(email)) {
            throw new DuplicateAdmissionCandidateException("이미 Academic 사용자가 사용 중인 이메일입니다.");
        }
    }

    private void validateCandidateForConfirmation(AdmissionCandidate candidate) {
        findActiveDepartment(candidate.getDepartment().getId());
        validateAdmissionYear((int) candidate.getAdmissionYear());
        validateEmailAvailable(candidate.getEmail());
    }

    private String normalizeApplicationNumber(String value) {
        String normalized = value == null ? null : value.strip();
        if (normalized == null || normalized.isEmpty() || normalized.length() > 50) {
            throw new InvalidAdmissionCandidateRequestException(
                    "applicationNumber는 공백이 아닌 50자 이하의 값이어야 합니다."
            );
        }
        return normalized;
    }

    private String normalizeRequiredName(String value) {
        String normalized = value == null ? null : value.strip();
        if (normalized == null || normalized.isEmpty() || normalized.length() > 50) {
            throw new InvalidAdmissionCandidateRequestException("name은 공백이 아닌 50자 이하의 값이어야 합니다.");
        }
        return normalized;
    }

    private String resolveNullablePatch(String requested, String current) {
        if (requested == null) {
            return current;
        }
        return normalizeNullable(requested);
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private List<String> changedFields(
            AdmissionCandidate candidate,
            String name,
            LocalDate birthDate,
            String email,
            String phoneNumber,
            String address,
            Department department,
            short admissionYear
    ) {
        List<String> changedFields = new ArrayList<>();
        addIfChanged(changedFields, "name", candidate.getName(), name);
        addIfChanged(changedFields, "birthDate", candidate.getBirthDate(), birthDate);
        addIfChanged(changedFields, "email", candidate.getEmail(), email);
        addIfChanged(changedFields, "phoneNumber", candidate.getPhoneNumber(), phoneNumber);
        addIfChanged(changedFields, "address", candidate.getAddress(), address);
        addIfChanged(changedFields, "departmentId", candidate.getDepartment().getId(), department.getId());
        addIfChanged(changedFields, "admissionYear", candidate.getAdmissionYear(), admissionYear);
        return List.copyOf(changedFields);
    }

    private void addIfChanged(List<String> fields, String fieldName, Object current, Object target) {
        if (!Objects.equals(current, target)) {
            fields.add(fieldName);
        }
    }

    private List<String> createdFields(AdmissionCandidate candidate) {
        List<String> fields = new ArrayList<>(List.of(
                "applicationNumber", "name", "birthDate", "departmentId", "admissionYear", "status"
        ));
        if (candidate.getEmail() != null) fields.add("email");
        if (candidate.getPhoneNumber() != null) fields.add("phoneNumber");
        if (candidate.getAddress() != null) fields.add("address");
        return List.copyOf(fields);
    }

    private Map<String, Object> auditSnapshot(AdmissionCandidate candidate, List<String> changedFields) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("changedFields", changedFields);
        snapshot.put("departmentId", candidate.getDepartment().getId());
        snapshot.put("departmentName", candidate.getDepartment().getName());
        snapshot.put("admissionYear", candidate.getAdmissionYear());
        snapshot.put("status", candidate.getStatus().name());
        return snapshot;
    }

    private Map<String, Object> statusSnapshot(AdmissionCandidate candidate) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("status", candidate.getStatus().name());
        return snapshot;
    }

    private record ValidatedCandidateValues(
            String applicationNumber,
            String name,
            String email,
            String phoneNumber,
            String address
    ) {
    }
}
