package com.msa4lmsv2academic.domain.leaverequest.service;

import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequestPeriod;
import com.msa4lmsv2academic.domain.leaverequest.repository.LeavePeriodRepository;
import com.msa4lmsv2academic.domain.leaverequest.repository.LeaveRequestQueryRepository;
import com.msa4lmsv2academic.domain.leaverequest.request.LeavePeriodSaveRequestDTO;
import com.msa4lmsv2academic.domain.leaverequest.request.LeavePeriodSearchRequestDTO;
import com.msa4lmsv2academic.domain.leaverequest.response.LeavePeriodResponseDTO;
import com.msa4lmsv2academic.global.error.LeaveRequestConflictException;
import com.msa4lmsv2academic.global.error.LeaveRequestNotFoundException;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeavePeriodService {
    private final LeavePeriodRepository repository;
    private final LeaveRequestQueryRepository queryRepository;
    private final LeaveRequestPolicy policy;
    private final LeaveIdempotencyService idempotency;
    private final LeaveAuditService audit;

    public PageResponseDTO<LeavePeriodResponseDTO> search(LeavePeriodSearchRequestDTO filter, CurrentUser actor,
                                                         Pageable pageable) {
        policy.requireReader(actor);
        var now = LeaveRequestPolicy.now();
        var result = queryRepository.searchPeriods(filter, !actor.isAdmin(), pageable);
        return new PageResponseDTO<>(result.map(p -> LeavePeriodResponseDTO.from(p, now)).getContent(),
                result.getTotalElements(), filter.resolvedPage(), filter.resolvedSize(), result.hasNext());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LeavePeriodResponseDTO create(LeavePeriodSaveRequestDTO body, String key, CurrentUser actor,
                                          LeaveAuditContext context) {
        policy.requireRole(actor, "ADMIN");
        policy.validatePeriod(body);
        idempotency.validateKey(key);
        var semester = queryRepository.findSemesterForUpdate(body.semesterId())
                .orElseThrow(() -> new LeaveRequestNotFoundException("적용 학기가 없습니다."));
        String endpoint = "POST /api/academic/leave-request-periods";
        String hash = idempotency.hash(body);
        var now = LeaveRequestPolicy.now();
        var replay = idempotency.replay(key, actor.id(), endpoint, hash, now, LeavePeriodResponseDTO.class);
        if (replay.isPresent()) return replay.orElseThrow();
        if (repository.existsBySemesterIdAndRequestType(body.semesterId(), body.requestType())) {
            throw new LeaveRequestConflictException("같은 학기·유형의 기간 설정이 이미 있습니다. 기존 설정을 수정하십시오.");
        }
        var reserved = idempotency.reserve(key, actor.id(), endpoint, hash, now);
        var period = repository.saveAndFlush(LeaveRequestPeriod.create(semester, body.requestType(), body.startAt(),
                body.endAt(), body.approvalStartAt(), body.approvalEndAt(), body.active()));
        audit.record(period.getId(), "LEAVE_REQUEST_PERIOD", null, audit.snapshot(period),
                "LEAVE_PERIOD_CREATED", body.reason(), actor, context);
        var response = LeavePeriodResponseDTO.from(period, now);
        idempotency.complete(reserved, response);
        return response;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LeavePeriodResponseDTO update(Long id, LeavePeriodSaveRequestDTO body, String key, CurrentUser actor,
                                          LeaveAuditContext context) {
        policy.requireRole(actor, "ADMIN");
        policy.requireId(id);
        policy.validatePeriod(body);
        idempotency.validateKey(key);
        var period = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new LeaveRequestNotFoundException("기간 설정이 없습니다."));
        String endpoint = "PUT /api/academic/leave-request-periods/" + id;
        String hash = idempotency.hash(body);
        var now = LeaveRequestPolicy.now();
        var replay = idempotency.replay(key, actor.id(), endpoint, hash, now, LeavePeriodResponseDTO.class);
        if (replay.isPresent()) return replay.orElseThrow();
        if (!period.getSemester().getId().equals(body.semesterId()) || period.getRequestType() != body.requestType()) {
            throw new LeaveRequestConflictException("기존 기간의 적용 학기와 유형은 변경할 수 없습니다.");
        }
        var reserved = idempotency.reserve(key, actor.id(), endpoint, hash, now);
        var before = audit.snapshot(period);
        period.change(body.startAt(), body.endAt(), body.approvalStartAt(), body.approvalEndAt(), body.active());
        repository.flush();
        audit.record(id, "LEAVE_REQUEST_PERIOD", before, audit.snapshot(period), "LEAVE_PERIOD_UPDATED",
                body.reason(), actor, context);
        var response = LeavePeriodResponseDTO.from(period, now);
        idempotency.complete(reserved, response);
        return response;
    }
}
