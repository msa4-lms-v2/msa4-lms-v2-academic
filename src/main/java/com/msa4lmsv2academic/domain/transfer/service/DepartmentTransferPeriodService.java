package com.msa4lmsv2academic.domain.transfer.service;

import com.msa4lmsv2academic.domain.semester.repository.SemesterRepository;
import com.msa4lmsv2academic.domain.transfer.entity.*;
import com.msa4lmsv2academic.domain.transfer.repository.*;
import com.msa4lmsv2academic.domain.transfer.request.*;
import com.msa4lmsv2academic.domain.transfer.response.DepartmentTransferPeriodResponseDTO;
import com.msa4lmsv2academic.global.error.*;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentTransferPeriodService {
    private static final AcademicChangeRequestType TYPE = AcademicChangeRequestType.TRANSFER_DEPARTMENT;
    private final AcademicChangeRequestPeriodRepository repository;
    private final DepartmentTransferQueryRepository queries;
    private final SemesterRepository semesterRepository;
    private final DepartmentTransferPolicy policy;
    private final DepartmentTransferIdempotencyService idempotency;
    private final DepartmentTransferAuditService audit;

    public PageResponseDTO<DepartmentTransferPeriodResponseDTO> search(DepartmentTransferPeriodSearchRequestDTO filter,
                                                                       CurrentUser actor, Pageable pageable) {
        policy.requireReader(actor);
        var now = DepartmentTransferPolicy.now();
        var result = queries.searchPeriods(filter, !actor.isAdmin(), pageable);
        return new PageResponseDTO<>(result.map(period -> DepartmentTransferPeriodResponseDTO.from(period, now)).getContent(),
                result.getTotalElements(), filter.resolvedPage(), filter.resolvedSize(), result.hasNext());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DepartmentTransferPeriodResponseDTO create(DepartmentTransferPeriodSaveRequestDTO body, String key,
                                                       CurrentUser actor, DepartmentTransferAuditContext context) {
        policy.requireRole(actor, "ADMIN");
        policy.validatePeriod(body);
        idempotency.validateKey(key);
        String endpoint = "POST /api/academic/catalog/department-transfer-periods";
        String hash = idempotency.hash(body);
        var now = DepartmentTransferPolicy.now();
        var replay = idempotency.replay(key, actor.id(), endpoint, hash, now,
                DepartmentTransferPeriodResponseDTO.class);
        if (replay.isPresent()) return replay.orElseThrow();
        var semester = semesterRepository.findById(body.semesterId())
                .orElseThrow(() -> new DepartmentTransferNotFoundException("적용 학기를 찾을 수 없습니다."));
        if (repository.existsBySemesterIdAndRequestType(body.semesterId(), TYPE)) {
            throw new DepartmentTransferConflictException("같은 적용 학기의 전과 접수 기간이 이미 있습니다.");
        }
        var reserved = idempotency.reserve(key, actor.id(), endpoint, hash, now);
        AcademicChangeRequestPeriod period;
        try {
            period = repository.saveAndFlush(AcademicChangeRequestPeriod.create(semester, TYPE, body.startAt(),
                    body.endAt(), body.active()));
        } catch (DataIntegrityViolationException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof java.sql.SQLException sql && sql.getErrorCode() == 1062
                        && sql.getMessage().contains("uk_academic_change_periods_semester_type")) {
                    throw new DepartmentTransferConflictException("같은 적용 학기의 전과 접수 기간이 이미 있습니다.");
                }
            }
            throw exception;
        }
        audit.record(period.getId(), "ACADEMIC_CHANGE_PERIOD", null, audit.snapshot(period),
                "TRANSFER_PERIOD_CREATED", body.reason(), actor, context);
        var response = DepartmentTransferPeriodResponseDTO.from(period, now);
        idempotency.complete(reserved, response);
        return response;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DepartmentTransferPeriodResponseDTO update(Long id, DepartmentTransferPeriodSaveRequestDTO body,
                                                       String key, CurrentUser actor,
                                                       DepartmentTransferAuditContext context) {
        policy.requireRole(actor, "ADMIN");
        policy.requireId(id);
        policy.validatePeriod(body);
        idempotency.validateKey(key);
        var period = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new DepartmentTransferNotFoundException("전과 접수 기간을 찾을 수 없습니다."));
        if (period.getRequestType() != TYPE || !period.getSemester().getId().equals(body.semesterId())) {
            throw new DepartmentTransferConflictException("기존 기간의 적용 학기와 신청 유형은 변경할 수 없습니다.");
        }
        String endpoint = "PUT /api/academic/catalog/department-transfer-periods/" + id;
        String hash = idempotency.hash(body);
        var now = DepartmentTransferPolicy.now();
        var replay = idempotency.replay(key, actor.id(), endpoint, hash, now,
                DepartmentTransferPeriodResponseDTO.class);
        if (replay.isPresent()) return replay.orElseThrow();
        var reserved = idempotency.reserve(key, actor.id(), endpoint, hash, now);
        var before = audit.snapshot(period);
        period.change(body.startAt(), body.endAt(), body.active());
        repository.flush();
        audit.record(id, "ACADEMIC_CHANGE_PERIOD", before, audit.snapshot(period),
                "TRANSFER_PERIOD_UPDATED", body.reason(), actor, context);
        var response = DepartmentTransferPeriodResponseDTO.from(period, now);
        idempotency.complete(reserved, response);
        return response;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DepartmentTransferPeriodResponseDTO changeStatus(Long id, DepartmentTransferPeriodStatusRequestDTO body,
                                                             String key, CurrentUser actor,
                                                             DepartmentTransferAuditContext context) {
        policy.requireRole(actor, "ADMIN");
        policy.requireId(id);
        if (body == null || body.active() == null) {
            throw new InvalidDepartmentTransferRequestException("활성 여부와 변경 사유가 필요합니다.");
        }
        String reason = policy.requiredReason(body.reason(), 255);
        idempotency.validateKey(key);
        var period = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new DepartmentTransferNotFoundException("전과 접수 기간을 찾을 수 없습니다."));
        if (period.getRequestType() != TYPE) {
            throw new DepartmentTransferNotFoundException("전과 접수 기간을 찾을 수 없습니다.");
        }
        String endpoint = "PATCH /api/academic/catalog/department-transfer-periods/" + id + "/status";
        String hash = idempotency.hash(body);
        var now = DepartmentTransferPolicy.now();
        var replay = idempotency.replay(key, actor.id(), endpoint, hash, now,
                DepartmentTransferPeriodResponseDTO.class);
        if (replay.isPresent()) return replay.orElseThrow();
        var reserved = idempotency.reserve(key, actor.id(), endpoint, hash, now);
        var before = audit.snapshot(period);
        period.changeActive(body.active());
        repository.flush();
        audit.record(id, "ACADEMIC_CHANGE_PERIOD", before, audit.snapshot(period),
                "TRANSFER_PERIOD_STATUS_CHANGED", reason, actor, context);
        var response = DepartmentTransferPeriodResponseDTO.from(period, now);
        idempotency.complete(reserved, response);
        return response;
    }
}
