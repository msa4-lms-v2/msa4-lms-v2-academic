package com.msa4lmsv2academic.domain.lecture.service;

import com.msa4lmsv2academic.domain.enrollment.repository.StudentClassQueryRepository;
import com.msa4lmsv2academic.domain.lecture.request.StudentClassSearchRequestDTO;
import com.msa4lmsv2academic.domain.lecture.response.StudentClassResponseDTO;
import com.msa4lmsv2academic.global.error.StudentClassAccessDeniedException;
import com.msa4lmsv2academic.global.error.StudentNotFoundException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentClassQueryService {

    private final StudentClassQueryRepository studentClassQueryRepository;

    public List<StudentClassResponseDTO> getMyClasses(
            StudentClassSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        validateStudent(currentUser);
        if (!studentClassQueryRepository.existsStudentByUserId(currentUser.id())) {
            throw new StudentNotFoundException();
        }

        StudentClassSearchRequestDTO resolvedRequest = request == null
                ? new StudentClassSearchRequestDTO(null, null)
                : request;
        return studentClassQueryRepository.findActiveClassesByStudentUserId(
                        currentUser.id(),
                        resolvedRequest.academicYear(),
                        resolvedRequest.term()
                ).stream()
                .map(StudentClassResponseDTO::from)
                .toList();
    }

    private void validateStudent(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || !"STUDENT".equals(currentUser.role())) {
            throw new StudentClassAccessDeniedException();
        }
    }
}
