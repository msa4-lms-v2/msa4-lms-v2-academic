package com.msa4lmsv2academic.domain.professor.controller;

import com.msa4lmsv2academic.domain.professor.repository.ProfessorRepository;
import com.msa4lmsv2academic.domain.lecture.repository.LectureRepository;
import com.msa4lmsv2academic.domain.professor.response.ProfessorCareerResponse;
import com.msa4lmsv2academic.global.error.ProfessorNotFoundException;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class ProfessorCertificateController {
    private final ProfessorRepository professorRepository;
    private final LectureRepository lectureRepository;

    @GetMapping("/api/academic/professors/me/certificate-career")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Transactional(readOnly = true)
    public GlobalResponseDTO<ProfessorCareerResponse> getCareer(@AuthenticationPrincipal CurrentUser user) {
        var professor = professorRepository.findByUserId(user.id()).orElseThrow(ProfessorNotFoundException::new);
        return GlobalResponseDTO.success(ProfessorCareerResponse.from(professor,
                lectureRepository.findCertificateCareer(professor.getId(), LocalDate.now())));
    }
}
