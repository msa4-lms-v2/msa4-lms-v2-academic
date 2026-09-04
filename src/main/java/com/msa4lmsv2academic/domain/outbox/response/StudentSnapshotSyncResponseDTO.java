package com.msa4lmsv2academic.domain.outbox.response;

import com.msa4lmsv2academic.domain.student.entity.Student;

public record StudentSnapshotSyncResponseDTO(
        Long studentId,
        Long userId,
        String displayName,
        String departmentName,
        Long sourceVersion
) {
    public static StudentSnapshotSyncResponseDTO from(Student student) {
        return new StudentSnapshotSyncResponseDTO(
                student.getId(),
                student.getUser().getId(),
                student.getUser().getName(),
                student.getDepartment().getName(),
                student.getSnapshotVersion()
        );
    }
}
