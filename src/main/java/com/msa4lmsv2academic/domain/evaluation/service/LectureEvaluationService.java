package com.msa4lmsv2academic.domain.evaluation.service;

import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.evaluation.entity.LectureEvaluation;
import com.msa4lmsv2academic.domain.evaluation.repository.LectureEvaluationEnrollmentRepository;
import com.msa4lmsv2academic.domain.evaluation.repository.LectureEvaluationRepository;
import com.msa4lmsv2academic.domain.evaluation.request.LectureEvaluationSubmitRequestDTO;
import com.msa4lmsv2academic.domain.evaluation.response.LectureEvaluationResponseDTO;
import com.msa4lmsv2academic.global.error.LectureEvaluationAccessDeniedException;
import com.msa4lmsv2academic.global.error.LectureEvaluationConflictException;
import com.msa4lmsv2academic.global.error.LectureEvaluationEnrollmentNotFoundException;
import com.msa4lmsv2academic.global.error.InvalidLectureEvaluationRequestException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LectureEvaluationService {

    private static final int MAX_RATING_COUNT = 20;
    private static final int MAX_QUESTION_CODE_LENGTH = 100;
    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;

    private final LectureEvaluationRepository lectureEvaluationRepository;
    private final LectureEvaluationEnrollmentRepository enrollmentRepository;
    private final LectureEvaluationTimeProvider timeProvider;

    @Transactional
    public LectureEvaluationResponseDTO submit(
            LectureEvaluationSubmitRequestDTO request,
            CurrentUser currentUser
    ) {
        validateStudent(currentUser);
        validateRequest(request);

        Enrollment enrollment = enrollmentRepository.findOwnedEnrollmentForUpdate(
                        request.enrollmentId(),
                        currentUser.id()
                )
                .orElseThrow(LectureEvaluationEnrollmentNotFoundException::new);
        validateEnrollmentStatus(enrollment);

        LocalDateTime submittedAt = timeProvider.now();
        if (!enrollment.getLecture().getSemester().isEvaluationOpenAt(submittedAt)) {
            throw new LectureEvaluationConflictException("현재는 해당 학기의 강의평가 기간이 아닙니다.");
        }
        if (lectureEvaluationRepository.existsByEnrollment_Id(enrollment.getId())) {
            throw new LectureEvaluationConflictException("이미 제출한 강의평가는 다시 제출할 수 없습니다.");
        }

        LectureEvaluation evaluation = LectureEvaluation.create(
                enrollment,
                request.ratings(),
                request.comment(),
                submittedAt
        );
        try {
            LectureEvaluation saved = lectureEvaluationRepository.saveAndFlush(evaluation);
            return LectureEvaluationResponseDTO.from(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new LectureEvaluationConflictException("이미 제출한 강의평가는 다시 제출할 수 없습니다.");
        }
    }

    private void validateStudent(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || !"STUDENT".equals(currentUser.role())) {
            throw new LectureEvaluationAccessDeniedException();
        }
    }

    private void validateRequest(LectureEvaluationSubmitRequestDTO request) {
        if (request == null || request.enrollmentId() == null || request.enrollmentId() <= 0) {
            throw new InvalidLectureEvaluationRequestException("평가할 수강 내역 ID가 올바르지 않습니다.");
        }
        Map<String, Integer> ratings = request.ratings();
        if (ratings == null || ratings.isEmpty() || ratings.size() > MAX_RATING_COUNT) {
            throw new InvalidLectureEvaluationRequestException("평가 문항은 1개 이상 20개 이하로 입력해야 합니다.");
        }
        boolean invalidRating = ratings.entrySet().stream().anyMatch(entry ->
                entry.getKey() == null
                        || entry.getKey().isBlank()
                        || entry.getKey().length() > MAX_QUESTION_CODE_LENGTH
                        || entry.getValue() == null
                        || entry.getValue() < MIN_RATING
                        || entry.getValue() > MAX_RATING
        );
        if (invalidRating) {
            throw new InvalidLectureEvaluationRequestException(
                    "평가 문항 코드는 100자 이하로 입력하고 점수는 1점부터 5점까지 입력해야 합니다."
            );
        }
        if (request.comment() != null && request.comment().length() > 2000) {
            throw new InvalidLectureEvaluationRequestException("서술형 의견은 2000자 이하로 입력해야 합니다.");
        }
    }

    private void validateEnrollmentStatus(Enrollment enrollment) {
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new LectureEvaluationConflictException("현재 수강 중인 강의만 평가할 수 있습니다.");
        }
    }
}
