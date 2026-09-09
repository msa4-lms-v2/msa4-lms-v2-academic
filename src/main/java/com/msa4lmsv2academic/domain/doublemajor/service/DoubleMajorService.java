package com.msa4lmsv2academic.domain.doublemajor.service;

import com.msa4lmsv2academic.domain.doublemajor.repository.DoubleMajorQueryRepository;
import com.msa4lmsv2academic.domain.doublemajor.request.*;
import com.msa4lmsv2academic.domain.doublemajor.response.DoubleMajorResponseDTO;
import com.msa4lmsv2academic.domain.graduation.repository.GraduationCreditQueryRepository;
import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.organization.repository.DepartmentQueryRepository;
import com.msa4lmsv2academic.domain.outbox.service.OutboxEventService;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.student.repository.StudentRepository;
import com.msa4lmsv2academic.domain.transfer.entity.*;
import com.msa4lmsv2academic.domain.transfer.repository.*;
import com.msa4lmsv2academic.domain.transfer.request.AdminAcademicChangeRejectionRequestDTO;
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
    private static final List<AcademicChangeRequestStatus> IN_PROGRESS_STATUSES =
            List.of(AcademicChangeRequestStatus.PENDING, AcademicChangeRequestStatus.ADVISOR_APPROVED);
    private static final long REQUIRED_COMPLETED_SEMESTERS = 2;
    private static final int REQUIRED_EARNED_CREDITS = 33;
    private static final String CREATE_ENDPOINT = "POST /api/academic/double-major-requests";
    private static final String AGGREGATE_TYPE_STUDENT = "STUDENT";
    private static final String EVENT_STUDENT_SNAPSHOT_CHANGED = "StudentSnapshotChanged";
    private final AcademicChangeRequestRepository repository;
    private final AcademicChangeRequestFileRepository fileRepository;
    private final AcademicChangeRequestPeriodRepository periodRepository;
    private final DoubleMajorQueryRepository queries;
    private final GraduationCreditQueryRepository graduationCreditRepository;
    private final StudentRepository studentRepository;
    private final DepartmentQueryRepository departmentRepository;
    private final UserRepository userRepository;
    private final DoubleMajorPolicy policy;
    private final DepartmentTransferIdempotencyService idempotency;
    private final DepartmentTransferAuditService audit;
    private final OutboxEventService outboxEventService;

    public PageResponseDTO<DoubleMajorResponseDTO> search(DoubleMajorSearchRequestDTO filter,
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
        return new PageResponseDTO<>(result.map(DoubleMajorResponseDTO::from).getContent(),
                result.getTotalElements(), filter.resolvedPage(), filter.resolvedSize(), result.hasNext());
    }

    public DoubleMajorResponseDTO get(Long id, CurrentUser actor) {
        return DoubleMajorResponseDTO.from(readable(id, actor));
    }

    public StoredTransferDocument document(Long id, Long fileId, CurrentUser actor) {
        readable(id, actor);
        policy.requireId(fileId);
        var file = fileRepository.findFile(id, TYPE, fileId)
                .orElseThrow(() -> new DoubleMajorNotFoundException("제출 서류를 찾을 수 없습니다."));
        return new StoredTransferDocument(file.getOriginalName(), file.getStoredName(),
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
        if (documents == null || documents.size() != 2) {
            throw new InvalidDoubleMajorRequestException("HWP/HWPX 제출 서류 2개가 필요합니다.");
        }
        var reserved = idempotency.reserve(key, actor.id(), CREATE_ENDPOINT, hash, now);
        try {
            AcademicChangeRequest request = AcademicChangeRequest.createDoubleMajor(student, resolved.targetDepartment(),
                    resolved.period());
            for (StoredTransferDocument document : documents) {
                request.addFile(AcademicChangeRequestFile.create(request, document.originalName(),
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
    public DoubleMajorResponseDTO reviewByAdvisor(Long id, AdvisorDoubleMajorReviewRequestDTO body, String key,
                                                  CurrentUser actor, DepartmentTransferAuditContext context) {
        policy.requireRole(actor, "PROFESSOR");
        policy.requireId(id);
        if (body == null || !body.isValidDecision()) {
            throw new InvalidDoubleMajorRequestException("승인 여부와 반려 시 사유가 필요합니다.");
        }
        idempotency.validateKey(key);
        Long studentId = repository.findStudentIdByIdAndType(id, TYPE).orElseThrow(this::requestMissing);
        Student student = studentRepository.findByIdForUpdate(studentId).orElseThrow(this::studentMissing);
        AcademicChangeRequest request = repository.findByIdAndTypeForUpdate(id, TYPE).orElseThrow(this::requestMissing);
        if (student.getAdvisor() == null || !student.getAdvisor().getUser().getId().equals(actor.id())) {
            throw accessDenied();
        }
        String endpoint = "PATCH /api/academic/double-major-requests/" + id + "/advisor-review";
        String hash = idempotency.hash(body);
        var now = DoubleMajorPolicy.now();
        var replay = idempotency.replay(key, actor.id(), endpoint, hash, now, DoubleMajorResponseDTO.class);
        if (replay.isPresent()) {
            return replay.orElseThrow();
        }
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
                        ? "DOUBLE_MAJOR_REQUEST_ADVISOR_APPROVED" : "DOUBLE_MAJOR_REQUEST_ADVISOR_REJECTED",
                Boolean.TRUE.equals(body.approved()) ? "지도교수 복수전공 승인" : "지도교수 복수전공 반려",
                actor, context);
        var response = DoubleMajorResponseDTO.from(request);
        idempotency.complete(reserved, response);
        return response;
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Optional<DoubleMajorResponseDTO> preflightApplication(Long id, String key, String hash,
                                                                 CurrentUser actor) {
        policy.requireRole(actor, "ADMIN");
        policy.requireId(id);
        idempotency.validateKey(key);
        String endpoint = "PATCH /api/academic/double-major-requests/" + id + "/application";
        var replay = idempotency.replay(key, actor.id(), endpoint, hash, DoubleMajorPolicy.now(),
                DoubleMajorResponseDTO.class);
        if (replay.isPresent()) {
            return replay;
        }
        AcademicChangeRequest request = queries.findDetail(id).orElseThrow(this::requestMissing);
        policy.requireAdvisorApproved(request);
        validateApproval(request.getStudent(), request);
        return Optional.empty();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AcademicChangeApplicationResult<DoubleMajorResponseDTO> apply(
            Long id, List<StoredTransferDocument> documents, String key, String hash, CurrentUser actor,
            DepartmentTransferAuditContext context) {
        policy.requireRole(actor, "ADMIN");
        policy.requireId(id);
        idempotency.validateKey(key);
        Long studentId = repository.findStudentIdByIdAndType(id, TYPE).orElseThrow(this::requestMissing);
        Student student = studentRepository.findByIdForUpdate(studentId).orElseThrow(this::studentMissing);
        AcademicChangeRequest request = repository.findByIdAndTypeForUpdate(id, TYPE).orElseThrow(this::requestMissing);
        String endpoint = "PATCH /api/academic/double-major-requests/" + id + "/application";
        var now = DoubleMajorPolicy.now();
        var replay = idempotency.replay(key, actor.id(), endpoint, hash, now, DoubleMajorResponseDTO.class);
        if (replay.isPresent()) {
            return new AcademicChangeApplicationResult<>(replay.orElseThrow(), false, List.of());
        }
        policy.requireAdvisorApproved(request);
        if (documents == null || documents.size() != 2) {
            throw new InvalidDoubleMajorRequestException("학장 날인이 포함된 HWP/HWPX 파일 2개가 필요합니다.");
        }
        validateApproval(student, request);
        User processor = userRepository.findById(actor.id()).orElseThrow(this::userMissing);
        var reserved = idempotency.reserve(key, actor.id(), endpoint, hash, now);
        Map<String, Object> beforeRequest = audit.snapshot(request);
        List<String> replacedStoredNames = replaceDocuments(request, documents);
        Map<String, Object> beforeAffiliation = audit.affiliation(student);
        request.apply(processor, now);
        student.assignDoubleMajor(request.getTargetDepartment());
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
                "STUDENT_DOUBLE_MAJOR_ASSIGNED", "학장 날인 확인 후 관리자 복수전공 학적 반영", actor, context);
        audit.record(id, "ACADEMIC_CHANGE_REQUEST", beforeRequest, audit.snapshot(request),
                "DOUBLE_MAJOR_REQUEST_APPLIED", "학장 날인 문서 확인 및 복수전공 학적 반영", actor, context);
        var response = DoubleMajorResponseDTO.from(request);
        idempotency.complete(reserved, response);
        return new AcademicChangeApplicationResult<>(response, true, replacedStoredNames);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DoubleMajorResponseDTO rejectByAdmin(Long id, AdminAcademicChangeRejectionRequestDTO body,
                                                String key, CurrentUser actor,
                                                DepartmentTransferAuditContext context) {
        policy.requireRole(actor, "ADMIN");
        policy.requireId(id);
        String reason = policy.requiredReason(body == null ? null : body.rejectReason(), 500);
        idempotency.validateKey(key);
        Long studentId = repository.findStudentIdByIdAndType(id, TYPE).orElseThrow(this::requestMissing);
        studentRepository.findByIdForUpdate(studentId).orElseThrow(this::studentMissing);
        AcademicChangeRequest request = repository.findByIdAndTypeForUpdate(id, TYPE).orElseThrow(this::requestMissing);
        String endpoint = "PATCH /api/academic/double-major-requests/" + id + "/rejection";
        String hash = idempotency.hash(body);
        var now = DoubleMajorPolicy.now();
        var replay = idempotency.replay(key, actor.id(), endpoint, hash, now, DoubleMajorResponseDTO.class);
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
                "DOUBLE_MAJOR_REQUEST_REJECTED", "학장 날인 누락 확인 후 관리자 복수전공 반려", actor, context);
        var response = DoubleMajorResponseDTO.from(request);
        idempotency.complete(reserved, response);
        return response;
    }

    private List<String> replaceDocuments(AcademicChangeRequest request, List<StoredTransferDocument> documents) {
        List<AcademicChangeRequestFile> currentFiles = request.getFiles().stream()
                .sorted(Comparator.comparing(AcademicChangeRequestFile::getId))
                .toList();
        if (currentFiles.size() != 2) {
            throw new DoubleMajorConflictException("교체할 기존 복수전공 서류 2개를 찾을 수 없습니다.");
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

    private ResolvedCreation resolveCreation(Student student, Long targetDepartmentId, boolean lock) {
        policy.requireEnrolled(student.getAcademicStatus());
        if (student.getDoubleMajor() != null) {
            throw new DoubleMajorConflictException("이미 복수전공이 배정된 학생입니다.");
        }
        if (repository.existsByStudentIdAndStatusIn(student.getId(), IN_PROGRESS_STATUSES)) {
            throw new DoubleMajorConflictException("진행 중인 전과 또는 복수전공 신청이 있습니다.");
        }
        if (queries.countCompletedRegularSemesters(student.getId(), DoubleMajorPolicy.today())
                < REQUIRED_COMPLETED_SEMESTERS) {
            throw new DoubleMajorConflictException("정규학기를 2개 이상 이수한 학생만 복수전공을 신청할 수 있습니다.");
        }
        if (graduationCreditRepository.sumTotalCreditsByStudentId(student.getId()) < REQUIRED_EARNED_CREDITS) {
            throw new DoubleMajorConflictException("33학점 이상 취득한 학생만 복수전공을 신청할 수 있습니다.");
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
        if (student.getDoubleMajor() != null) throw new DoubleMajorConflictException("이미 복수전공이 배정된 학생입니다.");
        if (queries.countCompletedRegularSemesters(student.getId(), DoubleMajorPolicy.today())
                < REQUIRED_COMPLETED_SEMESTERS
                || graduationCreditRepository.sumTotalCreditsByStudentId(student.getId()) < REQUIRED_EARNED_CREDITS) {
            throw new DoubleMajorConflictException("복수전공 학적 반영 시점의 이수학기·취득학점 요건을 충족하지 않습니다.");
        }
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
