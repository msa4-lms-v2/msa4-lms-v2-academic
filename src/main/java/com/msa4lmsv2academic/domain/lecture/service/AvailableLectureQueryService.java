package com.msa4lmsv2academic.domain.lecture.service;

import com.msa4lmsv2academic.domain.lecture.repository.AvailableLectureQueryRepository;
import com.msa4lmsv2academic.domain.lecture.repository.ProfessorLectureSearchResult;
import com.msa4lmsv2academic.domain.lecture.request.AvailableLectureSearchRequestDTO;
import com.msa4lmsv2academic.domain.lecture.response.ProfessorLectureResponseDTO;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailableLectureQueryService {

    private final AvailableLectureQueryRepository repository;

    public PageResponseDTO<ProfessorLectureResponseDTO> search(
            AvailableLectureSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        if (currentUser == null || currentUser.id() == null || !"STUDENT".equals(currentUser.role())) {
            throw new AccessDeniedException("학생만 수강신청 대상 강의를 조회할 수 있습니다.");
        }
        AvailableLectureSearchRequestDTO resolved = request == null
                ? new AvailableLectureSearchRequestDTO(null, null, null, null, null, null, null, null, null, null)
                : request;
        int page = resolved.resolvedPage();
        int size = resolved.resolvedSize();
        long offset = (page - 1L) * size;
        ProfessorLectureSearchResult result = repository.search(
                resolved.academicYear(), resolved.term(), resolved.collegeId(), resolved.departmentId(),
                resolved.targetGrade(), resolved.courseName(), resolved.professorName(),
                resolved.resolvedStatus(), offset, size
        );
        List<ProfessorLectureResponseDTO> items = result.items().stream()
                .map(ProfessorLectureResponseDTO::from)
                .toList();
        return new PageResponseDTO<>(items, result.totalCount(), page, size, offset + items.size() < result.totalCount());
    }
}
