package com.msa4lmsv2academic.domain.enrollment.service;

import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentCancellationRejectionReason;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentHistory;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentCancellationQueryRepository;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentHistoryRepository;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentRepository;
import com.msa4lmsv2academic.domain.enrollment.response.EnrollmentApplicationReasonResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.response.StudentEnrollmentCancellationResponseDTO;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.global.error.EnrollmentApplicationRejectedException;
import com.msa4lmsv2academic.global.error.EnrollmentAcademicStatusNotAllowedException;
import com.msa4lmsv2academic.global.error.EnrollmentCancellationAccessDeniedException;
import com.msa4lmsv2academic.global.error.EnrollmentNotFoundException;
import com.msa4lmsv2academic.global.error.InvalidEnrollmentCancellationRequestException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentEnrollmentCancellationService {

    private final EnrollmentCancellationQueryRepository queryRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentHistoryRepository historyRepository;
    private final EnrollmentAcademicStatusValidator academicStatusValidator;

    @Transactional
    public StudentEnrollmentCancellationResponseDTO cancel(Long enrollmentId, CurrentUser currentUser) {
        validateRequest(enrollmentId, currentUser);
        Enrollment enrollment = queryRepository.findOwnedEnrollmentForUpdate(enrollmentId, currentUser.id())
                .orElseThrow(EnrollmentNotFoundException::new);
        validateAcademicStatus(enrollment);
        validateActive(enrollment);
        LocalDateTime cancelledAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        validateCancellationPeriod(enrollment.getLecture().getSemester(), cancelledAt);

        enrollment.cancel();
        enrollmentRepository.saveAndFlush(enrollment);
        historyRepository.saveAndFlush(EnrollmentHistory.fromCancellation(enrollment, cancelledAt));
        return StudentEnrollmentCancellationResponseDTO.from(enrollment, cancelledAt);
    }

    private void validateRequest(Long enrollmentId, CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || !"STUDENT".equals(currentUser.role())) {
            throw new EnrollmentCancellationAccessDeniedException();
        }
        if (enrollmentId == null || enrollmentId <= 0) {
            throw new InvalidEnrollmentCancellationRequestException();
        }
    }

    private void validateActive(Enrollment enrollment) {
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            reject(EnrollmentCancellationRejectionReason.ENROLLMENT_ALREADY_CANCELLED);
        }
    }

    private void validateAcademicStatus(Enrollment enrollment) {
        try {
            academicStatusValidator.validate(enrollment.getStudent().getAcademicStatus());
        } catch (EnrollmentAcademicStatusNotAllowedException exception) {
            throw new EnrollmentApplicationRejectedException(List.of(
                    EnrollmentApplicationReasonResponseDTO.from(
                            exception.getReason().name(),
                            exception.getReason().getMessage()
                    )
            ));
        }
    }

    private void validateCancellationPeriod(Semester semester, LocalDateTime now) {
        if (semester.getEnrollmentStartAt() == null || semester.getEnrollmentEndAt() == null
                || now.isBefore(semester.getEnrollmentStartAt()) || now.isAfter(semester.getEnrollmentEndAt())) {
            reject(EnrollmentCancellationRejectionReason.ENROLLMENT_PERIOD_CLOSED);
        }
    }

    private void reject(EnrollmentCancellationRejectionReason reason) {
        throw new EnrollmentApplicationRejectedException(List.of(
                EnrollmentApplicationReasonResponseDTO.from(reason.name(), reason.getMessage())
        ));
    }
}
