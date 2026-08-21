package com.msa4lmsv2academic.domain.withdrawal.service;

import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.student.repository.StudentRepository;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.repository.UserRepository;
import com.msa4lmsv2academic.domain.withdrawal.entity.AcademicStatusHistory;
import com.msa4lmsv2academic.domain.withdrawal.entity.WithdrawalRequest;
import com.msa4lmsv2academic.domain.withdrawal.entity.WithdrawalStatus;
import com.msa4lmsv2academic.domain.withdrawal.repository.AcademicStatusHistoryRepository;
import com.msa4lmsv2academic.domain.withdrawal.repository.WithdrawalRequestRepository;
import com.msa4lmsv2academic.domain.withdrawal.request.AdvisorWithdrawalReviewRequestDTO;
import com.msa4lmsv2academic.domain.withdrawal.request.FinalWithdrawalReviewRequestDTO;
import com.msa4lmsv2academic.domain.withdrawal.request.WithdrawalCreateRequestDTO;
import com.msa4lmsv2academic.domain.withdrawal.request.WithdrawalSearchRequestDTO;
import com.msa4lmsv2academic.domain.withdrawal.response.WithdrawalResponseDTO;
import com.msa4lmsv2academic.global.error.DuplicateWithdrawalRequestException;
import com.msa4lmsv2academic.global.error.InvalidWithdrawalRequestException;
import com.msa4lmsv2academic.global.error.WithdrawalAccessDeniedException;
import com.msa4lmsv2academic.global.error.WithdrawalNotFoundException;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public PageResponseDTO<WithdrawalResponseDTO> search(
            WithdrawalSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        validateUser(currentUser);
        int page = request.resolvedPage();
        int size = request.resolvedSize();
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
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
        WithdrawalRequest request = withdrawalRepository.findDetailById(withdrawalId)
                .orElseThrow(WithdrawalNotFoundException::new);
        validateReadable(request, currentUser);
        return toResponse(request);
    }

    @Transactional
    public WithdrawalResponseDTO create(WithdrawalCreateRequestDTO request, CurrentUser currentUser) {
        validateRole(currentUser, "STUDENT");
        Student student = studentRepository.findByUserIdForUpdate(currentUser.id())
                .orElseThrow(() -> new InvalidWithdrawalRequestException("학생 정보를 찾을 수 없습니다."));
        if (student.getAdvisor() == null) {
            throw new InvalidWithdrawalRequestException("지도교수가 배정된 학생만 자퇴를 신청할 수 있습니다.");
        }
        if (student.getAcademicStatus() == AcademicStatus.WITHDRAWN
                || student.getAcademicStatus() == AcademicStatus.GRADUATED
                || student.getAcademicStatus() == AcademicStatus.DISMISSED) {
            throw new InvalidWithdrawalRequestException("현재 학적 상태에서는 자퇴를 신청할 수 없습니다.");
        }
        if (withdrawalRepository.existsByStudentIdAndStatusIn(student.getId(), ACTIVE_STATUSES)) {
            throw new DuplicateWithdrawalRequestException();
        }

        WithdrawalRequest withdrawal = WithdrawalRequest.create(
                student,
                request.reason().trim(),
                request.requestedEffectiveDate(),
                student.getUser()
        );
        try {
            return toResponse(withdrawalRepository.saveAndFlush(withdrawal));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateWithdrawalRequestException();
        }
    }

    @Transactional
    public WithdrawalResponseDTO reviewByAdvisor(
            Long withdrawalId,
            AdvisorWithdrawalReviewRequestDTO review,
            CurrentUser currentUser
    ) {
        validateRole(currentUser, "PROFESSOR");
        WithdrawalRequest request = getForUpdate(withdrawalId);
        if (request.getStudent().getAdvisor() == null
                || !request.getStudent().getAdvisor().getUser().getId().equals(currentUser.id())) {
            throw new WithdrawalAccessDeniedException("배정된 지도교수만 자퇴 신청을 검토할 수 있습니다.");
        }
        User reviewer = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new InvalidWithdrawalRequestException("검토자 정보를 찾을 수 없습니다."));
        LocalDateTime now = LocalDateTime.now();
        try {
            if (Boolean.TRUE.equals(review.approved())) {
                request.advisorApprove(reviewer, now);
            } else {
                request.advisorReject(reviewer, requiredRejectReason(review.rejectReason()), now);
            }
        } catch (IllegalStateException exception) {
            throw new InvalidWithdrawalRequestException("지도교수 검토 대기 상태인 신청만 처리할 수 있습니다.");
        }
        return toResponse(withdrawalRepository.saveAndFlush(request));
    }

    @Transactional
    public WithdrawalResponseDTO reviewByAdmin(
            Long withdrawalId,
            FinalWithdrawalReviewRequestDTO review,
            CurrentUser currentUser
    ) {
        validateRole(currentUser, "ADMIN");
        WithdrawalRequest request = getForUpdate(withdrawalId);
        User processor = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new InvalidWithdrawalRequestException("처리자 정보를 찾을 수 없습니다."));
        LocalDateTime now = LocalDateTime.now();
        try {
            if (Boolean.TRUE.equals(review.approved())) {
                LocalDate effectiveDate = requiredEffectiveDate(review.effectiveDate());
                Student student = studentRepository.findByIdForUpdate(request.getStudent().getId())
                        .orElseThrow(() -> new InvalidWithdrawalRequestException("학생 정보를 찾을 수 없습니다."));
                AcademicStatus previousStatus = student.getAcademicStatus();
                request.approve(processor, effectiveDate, now);
                student.changeAcademicStatus(AcademicStatus.WITHDRAWN);
                withdrawalRepository.flush();
                historyRepository.saveAndFlush(AcademicStatusHistory.withdrawalApproved(
                        student, previousStatus, processor, request.getId()
                ));
                return WithdrawalResponseDTO.from(request);
            }
            request.reject(processor, requiredRejectReason(review.rejectReason()), now);
        } catch (IllegalStateException exception) {
            throw new InvalidWithdrawalRequestException("지도교수 승인 상태인 신청만 최종 처리할 수 있습니다.");
        }
        return toResponse(withdrawalRepository.saveAndFlush(request));
    }

    private WithdrawalRequest getForUpdate(Long withdrawalId) {
        if (withdrawalId == null || withdrawalId <= 0) {
            throw new InvalidWithdrawalRequestException("withdrawalId는 양수여야 합니다.");
        }
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
        return rejectReason.trim();
    }

    private void validateUser(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null) {
            throw new WithdrawalAccessDeniedException("인증된 사용자만 자퇴 신청을 조회할 수 있습니다.");
        }
    }

    private void validateRole(CurrentUser currentUser, String role) {
        if (currentUser == null || currentUser.id() == null || !role.equals(currentUser.role())) {
            throw new WithdrawalAccessDeniedException("자퇴 신청 처리 권한이 없습니다.");
        }
    }
}
