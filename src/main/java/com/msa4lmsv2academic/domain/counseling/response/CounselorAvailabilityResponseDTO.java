package com.msa4lmsv2academic.domain.counseling.response;

import com.msa4lmsv2academic.domain.counseling.entity.CounselorAvailability;
import java.time.DayOfWeek;
import java.time.LocalDate;

public record CounselorAvailabilityResponseDTO(
        Long id,
        Long professorId,
        String professorName,
        DayOfWeek dayOfWeek,
        String startTime,
        String endTime,
        LocalDate validFrom,
        LocalDate validTo
) {
    public static CounselorAvailabilityResponseDTO from(CounselorAvailability availability) {
        return new CounselorAvailabilityResponseDTO(
                availability.getId(),
                availability.getProfessor().getId(),
                availability.getProfessor().getUser().getName(),
                availability.getDayOfWeek(),
                availability.getStartTime(),
                availability.getEndTime(),
                availability.getValidFrom(),
                availability.getValidTo()
        );
    }
}
