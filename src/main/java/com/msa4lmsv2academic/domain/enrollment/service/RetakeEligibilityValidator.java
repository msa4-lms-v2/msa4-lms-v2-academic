package com.msa4lmsv2academic.domain.enrollment.service;

import com.msa4lmsv2academic.domain.course.entity.Course;
import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.enrollment.entity.GradeStatus;
import com.msa4lmsv2academic.domain.enrollment.entity.RetakeGradePolicy;
import com.msa4lmsv2academic.domain.enrollment.entity.RetakeRejectionReason;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentApplicationQueryRepository;
import com.msa4lmsv2academic.global.error.EnrollmentRetakeNotAllowedException;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RetakeEligibilityValidator {

    private final EnrollmentApplicationQueryRepository enrollmentApplicationQueryRepository;

    public long validateAndCount(Long studentId, Course course) {
        List<Enrollment> attempts = enrollmentApplicationQueryRepository
                .findNonCancelledCourseEnrollments(studentId, course.getId());

        if (attempts.stream().anyMatch(attempt ->
                attempt.getLecture().getSemester().isCurrent() && attempt.getGradeStatus() == GradeStatus.DRAFT)) {
            reject(RetakeRejectionReason.ACTIVE_ENROLLMENT_EXISTS);
        }
        if (attempts.stream()
                .filter(attempt -> attempt.getGradeStatus() == GradeStatus.OPENED)
                .map(Enrollment::getLetterGrade)
                .filter(Objects::nonNull)
                .anyMatch(RetakeGradePolicy::blocksRetake)) {
            reject(RetakeRejectionReason.RETAKE_BLOCKED_HIGH_GRADE);
        }
        if (attempts.stream().anyMatch(attempt -> attempt.getGradeStatus() == GradeStatus.DRAFT)) {
            reject(RetakeRejectionReason.GRADE_NOT_OPENED);
        }
        if (attempts.stream().anyMatch(attempt ->
                attempt.getGradeStatus() == GradeStatus.OPENED && attempt.getLetterGrade() == null)) {
            reject(RetakeRejectionReason.GRADE_NOT_ENTERED);
        }
        if (attempts.stream()
                .filter(attempt -> attempt.getGradeStatus() == GradeStatus.OPENED)
                .map(Enrollment::getLetterGrade)
                .filter(Objects::nonNull)
                .anyMatch(grade -> !RetakeGradePolicy.isRecognized(grade))) {
            reject(RetakeRejectionReason.INVALID_GRADE_DATA);
        }
        return attempts.size();
    }

    private void reject(RetakeRejectionReason reason) {
        throw new EnrollmentRetakeNotAllowedException(reason);
    }
}
