package com.msa4lmsv2academic.domain.graduation.service;

import com.msa4lmsv2academic.domain.graduation.entity.CreditDiagnosisStatus;
import com.msa4lmsv2academic.domain.graduation.repository.CreditDiagnosisCandidateRow;
import com.msa4lmsv2academic.domain.graduation.repository.CreditDiagnosisSearchCondition;
import com.msa4lmsv2academic.domain.graduation.repository.EarnedCreditTotals;
import com.msa4lmsv2academic.domain.graduation.repository.GraduationCreditDiagnosisQueryResult;
import com.msa4lmsv2academic.domain.graduation.repository.GraduationCreditQueryRepository;
import com.msa4lmsv2academic.domain.graduation.request.CreditDiagnosisSearchRequestDTO;
import com.msa4lmsv2academic.domain.graduation.response.CreditDiagnosisResponseDTO;
import com.msa4lmsv2academic.domain.graduation.response.CreditDiagnosisSummaryResponseDTO;
import com.msa4lmsv2academic.domain.student.repository.ProfessorStudentScope;
import com.msa4lmsv2academic.domain.student.repository.StudentQueryRepository;
import com.msa4lmsv2academic.global.error.GraduationCreditAccessDeniedException;
import com.msa4lmsv2academic.global.error.GraduationCreditDataNotFoundException;
import com.msa4lmsv2academic.global.error.InvalidCreditDiagnosisRequestException;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraduationCreditDiagnosisService {

    private static final int MIN_ADMISSION_YEAR = 1900;
    private static final int CREDIT_BATCH_SIZE = 500;

    private final GraduationCreditQueryRepository graduationCreditQueryRepository;
    private final StudentQueryRepository studentQueryRepository;

    public CreditDiagnosisResponseDTO diagnose(Long studentId, CurrentUser currentUser) {
        if (studentId == null || studentId <= 0) {
            throw new InvalidCreditDiagnosisRequestException("studentId는 양수여야 합니다.");
        }
        validateAccess(studentId, currentUser);

        GraduationCreditDiagnosisQueryResult queryResult = graduationCreditQueryRepository
                .findCreditDiagnosisByStudentId(studentId)
                .orElseThrow(GraduationCreditDataNotFoundException::new);

        int shortageMajorCredits = shortage(
                queryResult.requiredMajorCredits(),
                queryResult.earnedMajorCredits()
        );
        int shortageGeneralCredits = shortage(
                queryResult.requiredGeneralCredits(),
                queryResult.earnedGeneralCredits()
        );
        int shortageTotalCredits = shortage(
                queryResult.requiredTotalCredits(),
                queryResult.earnedTotalCredits()
        );

        GraduationCreditDiagnosisResult diagnosisResult = new GraduationCreditDiagnosisResult(
                studentId,
                queryResult.earnedMajorCredits(),
                queryResult.earnedGeneralCredits(),
                queryResult.earnedRequiredCredits(),
                queryResult.earnedElectiveCredits(),
                queryResult.earnedTotalCredits(),
                shortageMajorCredits,
                shortageGeneralCredits,
                shortageTotalCredits,
                shortageMajorCredits == 0 && shortageGeneralCredits == 0 && shortageTotalCredits == 0
        );
        return CreditDiagnosisResponseDTO.from(diagnosisResult);
    }

    public PageResponseDTO<CreditDiagnosisSummaryResponseDTO> search(
            CreditDiagnosisSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        validateSearchRequest(request);
        DiagnosisAccessScope accessScope = resolveAccessScope(currentUser);
        int page = request.resolvedPage();
        int size = request.resolvedSize();
        long offset = (page - 1L) * size;
        CreditDiagnosisSearchCondition condition = new CreditDiagnosisSearchCondition(
                offset,
                size,
                request.normalizedKeyword(),
                request.departmentId(),
                request.admissionYear() == null ? null : request.admissionYear().shortValue(),
                request.academicStatus(),
                request.resolvedSortBy(),
                request.descending(),
                accessScope.studentUserId(),
                accessScope.professorScope()
        );

        if (request.diagnosisStatus() == null) {
            List<CreditDiagnosisCandidateRow> candidates =
                    graduationCreditQueryRepository.findDiagnosisCandidates(condition, true);
            List<CreditDiagnosisSummaryResponseDTO> items = diagnoseCandidates(candidates);
            long totalCount = graduationCreditQueryRepository.countDiagnosisCandidates(condition);
            return new PageResponseDTO<>(
                    items,
                    totalCount,
                    page,
                    size,
                    offset + items.size() < totalCount
            );
        }

        List<CreditDiagnosisSummaryResponseDTO> allDiagnoses = diagnoseCandidates(
                graduationCreditQueryRepository.findDiagnosisCandidates(condition, false)
        );
        List<CreditDiagnosisSummaryResponseDTO> filtered = allDiagnoses
                .stream()
                .filter(item -> item.diagnosisStatus() == request.diagnosisStatus())
                .toList();
        int fromIndex = (int) Math.min(offset, filtered.size());
        int toIndex = Math.min(fromIndex + size, filtered.size());
        List<CreditDiagnosisSummaryResponseDTO> items = List.copyOf(filtered.subList(fromIndex, toIndex));
        return new PageResponseDTO<>(
                items,
                filtered.size(),
                page,
                size,
                toIndex < filtered.size()
        );
    }

    private List<CreditDiagnosisSummaryResponseDTO> diagnoseCandidates(
            List<CreditDiagnosisCandidateRow> candidates
    ) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        List<Long> studentIds = candidates.stream().map(CreditDiagnosisCandidateRow::studentId).toList();
        Map<Long, EarnedCreditTotals> earnedByStudent = loadEarnedCredits(studentIds);
        return candidates.stream()
                .map(candidate -> toSummary(
                        candidate,
                        earnedByStudent.getOrDefault(candidate.studentId(), EarnedCreditTotals.empty())
                ))
                .toList();
    }

    private Map<Long, EarnedCreditTotals> loadEarnedCredits(List<Long> studentIds) {
        if (studentIds.size() <= CREDIT_BATCH_SIZE) {
            return graduationCreditQueryRepository.findEarnedCreditsByStudentIds(studentIds);
        }
        Map<Long, EarnedCreditTotals> result = new java.util.HashMap<>();
        for (int fromIndex = 0; fromIndex < studentIds.size(); fromIndex += CREDIT_BATCH_SIZE) {
            int toIndex = Math.min(fromIndex + CREDIT_BATCH_SIZE, studentIds.size());
            result.putAll(graduationCreditQueryRepository.findEarnedCreditsByStudentIds(
                    studentIds.subList(fromIndex, toIndex)
            ));
        }
        return Map.copyOf(result);
    }

    private CreditDiagnosisSummaryResponseDTO toSummary(
            CreditDiagnosisCandidateRow candidate,
            EarnedCreditTotals earned
    ) {
        if (candidate.requirementId() == null) {
            return new CreditDiagnosisSummaryResponseDTO(
                    candidate.studentId(),
                    candidate.studentName(),
                    candidate.departmentId(),
                    candidate.departmentName(),
                    candidate.admissionYear(),
                    candidate.academicStatus(),
                    null,
                    null,
                    null,
                    earned.major(),
                    earned.general(),
                    earned.required(),
                    earned.elective(),
                    earned.total(),
                    null,
                    null,
                    null,
                    CreditDiagnosisStatus.REQUIREMENT_NOT_CONFIGURED,
                    candidate.admissionYear() + "학년도 " + candidate.departmentName()
                            + " 졸업요건이 등록되지 않았습니다."
            );
        }

        int shortageMajor = shortage(candidate.requiredMajorCredits(), earned.major());
        int shortageGeneral = shortage(candidate.requiredGeneralCredits(), earned.general());
        int shortageTotal = shortage(candidate.requiredTotalCredits(), earned.total());
        boolean satisfied = shortageMajor == 0 && shortageGeneral == 0 && shortageTotal == 0;
        return new CreditDiagnosisSummaryResponseDTO(
                candidate.studentId(),
                candidate.studentName(),
                candidate.departmentId(),
                candidate.departmentName(),
                candidate.admissionYear(),
                candidate.academicStatus(),
                candidate.requiredMajorCredits(),
                candidate.requiredGeneralCredits(),
                candidate.requiredTotalCredits(),
                earned.major(),
                earned.general(),
                earned.required(),
                earned.elective(),
                earned.total(),
                shortageMajor,
                shortageGeneral,
                shortageTotal,
                satisfied ? CreditDiagnosisStatus.SATISFIED : CreditDiagnosisStatus.NOT_SATISFIED,
                satisfied ? "모든 학점요건을 충족했습니다." : shortageReason(
                        shortageMajor, shortageGeneral, shortageTotal)
        );
    }

    private String shortageReason(int major, int general, int total) {
        List<String> shortages = new ArrayList<>();
        if (major > 0) shortages.add("전공 " + major + "학점");
        if (general > 0) shortages.add("교양 " + general + "학점");
        if (total > 0) shortages.add("총 " + total + "학점");
        return String.join(", ", shortages) + "이 부족합니다.";
    }

    private void validateAccess(Long studentId, CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || currentUser.role() == null) {
            throw new GraduationCreditAccessDeniedException("인증 사용자 정보가 없습니다.");
        }

        boolean accessible = switch (currentUser.role()) {
            case "ADMIN" -> true;
            case "STUDENT" -> graduationCreditQueryRepository.isStudentOwnedByUser(studentId, currentUser.id());
            case "PROFESSOR" -> studentQueryRepository.findProfessorScopeByUserId(currentUser.id())
                    .map(scope -> graduationCreditQueryRepository.isStudentInProfessorScope(studentId, scope))
                    .orElse(false);
            default -> false;
        };
        if (!accessible) {
            throw new GraduationCreditAccessDeniedException("해당 학생의 졸업 학점 진단 권한이 없습니다.");
        }
    }

    private DiagnosisAccessScope resolveAccessScope(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || currentUser.role() == null) {
            throw new GraduationCreditAccessDeniedException("인증 사용자 정보가 없습니다.");
        }
        return switch (currentUser.role()) {
            case "ADMIN" -> new DiagnosisAccessScope(null, null);
            case "STUDENT" -> new DiagnosisAccessScope(currentUser.id(), null);
            case "PROFESSOR" -> new DiagnosisAccessScope(
                    null,
                    studentQueryRepository.findProfessorScopeByUserId(currentUser.id())
                            .orElseThrow(() -> new GraduationCreditAccessDeniedException(
                                    "Academic에 동기화된 교수 정보가 없습니다."
                            ))
            );
            default -> throw new GraduationCreditAccessDeniedException("학점 진단 현황 조회 권한이 없습니다.");
        };
    }

    private void validateSearchRequest(CreditDiagnosisSearchRequestDTO request) {
        if (request == null) {
            throw new InvalidCreditDiagnosisRequestException("검색 조건이 필요합니다.");
        }
        if (request.admissionYear() != null) {
            int maximumYear = Year.now().getValue() + 1;
            if (request.admissionYear() < MIN_ADMISSION_YEAR || request.admissionYear() > maximumYear) {
                throw new InvalidCreditDiagnosisRequestException(
                        "admissionYear는 " + MIN_ADMISSION_YEAR + "년부터 " + maximumYear + "년까지 허용됩니다."
                );
            }
        }
    }

    private int shortage(int requiredCredits, int earnedCredits) {
        return Math.max(requiredCredits - earnedCredits, 0);
    }

    private record DiagnosisAccessScope(
            Long studentUserId,
            ProfessorStudentScope professorScope
    ) {
    }
}
