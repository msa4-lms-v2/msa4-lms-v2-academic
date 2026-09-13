package com.msa4lmsv2academic.domain.dashboard;

import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import java.time.LocalDateTime;
import java.util.List;

public record AdminDashboardResponse(
        CurrentSemester currentSemester, Summary summary, List<Task> tasks, List<AcademicStat> academicStats) {
    public record CurrentSemester(Long id, int year, String label) {
        public static CurrentSemester from(Semester semester) {
            return new CurrentSemester(semester.getId(), semester.getAcademicYear(),
                    semester.getTerm() == SemesterTerm.FIRST ? "1학기" : "2학기");
        }
    }
    public record Summary(long lecturePending, long academicChangePending) {}
    public record Task(long id, String type, String requesterName, LocalDateTime requestedAt) {}
    public record AcademicStat(String type, long completed, long pending) {}
}
