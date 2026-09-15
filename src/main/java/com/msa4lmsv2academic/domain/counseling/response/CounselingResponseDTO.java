package com.msa4lmsv2academic.domain.counseling.response;

import com.msa4lmsv2academic.domain.counseling.entity.Counseling;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingStatus;
import java.time.LocalDateTime;

public record CounselingResponseDTO(
        Long id,
        Long studentId,
        String studentNumber,
        String studentName,
        String studentDepartmentName,
        Long professorId,
        String professorName,
        String title,
        String question,
        String answer,
        CounselingStatus status,
        LocalDateTime answeredAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CounselingResponseDTO from(Counseling counseling) {
        return new CounselingResponseDTO(
                counseling.getId(), counseling.getStudent().getId(), counseling.getStudent().getStudentNumber(),
                counseling.getStudent().getUser().getName(),
                counseling.getStudent().getDepartment().getName(),
                counseling.getProfessor().getId(), counseling.getProfessor().getUser().getName(),
                counseling.getTitle(), counseling.getQuestion(), counseling.getAnswer(), counseling.getStatus(),
                counseling.getAnsweredAt(), counseling.getCreatedAt(), counseling.getUpdatedAt()
        );
    }

}
