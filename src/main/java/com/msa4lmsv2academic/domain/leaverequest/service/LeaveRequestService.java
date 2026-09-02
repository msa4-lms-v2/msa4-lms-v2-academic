package com.msa4lmsv2academic.domain.leaverequest.service;

import com.msa4lmsv2academic.domain.leaverequest.entity.*;
import com.msa4lmsv2academic.domain.leaverequest.repository.LeaveRequestQueryRepository;
import com.msa4lmsv2academic.domain.leaverequest.repository.LeaveRequestRepository;
import com.msa4lmsv2academic.domain.leaverequest.repository.LeaveRequestFileRepository;
import com.msa4lmsv2academic.domain.leaverequest.request.*;
import com.msa4lmsv2academic.domain.leaverequest.response.LeaveRequestResponseDTO;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.withdrawal.repository.WithdrawalQueryRepository;
import com.msa4lmsv2academic.domain.withdrawal.service.AcademicStatusHistoryWriter;
import com.msa4lmsv2academic.global.error.*;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaveRequestService {
    private static final String CREATE_ENDPOINT = "POST /api/academic/leave-requests";
    private final LeaveRequestRepository repository;
    private final LeaveRequestFileRepository fileRepository;
    private final LeaveRequestQueryRepository queries;
    private final WithdrawalQueryRepository studentQueries;
    private final LeaveRequestPolicy policy;
    private final LeaveIdempotencyService idempotency;
    private final LeaveAuditService audit;
    private final AcademicStatusHistoryWriter historyWriter;

    public PageResponseDTO<LeaveRequestResponseDTO> search(LeaveRequestSearchRequestDTO filter, CurrentUser actor,
                                                          Pageable pageable) {
        policy.requireReader(actor);
        if (!actor.isAdmin() && filter.studentId() != null) {
            Student student = queries.findStudentByUserId(actor.id()).orElseThrow(this::studentMissing);
            if (!student.getId().equals(filter.studentId())) throw accessDenied();
        }
        var result = queries.search(filter, actor.isAdmin() ? null : actor.id(), pageable);
        return new PageResponseDTO<>(result.map(LeaveRequestResponseDTO::from).getContent(), result.getTotalElements(),
                filter.resolvedPage(), filter.resolvedSize(), result.hasNext());
    }

    public LeaveRequestResponseDTO get(Long id, CurrentUser actor) {
        return LeaveRequestResponseDTO.from(readable(id, actor));
    }

    public LeaveAttachment attachment(Long id, Long fileId, CurrentUser actor) {
        LeaveRequest request = readable(id, actor);
        var file = fileRepository.findByIdAndRequestId(fileId, request.getId())
                .orElseThrow(() -> new LeaveRequestNotFoundException("등록된 증빙 파일이 없습니다."));
        return new LeaveAttachment(file.getOriginalName(), file.getStoredName(), file.getContentType(), file.getSize());
    }

    public LeaveAttachment firstAttachment(Long id, CurrentUser actor) {
        LeaveRequest request = readable(id, actor);
        if (!request.getFiles().isEmpty()) {
            var file = request.getFiles().getFirst();
            return new LeaveAttachment(file.getOriginalName(), file.getStoredName(), file.getContentType(), file.getSize());
        }
        if (request.getAttachmentStoredName() != null) {
            return new LeaveAttachment(request.getAttachmentOriginalName(), request.getAttachmentStoredName(),
                    request.getAttachmentContentType(), request.getAttachmentSize());
        }
        throw new LeaveRequestNotFoundException("등록된 증빙이 없습니다.");
    }

    // 원격 업로드 전에 권한/완료 재생/업무 조건을 읽기로 검사합니다. 쓰기 transaction에서 반드시 재검증합니다.
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Optional<LeaveRequestResponseDTO> preflight(LeaveRequestCreateRequestDTO body, String key, String hash,
                                                       CurrentUser actor) {
        validateCreateInput(body, key, actor);
        Student student = queries.findStudentByUserId(actor.id()).orElseThrow(this::studentMissing);
        var replay = idempotency.replay(key, actor.id(), CREATE_ENDPOINT, hash, LeaveRequestPolicy.now(),
                LeaveRequestResponseDTO.class);
        if (replay.isPresent()) return replay;
        try {
            resolveCreation(student, body, false);
        } catch (LeaveRequestConflictException exception) {
            // 최초 조회 직후 같은 요청이 완료된 경우 중복/학적 충돌 대신 완료 응답을 재생합니다.
            var completed = idempotency.replay(key, actor.id(), CREATE_ENDPOINT, hash, LeaveRequestPolicy.now(),
                    LeaveRequestResponseDTO.class);
            if (completed.isPresent()) return completed;
            throw exception;
        }
        return Optional.empty();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LeaveRequestCreationResult create(LeaveRequestCreateRequestDTO body, List<LeaveAttachment> attachments,
                                             String key, String hash, CurrentUser actor, LeaveAuditContext context) {
        validateCreateInput(body, key, actor);
        Student student = studentQueries.findStudentByUserIdForUpdate(actor.id()).orElseThrow(this::studentMissing);
        var now = LeaveRequestPolicy.now();
        var replay = idempotency.replay(key, actor.id(), CREATE_ENDPOINT, hash, now, LeaveRequestResponseDTO.class);
        if (replay.isPresent()) return new LeaveRequestCreationResult(replay.orElseThrow(), false);
        ResolvedCreation resolved = resolveCreation(student, body, true);
        if (resolved.type() == LeaveRequestType.MILITARY_LEAVE && attachments.size() != 1) {
            throw new InvalidLeaveRequestException("군휴학에는 입영통지서 PDF 1개가 필수입니다.");
        }
        var reserved = idempotency.reserve(key, actor.id(), CREATE_ENDPOINT, hash, now);
        LeaveRequest request = LeaveRequest.create(student, resolved.type(), resolved.reason(), body.targetYear(),
                body.targetSemester(), resolved.returnYear(), resolved.returnTerm());
        for (LeaveAttachment file : attachments) {
            request.addFile(file.originalName(), file.storedName(), file.contentType(), file.size());
        }
        request = repository.saveAndFlush(request);
        var after = audit.snapshot(request);
        if (resolved.basis() != null) {
            after.put("applicationSemesterId", resolved.basis().getId());
            after.put("applicationYear", resolved.basis().getAcademicYear());
            after.put("applicationTerm", resolved.basis().getTerm());
        }
        audit.record(request.getId(), "LEAVE_REQUEST", null, after, "LEAVE_CREATED", "휴·복학 신청", actor, context);
        var response = LeaveRequestResponseDTO.from(request);
        idempotency.complete(reserved, response);
        return new LeaveRequestCreationResult(response, true);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LeaveRequestResponseDTO changeStatus(Long id, LeaveRequestStatusChangeRequestDTO body, String key,
                                                CurrentUser actor, LeaveAuditContext context) {
        policy.requireReader(actor);
        policy.requireId(id);
        idempotency.validateKey(key);
        if (body == null || body.status() == null || body.status() == LeaveRequestStatus.PENDING
                || (body.reason() != null && body.reason().length() > 500)) {
            throw new InvalidLeaveRequestException("승인·반려·취소 상태와 500자 이하 사유가 필요합니다.");
        }
        if (body.status() == LeaveRequestStatus.CANCELLED) policy.requireRole(actor, "STUDENT");
        else policy.requireRole(actor, "ADMIN");
        Long studentId = repository.findStudentIdById(id).orElseThrow(this::requestMissing);
        studentQueries.findStudentByIdForUpdate(studentId).orElseThrow(this::studentMissing);
        LeaveRequest request = repository.findByIdForUpdate(id).orElseThrow(this::requestMissing);
        requireOwnerOrAdmin(request, actor);
        String endpoint = "PATCH /api/academic/leave-requests/" + id + "/status";
        String hash = idempotency.hash(body);
        var now = LeaveRequestPolicy.now();
        var replay = idempotency.replay(key, actor.id(), endpoint, hash, now, LeaveRequestResponseDTO.class);
        if (replay.isPresent()) return replay.orElseThrow();
        policy.requirePending(request);
        var reserved = idempotency.reserve(key, actor.id(), endpoint, hash, now);
        Map<String, Object> before = audit.snapshot(request);
        switch (body.status()) {
            case APPROVED -> approve(request, actor, now);
            case REJECTED -> request.reject(policy.requiredReason(body.reason(), 500));
            case CANCELLED -> request.cancel(policy.requiredReason(body.reason(), 500));
            default -> throw new InvalidLeaveRequestException("허용되지 않은 상태입니다.");
        }
        repository.flush();
        var after = audit.snapshot(request);
        after.put("decisionReason", body.reason());
        audit.record(id, "LEAVE_REQUEST", before, after,
                "LEAVE_" + body.status().name(), "휴·복학 신청 " + body.status().name(), actor, context);
        var response = LeaveRequestResponseDTO.from(request);
        idempotency.complete(reserved, response);
        return response;
    }

    private void approve(LeaveRequest request, CurrentUser actor, LocalDateTime now) {
        Student student = request.getStudent();
        policy.validateAcademicStatus(student.getAcademicStatus(), request.getRequestType());
        if (request.getRequestType() == LeaveRequestType.MILITARY_LEAVE) requireMilitaryUnused(student.getId());
        if (!request.getRequestType().isLeave()) {
            LeaveRequest original = currentLeave(student);
            requireReturnTarget(original, request.getTargetYear(), request.getTargetSemester());
            if (returnType(original) != request.getRequestType()) {
                throw new LeaveRequestConflictException("현재 휴학 근거와 복학 신청 유형이 일치하지 않습니다.");
            }
        }
        var period = queries.findPeriod(request.getTargetYear(), request.getTargetSemester(), request.getRequestType(), true)
                .orElseThrow(() -> new LeaveRequestConflictException("승인 기간 설정이 없습니다."));
        if (!period.allowsApproval(now)) throw new LeaveRequestConflictException("현재는 승인 가능한 기간이 아닙니다.");
        var reviewer = studentQueries.findUserById(actor.id()).orElseThrow(this::studentMissing);
        AcademicStatus previous = student.getAcademicStatus();
        request.approve();
        student.changeAcademicStatus(request.getRequestType().isLeave() ? AcademicStatus.ON_LEAVE : AcademicStatus.ENROLLED);
        repository.flush();
        historyWriter.recordLeave(student, previous, reviewer, request.getId());
    }

    private ResolvedCreation resolveCreation(Student student, LeaveRequestCreateRequestDTO body, boolean lock) {
        policy.validateAcademicStatus(student.getAcademicStatus(), body.requestType());
        if (repository.existsByStudentIdAndStatus(student.getId(), LeaveRequestStatus.PENDING)) {
            throw new LeaveRequestConflictException("진행 중인 휴·복학 신청이 있습니다.");
        }
        LeaveRequestType type = body.requestType();
        Short returnYear = body.returnYear();
        Byte returnTerm = body.returnSemester();
        Semester basis = null;
        if (type.isLeave()) {
            var current = queries.findCurrentSemesters();
            if (current.size() != 1) throw new LeaveRequestConflictException("현재 학기가 유일하게 지정되어야 합니다.");
            basis = current.getFirst();
            byte currentTerm = (byte) (basis.getTerm() == SemesterTerm.FIRST ? 1 : 2);
            int currentIndex = policy.termIndex(basis.getAcademicYear(), currentTerm);
            int targetIndex = policy.termIndex(body.targetYear(), body.targetSemester());
            if (type == LeaveRequestType.GENERAL_LEAVE && targetIndex != currentIndex + 1) {
                throw new LeaveRequestConflictException("일반휴학의 적용 학기는 현재 학기의 다음 학기여야 합니다.");
            }
            if (type == LeaveRequestType.MILITARY_LEAVE) {
                requireMilitaryUnused(student.getId());
                int computedYear = (currentIndex + 4) / 2;
                if (computedYear > Short.MAX_VALUE || targetIndex >= currentIndex + 4) {
                    throw new InvalidLeaveRequestException("군휴학 적용 학기와 복학 예정의 범위가 올바르지 않습니다.");
                }
                returnYear = (short) computedYear;
                returnTerm = currentTerm;
            }
        } else {
            var original = currentLeave(student);
            requireReturnTarget(original, body.targetYear(), body.targetSemester());
            type = returnType(original);
        }
        var period = queries.findPeriod(body.targetYear(), body.targetSemester(), type, lock)
                .orElseThrow(() -> new LeaveRequestConflictException("신청 유형과 적용 학기의 접수 기간 설정이 없습니다."));
        if (!period.accepts(LeaveRequestPolicy.now())) {
            throw new LeaveRequestConflictException("현재는 접수 가능한 기간이 아닙니다.");
        }
        String reason = body.reason();
        if (reason == null || reason.isBlank()) reason = type == LeaveRequestType.MILITARY_LEAVE ? "군입대" : "복학";
        return new ResolvedCreation(type, reason, returnYear, returnTerm, basis);
    }

    private LeaveRequest currentLeave(Student student) {
        var history = queries.findLatestHistory(student.getId())
                .orElseThrow(() -> new LeaveRequestConflictException("현재 휴학의 실제 승인 이력을 확인할 수 없습니다."));
        if (!"LEAVE_REQUEST".equals(history.getSourceType()) || history.getSourceId() == null
                || history.getNewStatus() != AcademicStatus.ON_LEAVE) {
            throw new LeaveRequestConflictException("현재 휴학의 실제 승인 근거를 보정한 후 복학을 신청하십시오.");
        }
        var original = repository.findById(history.getSourceId()).orElseThrow(
                () -> new LeaveRequestConflictException("원본 휴학 신청이 없습니다."));
        if (!original.getStudent().getId().equals(student.getId()) || original.getStatus() != LeaveRequestStatus.APPROVED
                || !original.getRequestType().isLeave() || original.getReturnYear() == null || original.getReturnSemester() == null) {
            throw new LeaveRequestConflictException("원본 승인 휴학과 현재 학적이 일치하지 않습니다.");
        }
        return original;
    }

    private void requireReturnTarget(LeaveRequest original, short year, byte term) {
        if (original.getReturnYear() != year || original.getReturnSemester() != term) {
            throw new LeaveRequestConflictException("복학 대상 학기는 원본 휴학의 복학 예정 학기와 일치해야 합니다.");
        }
    }

    private LeaveRequestType returnType(LeaveRequest original) {
        return original.getRequestType() == LeaveRequestType.MILITARY_LEAVE
                ? LeaveRequestType.MILITARY_RETURN : LeaveRequestType.GENERAL_RETURN;
    }

    private void requireMilitaryUnused(Long studentId) {
        if (repository.existsByStudentIdAndRequestTypeAndStatus(studentId, LeaveRequestType.MILITARY_LEAVE,
                LeaveRequestStatus.APPROVED)) throw new LeaveRequestConflictException("군휴학은 한 번만 승인받을 수 있습니다.");
    }

    private void validateCreateInput(LeaveRequestCreateRequestDTO body, String key, CurrentUser actor) {
        policy.requireRole(actor, "STUDENT");
        policy.validateCreate(body);
        idempotency.validateKey(key);
    }

    private LeaveRequest readable(Long id, CurrentUser actor) {
        policy.requireReader(actor);
        policy.requireId(id);
        var request = queries.findDetail(id).orElseThrow(this::requestMissing);
        requireOwnerOrAdmin(request, actor);
        return request;
    }

    private void requireOwnerOrAdmin(LeaveRequest request, CurrentUser actor) {
        if (!actor.isAdmin() && !request.getStudent().getUser().getId().equals(actor.id())) throw accessDenied();
    }

    private LeaveRequestAccessDeniedException accessDenied() {
        return new LeaveRequestAccessDeniedException("본인의 휴·복학 신청만 접근할 수 있습니다.");
    }

    private LeaveRequestNotFoundException requestMissing() {
        return new LeaveRequestNotFoundException("휴·복학 신청이 없습니다.");
    }

    private LeaveRequestNotFoundException studentMissing() {
        return new LeaveRequestNotFoundException("사용자 또는 학생 정보를 찾을 수 없습니다.");
    }

    private record ResolvedCreation(LeaveRequestType type, String reason, Short returnYear, Byte returnTerm, Semester basis) { }
}
