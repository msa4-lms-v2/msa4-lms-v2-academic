package com.msa4lmsv2academic.domain.student.service;

import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.student.repository.StudentRepository;
import com.msa4lmsv2academic.domain.student.response.StudentProfileResponseDTO;
import com.msa4lmsv2academic.global.error.StudentNotFoundException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentProfileResponseDTO getMyProfile(CurrentUser currentUser) {
        Student student = studentRepository.findByUserId(currentUser.id())
                .orElseThrow(StudentNotFoundException::new);
        return StudentProfileResponseDTO.from(student);
    }
}
