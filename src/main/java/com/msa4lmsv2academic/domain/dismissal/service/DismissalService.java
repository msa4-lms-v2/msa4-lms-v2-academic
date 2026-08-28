package com.msa4lmsv2academic.domain.dismissal.service;

import com.msa4lmsv2academic.domain.dismissal.entity.*;
import com.msa4lmsv2academic.domain.dismissal.repository.*;
import com.msa4lmsv2academic.domain.dismissal.request.*;
import com.msa4lmsv2academic.domain.dismissal.response.DismissalResponseDTO;
import com.msa4lmsv2academic.domain.leaverequest.service.LeaveAuditContext;
import com.msa4lmsv2academic.domain.leaverequest.service.LeaveDismissalCancellationService;
import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.withdrawal.repository.WithdrawalQueryRepository;
import com.msa4lmsv2academic.domain.withdrawal.service.AcademicStatusHistoryWriter;
import com.msa4lmsv2academic.domain.withdrawal.service.WithdrawalAuditContext;
import com.msa4lmsv2academic.domain.withdrawal.service.WithdrawalDismissalCancellationService;
import com.msa4lmsv2academic.global.error.*;
import com.msa4lmsv2academic.global.idempotency.AcademicIdempotencyKey;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DismissalService {
    private final DismissalCandidateRepository repository;
    private final DismissalQueryRepository queries;
    private final WithdrawalQueryRepository participants;
    private final DismissalPolicy policy;
    private final DismissalIdempotencyService idempotency;
    private final DismissalAuditService audit;
    private final AcademicStatusHistoryWriter history;
    private final LeaveDismissalCancellationService leaves;
    private final WithdrawalDismissalCancellationService withdrawals;

    public DismissalResponseDTO get(Long id, CurrentUser actor) {
        policy.requireAdmin(actor);
        requireId(id);
        return DismissalResponseDTO.from(queries.findDetail(id)
                .orElseThrow(() -> new DismissalNotFoundException("제적 후보를 찾을 수 없습니다.")));
    }

    public PageResponseDTO<DismissalResponseDTO> search(DismissalSearchRequestDTO filter, CurrentUser actor, Pageable pageable) {
        policy.requireAdmin(actor);
        var page = queries.search(filter, pageable);
        return new PageResponseDTO<>(page.getContent().stream().map(DismissalResponseDTO::from).toList(),
                page.getTotalElements(), filter.resolvedPage(), filter.resolvedSize(), page.hasNext());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DismissalResponseDTO create(DismissalCreateRequestDTO body, String key, CurrentUser actor, DismissalAuditContext context) {
        policy.requireAdmin(actor);
        idempotency.validateKey(key);
        if (body == null) throw new InvalidDismissalRequestException("등록 본문이 필요합니다.");
        requireId(body.studentId());
        policy.validateReason(body.reasonType(), body.reason());
        Student student = lockStudent(body.studentId());
        requireActor(actor);
        String endpoint = "POST /api/academic/dismissals";
        String hash = idempotency.hash(body);
        var replay = idempotency.replay(key, actor.id(), endpoint, hash, DismissalPolicy.now(), DismissalResponseDTO.class);
        if (replay.isPresent()) return replay.orElseThrow();
        var reserved = idempotency.reserve(key, actor.id(), endpoint, hash, DismissalPolicy.now());
        policy.validateAcademicStatus(student.getAcademicStatus(), body.reasonType());
        if (repository.existsByStudentIdAndStatus(student.getId(), DismissalStatus.PENDING)) {
            throw new DismissalConflictException("학생에게 이미 대기 중인 제적 후보가 있습니다.");
        }
        var candidate = DismissalCandidate.create(student, body.reasonType(), body.reason(), actor.id());
        return finish(candidate, reserved, null, "DISMISSAL_CREATED", actor, context);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DismissalResponseDTO update(Long id, DismissalUpdateRequestDTO body, String key, CurrentUser actor, DismissalAuditContext context) {
        policy.requireAdmin(actor);
        idempotency.validateKey(key);
        if (body == null) throw new InvalidDismissalRequestException("수정 본문이 필요합니다.");
        policy.validateReason(body.reasonType(), body.reason());
        var candidate = lockCandidate(id);
        requireActor(actor);
        String endpoint = "PUT /api/academic/dismissals/" + id;
        String hash = idempotency.hash(body);
        var replay = idempotency.replay(key, actor.id(), endpoint, hash, DismissalPolicy.now(), DismissalResponseDTO.class);
        if (replay.isPresent()) return replay.orElseThrow();
        var reserved = idempotency.reserve(key, actor.id(), endpoint, hash, DismissalPolicy.now());
        policy.validateVersion(candidate, body.version());
        policy.validateAcademicStatus(candidate.getStudent().getAcademicStatus(), body.reasonType());
        var before = audit.snapshot(candidate);
        candidate.revise(body.reasonType(), body.reason());
        return finish(candidate, reserved, before, "DISMISSAL_UPDATED", actor, context);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DismissalResponseDTO changeStatus(Long id, DismissalStatusRequestDTO body, String key, CurrentUser actor, DismissalAuditContext context) {
        policy.requireAdmin(actor);
        idempotency.validateKey(key);
        if (body == null || (body.status() != DismissalStatus.CONFIRMED && body.status() != DismissalStatus.CANCELLED)) {
            throw new InvalidDismissalRequestException("CONFIRMED 또는 CANCELLED만 요청할 수 있습니다.");
        }
        if (body.status() == DismissalStatus.CANCELLED) policy.requireReason(body.cancelReason());
        else if (body.cancelReason() != null) throw new InvalidDismissalRequestException("확정 요청에는 취소 사유를 넣을 수 없습니다.");
        var candidate = lockCandidate(id);
        var processor = requireActor(actor);
        String endpoint = "PATCH /api/academic/dismissals/" + id + "/status";
        String hash = idempotency.hash(body);
        var replay = idempotency.replay(key, actor.id(), endpoint, hash, DismissalPolicy.now(), DismissalResponseDTO.class);
        if (replay.isPresent()) return replay.orElseThrow();
        var reserved = idempotency.reserve(key, actor.id(), endpoint, hash, DismissalPolicy.now());
        policy.validateVersion(candidate, body.version());
        var before = audit.snapshot(candidate);
        var now = DismissalPolicy.now();
        if (body.status() == DismissalStatus.CONFIRMED) {
            var student = candidate.getStudent();
            policy.validateAcademicStatus(student.getAcademicStatus(), candidate.getReasonType());
            var previous = student.getAcademicStatus();
            leaves.cancelPending(student.getId(), id, actor, new LeaveAuditContext(context.requestId(), context.ipAddress()));
            withdrawals.cancelPending(student.getId(), id, processor, actor, now,
                    new WithdrawalAuditContext(context.requestId(), context.ipAddress()));
            candidate.confirm(actor.id(), now);
            student.changeAcademicStatus(AcademicStatus.DISMISSED);
            history.recordDismissal(student, previous, processor, id);
        } else {
            // 복학 등 학적 변경 이후에도 유효하지 않은 대기 후보를 취소할 수 있습니다.
            candidate.cancel(actor.id(), body.cancelReason(), now);
        }
        return finish(candidate, reserved, before,
                body.status() == DismissalStatus.CONFIRMED ? "DISMISSAL_CONFIRMED" : "DISMISSAL_CANCELLED", actor, context);
    }

    private DismissalCandidate lockCandidate(Long id) {
        requireId(id);
        Long studentId = repository.findStudentIdById(id)
                .orElseThrow(() -> new DismissalNotFoundException("제적 후보를 찾을 수 없습니다."));
        lockStudent(studentId);
        return repository.findByIdForUpdate(id)
                .orElseThrow(() -> new DismissalNotFoundException("제적 후보를 찾을 수 없습니다."));
    }

    private Student lockStudent(Long studentId) {
        var student = participants.findStudentByIdForUpdate(studentId)
                .orElseThrow(() -> new DismissalNotFoundException("학생을 찾을 수 없습니다."));
        // soft-deleted 사용자의 신규 처리와 조회 우회를 방지합니다.
        participants.findUserById(student.getUser().getId())
                .orElseThrow(() -> new DismissalNotFoundException("학생 사용자를 찾을 수 없습니다."));
        return student;
    }

    private User requireActor(CurrentUser actor) {
        return participants.findUserById(actor.id())
                .orElseThrow(() -> new DismissalAccessDeniedException("처리자 사용자 정보를 찾을 수 없습니다."));
    }

    private void requireId(Long id) {
        if (id == null || id <= 0) throw new InvalidDismissalRequestException("양수 ID가 필요합니다.");
    }

    private DismissalResponseDTO finish(DismissalCandidate candidate, AcademicIdempotencyKey key, Map<String, Object> before,
                                        String action, CurrentUser actor, DismissalAuditContext context) {
        try {
            repository.saveAndFlush(candidate);
        } catch (DataIntegrityViolationException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof java.sql.SQLException sql && sql.getErrorCode() == 1062
                        && sql.getMessage().contains("uk_dismissal_candidates_active_student")) {
                    throw new DismissalConflictException("학생에게 이미 대기 중인 제적 후보가 있습니다.");
                }
            }
            throw exception;
        }
        var response = DismissalResponseDTO.from(candidate);
        audit.record(candidate, before, action, actor, context);
        idempotency.complete(key, response);
        return response;
    }
}
