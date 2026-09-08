package com.msa4lmsv2academic.domain.evaluation.service;

import com.msa4lmsv2academic.domain.evaluation.entity.LectureEvaluation;
import com.msa4lmsv2academic.domain.evaluation.repository.ProfessorLectureEvaluationQueryRepository;
import com.msa4lmsv2academic.domain.evaluation.repository.ProfessorLectureEvaluationQueryResult;
import com.msa4lmsv2academic.domain.evaluation.repository.ProfessorLectureEvaluationSearchResult;
import com.msa4lmsv2academic.domain.evaluation.request.ProfessorLectureEvaluationSearchRequestDTO;
import com.msa4lmsv2academic.domain.evaluation.response.ProfessorLectureEvaluationResponseDTO;
import com.msa4lmsv2academic.global.error.ProfessorLectureAccessDeniedException;
import com.msa4lmsv2academic.global.error.ProfessorNotFoundException;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfessorLectureEvaluationQueryService {

    private static final int DECIMAL_SCALE = 2;
    private static final BigDecimal PERCENT = new BigDecimal("100");

    private final ProfessorLectureEvaluationQueryRepository queryRepository;

    public PageResponseDTO<ProfessorLectureEvaluationResponseDTO> getMyResults(
            ProfessorLectureEvaluationSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        validateProfessor(currentUser);
        if (!queryRepository.existsProfessorByUserId(currentUser.id())) {
            throw new ProfessorNotFoundException();
        }

        ProfessorLectureEvaluationSearchRequestDTO resolvedRequest = request == null
                ? new ProfessorLectureEvaluationSearchRequestDTO(null, null, null, null, null, null)
                : request;
        int page = resolvedRequest.resolvedPage();
        int size = resolvedRequest.resolvedSize();
        long offset = (page - 1L) * size;
        ProfessorLectureEvaluationSearchResult searchResult = queryRepository
                .searchLecturesByProfessorUserId(
                        currentUser.id(),
                        resolvedRequest.lectureId(),
                        resolvedRequest.academicYear(),
                        resolvedRequest.term(),
                        resolvedRequest.current(),
                        offset,
                        size
                );

        List<Long> lectureIds = searchResult.items().stream()
                .map(ProfessorLectureEvaluationQueryResult::lectureId)
                .toList();
        Map<Long, List<LectureEvaluation>> responsesByLectureId = groupByLectureId(
                queryRepository.findEvaluationResponses(currentUser.id(), lectureIds)
        );
        List<ProfessorLectureEvaluationResponseDTO> items = searchResult.items().stream()
                .map(lecture -> summarize(
                        lecture,
                        responsesByLectureId.getOrDefault(lecture.lectureId(), List.of())
                ))
                .toList();
        boolean hasNext = offset + items.size() < searchResult.totalCount();
        return new PageResponseDTO<>(items, searchResult.totalCount(), page, size, hasNext);
    }

    private Map<Long, List<LectureEvaluation>> groupByLectureId(List<LectureEvaluation> evaluations) {
        Map<Long, List<LectureEvaluation>> responsesByLectureId = new LinkedHashMap<>();
        for (LectureEvaluation evaluation : evaluations) {
            Long lectureId = evaluation.getEnrollment().getLecture().getId();
            responsesByLectureId.computeIfAbsent(lectureId, ignored -> new ArrayList<>())
                    .add(evaluation);
        }
        return responsesByLectureId;
    }

    private ProfessorLectureEvaluationResponseDTO summarize(
            ProfessorLectureEvaluationQueryResult lecture,
            List<LectureEvaluation> evaluations
    ) {
        Map<String, RatingAccumulator> ratingsByQuestion = new TreeMap<>();
        long totalRatingSum = 0L;
        long totalRatingCount = 0L;
        List<String> comments = new ArrayList<>();

        for (LectureEvaluation evaluation : evaluations) {
            for (Map.Entry<String, Integer> rating : evaluation.getRatings().entrySet()) {
                ratingsByQuestion.computeIfAbsent(rating.getKey(), ignored -> new RatingAccumulator())
                        .add(rating.getValue());
                totalRatingSum += rating.getValue();
                totalRatingCount++;
            }
            if (evaluation.getComment() != null && !evaluation.getComment().isBlank()) {
                comments.add(evaluation.getComment());
            }
        }

        Map<String, BigDecimal> questionAverages = new LinkedHashMap<>();
        ratingsByQuestion.forEach((question, accumulator) ->
                questionAverages.put(question, accumulator.average())
        );
        BigDecimal overallAverage = totalRatingCount == 0L
                ? null
                : average(totalRatingSum, totalRatingCount);
        long responseCount = evaluations.size();

        return new ProfessorLectureEvaluationResponseDTO(
                lecture.lectureId(),
                lecture.courseCode(),
                lecture.courseName(),
                lecture.sectionNo(),
                lecture.academicYear(),
                lecture.term(),
                lecture.activeEnrollmentCount(),
                responseCount,
                responseRate(responseCount, lecture.activeEnrollmentCount()),
                responseCount > 0L,
                overallAverage,
                Collections.unmodifiableMap(new LinkedHashMap<>(questionAverages)),
                List.copyOf(comments)
        );
    }

    private BigDecimal responseRate(long responseCount, long enrollmentCount) {
        if (enrollmentCount == 0L) {
            return BigDecimal.ZERO.setScale(DECIMAL_SCALE);
        }
        return BigDecimal.valueOf(responseCount)
                .multiply(PERCENT)
                .divide(BigDecimal.valueOf(enrollmentCount), DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal average(long sum, long count) {
        return BigDecimal.valueOf(sum)
                .divide(BigDecimal.valueOf(count), DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    private final class RatingAccumulator {

        private long sum;
        private long count;

        private void add(int rating) {
            sum += rating;
            count++;
        }

        private BigDecimal average() {
            return ProfessorLectureEvaluationQueryService.this.average(sum, count);
        }
    }

    private void validateProfessor(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || !"PROFESSOR".equals(currentUser.role())) {
            throw new ProfessorLectureAccessDeniedException();
        }
    }
}
