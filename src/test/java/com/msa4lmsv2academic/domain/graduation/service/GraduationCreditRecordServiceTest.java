package com.msa4lmsv2academic.domain.graduation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.enrollment.entity.GradeStatus;
import com.msa4lmsv2academic.domain.graduation.entity.GraduationCreditExclusionReason;
import com.msa4lmsv2academic.domain.graduation.entity.GraduationCreditRecordResult;
import com.msa4lmsv2academic.domain.graduation.repository.GraduationCreditQueryRepository;
import com.msa4lmsv2academic.domain.graduation.repository.GraduationCreditRecordQueryResult;
import com.msa4lmsv2academic.domain.graduation.request.GraduationCreditRecordSearchRequestDTO;
import com.msa4lmsv2academic.domain.graduation.response.GraduationCreditRecordResponseDTO;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.student.repository.ProfessorStudentScope;
import com.msa4lmsv2academic.domain.student.repository.StudentQueryRepository;
import com.msa4lmsv2academic.global.error.GraduationCreditAccessDeniedException;
import com.msa4lmsv2academic.global.error.StudentNotFoundException;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GraduationCreditRecordServiceTest {

    private static final long STUDENT_ID = 1001L;
    private static final long STUDENT_USER_ID = 2001L;

    @Test
    void classifiesAllRecordsAndAppliesOnlyNewestPassingRetake() {
        GraduationCreditQueryRepository repository = mock(GraduationCreditQueryRepository.class);
        when(repository.isStudentOwnedByUser(STUDENT_ID, STUDENT_USER_ID)).thenReturn(true);
        when(repository.findCreditRecordsByStudentId(STUDENT_ID)).thenReturn(List.of(
                record(1L, 11L, 2024, SemesterTerm.FIRST,
                        EnrollmentStatus.ACTIVE, GradeStatus.OPENED, "B+"),
                record(2L, 11L, 2025, SemesterTerm.FIRST,
                        EnrollmentStatus.ACTIVE, GradeStatus.OPENED, "A"),
                record(3L, 12L, 2025, SemesterTerm.SECOND,
                        EnrollmentStatus.CANCELLED, GradeStatus.OPENED, "A"),
                record(4L, 13L, 2025, SemesterTerm.SECOND,
                        EnrollmentStatus.ACTIVE, GradeStatus.DRAFT, "A+"),
                record(5L, 14L, 2025, SemesterTerm.SECOND,
                        EnrollmentStatus.ACTIVE, GradeStatus.OPENED, null),
                record(6L, 15L, 2025, SemesterTerm.SECOND,
                        EnrollmentStatus.ACTIVE, GradeStatus.OPENED, "F"),
                record(7L, 16L, 2025, SemesterTerm.SECOND,
                        EnrollmentStatus.ACTIVE, GradeStatus.OPENED, "P")
        ));
        GraduationCreditRecordService service = service(repository, mock(StudentQueryRepository.class));

        PageResponseDTO<GraduationCreditRecordResponseDTO> response = service.search(
                STUDENT_ID,
                request(null, null, null, null, "desc"),
                new CurrentUser(STUDENT_USER_ID, "STUDENT")
        );

        assertThat(response.totalCount()).isEqualTo(7);
        assertThat(item(response, 2L).result()).isEqualTo(GraduationCreditRecordResult.APPLIED);
        assertThat(item(response, 2L).appliedCredits()).isEqualTo(3);
        assertThat(item(response, 1L).exclusionReason())
                .isEqualTo(GraduationCreditExclusionReason.RETAKE_DUPLICATE);
        assertThat(item(response, 3L).exclusionReason())
                .isEqualTo(GraduationCreditExclusionReason.ENROLLMENT_CANCELLED);
        assertThat(item(response, 4L).exclusionReason())
                .isEqualTo(GraduationCreditExclusionReason.GRADE_NOT_OPENED);
        assertThat(item(response, 4L).letterGrade()).isNull();
        assertThat(item(response, 5L).exclusionReason())
                .isEqualTo(GraduationCreditExclusionReason.GRADE_NOT_ENTERED);
        assertThat(item(response, 6L).exclusionReason())
                .isEqualTo(GraduationCreditExclusionReason.FAILED_GRADE);
        assertThat(item(response, 7L).exclusionReason())
                .isEqualTo(GraduationCreditExclusionReason.INVALID_GRADE_DATA);
    }

    @Test
    void appliesFiltersAfterRetakeClassification() {
        GraduationCreditQueryRepository repository = mock(GraduationCreditQueryRepository.class);
        when(repository.isStudentOwnedByUser(STUDENT_ID, STUDENT_USER_ID)).thenReturn(true);
        when(repository.findCreditRecordsByStudentId(STUDENT_ID)).thenReturn(List.of(
                record(1L, 11L, 2024, SemesterTerm.FIRST,
                        EnrollmentStatus.ACTIVE, GradeStatus.OPENED, "B+"),
                record(2L, 11L, 2025, SemesterTerm.FIRST,
                        EnrollmentStatus.ACTIVE, GradeStatus.OPENED, "A")
        ));
        GraduationCreditRecordService service = service(repository, mock(StudentQueryRepository.class));

        PageResponseDTO<GraduationCreditRecordResponseDTO> response = service.search(
                STUDENT_ID,
                request(2024, null, null, GraduationCreditRecordResult.APPLIED, "desc"),
                new CurrentUser(STUDENT_USER_ID, "STUDENT")
        );

        assertThat(response.items()).isEmpty();
        assertThat(response.totalCount()).isZero();
    }

    @Test
    void allowsProfessorOnlyThroughExistingStudentScope() {
        GraduationCreditQueryRepository repository = mock(GraduationCreditQueryRepository.class);
        StudentQueryRepository studentQueryRepository = mock(StudentQueryRepository.class);
        ProfessorStudentScope scope = new ProfessorStudentScope(3001L, 4001L);
        when(studentQueryRepository.findProfessorScopeByUserId(5001L)).thenReturn(Optional.of(scope));
        when(repository.isStudentInProfessorScope(STUDENT_ID, scope)).thenReturn(true);
        when(repository.findCreditRecordsByStudentId(STUDENT_ID)).thenReturn(List.of());
        GraduationCreditRecordService service = service(repository, studentQueryRepository);

        PageResponseDTO<GraduationCreditRecordResponseDTO> response = service.search(
                STUDENT_ID,
                request(null, null, null, null, "desc"),
                new CurrentUser(5001L, "PROFESSOR")
        );

        assertThat(response.items()).isEmpty();
        assertThat(response.totalCount()).isZero();
    }

    @Test
    void rejectsOutOfScopeStudentAndMissingAdminTarget() {
        GraduationCreditQueryRepository repository = mock(GraduationCreditQueryRepository.class);
        when(repository.isStudentOwnedByUser(STUDENT_ID, STUDENT_USER_ID)).thenReturn(false);
        when(repository.existsStudentById(STUDENT_ID)).thenReturn(false);
        GraduationCreditRecordService service = service(repository, mock(StudentQueryRepository.class));
        GraduationCreditRecordSearchRequestDTO request = request(null, null, null, null, "desc");

        assertThatThrownBy(() -> service.search(
                STUDENT_ID,
                request,
                new CurrentUser(STUDENT_USER_ID, "STUDENT")
        )).isInstanceOf(GraduationCreditAccessDeniedException.class);
        assertThatThrownBy(() -> service.search(
                STUDENT_ID,
                request,
                new CurrentUser(9001L, "ADMIN")
        )).isInstanceOf(StudentNotFoundException.class);
    }

    private GraduationCreditRecordService service(
            GraduationCreditQueryRepository repository,
            StudentQueryRepository studentQueryRepository
    ) {
        return new GraduationCreditRecordService(repository, studentQueryRepository);
    }

    private GraduationCreditRecordSearchRequestDTO request(
            Integer academicYear,
            SemesterTerm term,
            CompletionType completionType,
            GraduationCreditRecordResult result,
            String sortDirection
    ) {
        return new GraduationCreditRecordSearchRequestDTO(
                1,
                20,
                academicYear,
                term,
                completionType,
                result,
                sortDirection
        );
    }

    private GraduationCreditRecordQueryResult record(
            long enrollmentId,
            long courseId,
            int academicYear,
            SemesterTerm term,
            EnrollmentStatus enrollmentStatus,
            GradeStatus gradeStatus,
            String letterGrade
    ) {
        return new GraduationCreditRecordQueryResult(
                enrollmentId,
                courseId,
                "COURSE-" + courseId,
                "교과목" + courseId,
                (byte) 3,
                CompletionType.MAJOR_REQUIRED,
                (short) academicYear,
                term,
                enrollmentStatus,
                gradeStatus,
                letterGrade
        );
    }

    private GraduationCreditRecordResponseDTO item(
            PageResponseDTO<GraduationCreditRecordResponseDTO> response,
            long enrollmentId
    ) {
        return response.items().stream()
                .filter(item -> item.enrollmentId() == enrollmentId)
                .findFirst()
                .orElseThrow();
    }
}
