package com.msa4lmsv2academic.domain.outbox.response;

import com.msa4lmsv2academic.domain.semester.entity.Semester;
import java.time.LocalDate;

public record SemesterSnapshotSyncResponseDTO(
        Long semesterId,
        String displayName,
        LocalDate startDate,
        LocalDate endDate,
        Long sourceVersion
) {
    public static SemesterSnapshotSyncResponseDTO from(Semester semester) {
        return new SemesterSnapshotSyncResponseDTO(
                semester.getId(),
                semester.getAcademicYear() + "-" + semester.getTerm().name(),
                semester.getStartDate(),
                semester.getEndDate(),
                semester.getSnapshotVersion()
        );
    }
}
