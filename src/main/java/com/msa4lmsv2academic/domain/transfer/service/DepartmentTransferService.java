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
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
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
    private static final List<AcademicChangeRequestStatus> IN_PROGRESS_STATUSES =
            List.of(AcademicChangeRequestStatus.PENDING, AcademicChangeRequestStatus.ADVISOR_APPROVED);
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
        if ("STUDENT".equals(actor.role()) && filter.studentId() != null) {
            Student student = studentRepository.findByUserId(actor.id()).orElseThrow(this::studentMissing);
            if (!student.getId().equals(filter.studentId())) throw accessDenied();
        }
        var result = queries.search(filter,
                "STUDENT".equals(actor.role()) ? actor.id() : null,
                "PROFESSOR".equals(actor.role()) ? actor.id() : null,
                pageable);
        return new PageResponseDTO<>(result.map(DepartmentTransferResponseDTO::from).getContent(),
                result.getTotalElements(), filter.resolvedPage(), filter.resolvedSize(), result.hasNext());
    }

    public DepartmentTransferResponseDTO get(Long id, CurrentUser actor) {
        return DepartmentTransferResponseDTO.from(readable(id, actor));
    }

    public StoredTransferDocument document(Long id, Long fileId, CurrentUser actor) {
        readable(id, actor);
        policy.requireId(fileId);
        var file = fileRepository.findFile(id, TYPE, fileId)
                .orElseThrow(() -> new DepartmentTransferNotFoundException("제출 서류를 찾을 수 없습니다."));
        return new StoredTransferDocument(file.getOriginalName(), file.getStoredName(),
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
        if (documents == null || documents.size() != 2) {
            throw new InvalidDepartmentTransferRequestException("HWP 또는 HWPX 첨부파일이 정확히 2개 필요합니다.");
        }
        var reserved = idempotency.reserve(key, actor.id(), CREATE_ENDPOINT, hash, now);
        try {
            AcademicChangeRequest request = AcademicChangeRequest.createTransfer(student, resolved.department(),
                    resolved.semester(), resolved.period());
            for (StoredTransferDocument document : documents) {
                request.addFile(AcademicChangeRequestFile.create(request, document.originalName(),
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
        policy.requireInProgress(request);
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
    public DepartmentTransferResponseDTO reviewByAdvisor(Long id, AdvisorDepartmentTransferReviewRequestDTO body,
                                                         String key, CurrentUser actor,
                                                         DepartmentTransferAuditContext context) {
        policy.requireRole(actor, "PROFESSOR");
        policy.requireId(id);
        if (body == null || !body.isValidDecision()) {
            throw new InvalidDepartmentTransferRequestException("승인 여부와 반려 시 사유가 필요합니다.");
        }
        idempotency.validateKey(key);
        Long studentId = repository.findStudentIdByIdAndType(id, TYPE).orElseThrow(this::requestMissing);
        Student student = studentRepository.findByIdForUpdate(studentId).orElseThrow(this::studentMissing);
        AcademicChangeRequest request = repository.findByIdAndTypeForUpdate(id, TYPE).orElseThrow(this::requestMissing);
        if (student.getAdvisor() == null || !student.getAdvisor().getUser().getId().equals(actor.id())) {
            throw accessDenied();
        }
        String endpoint = "PATCH /api/academic/department-transfer-requests/" + id + "/advisor-review";
        String hash = idempotency.hash(body);
        var now = DepartmentTransferPolicy.now();
        var replay = idempotency.replay(key, actor.id(), endpoint, hash, now,
                DepartmentTransferResponseDTO.class);
        if (replay.isPresent()) return replay.orElseThrow();
        policy.requirePending(request);
        User reviewer = userRepository.findById(actor.id()).orElseThrow(this::userMissing);
        var reserved = idempotency.reserve(key, actor.id(), endpoint, hash, now);
        Map<String, Object> beforeRequest = audit.snapshot(request);
        if (Boolean.TRUE.equals(body.approved())) {
            request.advisorApprove(reviewer, now);
        } else {
            request.advisorReject(reviewer, policy.requiredReason(body.rejectReason(), 500), now);
        }
        repository.flush();
        audit.record(id, "ACADEMIC_CHANGE_REQUEST", beforeRequest, audit.snapshot(request),
                Boolean.TRUE.equals(body.approved())
                        ? "TRANSFER_REQUEST_ADVISOR_APPROVED" : "TRANSFER_REQUEST_ADVISOR_REJECTED",
                Boolean.TRUE.equals(body.approved()) ? "지도교수 전과 승인" : "지도교수 전과 반려",
                actor, context);
        var response = DepartmentTransferResponseDTO.from(request);
        idempotency.complete(reserved, response);
        return response;
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Optional<DepartmentTransferResponseDTO> preflightApplication(Long id, String key, String hash,
                                                                        CurrentUser actor) {
        policy.requireRole(actor, "ADMIN");
        policy.requireId(id);
        idempotency.validateKey(key);
        String endpoint = "PATCH /api/academic/department-transfer-requests/" + id + "/application";
        var replay = idempotency.replay(key, actor.id(), endpoint, hash, DepartmentTransferPolicy.now(),
                DepartmentTransferResponseDTO.class);
        if (replay.isPresent()) {
            return replay;
        }
        AcademicChangeRequest request = queries.findDetail(id).orElseThrow(this::requestMissing);
        policy.requireAdvisorApproved(request);
        validateApproval(request.getStudent(), request);
        return Optional.empty();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AcademicChangeApplicationResult<DepartmentTransferResponseDTO> apply(
            Long id, List<StoredTransferDocument> documents, String key, String hash, CurrentUser actor,
            DepartmentTransferAuditContext context) {
        policy.requireRole(actor, "ADMIN");
        policy.requireId(id);
        idempotency.validateKey(key);
        Long studentId = repository.findStudentIdByIdAndType(id, TYPE).orElseThrow(this::requestMissing);
        Student student = studentRepository.findByIdForUpdate(studentId).orElseThrow(this::studentMissing);
        AcademicChangeRequest request = repository.findByIdAndTypeForUpdate(id, TYPE).orElseThrow(this::requestMissing);
        String endpoint = "PATCH /api/academic/department-transfer-requests/" + id + "/application";
        var now = DepartmentTransferPolicy.now();
        var replay = idempotency.replay(key, actor.id(), endpoint, hash, now,
                DepartmentTransferResponseDTO.class);
        if (replay.isPresent()) {
            return new AcademicChangeApplicationResult<>(replay.orElseThrow(), false, List.of());
        }
        policy.requireAdvisorApproved(request);
        if (documents == null || documents.size() != 2) {
            throw new InvalidDepartmentTransferRequestException("학장 날인이 포함된 HWP/HWPX 파일 2개가 필요합니다.");
        }
        validateApproval(student, request);
        User processor = userRepository.findById(actor.id()).orElseThrow(this::userMissing);
        var reserved = idempotency.reserve(key, actor.id(), endpoint, hash, now);
        Map<String, Object> beforeRequest = audit.snapshot(request);
        List<String> replacedStoredNames = replaceDocuments(request, documents);
        Map<String, Object> beforeAffiliation = audit.affiliation(student);
        request.apply(processor, now);
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
                "STUDENT_TRANSFER_APPLIED", "학장 날인 확인 후 관리자 전과 학적 반영", actor, context);
        audit.record(id, "ACADEMIC_CHANGE_REQUEST", beforeRequest, audit.snapshot(request),
                "TRANSFER_REQUEST_APPLIED", "학장 날인 문서 확인 및 전과 학적 반영", actor, context);
        var response = DepartmentTransferResponseDTO.from(request);
        idempotency.complete(reserved, response);
        return new AcademicChangeApplicationResult<>(response, true, replacedStoredNames);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DepartmentTransferResponseDTO rejectByAdmin(Long id, AdminAcademicChangeRejectionRequestDTO body,
                                                       String key, CurrentUser actor,
                                                       DepartmentTransferAuditContext context) {
        policy.requireRole(actor, "ADMIN");
        policy.requireId(id);
        String reason = policy.requiredReason(body == null ? null : body.rejectReason(), 500);
        idempotency.validateKey(key);
        Long studentId = repository.findStudentIdByIdAndType(id, TYPE).orElseThrow(this::requestMissing);
        studentRepository.findByIdForUpdate(studentId).orElseThrow(this::studentMissing);
        AcademicChangeRequest request = repository.findByIdAndTypeForUpdate(id, TYPE).orElseThrow(this::requestMissing);
        String endpoint = "PATCH /api/academic/department-transfer-requests/" + id + "/rejection";
        String hash = idempotency.hash(body);
        var now = DepartmentTransferPolicy.now();
        var replay = idempotency.replay(key, actor.id(), endpoint, hash, now,
                DepartmentTransferResponseDTO.class);
        if (replay.isPresent()) {
            return replay.orElseThrow();
        }
        policy.requireAdvisorApproved(request);
        User processor = userRepository.findById(actor.id()).orElseThrow(this::userMissing);
        var reserved = idempotency.reserve(key, actor.id(), endpoint, hash, now);
        Map<String, Object> beforeRequest = audit.snapshot(request);
        request.finalReject(processor, reason, now);
        repository.flush();
        audit.record(id, "ACADEMIC_CHANGE_REQUEST", beforeRequest, audit.snapshot(request),
                "TRANSFER_REQUEST_REJECTED", "학장 날인 누락 확인 후 관리자 전과 반려", actor, context);
        var response = DepartmentTransferResponseDTO.from(request);
        idempotency.complete(reserved, response);
        return response;
    }

    private List<String> replaceDocuments(AcademicChangeRequest request, List<StoredTransferDocument> documents) {
        List<AcademicChangeRequestFile> currentFiles = request.getFiles().stream()
                .sorted(Comparator.comparing(AcademicChangeRequestFile::getId))
                .toList();
        if (currentFiles.size() != 2) {
            throw new DepartmentTransferConflictException("교체할 기존 전과 서류 2개를 찾을 수 없습니다.");
        }
        List<String> replacedStoredNames = currentFiles.stream()
                .map(AcademicChangeRequestFile::getStoredName)
                .toList();
        for (int index = 0; index < currentFiles.size(); index++) {
            StoredTransferDocument document = documents.get(index);
            currentFiles.get(index).replace(document.originalName(), document.storedName(),
                    document.contentType(), document.size());
        }
        return replacedStoredNames;
    }

    private ResolvedCreation resolveCreation(Student student, DepartmentTransferCreateRequestDTO body, boolean lock) {
        policy.requireEnrolled(student.getAcademicStatus());
        if (student.getGradeLevel() >= 3) {
            throw new DepartmentTransferConflictException("3학년 1학기 전 학생만 전과를 신청할 수 있습니다.");
        }
        if (!queries.hasCompletedSemesterEnrollment(student.getId(), DepartmentTransferPolicy.today())) {
            throw new DepartmentTransferConflictException("1학년 1학기 이상 이수한 학생만 전과를 신청할 수 있습니다.");
        }
        if (repository.existsByStudentIdAndRequestTypeAndStatusIn(student.getId(), TYPE,
                List.of(AcademicChangeRequestStatus.APPROVED, AcademicChangeRequestStatus.APPLIED))) {
            throw new DepartmentTransferConflictException("재학 중 전과는 1회만 승인받을 수 있습니다.");
        }
        if (repository.existsByStudentIdAndStatusIn(student.getId(), IN_PROGRESS_STATUSES)) {
            throw new DepartmentTransferConflictException("진행 중인 전과 또는 복수전공 신청이 있습니다.");
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
        if (repository.existsByStudentIdAndRequestTypeAndStatusIn(student.getId(), TYPE,
                List.of(AcademicChangeRequestStatus.APPROVED, AcademicChangeRequestStatus.APPLIED))) {
            throw new DepartmentTransferConflictException("재학 중 이미 승인된 전과 이력이 있습니다.");
        }
    }

    private AcademicChangeRequest readable(Long id, CurrentUser actor) {
        policy.requireReader(actor);
        policy.requireId(id);
        AcademicChangeRequest request = queries.findDetail(id).orElseThrow(this::requestMissing);
        if ("STUDENT".equals(actor.role())) {
            requireOwner(request, actor);
        } else if ("PROFESSOR".equals(actor.role())) {
            if (request.getStudent().getAdvisor() == null
                    || !request.getStudent().getAdvisor().getUser().getId().equals(actor.id())) {
                throw accessDenied();
            }
        }
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
