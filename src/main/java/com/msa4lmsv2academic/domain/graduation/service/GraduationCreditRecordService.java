package com.msa4lmsv2academic.domain.graduation.service;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.enrollment.entity.GradeStatus;
import com.msa4lmsv2academic.domain.graduation.entity.GraduationCreditExclusionReason;
import com.msa4lmsv2academic.domain.graduation.entity.GraduationCreditGradePolicy;
import com.msa4lmsv2academic.domain.graduation.entity.GraduationCreditRecordResult;
import com.msa4lmsv2academic.domain.graduation.repository.GraduationCreditQueryRepository;
import com.msa4lmsv2academic.domain.graduation.repository.GraduationCreditRecordQueryResult;
import com.msa4lmsv2academic.domain.graduation.request.GraduationCreditRecordSearchRequestDTO;
import com.msa4lmsv2academic.domain.graduation.response.GraduationCreditRecordResponseDTO;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.student.repository.StudentQueryRepository;
import com.msa4lmsv2academic.global.error.GraduationCreditAccessDeniedException;
import com.msa4lmsv2academic.global.error.InvalidCreditDiagnosisRequestException;
import com.msa4lmsv2academic.global.error.StudentNotFoundException;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraduationCreditRecordService {

    private static final int MIN_ACADEMIC_YEAR = 1900;

    private final GraduationCreditQueryRepository graduationCreditQueryRepository;
    private final StudentQueryRepository studentQueryRepository;

    public PageResponseDTO<GraduationCreditRecordResponseDTO> search(
            Long studentId,
            GraduationCreditRecordSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        validateStudentId(studentId);
        validateRequest(request);
        validateAccess(studentId, currentUser);

        List<GraduationCreditRecordQueryResult> newestFirst = graduationCreditQueryRepository
                .findCreditRecordsByStudentId(studentId)
                .stream()
                .sorted(recordComparator().reversed())
                .toList();
        List<GraduationCreditRecordResponseDTO> classified = classify(newestFirst);
        List<GraduationCreditRecordResponseDTO> filtered = new ArrayList<>(classified.stream()
                .filter(item -> request.academicYear() == null
                        || item.academicYear() == request.academicYear())
                .filter(item -> request.term() == null || item.term() == request.term())
                .filter(item -> request.completionType() == null
                        || item.completionType() == request.completionType())
                .filter(item -> request.result() == null || item.result() == request.result())
                .toList());
        if (request.ascending()) {
            java.util.Collections.reverse(filtered);
        }

        int page = request.resolvedPage();
        int size = request.resolvedSize();
        long offset = (page - 1L) * size;
        int fromIndex = (int) Math.min(offset, filtered.size());
        int toIndex = Math.min(fromIndex + size, filtered.size());
        return new PageResponseDTO<>(
                List.copyOf(filtered.subList(fromIndex, toIndex)),
                filtered.size(),
                page,
                size,
                toIndex < filtered.size()
        );
    }

    private List<GraduationCreditRecordResponseDTO> classify(
            List<GraduationCreditRecordQueryResult> newestFirst
    ) {
        Set<Long> reflectedCourseIds = new HashSet<>();
        return newestFirst.stream()
                .map(record -> classify(record, reflectedCourseIds))
                .toList();
    }

    private GraduationCreditRecordResponseDTO classify(
            GraduationCreditRecordQueryResult record,
            Set<Long> reflectedCourseIds
    ) {
        GraduationCreditExclusionReason exclusionReason = exclusionReason(record, reflectedCourseIds);
        GraduationCreditRecordResult result = exclusionReason == null
                ? GraduationCreditRecordResult.APPLIED
                : GraduationCreditRecordResult.EXCLUDED;
        String exposedLetterGrade = record.gradeStatus() == GradeStatus.OPENED
                ? record.letterGrade()
                : null;
        return new GraduationCreditRecordResponseDTO(
                record.enrollmentId(),
                record.courseId(),
                record.courseCode(),
                record.courseName(),
                record.credits(),
                record.completionType(),
                record.academicYear(),
                record.term(),
                record.enrollmentStatus(),
                record.gradeStatus(),
                exposedLetterGrade,
                result,
                result == GraduationCreditRecordResult.APPLIED ? record.credits() : 0,
                exclusionReason,
                exclusionReason == null ? null : exclusionReason.getMessage()
        );
    }

    private GraduationCreditExclusionReason exclusionReason(
            GraduationCreditRecordQueryResult record,
            Set<Long> reflectedCourseIds
    ) {
        if (record.enrollmentStatus() == EnrollmentStatus.CANCELLED) {
            return GraduationCreditExclusionReason.ENROLLMENT_CANCELLED;
        }
        if (record.gradeStatus() != GradeStatus.OPENED) {
            return GraduationCreditExclusionReason.GRADE_NOT_OPENED;
        }
        if (!reflectedCourseIds.add(record.courseId())) {
            return GraduationCreditExclusionReason.RETAKE_DUPLICATE;
        }
        if (record.letterGrade() == null) {
            return GraduationCreditExclusionReason.GRADE_NOT_ENTERED;
        }
        if ("F".equals(record.letterGrade())) {
            return GraduationCreditExclusionReason.FAILED_GRADE;
        }
        if (!GraduationCreditGradePolicy.isPassing(record.letterGrade())) {
            return GraduationCreditExclusionReason.INVALID_GRADE_DATA;
        }
        return null;
    }

    private void validateStudentId(Long studentId) {
        if (studentId == null || studentId <= 0) {
            throw new InvalidCreditDiagnosisRequestException("studentId는 양수여야 합니다.");
        }
    }

    private void validateRequest(GraduationCreditRecordSearchRequestDTO request) {
        if (request == null) {
            throw new InvalidCreditDiagnosisRequestException("검색 조건이 필요합니다.");
        }
        if (request.academicYear() != null) {
            int maximumYear = Year.now().getValue() + 1;
            if (request.academicYear() < MIN_ACADEMIC_YEAR || request.academicYear() > maximumYear) {
                throw new InvalidCreditDiagnosisRequestException(
                        "academicYear는 " + MIN_ACADEMIC_YEAR + "년부터 " + maximumYear + "년까지 허용됩니다."
                );
            }
        }
    }

    private void validateAccess(Long studentId, CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || currentUser.role() == null) {
            throw new GraduationCreditAccessDeniedException("인증 사용자 정보가 없습니다.");
        }
        boolean accessible = switch (currentUser.role()) {
            case "ADMIN" -> {
                if (!graduationCreditQueryRepository.existsStudentById(studentId)) {
                    throw new StudentNotFoundException();
                }
                yield true;
            }
            case "STUDENT" -> graduationCreditQueryRepository.isStudentOwnedByUser(studentId, currentUser.id());
            case "PROFESSOR" -> studentQueryRepository.findProfessorScopeByUserId(currentUser.id())
                    .map(scope -> graduationCreditQueryRepository.isStudentInProfessorScope(studentId, scope))
                    .orElse(false);
            default -> false;
        };
        if (!accessible) {
            throw new GraduationCreditAccessDeniedException(
                    "해당 학생의 졸업학점 수강·성적 기록 조회 권한이 없습니다."
            );
        }
    }

    private Comparator<GraduationCreditRecordQueryResult> recordComparator() {
        return Comparator
                .comparing(GraduationCreditRecordQueryResult::academicYear)
                .thenComparingInt(record -> termOrder(record.term()))
                .thenComparing(GraduationCreditRecordQueryResult::enrollmentId);
    }

    private int termOrder(SemesterTerm term) {
        return term == SemesterTerm.SECOND ? 2 : 1;
    }
}
