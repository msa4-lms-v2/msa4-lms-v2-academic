package com.msa4lmsv2academic.domain.enrollment.service;

import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentApplicationRejectionReason;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentHistory;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentApplicationQueryRepository;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentHistoryRepository;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentRepository;
import com.msa4lmsv2academic.domain.enrollment.request.StudentEnrollmentCreateRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.response.EnrollmentApplicationReasonResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.response.StudentEnrollmentCreateResponseDTO;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.lecture.entity.LectureStatus;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.global.error.EnrollmentAcademicStatusNotAllowedException;
import com.msa4lmsv2academic.global.error.EnrollmentApplicationRejectedException;
import com.msa4lmsv2academic.global.error.EnrollmentCreditLimitNotAllowedException;
import com.msa4lmsv2academic.global.error.EnrollmentLectureNotFoundException;
import com.msa4lmsv2academic.global.error.EnrollmentPrerequisiteRetakeNotAllowedException;
import com.msa4lmsv2academic.global.error.InvalidEnrollmentApplicationRequestException;
import com.msa4lmsv2academic.global.error.StudentEnrollmentAccessDeniedException;
import com.msa4lmsv2academic.global.error.StudentNotFoundException;
import com.msa4lmsv2academic.global.idempotency.AcademicIdempotencyKey;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentEnrollmentApplicationService {
    private final EnrollmentApplicationQueryRepository queryRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentHistoryRepository historyRepository;
    private final EnrollmentAcademicStatusValidator academicStatusValidator;
    private final EnrollmentCourseRuleValidator courseRuleValidator;
    private final EnrollmentIdempotencyService idempotencyService;

    // 학생 → 강의 순서로 잠급니다. 잠금 대기 전 읽기가 있어도 대기 후 집계는 최신 커밋을 읽어야 합니다.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public GlobalResponseDTO<StudentEnrollmentCreateResponseDTO> create(
            StudentEnrollmentCreateRequestDTO request, String key, CurrentUser currentUser
    ) {
        validateRequest(request, key, currentUser);
        Student student = queryRepository.findStudentByUserIdForUpdate(currentUser.id())
                .orElseThrow(StudentNotFoundException::new);
        String hash = idempotencyService.hash(request);
        var replay = idempotencyService.replay(key, student.getId(), hash, LocalDateTime.now());
        if (replay.isPresent()) {
            return replay.orElseThrow();
        }

        Lecture lecture = queryRepository.findLectureForUpdate(request.lectureId())
                .orElseThrow(EnrollmentLectureNotFoundException::new);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime recordedAt = now.truncatedTo(ChronoUnit.SECONDS);
        AcademicIdempotencyKey reserved = idempotencyService.reserve(key, student.getId(), hash, recordedAt);
        validateAcademicStatus(student);
        // 전역 키 예약이 다른 transaction을 기다린 경우에도 실제 검증 시점의 기간을 사용합니다.
        validateLecture(student, lecture, LocalDateTime.now());
        validateCourseRules(student, lecture);

        Enrollment enrollment = enrollmentRepository.saveAndFlush(Enrollment.create(student, lecture, recordedAt));
        historyRepository.saveAndFlush(EnrollmentHistory.from(enrollment));
        GlobalResponseDTO<StudentEnrollmentCreateResponseDTO> response =
                GlobalResponseDTO.success(StudentEnrollmentCreateResponseDTO.from(enrollment));
        idempotencyService.complete(reserved, response);
        return response;
    }

    private void validateRequest(StudentEnrollmentCreateRequestDTO request, String key, CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || !"STUDENT".equals(currentUser.role())) {
            throw new StudentEnrollmentAccessDeniedException();
        }
        if (request == null || request.lectureId() == null || request.lectureId() <= 0
                || key == null || key.isBlank() || key.length() > 100
                || key.chars().anyMatch(Character::isWhitespace)) {
            throw new InvalidEnrollmentApplicationRequestException();
        }
    }

    private void validateAcademicStatus(Student student) {
        try {
            academicStatusValidator.validate(student.getAcademicStatus());
        } catch (EnrollmentAcademicStatusNotAllowedException exception) {
            throw new EnrollmentApplicationRejectedException(List.of(EnrollmentApplicationReasonResponseDTO.from(
                    exception.getReason().name(), exception.getReason().getMessage())));
        }
    }

    private void validateLecture(Student student, Lecture lecture, LocalDateTime now) {
        if (lecture.getStatus() != LectureStatus.OPEN) {
            reject(EnrollmentApplicationRejectionReason.LECTURE_NOT_OPEN);
        }
        Semester semester = lecture.getSemester();
        if (semester.getEnrollmentStartAt() == null || semester.getEnrollmentEndAt() == null
                || now.isBefore(semester.getEnrollmentStartAt()) || now.isAfter(semester.getEnrollmentEndAt())) {
            reject(EnrollmentApplicationRejectionReason.ENROLLMENT_PERIOD_CLOSED);
        }
        if (queryRepository.existsActiveEnrollment(student.getId(), lecture.getId())) {
            reject(EnrollmentApplicationRejectionReason.DUPLICATE_ENROLLMENT);
        }
        if (queryRepository.countActiveEnrollments(lecture.getId()) >= lecture.getCapacity()) {
            reject(EnrollmentApplicationRejectionReason.CAPACITY_EXCEEDED);
        }
        if (queryRepository.hasScheduleConflict(student.getId(), lecture)) {
            reject(EnrollmentApplicationRejectionReason.SCHEDULE_CONFLICT);
        }
    }

    private void validateCourseRules(Student student, Lecture lecture) {
        try {
            courseRuleValidator.validate(student.getId(), lecture);
        } catch (EnrollmentCreditLimitNotAllowedException exception) {
            throw new EnrollmentApplicationRejectedException(List.of(EnrollmentApplicationReasonResponseDTO.from(
                    exception.getReason().name(), exception.getReason().getMessage())));
        } catch (EnrollmentPrerequisiteRetakeNotAllowedException exception) {
            throw new EnrollmentApplicationRejectedException(exception.getReasons().stream()
                    .map(reason -> EnrollmentApplicationReasonResponseDTO.from(reason.name(), reason.getMessage())).toList());
        }
    }

    private void reject(EnrollmentApplicationRejectionReason reason) {
        throw EnrollmentApplicationRejectedException.from(reason);
    }
}
