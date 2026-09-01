package com.msa4lmsv2academic.domain.doublemajor.service;

import com.msa4lmsv2academic.domain.doublemajor.repository.DoubleMajorQueryRepository;
import com.msa4lmsv2academic.domain.doublemajor.request.*;
import com.msa4lmsv2academic.domain.doublemajor.response.DoubleMajorPeriodResponseDTO;
import com.msa4lmsv2academic.domain.semester.repository.SemesterRepository;
import com.msa4lmsv2academic.domain.transfer.entity.*;
import com.msa4lmsv2academic.domain.transfer.repository.AcademicChangeRequestPeriodRepository;
import com.msa4lmsv2academic.domain.transfer.service.*;
import com.msa4lmsv2academic.global.error.*;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoubleMajorPeriodService {
    private static final AcademicChangeRequestType TYPE = AcademicChangeRequestType.DOUBLE_MAJOR;
    private final AcademicChangeRequestPeriodRepository repository;
    private final DoubleMajorQueryRepository queries;
    private final SemesterRepository semesterRepository;
    private final DoubleMajorPolicy policy;
    private final DepartmentTransferIdempotencyService idempotency;
    private final DepartmentTransferAuditService audit;

    public PageResponseDTO<DoubleMajorPeriodResponseDTO> search(DoubleMajorPeriodSearchRequestDTO filter,
                                                                 CurrentUser actor, Pageable pageable) {
        policy.requireReader(actor);
        var now = DoubleMajorPolicy.now();
        var result = queries.searchPeriods(filter, !actor.isAdmin(), pageable);
        return new PageResponseDTO<>(result.map(period -> DoubleMajorPeriodResponseDTO.from(period, now)).getContent(),
                result.getTotalElements(), filter.resolvedPage(), filter.resolvedSize(), result.hasNext());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DoubleMajorPeriodResponseDTO create(DoubleMajorPeriodSaveRequestDTO body, String key,
                                                CurrentUser actor, DepartmentTransferAuditContext context) {
        policy.requireRole(actor, "ADMIN");
        policy.validatePeriod(body);
        idempotency.validateKey(key);
        String endpoint = "POST /api/academic/catalog/double-major-periods";
        String hash = idempotency.hash(body);
        var now = DoubleMajorPolicy.now();
        var replay = idempotency.replay(key, actor.id(), endpoint, hash, now, DoubleMajorPeriodResponseDTO.class);
        if (replay.isPresent()) return replay.orElseThrow();
        var semester = semesterRepository.findById(body.semesterId())
                .orElseThrow(() -> new DoubleMajorNotFoundException("모집 회차 기준 학기를 찾을 수 없습니다."));
        if (repository.existsBySemesterIdAndRequestType(body.semesterId(), TYPE)) {
            throw new DoubleMajorConflictException("같은 기준 학기의 복수전공 모집 기간이 이미 있습니다.");
        }
        validateNoOverlap(null, body.startAt(), body.endAt(), body.active());
        var reserved = idempotency.reserve(key, actor.id(), endpoint, hash, now);
        AcademicChangeRequestPeriod period;
        try {
            period = repository.saveAndFlush(AcademicChangeRequestPeriod.create(semester, TYPE, body.startAt(),
                    body.endAt(), body.active()));
        } catch (DataIntegrityViolationException exception) {
            throw duplicateOrRethrow(exception);
        }
        audit.record(period.getId(), "ACADEMIC_CHANGE_PERIOD", null, audit.snapshot(period),
                "DOUBLE_MAJOR_PERIOD_CREATED", body.reason(), actor, context);
        var response = DoubleMajorPeriodResponseDTO.from(period, now);
        idempotency.complete(reserved, response);
        return response;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DoubleMajorPeriodResponseDTO update(Long id, DoubleMajorPeriodSaveRequestDTO body, String key,
                                                CurrentUser actor, DepartmentTransferAuditContext context) {
        policy.requireRole(actor, "ADMIN");
        policy.requireId(id);
        policy.validatePeriod(body);
        idempotency.validateKey(key);
        var period = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new DoubleMajorNotFoundException("복수전공 모집 기간을 찾을 수 없습니다."));
        if (period.getRequestType() != TYPE || !period.getSemester().getId().equals(body.semesterId())) {
            throw new DoubleMajorConflictException("기존 기간의 기준 학기와 신청 유형은 변경할 수 없습니다.");
        }
        String endpoint = "PUT /api/academic/catalog/double-major-periods/" + id;
        String hash = idempotency.hash(body);
        var now = DoubleMajorPolicy.now();
        var replay = idempotency.replay(key, actor.id(), endpoint, hash, now, DoubleMajorPeriodResponseDTO.class);
        if (replay.isPresent()) return replay.orElseThrow();
        validateNoOverlap(id, body.startAt(), body.endAt(), body.active());
        var reserved = idempotency.reserve(key, actor.id(), endpoint, hash, now);
        var before = audit.snapshot(period);
        period.change(body.startAt(), body.endAt(), body.active());
        repository.flush();
        audit.record(id, "ACADEMIC_CHANGE_PERIOD", before, audit.snapshot(period),
                "DOUBLE_MAJOR_PERIOD_UPDATED", body.reason(), actor, context);
        var response = DoubleMajorPeriodResponseDTO.from(period, now);
        idempotency.complete(reserved, response);
        return response;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DoubleMajorPeriodResponseDTO changeStatus(Long id, DoubleMajorPeriodStatusRequestDTO body, String key,
                                                      CurrentUser actor, DepartmentTransferAuditContext context) {
        policy.requireRole(actor, "ADMIN");
        policy.requireId(id);
        if (body == null || body.active() == null) {
            throw new InvalidDoubleMajorRequestException("활성 여부와 변경 사유가 필요합니다.");
        }
        String reason = policy.requiredReason(body.reason(), 255);
        idempotency.validateKey(key);
        var period = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new DoubleMajorNotFoundException("복수전공 모집 기간을 찾을 수 없습니다."));
        if (period.getRequestType() != TYPE) {
            throw new DoubleMajorNotFoundException("복수전공 모집 기간을 찾을 수 없습니다.");
        }
        String endpoint = "PATCH /api/academic/catalog/double-major-periods/" + id + "/status";
        String hash = idempotency.hash(body);
        var now = DoubleMajorPolicy.now();
        var replay = idempotency.replay(key, actor.id(), endpoint, hash, now, DoubleMajorPeriodResponseDTO.class);
        if (replay.isPresent()) return replay.orElseThrow();
        validateNoOverlap(id, period.getStartAt(), period.getEndAt(), body.active());
        var reserved = idempotency.reserve(key, actor.id(), endpoint, hash, now);
        var before = audit.snapshot(period);
        period.changeActive(body.active());
        repository.flush();
        audit.record(id, "ACADEMIC_CHANGE_PERIOD", before, audit.snapshot(period),
                "DOUBLE_MAJOR_PERIOD_STATUS_CHANGED", reason, actor, context);
        var response = DoubleMajorPeriodResponseDTO.from(period, now);
        idempotency.complete(reserved, response);
        return response;
    }

    private void validateNoOverlap(Long excludedId, java.time.LocalDateTime startAt,
                                   java.time.LocalDateTime endAt, boolean active) {
        if (!active) return;
        long overlaps = excludedId == null
                ? repository.countActiveOverlaps(TYPE, startAt, endAt)
                : repository.countActiveOverlaps(TYPE, excludedId, startAt, endAt);
        if (overlaps > 0) {
            throw new DoubleMajorConflictException("활성 복수전공 모집 기간은 서로 겹칠 수 없습니다.");
        }
    }

    private RuntimeException duplicateOrRethrow(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.sql.SQLException sql && sql.getErrorCode() == 1062
                    && sql.getMessage().contains("uk_academic_change_periods_semester_type")) {
                return new DoubleMajorConflictException("같은 기준 학기의 복수전공 모집 기간이 이미 있습니다.");
            }
        }
        return exception;
    }
}
