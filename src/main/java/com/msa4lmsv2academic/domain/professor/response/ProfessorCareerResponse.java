package com.msa4lmsv2academic.domain.professor.response;

import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.outbox.response.ProfessorCertificateEligibilityResponseDTO;
import java.time.LocalDate;
import java.util.List;

public record ProfessorCareerResponse(Long userId, ProfessorCertificateEligibilityResponseDTO professor,
                                      List<Teaching> lectures) {
    public record Teaching(Long classId, short academicYear, String term, String courseCode, String courseName,
                           String sectionNo, byte credits, LocalDate startDate, LocalDate endDate) {}
    public static ProfessorCareerResponse from(Professor professor, List<Lecture> lectures) {
        return new ProfessorCareerResponse(professor.getUser().getId(),
                ProfessorCertificateEligibilityResponseDTO.from(professor),
                lectures.stream().map(l -> new Teaching(l.getId(), l.getSemester().getAcademicYear(),
                        l.getSemester().getTerm().name(), l.getCourse().getCode(), l.getCourse().getName(),
                        l.getSectionNo(), l.getCourse().getCredits(), l.getSemester().getStartDate(),
                        l.getSemester().getEndDate())).toList());
    }
}
