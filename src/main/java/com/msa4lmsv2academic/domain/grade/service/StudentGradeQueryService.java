package com.msa4lmsv2academic.domain.grade.service;

import com.msa4lmsv2academic.domain.grade.entity.GradePointPolicy;
import com.msa4lmsv2academic.domain.grade.repository.StudentGradeQueryRepository;
import com.msa4lmsv2academic.domain.grade.repository.StudentGradeQueryResult;
import com.msa4lmsv2academic.domain.grade.request.StudentGradeSearchRequestDTO;
import com.msa4lmsv2academic.domain.grade.request.StudentGradeSortBy;
import com.msa4lmsv2academic.domain.grade.request.StudentGradeSortDirection;
import com.msa4lmsv2academic.domain.grade.response.StudentGradeItemResponseDTO;
import com.msa4lmsv2academic.domain.grade.response.StudentGradeResponseDTO;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.global.error.StudentGradeAccessDeniedException;
import com.msa4lmsv2academic.global.error.StudentNotFoundException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentGradeQueryService {

    private final StudentGradeQueryRepository queryRepository;

    public StudentGradeResponseDTO getMyGrades(
            StudentGradeSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        validateStudent(currentUser);
        if (!queryRepository.existsStudentByUserId(currentUser.id())) {
            throw new StudentNotFoundException();
        }

        StudentGradeSearchRequestDTO resolved = request == null
                ? new StudentGradeSearchRequestDTO(null, null, null, null, null)
                : request;
        List<StudentGradeQueryResult> allGrades = queryRepository
                .findOpenedGradesByStudentUserId(currentUser.id());
        validateGradeValues(allGrades);

        Set<Long> reflectedEnrollmentIds = findLatestEnrollmentIdsByCourse(allGrades);
        List<StudentGradeQueryResult> queriedGrades = allGrades.stream()
                .filter(grade -> matches(grade, resolved))
                .sorted(comparator(resolved.resolvedSortBy(), resolved.resolvedDirection()))
                .toList();

        GradeSummary total = summarize(allGrades, reflectedEnrollmentIds);
        GradeSummary query = summarize(queriedGrades, reflectedEnrollmentIds);
        List<StudentGradeItemResponseDTO> items = queriedGrades.stream()
                .map(grade -> StudentGradeItemResponseDTO.from(
                        grade,
                        reflectedEnrollmentIds.contains(grade.enrollmentId())
                ))
                .toList();

        return new StudentGradeResponseDTO(
                total.gpa(),
                total.credits(),
                query.gpa(),
                query.credits(),
                items
        );
    }

    private void validateStudent(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || !"STUDENT".equals(currentUser.role())) {
            throw new StudentGradeAccessDeniedException();
        }
    }

    private void validateGradeValues(List<StudentGradeQueryResult> grades) {
        grades.stream()
                .filter(grade -> !GradePointPolicy.isRecognized(grade.letterGrade()))
                .findFirst()
                .ifPresent(grade -> {
                    throw new IllegalStateException(
                            "공개 성적에 지원하지 않는 등급이 있습니다. enrollmentId=" + grade.enrollmentId()
                    );
                });
    }

    private Set<Long> findLatestEnrollmentIdsByCourse(List<StudentGradeQueryResult> grades) {
        Map<Long, StudentGradeQueryResult> latestByCourse = new HashMap<>();
        grades.forEach(grade -> latestByCourse.merge(
                grade.courseId(),
                grade,
                (left, right) -> compareAttempt(left, right) >= 0 ? left : right
        ));
        Set<Long> reflectedIds = new HashSet<>();
        latestByCourse.values().forEach(grade -> reflectedIds.add(grade.enrollmentId()));
        return Set.copyOf(reflectedIds);
    }

    private boolean matches(StudentGradeQueryResult grade, StudentGradeSearchRequestDTO request) {
        if (request.academicYear() != null && grade.academicYear() != request.academicYear()) {
            return false;
        }
        if (request.term() != null && grade.term() != request.term()) {
            return false;
        }
        String courseName = request.normalizedCourseName();
        return courseName == null || grade.courseName().toLowerCase(Locale.ROOT)
                .contains(courseName.toLowerCase(Locale.ROOT));
    }

    private Comparator<StudentGradeQueryResult> comparator(
            StudentGradeSortBy sortBy,
            StudentGradeSortDirection direction
    ) {
        Comparator<StudentGradeQueryResult> primary = switch (sortBy) {
            case COURSE_NAME -> Comparator.comparing(
                    StudentGradeQueryResult::courseName,
                    String.CASE_INSENSITIVE_ORDER
            );
            case GRADE -> Comparator.comparing(
                    grade -> GradePointPolicy.pointOf(grade.letterGrade())
            );
            case ACADEMIC_YEAR -> Comparator
                    .comparingInt((StudentGradeQueryResult grade) -> grade.academicYear())
                    .thenComparingInt(grade -> termOrder(grade.term()));
        };
        if (direction == StudentGradeSortDirection.DESC) {
            primary = primary.reversed();
        }
        return primary
                .thenComparing(StudentGradeQueryResult::courseName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(StudentGradeQueryResult::enrollmentId, Comparator.reverseOrder());
    }

    private GradeSummary summarize(
            List<StudentGradeQueryResult> grades,
            Set<Long> reflectedEnrollmentIds
    ) {
        List<StudentGradeQueryResult> reflectedGrades = grades.stream()
                .filter(grade -> reflectedEnrollmentIds.contains(grade.enrollmentId()))
                .toList();
        int credits = reflectedGrades.stream()
                .mapToInt(StudentGradeQueryResult::credits)
                .sum();
        if (credits == 0) {
            return new GradeSummary(BigDecimal.ZERO.setScale(2), 0);
        }
        BigDecimal weightedPoints = reflectedGrades.stream()
                .map(grade -> GradePointPolicy.pointOf(grade.letterGrade())
                        .multiply(BigDecimal.valueOf(grade.credits())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new GradeSummary(
                weightedPoints.divide(BigDecimal.valueOf(credits), 2, RoundingMode.HALF_UP),
                credits
        );
    }

    private int compareAttempt(StudentGradeQueryResult left, StudentGradeQueryResult right) {
        int year = Short.compare(left.academicYear(), right.academicYear());
        if (year != 0) {
            return year;
        }
        int term = Integer.compare(termOrder(left.term()), termOrder(right.term()));
        if (term != 0) {
            return term;
        }
        return left.enrollmentId().compareTo(right.enrollmentId());
    }

    private int termOrder(SemesterTerm term) {
        return term == SemesterTerm.SECOND ? 2 : 1;
    }

    private record GradeSummary(BigDecimal gpa, int credits) {
    }
}
