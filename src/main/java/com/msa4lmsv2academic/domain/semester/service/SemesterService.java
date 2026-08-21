package com.msa4lmsv2academic.domain.semester.service;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.repository.SemesterQueryRepository;
import com.msa4lmsv2academic.domain.semester.repository.SemesterRepository;
import com.msa4lmsv2academic.domain.semester.repository.SemesterSearchCondition;
import com.msa4lmsv2academic.domain.semester.repository.SemesterSearchResult;
import com.msa4lmsv2academic.domain.semester.request.SemesterCreateRequestDTO;
import com.msa4lmsv2academic.domain.semester.request.SemesterSearchRequestDTO;
import com.msa4lmsv2academic.domain.semester.response.SemesterResponseDTO;
import com.msa4lmsv2academic.global.error.DuplicateSemesterException;
import com.msa4lmsv2academic.global.error.InvalidSemesterRequestException;
import com.msa4lmsv2academic.global.error.SemesterAccessDeniedException;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SemesterService {

    private static final String TARGET_TYPE = "SEMESTER";
    private static final String CREATE_ACTION = "SEMESTER_CREATE";
    private static final String CURRENT_UNSET_ACTION = "SEMESTER_CURRENT_UNSET";

    private final SemesterRepository semesterRepository;
    private final SemesterQueryRepository semesterQueryRepository;
    private final AuditLogService auditLogService;

    public PageResponseDTO<SemesterResponseDTO> searchSemesters(SemesterSearchRequestDTO request) {
        int page = request.resolvedPage();
        int size = request.resolvedSize();
        long offset = (page - 1L) * size;
        SemesterSearchCondition condition = new SemesterSearchCondition(
                offset,
                size,
                request.academicYear(),
                request.term(),
                request.isCurrent()
        );

        SemesterSearchResult result = semesterQueryRepository.search(condition);
        List<SemesterResponseDTO> items = result.items().stream()
                .map(SemesterResponseDTO::from)
                .toList();
        boolean hasNext = offset + items.size() < result.totalCount();
        return new PageResponseDTO<>(items, result.totalCount(), page, size, hasNext);
    }

    @Transactional
    public SemesterResponseDTO createSemester(SemesterCreateRequestDTO request, CurrentUser currentUser,
                                               String requestId, String ipAddress) {
        if (currentUser == null || !currentUser.isAdmin()) {
            throw new SemesterAccessDeniedException();
        }
        validateRequest(request);
        validateDuplicate(request);

        if (request.resolvedCurrent()) {
            unsetCurrentSemesters(currentUser.id(), requestId, ipAddress);
            semesterRepository.flush();
        }

        Semester semester = Semester.create(
                request.academicYear(),
                request.term(),
                request.startDate(),
                request.endDate(),
                request.enrollmentStartAt(),
                request.enrollmentEndAt(),
                request.resolvedCurrent()
        );

        Semester savedSemester;
        try {
            savedSemester = semesterRepository.saveAndFlush(semester);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateSemesterException();
        }

        auditLogService.record(
                currentUser.id(),
                CREATE_ACTION,
                TARGET_TYPE,
                savedSemester.getId(),
                null,
                snapshot(savedSemester),
                null,
                normalizeNullable(requestId),
                normalizeNullable(ipAddress)
        );
        return SemesterResponseDTO.from(savedSemester);
    }

    private void unsetCurrentSemesters(Long actorId, String requestId, String ipAddress) {
        for (Semester currentSemester : semesterRepository.findCurrentSemestersForUpdate()) {
            Map<String, Object> beforeValue = snapshot(currentSemester);
            currentSemester.unsetCurrent();
            auditLogService.record(
                    actorId,
                    CURRENT_UNSET_ACTION,
                    TARGET_TYPE,
                    currentSemester.getId(),
                    beforeValue,
                    snapshot(currentSemester),
                    "새 현재 학기 등록에 따른 자동 해제",
                    normalizeNullable(requestId),
                    normalizeNullable(ipAddress)
            );
        }
    }

    private void validateRequest(SemesterCreateRequestDTO request) {
        if (request == null
                || request.academicYear() == null
                || request.academicYear() < 1
                || request.term() == null
                || request.startDate() == null
                || request.endDate() == null
                || request.enrollmentStartAt() == null
                || request.enrollmentEndAt() == null) {
            throw new InvalidSemesterRequestException("필수 학기 등록 값이 누락되었거나 올바르지 않습니다.");
        }
        if (!request.isPeriodOrderValid()) {
            throw new InvalidSemesterRequestException(
                    "startDate는 endDate보다 빨라야 하고 enrollmentStartAt은 enrollmentEndAt보다 빨라야 합니다."
            );
        }
    }

    private void validateDuplicate(SemesterCreateRequestDTO request) {
        if (semesterRepository.existsByAcademicYearAndTerm(request.academicYear(), request.term())) {
            throw new DuplicateSemesterException();
        }
    }

    private Map<String, Object> snapshot(Semester semester) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", semester.getId());
        value.put("academicYear", semester.getAcademicYear());
        value.put("term", semester.getTerm().name());
        value.put("startDate", semester.getStartDate().toString());
        value.put("endDate", semester.getEndDate().toString());
        value.put("enrollmentStartAt", semester.getEnrollmentStartAt().toString());
        value.put("enrollmentEndAt", semester.getEnrollmentEndAt().toString());
        value.put("isCurrent", semester.isCurrent());
        return value;
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
