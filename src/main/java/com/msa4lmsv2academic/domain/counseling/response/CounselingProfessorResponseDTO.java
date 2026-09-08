package com.msa4lmsv2academic.domain.counseling.response;

import com.msa4lmsv2academic.domain.professor.entity.Professor;

public record CounselingProfessorResponseDTO(
        Long professorId,
        String name,
        String departmentName
) {
    public static CounselingProfessorResponseDTO from(Professor professor) {
        return new CounselingProfessorResponseDTO(
                professor.getId(),
                professor.getUser().getName(),
                professor.getDepartment().getName()
        );
    }
}
