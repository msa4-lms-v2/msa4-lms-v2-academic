package com.msa4lmsv2academic.domain.gradeperiod.service;

import com.msa4lmsv2academic.domain.gradeperiod.entity.GradeOperationPeriod;
import com.msa4lmsv2academic.domain.gradeperiod.entity.GradeOperationType;
import com.msa4lmsv2academic.domain.gradeperiod.repository.GradeOperationPeriodRepository;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.global.error.GradeManagementConflictException;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradeOperationPeriodService {

    private final GradeOperationPeriodRepository gradeOperationPeriodRepository;

    @Transactional
    public void upsert(
            Semester semester,
            GradeOperationType operationType,
            LocalDate startDate,
            LocalDate endDate,
            boolean active
    ) {
        gradeOperationPeriodRepository.findBySemesterIdAndOperationTypeForUpdate(
                        semester.getId(), operationType
                )
                .ifPresentOrElse(
                        period -> period.change(startDate, endDate, active),
                        () -> gradeOperationPeriodRepository.save(
                                GradeOperationPeriod.create(
                                        semester, operationType, startDate, endDate, active
                                )
                        )
                );
    }

    public void requireGradeEntryAllowed(Long semesterId) {
        requireAllowed(semesterId, GradeOperationType.GRADE_ENTRY, LocalDate.now());
    }

    public void requireGradeCorrectionAllowed(Long semesterId) {
        requireAllowed(semesterId, GradeOperationType.GRADE_CORRECTION, LocalDate.now());
    }

    void requireAllowed(Long semesterId, GradeOperationType operationType, LocalDate date) {
        boolean allowed = gradeOperationPeriodRepository
                .findBySemesterIdAndOperationType(semesterId, operationType)
                .filter(period -> period.accepts(date))
                .isPresent();
        if (!allowed) {
            throw new GradeManagementConflictException(message(operationType));
        }
    }

    private String message(GradeOperationType operationType) {
        return operationType == GradeOperationType.GRADE_ENTRY
                ? "성적입력 기간이 아닙니다."
                : "성적정정 기간이 아닙니다.";
    }
}
