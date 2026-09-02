package com.msa4lmsv2academic.domain.professor.service;

import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.professor.repository.ProfessorRepository;
import com.msa4lmsv2academic.domain.professor.response.ProfessorProfileResponseDTO;
import com.msa4lmsv2academic.global.error.ProfessorNotFoundException;
import com.msa4lmsv2academic.global.file.FileStorageService;
import com.msa4lmsv2academic.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfessorProfileService {

    private final ProfessorRepository professorRepository;
    private final FileStorageService fileStorageService;

    public ProfessorProfileResponseDTO getMyProfile(CurrentUser currentUser) {
        Professor professor = professorRepository.findByUserId(currentUser.id())
                .orElseThrow(ProfessorNotFoundException::new);
        String profileImageUrl = professor.getUser().getProfileImageKey() == null
                ? null
                : fileStorageService.presignedDownloadUrl(professor.getUser().getProfileImageKey());
        return ProfessorProfileResponseDTO.from(professor, profileImageUrl);
    }
}
