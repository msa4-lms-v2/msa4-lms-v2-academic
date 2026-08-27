package com.msa4lmsv2academic.domain.withdrawal.service;

import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.withdrawal.entity.AcademicStatusHistory;
import com.msa4lmsv2academic.domain.withdrawal.entity.WithdrawalRequest;
import com.msa4lmsv2academic.domain.withdrawal.entity.WithdrawalStatus;
import com.msa4lmsv2academic.domain.withdrawal.repository.AcademicStatusHistoryRepository;
import com.msa4lmsv2academic.domain.withdrawal.repository.WithdrawalRequestRepository;
import com.msa4lmsv2academic.domain.withdrawal.repository.WithdrawalQueryRepository;
import com.msa4lmsv2academic.domain.withdrawal.request.AdvisorWithdrawalReviewRequestDTO;
import com.msa4lmsv2academic.domain.withdrawal.request.FinalWithdrawalReviewRequestDTO;
import com.msa4lmsv2academic.domain.withdrawal.request.WithdrawalCreateRequestDTO;
import com.msa4lmsv2academic.domain.withdrawal.request.WithdrawalCancelRequestDTO;
import com.msa4lmsv2academic.domain.withdrawal.request.WithdrawalSearchRequestDTO;
import com.msa4lmsv2academic.domain.withdrawal.response.WithdrawalResponseDTO;
import com.msa4lmsv2academic.global.error.DuplicateWithdrawalRequestException;
import com.msa4lmsv2academic.global.error.InvalidWithdrawalRequestException;
import com.msa4lmsv2academic.global.error.WithdrawalAccessDeniedException;
import com.msa4lmsv2academic.global.error.WithdrawalNotFoundException;
import com.msa4lmsv2academic.global.error.WithdrawalStateConflictException;
import com.msa4lmsv2academic.global.idempotency.AcademicIdempotencyKey;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WithdrawalService {

    private static final Set<WithdrawalStatus> ACTIVE_STATUSES = EnumSet.of(
            WithdrawalStatus.PENDING,
            WithdrawalStatus.ADVISOR_APPROVED
    );

    private final WithdrawalRequestRepository withdrawalRepository;
    private final AcademicStatusHistoryRepository historyRepository;
    private final WithdrawalQueryRepository queryRepository;
    private final WithdrawalIdempotencyService idempotencyService;
    private final WithdrawalPolicy policy;
    private final WithdrawalAuditService auditService;

    public PageResponseDTO<WithdrawalResponseDTO> search(
            WithdrawalSearchRequestDTO request,
            CurrentUser currentUser,
            Pageable pageable
    ) {
        validateUser(currentUser);
        int page = request.resolvedPage();
        int size = request.resolvedSize();
        Page<WithdrawalRequest> result = switch (currentUser.role()) {
            case "STUDENT" -> withdrawalRepository.findByStudentUserId(currentUser.id(), pageable);
            case "PROFESSOR" -> withdrawalRepository.findByStudentAdvisorUserId(currentUser.id(), pageable);
            case "ADMIN" -> withdrawalRepository.findAll(pageable);
            default -> throw new WithdrawalAccessDeniedException("자퇴 신청 조회 권한이 없습니다.");
        };
        List<WithdrawalResponseDTO> items = result.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new PageResponseDTO<>(items, result.getTotalElements(), page, size, result.hasNext());
    }

    public WithdrawalResponseDTO get(Long withdrawalId, CurrentUser currentUser) {
        validateUser(currentUser);
        validateId(withdrawalId);
        WithdrawalRequest request = withdrawalRepository.findDetailById(withdrawalId)
                .orElseThrow(WithdrawalNotFoundException::new);
        validateReadable(request, currentUser);
        return toResponse(request);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WithdrawalResponseDTO create(
            WithdrawalCreateRequestDTO request,
            String key,
            CurrentUser currentUser,
            WithdrawalAuditContext context
    ) {
        validateRole(currentUser, "STUDENT");
        idempotencyService.validateKey(key);
        if (request == null) {
            throw new InvalidWithdrawalRequestException("신청 본문이 필요합니다.");
        }
        String reason = requiredReason(request.reason(), 500);
        Student student = queryRepository.findStudentByUserIdForUpdate(currentUser.id())
                .orElseThrow(() -> new InvalidWithdrawalRequestException("학생 정보를 찾을 수 없습니다."));
        String endpoint = "POST /api/academic/withdrawals";
        String hash = idempotencyService.hash(request);
        var replay = idempotencyService.replay(key, currentUser.id(), endpoint, hash, now());
        if (replay.isPresent()) {
            return replay.orElseThrow();
        }
        AcademicIdempotencyKey reserved = idempotencyService.reserve(key, currentUser.id(), endpoint, hash, now());
        // 완료 재생 이후에만 새 신청 조건을 확인합니다.
        policy.validateRequestedDate(request.requestedEffectiveDate(), now().toLocalDate());
        if (student.getAdvisor() == null) {
            throw new InvalidWithdrawalRequestException("지도교수가 배정된 학생만 자퇴를 신청할 수 있습니다.");
        }
        policy.validateAcademicStatus(student.getAcademicStatus());
        if (withdrawalRepository.existsByStudentIdAndStatusIn(student.getId(), ACTIVE_STATUSES)) {
            throw new DuplicateWithdrawalRequestException();
        }

        WithdrawalRequest withdrawal = WithdrawalRequest.create(
                student,
                reason,
                request.requestedEffectiveDate(),
                student.getUser()
        );
        return finish(withdrawal, reserved, null, "WITHDRAWAL_CREATED", "자퇴 신청", currentUser, context);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WithdrawalResponseDTO reviewByAdvisor(
            Long withdrawalId,
            AdvisorWithdrawalReviewRequestDTO review,
            String key,
            CurrentUser currentUser,
            WithdrawalAuditContext context
    ) {
        validateRole(currentUser, "PROFESSOR");
        idempotencyService.validateKey(key);
        if (review == null || review.approved() == null) {
            throw new InvalidWithdrawalRequestException("승인 여부가 필요합니다.");
        }
        WithdrawalRequest request = getForUpdate(withdrawalId);
        if (request.getStudent().getAdvisor() == null
                || !request.getStudent().getAdvisor().getUser().getId().equals(currentUser.id())) {
            throw new WithdrawalAccessDeniedException("배정된 지도교수만 자퇴 신청을 검토할 수 있습니다.");
        }
        User reviewer = queryRepository.findUserById(currentUser.id())
                .orElseThrow(() -> new InvalidWithdrawalRequestException("검토자 정보를 찾을 수 없습니다."));
        String endpoint = "PATCH /api/academic/withdrawals/" + withdrawalId + "/advisor-review";
        String hash = idempotencyService.hash(review);
        var replay = idempotencyService.replay(key, currentUser.id(), endpoint, hash, now());
        if (replay.isPresent()) {
            return replay.orElseThrow();
        }
        AcademicIdempotencyKey reserved = idempotencyService.reserve(key, currentUser.id(), endpoint, hash, now());
        Map<String, Object> before = auditService.snapshot(request);
        LocalDateTime now = now();
        try {
            if (Boolean.TRUE.equals(review.approved())) {
                request.advisorApprove(reviewer, now);
            } else {
                request.advisorReject(reviewer, requiredRejectReason(review.rejectReason()), now);
            }
        } catch (IllegalStateException exception) {
            throw new WithdrawalStateConflictException("지도교수 검토 대기 상태인 신청만 처리할 수 있습니다.");
        }
        return finish(request, reserved, before,
                review.approved() ? "WITHDRAWAL_ADVISOR_APPROVED" : "WITHDRAWAL_ADVISOR_REJECTED",
                review.approved() ? "지도교수 승인" : "지도교수 반려", currentUser, context);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WithdrawalResponseDTO reviewByAdmin(
            Long withdrawalId,
            FinalWithdrawalReviewRequestDTO review,
            String key,
            CurrentUser currentUser,
            WithdrawalAuditContext context
    ) {
        validateRole(currentUser, "ADMIN");
        idempotencyService.validateKey(key);
        if (review == null || review.approved() == null) {
            throw new InvalidWithdrawalRequestException("승인 여부가 필요합니다.");
        }
        WithdrawalRequest request = getForUpdate(withdrawalId);
        User processor = queryRepository.findUserById(currentUser.id())
                .orElseThrow(() -> new InvalidWithdrawalRequestException("처리자 정보를 찾을 수 없습니다."));
        String endpoint = "PATCH /api/academic/withdrawals/" + withdrawalId + "/final-review";
        String hash = idempotencyService.hash(review);
        var replay = idempotencyService.replay(key, currentUser.id(), endpoint, hash, now());
        if (replay.isPresent()) {
            return replay.orElseThrow();
        }
        AcademicIdempotencyKey reserved = idempotencyService.reserve(key, currentUser.id(), endpoint, hash, now());
        Map<String, Object> before = auditService.snapshot(request);
        LocalDateTime now = now();
        if (request.getStatus() != WithdrawalStatus.ADVISOR_APPROVED) {
            throw new WithdrawalStateConflictException("지도교수 승인 상태인 신청만 최종 처리할 수 있습니다.");
        }
        try {
            if (Boolean.TRUE.equals(review.approved())) {
                LocalDate effectiveDate = requiredEffectiveDate(review.effectiveDate());
                Student student = request.getStudent(); // getForUpdate에서 학생 행부터 잠급니다.
                policy.validateAcademicStatus(student.getAcademicStatus());
                policy.validateFinalDate(effectiveDate, request.getRequestedEffectiveDate(), now.toLocalDate());
                AcademicStatus previousStatus = student.getAcademicStatus();
                request.approve(processor, effectiveDate, now);
                student.changeAcademicStatus(AcademicStatus.WITHDRAWN);
                withdrawalRepository.flush();
                historyRepository.saveAndFlush(AcademicStatusHistory.withdrawalApproved(
                        student, previousStatus, processor, request.getId()
                ));
            } else {
                request.reject(processor, requiredRejectReason(review.rejectReason()), now);
            }
        } catch (IllegalStateException exception) {
            throw new WithdrawalStateConflictException("지도교수 승인 상태인 신청만 최종 처리할 수 있습니다.");
        }
        return finish(request, reserved, before,
                review.approved() ? "WITHDRAWAL_APPROVED" : "WITHDRAWAL_REJECTED",
                review.approved() ? "자퇴 최종 승인" : "자퇴 최종 반려", currentUser, context);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WithdrawalResponseDTO cancel(Long withdrawalId, WithdrawalCancelRequestDTO cancelRequest, String key,
                                         CurrentUser currentUser, WithdrawalAuditContext context) {
        validateRole(currentUser, "STUDENT");
        idempotencyService.validateKey(key);
        if (cancelRequest == null) {
            throw new InvalidWithdrawalRequestException("취소 본문이 필요합니다.");
        }
        String reason = requiredReason(cancelRequest.cancelReason(), 255);
        WithdrawalRequest request = getForUpdate(withdrawalId);
        if (!request.getStudent().getUser().getId().equals(currentUser.id())) {
            throw new WithdrawalAccessDeniedException("본인의 자퇴 신청만 취소할 수 있습니다.");
        }
        String endpoint = "PATCH /api/academic/withdrawals/" + withdrawalId + "/status";
        String hash = idempotencyService.hash(cancelRequest);
        var replay = idempotencyService.replay(key, currentUser.id(), endpoint, hash, now());
        if (replay.isPresent()) {
            return replay.orElseThrow();
        }
        AcademicIdempotencyKey reserved = idempotencyService.reserve(key, currentUser.id(), endpoint, hash, now());
        Map<String, Object> before = auditService.snapshot(request);
        try {
            request.cancel(request.getStudent().getUser(), reason, now());
        } catch (IllegalStateException exception) {
            throw new WithdrawalStateConflictException("진행 중인 자퇴 신청만 취소할 수 있습니다.");
        }
        return finish(request, reserved, before, "WITHDRAWAL_CANCELLED", reason, currentUser, context);
    }

    private WithdrawalRequest getForUpdate(Long withdrawalId) {
        validateId(withdrawalId);
        // 수강신청/신규 자퇴와 같은 학생 → 신청 순서로 잠급니다.
        Long studentId = withdrawalRepository.findStudentIdById(withdrawalId)
                .orElseThrow(WithdrawalNotFoundException::new);
        queryRepository.findStudentByIdForUpdate(studentId).orElseThrow(WithdrawalNotFoundException::new);
        return withdrawalRepository.findByIdForUpdate(withdrawalId)
                .orElseThrow(WithdrawalNotFoundException::new);
    }

    private void validateReadable(WithdrawalRequest request, CurrentUser currentUser) {
        boolean readable = switch (currentUser.role()) {
            case "STUDENT" -> request.getStudent().getUser().getId().equals(currentUser.id());
            case "PROFESSOR" -> request.getStudent().getAdvisor() != null
                    && request.getStudent().getAdvisor().getUser().getId().equals(currentUser.id());
            case "ADMIN" -> true;
            default -> false;
        };
        if (!readable) {
            throw new WithdrawalAccessDeniedException("자퇴 신청 조회 권한이 없습니다.");
        }
    }

    private WithdrawalResponseDTO toResponse(WithdrawalRequest request) {
        return WithdrawalResponseDTO.from(request);
    }

    private LocalDate requiredEffectiveDate(LocalDate effectiveDate) {
        if (effectiveDate == null) {
            throw new InvalidWithdrawalRequestException("최종 승인 시 effectiveDate는 필수입니다.");
        }
        return effectiveDate;
    }

    private String requiredRejectReason(String rejectReason) {
        if (rejectReason == null || rejectReason.isBlank()) {
            throw new InvalidWithdrawalRequestException("반려 시 rejectReason은 필수입니다.");
        }
        return requiredReason(rejectReason, 500);
    }

    private void validateUser(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || currentUser.role() == null) {
            throw new WithdrawalAccessDeniedException("인증된 사용자만 자퇴 신청을 조회할 수 있습니다.");
        }
    }

    private void validateRole(CurrentUser currentUser, String role) {
        if (currentUser == null || currentUser.id() == null || !role.equals(currentUser.role())) {
            throw new WithdrawalAccessDeniedException("자퇴 신청 처리 권한이 없습니다.");
        }
    }

    private WithdrawalResponseDTO finish(WithdrawalRequest request, AcademicIdempotencyKey key, Map<String, Object> before,
                                          String action, String reason, CurrentUser currentUser, WithdrawalAuditContext context) {
        WithdrawalResponseDTO response = toResponse(withdrawalRepository.saveAndFlush(request));
        auditService.record(request, before, action, reason, currentUser, context);
        idempotencyService.complete(key, response);
        return response;
    }

    private String requiredReason(String reason, int max) {
        // 요청 DTO 생성자에서 정규화한 값을 검증해 멱등 해시와 저장 사유를 일치시킵니다.
        if (reason == null || reason.isBlank() || reason.length() > max) {
            throw new InvalidWithdrawalRequestException("사유는 공백이 아닌 1~" + max + "자여야 합니다.");
        }
        return reason;
    }

    private void validateId(Long withdrawalId) {
        if (withdrawalId == null || withdrawalId <= 0) {
            throw new InvalidWithdrawalRequestException("withdrawalId는 양수여야 합니다.");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}
