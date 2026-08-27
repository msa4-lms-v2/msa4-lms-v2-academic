package com.msa4lmsv2academic.domain.academicstatus.service;

import com.msa4lmsv2academic.domain.academicstatus.request.AcademicStatusHistorySearchRequestDTO;
import com.msa4lmsv2academic.domain.academicstatus.response.AcademicStatusHistoryResponseDTO;
import com.msa4lmsv2academic.domain.student.repository.ProfessorStudentScope;
import com.msa4lmsv2academic.domain.student.repository.StudentQueryRepository;
import com.msa4lmsv2academic.domain.withdrawal.repository.AcademicStatusHistoryQueryRepository;
import com.msa4lmsv2academic.domain.withdrawal.repository.AcademicStatusHistorySearchCondition;
import com.msa4lmsv2academic.global.error.AcademicStatusHistoryAccessDeniedException;
import com.msa4lmsv2academic.global.error.ProfessorNotFoundException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicStatusHistoryService {

    private final AcademicStatusHistoryQueryRepository historyQueryRepository;
    private final StudentQueryRepository studentQueryRepository;

    public Page<AcademicStatusHistoryResponseDTO> search(AcademicStatusHistorySearchRequestDTO request,
                                                        Pageable pageable, CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || currentUser.id() <= 0) {
            throw new AcademicStatusHistoryAccessDeniedException();
        }
        Long ownerUserId = null;
        ProfessorStudentScope professorScope = null;
        if ("STUDENT".equals(currentUser.role())) {
            ownerUserId = currentUser.id();
        } else if ("PROFESSOR".equals(currentUser.role())) {
            professorScope = studentQueryRepository.findProfessorScopeByUserId(currentUser.id())
                    .orElseThrow(ProfessorNotFoundException::new);
        } else if (!currentUser.isAdmin()) {
            throw new AcademicStatusHistoryAccessDeniedException();
        }
        AcademicStatusHistorySearchCondition condition = new AcademicStatusHistorySearchCondition(
                request.normalizedKeyword(), request.studentId(), request.departmentId(), request.previousStatus(),
                request.newStatus(), request.sourceType(), request.fromDate(), request.toDate(), ownerUserId, professorScope
        );
        return historyQueryRepository.search(condition, pageable).map(AcademicStatusHistoryResponseDTO::from);
    }
}
