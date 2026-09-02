package com.msa4lmsv2academic.domain.lecture.service;

import com.msa4lmsv2academic.domain.lecture.repository.ProfessorLectureQueryRepository;
import com.msa4lmsv2academic.domain.lecture.repository.ProfessorLectureSearchResult;
import com.msa4lmsv2academic.domain.lecture.request.ProfessorLectureSearchRequestDTO;
import com.msa4lmsv2academic.domain.lecture.response.ProfessorLectureResponseDTO;
import com.msa4lmsv2academic.global.error.ProfessorLectureAccessDeniedException;
import com.msa4lmsv2academic.global.error.ProfessorNotFoundException;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfessorLectureQueryService {

    private final ProfessorLectureQueryRepository professorLectureQueryRepository;

    public PageResponseDTO<ProfessorLectureResponseDTO> getMyLectures(
            ProfessorLectureSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        validateProfessor(currentUser);
        if (!professorLectureQueryRepository.existsProfessorByUserId(currentUser.id())) {
            throw new ProfessorNotFoundException();
        }

        ProfessorLectureSearchRequestDTO resolvedRequest = request == null
                ? new ProfessorLectureSearchRequestDTO(null, null, null, null, null, null)
                : request;
        int page = resolvedRequest.resolvedPage();
        int size = resolvedRequest.resolvedSize();
        long offset = (page - 1L) * size;
        ProfessorLectureSearchResult result = professorLectureQueryRepository.searchByProfessorUserId(
                currentUser.id(),
                resolvedRequest.academicYear(),
                resolvedRequest.term(),
                resolvedRequest.status(),
                resolvedRequest.current(),
                offset,
                size
        );
        List<ProfessorLectureResponseDTO> items = result.items().stream()
                .map(ProfessorLectureResponseDTO::from)
                .toList();
        boolean hasNext = offset + items.size() < result.totalCount();
        return new PageResponseDTO<>(items, result.totalCount(), page, size, hasNext);
    }

    private void validateProfessor(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || !"PROFESSOR".equals(currentUser.role())) {
            throw new ProfessorLectureAccessDeniedException();
        }
    }
}
