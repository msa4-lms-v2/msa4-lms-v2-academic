package com.msa4lmsv2academic.domain.doublemajor.service;

import com.msa4lmsv2academic.domain.doublemajor.repository.DoubleMajorQueryRepository;
import com.msa4lmsv2academic.domain.doublemajor.request.*;
import com.msa4lmsv2academic.domain.doublemajor.response.DoubleMajorResponseDTO;
import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.organization.repository.DepartmentQueryRepository;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.student.repository.StudentRepository;
import com.msa4lmsv2academic.domain.transfer.entity.*;
import com.msa4lmsv2academic.domain.transfer.repository.*;
import com.msa4lmsv2academic.domain.transfer.service.*;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.repository.UserRepository;
import com.msa4lmsv2academic.global.error.*;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoubleMajorService {
    private static final AcademicChangeRequestType TYPE = AcademicChangeRequestType.DOUBLE_MAJOR;
    private static final String CREATE_ENDPOINT = "POST /api/academic/double-major-requests";
    private final AcademicChangeRequestRepository repository;
    private final AcademicChangeRequestFileRepository fileRepository;
    private final AcademicChangeRequestPeriodRepository periodRepository;
    private final DoubleMajorQueryRepository queries;
    private final StudentRepository studentRepository;
    private final DepartmentQueryRepository departmentRepository;
    private final UserRepository userRepository;
    private final DoubleMajorPolicy policy;
    private final DepartmentTransferIdempotencyService idempotency;
    private final DepartmentTransferAuditService audit;

    public PageResponseDTO<DoubleMajorResponseDTO> search(DoubleMajorSearchRequestDTO filter,
                                                           CurrentUser actor, Pageable pageable) {
        policy.requireReader(actor);
        if (!actor.isAdmin() && filter.studentId() != null) {
            Student student = studentRepository.findByUserId(actor.id()).orElseThrow(this::studentMissing);
            if (!student.getId().equals(filter.studentId())) throw accessDenied();
        }
        var result = queries.search(filter, actor.isAdmin() ? null : actor.id(), pageable);
        return new PageResponseDTO<>(result.map(DoubleMajorResponseDTO::from).getContent(),
                result.getTotalElements(), filter.resolvedPage(), filter.resolvedSize(), result.hasNext());
    }

    public DoubleMajorResponseDTO get(Long id, CurrentUser actor) {
        return DoubleMajorResponseDTO.from(readable(id, actor));
    }

    public StoredTransferDocument document(Long id, TransferDocumentType documentType, CurrentUser actor) {
        readable(id, actor);
        var file = fileRepository.findFile(id, TYPE, documentType)
                .orElseThrow(() -> new DoubleMajorNotFoundException("제출 서류를 찾을 수 없습니다."));
        return new StoredTransferDocument(file.getDocumentType(), file.getOriginalName(), file.getStoredName(),
                file.getContentType(), file.getSize());
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Optional<DoubleMajorResponseDTO> preflight(DoubleMajorCreateRequestDTO body, String key,
                                                       String hash, CurrentUser actor) {
        validateCreateInput(body, key, actor);
        Student student = studentRepository.findByUserId(actor.id()).orElseThrow(this::studentMissing);
        var replay = idempotency.replay(key, actor.id(), CREATE_ENDPOINT, hash, DoubleMajorPolicy.now(),
                DoubleMajorResponseDTO.class);
        if (replay.isPresent()) return replay;
        resolveCreation(student, body.targetDepartmentId(), false);
        return Optional.empty();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DoubleMajorCreationResult create(DoubleMajorCreateRequestDTO body,
                                            List<StoredTransferDocument> documents,
                                            String key, String hash, CurrentUser actor,
                                            DepartmentTransferAuditContext context) {
        validateCreateInput(body, key, actor);
        Student student = studentRepository.findByUserIdForUpdate(actor.id()).orElseThrow(this::studentMissing);
        var now = DoubleMajorPolicy.now();
        var replay = idempotency.replay(key, actor.id(), CREATE_ENDPOINT, hash, now, DoubleMajorResponseDTO.class);
        if (replay.isPresent()) return new DoubleMajorCreationResult(replay.orElseThrow(), false);
        ResolvedCreation resolved = resolveCreation(student, body.targetDepartmentId(), true);
        Set<TransferDocumentType> requiredTypes = EnumSet.of(
                TransferDocumentType.SELF_INTRODUCTION,
                TransferDocumentType.STUDY_PLAN);
        if (documents == null || documents.size() != requiredTypes.size()
                || !documents.stream().map(StoredTransferDocument::type).collect(java.util.stream.Collectors.toSet())
                .equals(requiredTypes)) {
            throw new InvalidDoubleMajorRequestException("자기소개서·학업계획서 PDF가 모두 필요합니다.");
        }
        var reserved = idempotency.reserve(key, actor.id(), CREATE_ENDPOINT, hash, now);
        try {
            AcademicChangeRequest request = AcademicChangeRequest.createDoubleMajor(student, resolved.targetDepartment(),
                    resolved.period());
            for (StoredTransferDocument document : documents) {
                request.addFile(AcademicChangeRequestFile.create(request, document.type(), document.originalName(),
                        document.storedName(), document.contentType(), document.size()));
            }
            request = repository.saveAndFlush(request);
            audit.record(request.getId(), "ACADEMIC_CHANGE_REQUEST", null, audit.snapshot(request),
                    "DOUBLE_MAJOR_REQUEST_CREATED", "복수전공 신청", actor, context);
            var response = DoubleMajorResponseDTO.from(request);
            idempotency.complete(reserved, response);
            return new DoubleMajorCreationResult(response, true);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateOrRethrow(exception);
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DoubleMajorResponseDTO cancel(Long id, DoubleMajorCancelRequestDTO body, String key,
                                         CurrentUser actor, DepartmentTransferAuditContext context) {
        policy.requireRole(actor, "STUDENT");
        policy.requireId(id);
        String reason = policy.requiredReason(body == null ? null : body.reason(), 500);
        idempotency.validateKey(key);
        Long studentId = repository.findStudentIdByIdAndType(id, TYPE).orElseThrow(this::requestMissing);
        studentRepository.findByIdForUpdate(studentId).orElseThrow(this::studentMissing);
        AcademicChangeRequest request = repository.findByIdAndTypeForUpdate(id, TYPE).orElseThrow(this::requestMissing);
        requireOwner(request, actor);
        String endpoint = "PATCH /api/academic/double-major-requests/" + id + "/cancellation";
        String hash = idempotency.hash(body);
        var now = DoubleMajorPolicy.now();
        var replay = idempotency.replay(key, actor.id(), endpoint, hash, now, DoubleMajorResponseDTO.class);
        if (replay.isPresent()) return replay.orElseThrow();
        policy.requirePending(request);
        var reserved = idempotency.reserve(key, actor.id(), endpoint, hash, now);
        Map<String, Object> before = audit.snapshot(request);
        try {
            request.cancel(request.getStudent().getUser(), reason, now);
        } catch (IllegalStateException exception) {
            throw new DoubleMajorConflictException(exception.getMessage());
        }
        repository.flush();
        audit.record(id, "ACADEMIC_CHANGE_REQUEST", before, audit.snapshot(request),
                "DOUBLE_MAJOR_REQUEST_CANCELLED", "학생 복수전공 신청 취소", actor, context);
        var response = DoubleMajorResponseDTO.from(request);
        idempotency.complete(reserved, response);
        return response;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DoubleMajorResponseDTO review(Long id, DoubleMajorReviewRequestDTO body, String key,
                                         CurrentUser actor, DepartmentTransferAuditContext context) {
        policy.requireRole(actor, "ADMIN");
        policy.requireId(id);
        if (body == null || !body.isValidDecision()) {
            throw new InvalidDoubleMajorRequestException("APPROVED 또는 사유가 있는 REJECTED가 필요합니다.");
        }
        idempotency.validateKey(key);
        Long studentId = repository.findStudentIdByIdAndType(id, TYPE).orElseThrow(this::requestMissing);
        Student student = studentRepository.findByIdForUpdate(studentId).orElseThrow(this::studentMissing);
        AcademicChangeRequest request = repository.findByIdAndTypeForUpdate(id, TYPE).orElseThrow(this::requestMissing);
        String endpoint = "PATCH /api/academic/double-major-requests/" + id + "/review";
        String hash = idempotency.hash(body);
        var now = DoubleMajorPolicy.now();
        var replay = idempotency.replay(key, actor.id(), endpoint, hash, now, DoubleMajorResponseDTO.class);
        if (replay.isPresent()) return replay.orElseThrow();
        policy.requirePending(request);
        User processor = userRepository.findById(actor.id()).orElseThrow(this::userMissing);
        var reserved = idempotency.reserve(key, actor.id(), endpoint, hash, now);
        Map<String, Object> beforeRequest = audit.snapshot(request);
        if (body.status() == AcademicChangeRequestStatus.APPROVED) {
            validateApproval(student, request);
            Map<String, Object> beforeAffiliation = audit.affiliation(student);
            request.approve(processor, now);
            student.assignDoubleMajor(request.getTargetDepartment());
            repository.flush();
            audit.record(student.getId(), "STUDENT_AFFILIATION", beforeAffiliation, audit.affiliation(student),
                    "STUDENT_DOUBLE_MAJOR_ASSIGNED", "관리자 복수전공 승인", actor, context);
        } else {
            request.reject(processor, policy.requiredReason(body.reason(), 500), now);
            repository.flush();
        }
        audit.record(id, "ACADEMIC_CHANGE_REQUEST", beforeRequest, audit.snapshot(request),
                body.status() == AcademicChangeRequestStatus.APPROVED
                        ? "DOUBLE_MAJOR_REQUEST_APPROVED" : "DOUBLE_MAJOR_REQUEST_REJECTED",
                body.status() == AcademicChangeRequestStatus.APPROVED ? "관리자 복수전공 승인" : "관리자 복수전공 반려",
                actor, context);
        var response = DoubleMajorResponseDTO.from(request);
        idempotency.complete(reserved, response);
        return response;
    }

    private ResolvedCreation resolveCreation(Student student, Long targetDepartmentId, boolean lock) {
        policy.requireEnrolled(student.getAcademicStatus());
        if (student.getDoubleMajor() != null) {
            throw new DoubleMajorConflictException("이미 복수전공이 배정된 학생입니다.");
        }
        if (repository.existsByStudentIdAndRequestTypeAndStatus(student.getId(), TYPE,
                AcademicChangeRequestStatus.PENDING)) {
            throw new DoubleMajorConflictException("진행 중인 복수전공 신청이 있습니다.");
        }
        Department targetDepartment = departmentRepository.findByIdWithCollege(targetDepartmentId)
                .orElseThrow(() -> new DoubleMajorNotFoundException("희망 복수전공을 찾을 수 없습니다."));
        validateTarget(student, targetDepartment);
        var now = DoubleMajorPolicy.now();
        var periods = lock ? periodRepository.findAcceptingForUpdate(TYPE, now) : periodRepository.findAccepting(TYPE, now);
        if (periods.isEmpty()) throw new DoubleMajorConflictException("현재는 복수전공 접수 기간이 아닙니다.");
        if (periods.size() > 1) throw new DoubleMajorConflictException("동시에 열린 복수전공 모집 기간이 여러 개입니다.");
        return new ResolvedCreation(targetDepartment, periods.getFirst());
    }

    private void validateApproval(Student student, AcademicChangeRequest request) {
        policy.requireEnrolled(student.getAcademicStatus());
        if (student.getDoubleMajor() != null) throw new DoubleMajorConflictException("이미 복수전공이 배정된 학생입니다.");
        validateTarget(student, request.getTargetDepartment());
    }

    private void validateTarget(Student student, Department targetDepartment) {
        if (!targetDepartment.isActive() || targetDepartment.getCollege() != null
                && !targetDepartment.getCollege().isActive()) {
            throw new DoubleMajorConflictException("활성 학과만 복수전공으로 선택할 수 있습니다.");
        }
        if (student.getDepartment().getId().equals(targetDepartment.getId())) {
            throw new DoubleMajorConflictException("현재 소속과 다른 학과를 선택해야 합니다.");
        }
    }

    private AcademicChangeRequest readable(Long id, CurrentUser actor) {
        policy.requireReader(actor);
        policy.requireId(id);
        AcademicChangeRequest request = queries.findDetail(id).orElseThrow(this::requestMissing);
        if (!actor.isAdmin()) requireOwner(request, actor);
        return request;
    }

    private void requireOwner(AcademicChangeRequest request, CurrentUser actor) {
        if (!request.getStudent().getUser().getId().equals(actor.id())) throw accessDenied();
    }

    private void validateCreateInput(DoubleMajorCreateRequestDTO body, String key, CurrentUser actor) {
        policy.requireRole(actor, "STUDENT");
        policy.validateCreate(body);
        idempotency.validateKey(key);
    }

    private RuntimeException duplicateOrRethrow(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.sql.SQLException sql && sql.getErrorCode() == 1062
                    && sql.getMessage().contains("uk_academic_change_requests_active_type")) {
                return new DoubleMajorConflictException("진행 중인 복수전공 신청이 있습니다.");
            }
        }
        return exception;
    }

    private DoubleMajorNotFoundException requestMissing() { return new DoubleMajorNotFoundException("복수전공 신청을 찾을 수 없습니다."); }
    private DoubleMajorNotFoundException studentMissing() { return new DoubleMajorNotFoundException("학생 정보를 찾을 수 없습니다."); }
    private DoubleMajorNotFoundException userMissing() { return new DoubleMajorNotFoundException("처리자 정보를 찾을 수 없습니다."); }
    private DoubleMajorAccessDeniedException accessDenied() { return new DoubleMajorAccessDeniedException("본인의 복수전공 신청만 접근할 수 있습니다."); }

    private record ResolvedCreation(Department targetDepartment, AcademicChangeRequestPeriod period) { }
}
