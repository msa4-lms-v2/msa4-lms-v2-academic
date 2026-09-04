package com.msa4lmsv2academic.domain.transfer.service;

import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.organization.repository.DepartmentQueryRepository;
import com.msa4lmsv2academic.domain.outbox.service.OutboxEventService;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.repository.SemesterRepository;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.student.repository.StudentRepository;
import com.msa4lmsv2academic.domain.transfer.entity.*;
import com.msa4lmsv2academic.domain.transfer.repository.*;
import com.msa4lmsv2academic.domain.transfer.request.*;
import com.msa4lmsv2academic.domain.transfer.response.DepartmentTransferResponseDTO;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.repository.UserRepository;
import com.msa4lmsv2academic.global.error.*;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentTransferService {
    private static final AcademicChangeRequestType TYPE = AcademicChangeRequestType.TRANSFER_DEPARTMENT;
    private static final Set<TransferDocumentType> REQUIRED_DOCUMENT_TYPES =
            EnumSet.of(TransferDocumentType.SELF_INTRODUCTION, TransferDocumentType.STUDY_PLAN);
    private static final String CREATE_ENDPOINT = "POST /api/academic/department-transfer-requests";
    private static final String AGGREGATE_TYPE_STUDENT = "STUDENT";
    private static final String EVENT_STUDENT_SNAPSHOT_CHANGED = "StudentSnapshotChanged";
    private final AcademicChangeRequestRepository repository;
    private final AcademicChangeRequestFileRepository fileRepository;
    private final AcademicChangeRequestPeriodRepository periodRepository;
    private final DepartmentTransferQueryRepository queries;
    private final StudentRepository studentRepository;
    private final DepartmentQueryRepository departmentRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;
    private final DepartmentTransferPolicy policy;
    private final DepartmentTransferIdempotencyService idempotency;
    private final DepartmentTransferAuditService audit;
    private final OutboxEventService outboxEventService;

    public PageResponseDTO<DepartmentTransferResponseDTO> search(DepartmentTransferSearchRequestDTO filter,
                                                                 CurrentUser actor, Pageable pageable) {
        policy.requireReader(actor);
        if (!actor.isAdmin() && filter.studentId() != null) {
            Student student = studentRepository.findByUserId(actor.id()).orElseThrow(this::studentMissing);
            if (!student.getId().equals(filter.studentId())) throw accessDenied();
        }
        var result = queries.search(filter, actor.isAdmin() ? null : actor.id(), pageable);
        return new PageResponseDTO<>(result.map(DepartmentTransferResponseDTO::from).getContent(),
                result.getTotalElements(), filter.resolvedPage(), filter.resolvedSize(), result.hasNext());
    }

    public DepartmentTransferResponseDTO get(Long id, CurrentUser actor) {
        return DepartmentTransferResponseDTO.from(readable(id, actor));
    }

    public StoredTransferDocument document(Long id, TransferDocumentType documentType, CurrentUser actor) {
        readable(id, actor);
        var file = fileRepository.findFile(id, TYPE, documentType)
                .orElseThrow(() -> new DepartmentTransferNotFoundException("제출 서류를 찾을 수 없습니다."));
        return new StoredTransferDocument(file.getDocumentType(), file.getOriginalName(), file.getStoredName(),
                file.getContentType(), file.getSize());
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Optional<DepartmentTransferResponseDTO> preflight(DepartmentTransferCreateRequestDTO body, String key,
                                                             String hash, CurrentUser actor) {
        validateCreateInput(body, key, actor);
        Student student = studentRepository.findByUserId(actor.id()).orElseThrow(this::studentMissing);
        var replay = idempotency.replay(key, actor.id(), CREATE_ENDPOINT, hash, DepartmentTransferPolicy.now(),
                DepartmentTransferResponseDTO.class);
        if (replay.isPresent()) return replay;
        resolveCreation(student, body, false);
        return Optional.empty();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DepartmentTransferCreationResult create(DepartmentTransferCreateRequestDTO body,
                                                    java.util.List<StoredTransferDocument> documents,
                                                    String key, String hash, CurrentUser actor,
                                                    DepartmentTransferAuditContext context) {
        validateCreateInput(body, key, actor);
        Student student = studentRepository.findByUserIdForUpdate(actor.id()).orElseThrow(this::studentMissing);
        var now = DepartmentTransferPolicy.now();
        var replay = idempotency.replay(key, actor.id(), CREATE_ENDPOINT, hash, now,
                DepartmentTransferResponseDTO.class);
        if (replay.isPresent()) return new DepartmentTransferCreationResult(replay.orElseThrow(), false);
        ResolvedCreation resolved = resolveCreation(student, body, true);
        if (documents == null || documents.size() != REQUIRED_DOCUMENT_TYPES.size()
                || !documents.stream().map(StoredTransferDocument::type).collect(Collectors.toSet())
                        .equals(REQUIRED_DOCUMENT_TYPES)) {
            throw new InvalidDepartmentTransferRequestException("자기소개서·학업계획서 PDF가 모두 필요합니다.");
        }
        var reserved = idempotency.reserve(key, actor.id(), CREATE_ENDPOINT, hash, now);
        try {
            AcademicChangeRequest request = AcademicChangeRequest.createTransfer(student, resolved.department(),
                    resolved.semester(), resolved.period());
            for (StoredTransferDocument document : documents) {
                request.addFile(AcademicChangeRequestFile.create(request, document.type(), document.originalName(),
                        document.storedName(), document.contentType(), document.size()));
            }
            request = repository.saveAndFlush(request);
            audit.record(request.getId(), "ACADEMIC_CHANGE_REQUEST", null, audit.snapshot(request),
                    "TRANSFER_REQUEST_CREATED", "전과 신청", actor, context);
            var response = DepartmentTransferResponseDTO.from(request);
            idempotency.complete(reserved, response);
            return new DepartmentTransferCreationResult(response, true);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateOrRethrow(exception);
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DepartmentTransferResponseDTO cancel(Long id, DepartmentTransferCancelRequestDTO body, String key,
                                                CurrentUser actor, DepartmentTransferAuditContext context) {
        policy.requireRole(actor, "STUDENT");
        policy.requireId(id);
        String reason = policy.requiredReason(body == null ? null : body.reason(), 500);
        idempotency.validateKey(key);
        Long studentId = repository.findStudentIdByIdAndType(id, TYPE).orElseThrow(this::requestMissing);
        studentRepository.findByIdForUpdate(studentId).orElseThrow(this::studentMissing);
        AcademicChangeRequest request = repository.findByIdAndTypeForUpdate(id, TYPE).orElseThrow(this::requestMissing);
        requireOwner(request, actor);
        String endpoint = "PATCH /api/academic/department-transfer-requests/" + id + "/cancellation";
        String hash = idempotency.hash(body);
        var now = DepartmentTransferPolicy.now();
        var replay = idempotency.replay(key, actor.id(), endpoint, hash, now,
                DepartmentTransferResponseDTO.class);
        if (replay.isPresent()) return replay.orElseThrow();
        policy.requirePending(request);
        var reserved = idempotency.reserve(key, actor.id(), endpoint, hash, now);
        Map<String, Object> before = audit.snapshot(request);
        try {
            request.cancel(request.getStudent().getUser(), reason, now);
        } catch (IllegalStateException exception) {
            throw new DepartmentTransferConflictException(exception.getMessage());
        }
        repository.flush();
        audit.record(id, "ACADEMIC_CHANGE_REQUEST", before, audit.snapshot(request),
                "TRANSFER_REQUEST_CANCELLED", "학생 전과 신청 취소", actor, context);
        var response = DepartmentTransferResponseDTO.from(request);
        idempotency.complete(reserved, response);
        return response;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DepartmentTransferResponseDTO review(Long id, DepartmentTransferReviewRequestDTO body, String key,
                                                CurrentUser actor, DepartmentTransferAuditContext context) {
        policy.requireRole(actor, "ADMIN");
        policy.requireId(id);
        if (body == null || !body.isValidDecision()) {
            throw new InvalidDepartmentTransferRequestException("APPROVED 또는 사유가 있는 REJECTED가 필요합니다.");
        }
        idempotency.validateKey(key);
        Long studentId = repository.findStudentIdByIdAndType(id, TYPE).orElseThrow(this::requestMissing);
        Student student = studentRepository.findByIdForUpdate(studentId).orElseThrow(this::studentMissing);
        AcademicChangeRequest request = repository.findByIdAndTypeForUpdate(id, TYPE).orElseThrow(this::requestMissing);
        String endpoint = "PATCH /api/academic/department-transfer-requests/" + id + "/review";
        String hash = idempotency.hash(body);
        var now = DepartmentTransferPolicy.now();
        var replay = idempotency.replay(key, actor.id(), endpoint, hash, now,
                DepartmentTransferResponseDTO.class);
        if (replay.isPresent()) return replay.orElseThrow();
        policy.requirePending(request);
        User processor = userRepository.findById(actor.id()).orElseThrow(this::userMissing);
        var reserved = idempotency.reserve(key, actor.id(), endpoint, hash, now);
        Map<String, Object> beforeRequest = audit.snapshot(request);
        if (body.status() == AcademicChangeRequestStatus.APPROVED) {
            validateApproval(student, request);
            Map<String, Object> beforeAffiliation = audit.affiliation(student);
            request.approve(processor, now);
            student.changeAffiliation(request.getTargetDepartment());
            student.clearAdvisor();
            student.bumpSnapshotVersion();
            repository.flush();
            outboxEventService.record(
                    AGGREGATE_TYPE_STUDENT,
                    student.getId(),
                    EVENT_STUDENT_SNAPSHOT_CHANGED,
                    studentSnapshotPayload(student),
                    student.getSnapshotVersion()
            );
            audit.record(student.getId(), "STUDENT_AFFILIATION", beforeAffiliation, audit.affiliation(student),
                    "STUDENT_TRANSFER_APPLIED", "관리자 전과 승인", actor, context);
        } else {
            request.reject(processor, policy.requiredReason(body.reason(), 500), now);
            repository.flush();
        }
        audit.record(id, "ACADEMIC_CHANGE_REQUEST", beforeRequest, audit.snapshot(request),
                body.status() == AcademicChangeRequestStatus.APPROVED
                        ? "TRANSFER_REQUEST_APPROVED" : "TRANSFER_REQUEST_REJECTED",
                body.status() == AcademicChangeRequestStatus.APPROVED ? "관리자 전과 승인" : "관리자 전과 반려",
                actor, context);
        var response = DepartmentTransferResponseDTO.from(request);
        idempotency.complete(reserved, response);
        return response;
    }

    private ResolvedCreation resolveCreation(Student student, DepartmentTransferCreateRequestDTO body, boolean lock) {
        policy.requireEnrolled(student.getAcademicStatus());
        if (repository.existsByStudentIdAndRequestTypeAndStatus(student.getId(), TYPE,
                AcademicChangeRequestStatus.PENDING)) {
            throw new DepartmentTransferConflictException("진행 중인 전과 신청이 있습니다.");
        }
        Department targetDepartment = departmentRepository.findByIdWithCollege(body.targetDepartmentId())
                .orElseThrow(() -> new DepartmentTransferNotFoundException("희망 학과를 찾을 수 없습니다."));
        if (!targetDepartment.isActive() || targetDepartment.getCollege() != null
                && !targetDepartment.getCollege().isActive()) {
            throw new DepartmentTransferConflictException("활성 학과만 선택할 수 있습니다.");
        }
        if (student.getDepartment().getId().equals(targetDepartment.getId())) {
            throw new DepartmentTransferConflictException("현재 소속과 다른 학과를 선택해야 합니다.");
        }
        if (student.getDoubleMajor() != null && student.getDoubleMajor().getId().equals(targetDepartment.getId())) {
            throw new DepartmentTransferConflictException("현재 복수전공과 같은 학과로 전과를 신청할 수 없습니다.");
        }
        Semester targetSemester = semesterRepository.findById(body.targetSemesterId())
                .orElseThrow(() -> new DepartmentTransferNotFoundException("적용 희망 학기를 찾을 수 없습니다."));
        var period = lock
                ? periodRepository.findBySemesterAndTypeForUpdate(targetSemester.getId(), TYPE)
                : periodRepository.findBySemesterIdAndRequestType(targetSemester.getId(), TYPE);
        var configured = period.orElseThrow(() -> new DepartmentTransferConflictException("전과 접수 기간이 없습니다."));
        if (!configured.accepts(DepartmentTransferPolicy.now())) {
            throw new DepartmentTransferConflictException("현재는 전과 접수 기간이 아닙니다.");
        }
        return new ResolvedCreation(targetDepartment, targetSemester, configured);
    }

    private Map<String, Object> studentSnapshotPayload(Student student) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("studentId", student.getId());
        payload.put("userId", student.getUser().getId());
        payload.put("displayName", student.getUser().getName());
        payload.put("departmentName", student.getDepartment().getName());
        payload.put("sourceVersion", student.getSnapshotVersion());
        return payload;
    }

    private void validateApproval(Student student, AcademicChangeRequest request) {
        policy.requireEnrolled(student.getAcademicStatus());
        if (!student.getDepartment().getId().equals(request.getSourceDepartment().getId())) {
            throw new DepartmentTransferConflictException("신청 이후 학생 소속이 변경되어 승인할 수 없습니다.");
        }
        if (!request.getTargetDepartment().isActive()) {
            throw new DepartmentTransferConflictException("희망 학과가 비활성화되었습니다.");
        }
        if (student.getDoubleMajor() != null
                && student.getDoubleMajor().getId().equals(request.getTargetDepartment().getId())) {
            throw new DepartmentTransferConflictException("현재 복수전공과 같은 학과로 전과할 수 없습니다.");
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

    private void validateCreateInput(DepartmentTransferCreateRequestDTO body, String key, CurrentUser actor) {
        policy.requireRole(actor, "STUDENT");
        policy.validateCreate(body);
        idempotency.validateKey(key);
    }

    private RuntimeException duplicateOrRethrow(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.sql.SQLException sql && sql.getErrorCode() == 1062
                    && sql.getMessage().contains("uk_academic_change_requests_active_type")) {
                return new DepartmentTransferConflictException("진행 중인 전과 신청이 있습니다.");
            }
        }
        return exception;
    }

    private DepartmentTransferNotFoundException requestMissing() {
        return new DepartmentTransferNotFoundException("전과 신청을 찾을 수 없습니다.");
    }

    private DepartmentTransferNotFoundException studentMissing() {
        return new DepartmentTransferNotFoundException("학생 정보를 찾을 수 없습니다.");
    }

    private DepartmentTransferNotFoundException userMissing() {
        return new DepartmentTransferNotFoundException("처리자 정보를 찾을 수 없습니다.");
    }

    private DepartmentTransferAccessDeniedException accessDenied() {
        return new DepartmentTransferAccessDeniedException("본인의 전과 신청만 접근할 수 있습니다.");
    }

    private record ResolvedCreation(Department department, Semester semester,
                                    AcademicChangeRequestPeriod period) { }
}
