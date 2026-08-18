package com.msa4lmsv2academic.domain.enrollment.service;

import com.msa4lmsv2academic.domain.enrollment.repository.StudentEnrollmentQueryRepository;
import com.msa4lmsv2academic.domain.enrollment.request.StudentEnrollmentSearchRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.response.StudentEnrollmentResponseDTO;
import com.msa4lmsv2academic.global.error.StudentEnrollmentAccessDeniedException;
import com.msa4lmsv2academic.global.error.StudentNotFoundException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentEnrollmentQueryService {

    private final StudentEnrollmentQueryRepository studentEnrollmentQueryRepository;

    public List<StudentEnrollmentResponseDTO> getMyEnrollments(
            StudentEnrollmentSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        validateStudent(currentUser);
        if (!studentEnrollmentQueryRepository.existsStudentByUserId(currentUser.id())) {
            throw new StudentNotFoundException();
        }

        StudentEnrollmentSearchRequestDTO resolvedRequest = request == null
                ? new StudentEnrollmentSearchRequestDTO(null, null)
                : request;
        return studentEnrollmentQueryRepository.findActiveEnrollmentsByStudentUserId(
                        currentUser.id(),
                        resolvedRequest.academicYear(),
                        resolvedRequest.term()
                ).stream()
                .map(StudentEnrollmentResponseDTO::from)
                .toList();
    }

    private void validateStudent(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || !"STUDENT".equals(currentUser.role())) {
            throw new StudentEnrollmentAccessDeniedException();
        }
    }
}
