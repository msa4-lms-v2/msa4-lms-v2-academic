package com.msa4lmsv2academic.domain.enrollment.service;

import com.msa4lmsv2academic.domain.enrollment.repository.StudentTimetableQueryRepository;
import com.msa4lmsv2academic.domain.enrollment.request.StudentTimetableSearchRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.response.StudentTimetableEntryResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.response.StudentTimetableResponseDTO;
import com.msa4lmsv2academic.global.error.StudentEnrollmentAccessDeniedException;
import com.msa4lmsv2academic.global.error.StudentNotFoundException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentTimetableQueryService {

    private final StudentTimetableQueryRepository queryRepository;

    public StudentTimetableResponseDTO getMyTimetable(
            StudentTimetableSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        validateStudent(currentUser);
        if (!queryRepository.existsStudentByUserId(currentUser.id())) {
            throw new StudentNotFoundException();
        }

        var items = queryRepository.findActiveTimetable(
                        currentUser.id(),
                        request.academicYear(),
                        request.term()
                ).stream()
                .map(StudentTimetableEntryResponseDTO::from)
                .toList();
        return StudentTimetableResponseDTO.from(request.academicYear(), request.term(), items);
    }

    private void validateStudent(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || !"STUDENT".equals(currentUser.role())) {
            throw new StudentEnrollmentAccessDeniedException();
        }
    }
}
