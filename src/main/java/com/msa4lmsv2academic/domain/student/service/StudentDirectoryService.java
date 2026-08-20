package com.msa4lmsv2academic.domain.student.service;

import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.domain.student.repository.ProfessorStudentScope;
import com.msa4lmsv2academic.domain.student.repository.StudentQueryRepository;
import com.msa4lmsv2academic.domain.student.repository.StudentSearchCondition;
import com.msa4lmsv2academic.domain.student.repository.StudentSearchResult;
import com.msa4lmsv2academic.domain.student.request.StudentSearchRequestDTO;
import com.msa4lmsv2academic.domain.student.response.StudentSummaryResponseDTO;
import com.msa4lmsv2academic.global.error.InvalidStudentSearchRequestException;
import com.msa4lmsv2academic.global.error.ProfessorNotFoundException;
import com.msa4lmsv2academic.global.error.StudentDirectoryAccessDeniedException;
import com.msa4lmsv2academic.global.response.PageRes;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentDirectoryService {

    private static final Set<AcademicStatus> PROFESSOR_VISIBLE_STATUSES =
            Set.of(AcademicStatus.ENROLLED, AcademicStatus.ON_LEAVE);
    private static final Set<String> SORT_FIELDS = Set.of("name", "gradeLevel", "admissionYear");

    private final StudentQueryRepository studentQueryRepository;

    public PageRes<StudentSummaryResponseDTO> searchStudents(StudentSearchRequestDTO request,
                                                              CurrentUser currentUser) {
        validateRequest(request);
        boolean admin = validateRole(currentUser);
        ProfessorStudentScope professorScope = admin ? null : resolveProfessorScope(currentUser.id());
        validateAcademicStatus(request.academicStatus(), admin);

        int page = request.resolvedPage();
        int size = request.resolvedSize();
        long offset = (page - 1L) * size;
        StudentSearchCondition condition = new StudentSearchCondition(
                offset,
                size,
                request.normalizedKeyword(),
                request.departmentId(),
                request.gradeLevel(),
                request.admissionYear(),
                request.academicStatus(),
                request.resolvedSortBy(),
                request.descending(),
                professorScope
        );

        StudentSearchResult result = studentQueryRepository.search(condition);
        List<StudentSummaryResponseDTO> items = result.items().stream()
                .map(StudentSummaryResponseDTO::from)
                .toList();
        boolean hasNext = offset + items.size() < result.totalCount();
        return new PageRes<>(items, result.totalCount(), page, size, hasNext);
    }

    private void validateRequest(StudentSearchRequestDTO request) {
        if (request == null) {
            throw new InvalidStudentSearchRequestException("학생 검색 조건이 필요합니다.");
        }
        if (!SORT_FIELDS.contains(request.resolvedSortBy())) {
            throw new InvalidStudentSearchRequestException(
                    "sortBy는 name, gradeLevel, admissionYear 중 하나여야 합니다."
            );
        }
        if (request.sortDirection() != null
                && !"asc".equals(request.sortDirection())
                && !"desc".equals(request.sortDirection())) {
            throw new InvalidStudentSearchRequestException("sortDirection은 asc 또는 desc여야 합니다.");
        }
    }

    private boolean validateRole(CurrentUser currentUser) {
        if (currentUser == null) {
            throw new StudentDirectoryAccessDeniedException("인증된 교수 또는 관리자만 학생 목록을 조회할 수 있습니다.");
        }
        if (currentUser.isAdmin()) {
            return true;
        }
        if ("PROFESSOR".equals(currentUser.role())) {
            return false;
        }
        throw new StudentDirectoryAccessDeniedException("교수 또는 관리자만 학생 목록을 조회할 수 있습니다.");
    }

    private ProfessorStudentScope resolveProfessorScope(Long userId) {
        return studentQueryRepository.findProfessorScopeByUserId(userId)
                .orElseThrow(ProfessorNotFoundException::new);
    }

    private void validateAcademicStatus(AcademicStatus academicStatus, boolean admin) {
        if (!admin && academicStatus != null && !PROFESSOR_VISIBLE_STATUSES.contains(academicStatus)) {
            throw new StudentDirectoryAccessDeniedException(
                    "교수는 재학 또는 휴학 상태의 학생만 조회할 수 있습니다."
            );
        }
    }
}
