package com.msa4lmsv2academic.domain.gradeperiod.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.gradeperiod.entity.GradeOperationPeriod;
import com.msa4lmsv2academic.domain.gradeperiod.entity.GradeOperationType;
import com.msa4lmsv2academic.domain.gradeperiod.repository.GradeOperationPeriodRepository;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.global.error.GradeManagementConflictException;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GradeOperationPeriodServiceTest {

    private static final Long SEMESTER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 11);

    @Mock
    private GradeOperationPeriodRepository gradeOperationPeriodRepository;

    @Mock
    private Semester semester;

    @InjectMocks
    private GradeOperationPeriodService gradeOperationPeriodService;

    @Test
    void allowsBothInclusiveBoundaryDates() {
        GradeOperationPeriod period = GradeOperationPeriod.create(
                semester, GradeOperationType.GRADE_ENTRY, TODAY, TODAY.plusDays(2), true
        );
        when(gradeOperationPeriodRepository.findBySemesterIdAndOperationType(
                SEMESTER_ID, GradeOperationType.GRADE_ENTRY
        )).thenReturn(Optional.of(period));

        assertThatCode(() -> gradeOperationPeriodService.requireAllowed(
                SEMESTER_ID, GradeOperationType.GRADE_ENTRY, TODAY
        )).doesNotThrowAnyException();
        assertThatCode(() -> gradeOperationPeriodService.requireAllowed(
                SEMESTER_ID, GradeOperationType.GRADE_ENTRY, TODAY.plusDays(2)
        )).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingOrInactivePeriodWithOperationSpecificMessage() {
        when(gradeOperationPeriodRepository.findBySemesterIdAndOperationType(
                SEMESTER_ID, GradeOperationType.GRADE_ENTRY
        )).thenReturn(Optional.empty());
        when(gradeOperationPeriodRepository.findBySemesterIdAndOperationType(
                SEMESTER_ID, GradeOperationType.GRADE_CORRECTION
        )).thenReturn(Optional.of(GradeOperationPeriod.create(
                semester, GradeOperationType.GRADE_CORRECTION,
                TODAY.minusDays(1), TODAY.plusDays(1), false
        )));

        assertThatThrownBy(() -> gradeOperationPeriodService.requireAllowed(
                SEMESTER_ID, GradeOperationType.GRADE_ENTRY, TODAY
        )).isInstanceOf(GradeManagementConflictException.class)
                .hasMessage("성적입력 기간이 아닙니다.");
        assertThatThrownBy(() -> gradeOperationPeriodService.requireAllowed(
                SEMESTER_ID, GradeOperationType.GRADE_CORRECTION, TODAY
        )).isInstanceOf(GradeManagementConflictException.class)
                .hasMessage("성적정정 기간이 아닙니다.");
    }
}
