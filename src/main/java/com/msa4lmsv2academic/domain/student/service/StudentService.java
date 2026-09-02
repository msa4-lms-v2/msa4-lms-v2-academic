package com.msa4lmsv2academic.domain.student.service;

import com.msa4lmsv2academic.domain.graduation.repository.GraduationCreditQueryRepository;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.student.repository.StudentRepository;
import com.msa4lmsv2academic.domain.student.response.StudentProfileResponseDTO;
import com.msa4lmsv2academic.global.error.StudentNotFoundException;
import com.msa4lmsv2academic.global.file.FileStorageService;
import com.msa4lmsv2academic.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;
    private final GraduationCreditQueryRepository graduationCreditQueryRepository;
    private final FileStorageService fileStorageService;

    public StudentProfileResponseDTO getMyProfile(CurrentUser currentUser) {
        Student student = studentRepository.findByUserId(currentUser.id())
                .orElseThrow(StudentNotFoundException::new);
        int totalCredits = graduationCreditQueryRepository.sumTotalCreditsByStudentId(student.getId());
        String profileImageUrl = student.getUser().getProfileImageKey() == null
                ? null
                : fileStorageService.presignedDownloadUrl(student.getUser().getProfileImageKey());
        return StudentProfileResponseDTO.from(student, totalCredits, profileImageUrl);
    }
}
