package com.msa4lmsv2academic.domain.infochange.service;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.infochange.entity.InfoChangeRequestStatus;
import com.msa4lmsv2academic.domain.infochange.entity.StudentInfoChangeRequest;
import com.msa4lmsv2academic.domain.infochange.entity.StudentInfoChangeRequestFile;
import com.msa4lmsv2academic.domain.infochange.repository.InfoChangeRequestSearchCondition;
import com.msa4lmsv2academic.domain.infochange.repository.StudentInfoChangeRequestFileRepository;
import com.msa4lmsv2academic.domain.infochange.repository.StudentInfoChangeRequestQueryRepository;
import com.msa4lmsv2academic.domain.infochange.repository.StudentInfoChangeRequestRepository;
import com.msa4lmsv2academic.domain.infochange.repository.StudentInfoChangeRequestSearchResult;
import com.msa4lmsv2academic.domain.infochange.request.InfoChangeRequestRejectRequestDTO;
import com.msa4lmsv2academic.domain.infochange.request.InfoChangeRequestSearchRequestDTO;
import com.msa4lmsv2academic.domain.infochange.request.StudentInfoChangeRequestCreateDTO;
import com.msa4lmsv2academic.domain.infochange.response.StudentInfoChangeRequestFileResponseDTO;
import com.msa4lmsv2academic.domain.infochange.response.StudentInfoChangeRequestResponseDTO;
import com.msa4lmsv2academic.domain.outbox.service.OutboxEventService;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.student.repository.StudentRepository;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.repository.UserRepository;
import com.msa4lmsv2academic.global.error.DuplicateInfoChangeRequestException;
import com.msa4lmsv2academic.global.error.DuplicateProfileEmailException;
import com.msa4lmsv2academic.global.error.InfoChangeRequestAccessDeniedException;
import com.msa4lmsv2academic.global.error.InfoChangeRequestNotFoundException;
import com.msa4lmsv2academic.global.error.InfoChangeRequestStateConflictException;
import com.msa4lmsv2academic.global.error.InvalidInfoChangeRequestException;
import com.msa4lmsv2academic.global.error.StudentNotFoundException;
import com.msa4lmsv2academic.global.file.FileStorageService;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentInfoChangeRequestService {

    private static final String PROFILE_IMAGE_PATH_PREFIX = "student-info-change/profile-images";
    private static final String ATTACHMENT_PATH_PREFIX = "student-info-change/attachments";
    private static final String TARGET_TYPE = "STUDENT_PROFILE_CHANGE_REQUEST";
    private static final String AGGREGATE_TYPE_STUDENT = "STUDENT";
    private static final String EVENT_STUDENT_SNAPSHOT_CHANGED = "StudentSnapshotChanged";

    private final StudentInfoChangeRequestRepository requestRepository;
    private final StudentInfoChangeRequestQueryRepository requestQueryRepository;
    private final StudentInfoChangeRequestFileRepository fileRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ProfileFileValidator profileFileValidator;
    private final ProfileChangeValidator profileChangeValidator;
    private final StudentInfoChangePolicy studentInfoChangePolicy;
    private final AuditLogService auditLogService;
    private final OutboxEventService outboxEventService;

    public PageResponseDTO<StudentInfoChangeRequestResponseDTO> search(
            InfoChangeRequestSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        Long requesterUserId = switch (currentUser.role()) {
            case "STUDENT" -> currentUser.id();
            case "ADMIN" -> null;
            default -> throw new InfoChangeRequestAccessDeniedException("학생 프로필 변경 신청 조회 권한이 없습니다.");
        };
        int page = request.resolvedPage();
        int size = request.resolvedSize();
        StudentInfoChangeRequestSearchResult result = requestQueryRepository.search(
                new InfoChangeRequestSearchCondition(
                        request.normalizedKeyword(),
                        request.status(),
                        request.departmentId(),
                        requesterUserId,
                        request.resolvedSortDirection(),
                        (page - 1L) * size,
                        size
                )
        );
        Map<Long, Long> attachmentCounts = attachmentCounts(result.items());
        List<StudentInfoChangeRequestResponseDTO> items = result.items().stream()
                .map(item -> StudentInfoChangeRequestResponseDTO.summary(
                        item,
                        attachmentCounts.getOrDefault(item.getId(), 0L)
                ))
                .toList();
        boolean hasNext = (page - 1L) * size + items.size() < result.totalCount();
        return new PageResponseDTO<>(items, result.totalCount(), page, size, hasNext);
    }

    public StudentInfoChangeRequestResponseDTO get(Long requestId, CurrentUser currentUser) {
        StudentInfoChangeRequest request = requestRepository.findDetailById(requestId)
                .orElseThrow(InfoChangeRequestNotFoundException::new);
        validateReadable(request, currentUser);
        return toDetail(request);
    }

    @Transactional
    public StudentInfoChangeRequestResponseDTO create(
            StudentInfoChangeRequestCreateDTO createDTO,
            CurrentUser currentUser,
            String requestId,
            String ipAddress
    ) {
        validateRole(currentUser, "STUDENT");
        profileFileValidator.validateStudent(createDTO.profileImage(), createDTO.attachments());

        Student student = studentRepository.findByUserId(currentUser.id())
                .orElseThrow(StudentNotFoundException::new);
        studentInfoChangePolicy.requireRequestAllowed(student.getAcademicStatus());
        ProfileChangeValues values = profileChangeValidator.resolve(
                student.getUser(),
                createDTO.newName(),
                createDTO.newPhoneNumber(),
                createDTO.newEmail(),
                createDTO.newAddress(),
                createDTO.profileImage()
        );
        if (requestRepository.existsByStudentIdAndStatus(student.getId(), InfoChangeRequestStatus.REQUESTED)) {
            throw new DuplicateInfoChangeRequestException();
        }

        String newProfileImageKey = values.profileImageChanged()
                ? fileStorageService.upload(PROFILE_IMAGE_PATH_PREFIX, createDTO.profileImage())
                : null;
        StudentInfoChangeRequest request = requestRepository.saveAndFlush(StudentInfoChangeRequest.create(
                student,
                values.name(),
                values.phoneNumber(),
                values.email(),
                values.address(),
                newProfileImageKey,
                createDTO.reason().trim()
        ));
        saveAttachments(request, createDTO.attachments());
        recordAudit(
                currentUser.id(),
                "STUDENT_PROFILE_CHANGE_REQUESTED",
                request,
                null,
                InfoChangeRequestStatus.REQUESTED,
                requestId,
                ipAddress
        );
        return toDetail(request);
    }

    @Transactional
    public StudentInfoChangeRequestResponseDTO approve(
            Long requestId,
            CurrentUser currentUser,
            String traceRequestId,
            String ipAddress
    ) {
        validateRole(currentUser, "ADMIN");
        StudentInfoChangeRequest request = getForUpdate(requestId);
        User reviewer = findReviewer(currentUser.id());
        profileChangeValidator.validateEmailAvailable(request.getStudent().getUser(), request.getNewEmail());

        try {
            request.approve(reviewer, LocalDateTime.now());
            Student student = request.getStudent();
            student.getUser().applyProfileChange(
                    request.getNewName(),
                    request.getNewPhoneNumber(),
                    request.getNewEmail(),
                    request.getNewAddress(),
                    request.getNewProfileImageKey()
            );
            StudentInfoChangeRequest saved = requestRepository.saveAndFlush(request);
            if (request.getNewName() != null) {
                student.bumpSnapshotVersion();
                outboxEventService.record(
                        AGGREGATE_TYPE_STUDENT,
                        student.getId(),
                        EVENT_STUDENT_SNAPSHOT_CHANGED,
                        studentSnapshotPayload(student),
                        student.getSnapshotVersion()
                );
            }
            recordAudit(
                    currentUser.id(),
                    "STUDENT_PROFILE_CHANGE_APPROVED",
                    saved,
                    InfoChangeRequestStatus.REQUESTED,
                    InfoChangeRequestStatus.APPROVED,
                    traceRequestId,
                    ipAddress
            );
            return toDetail(saved);
        } catch (IllegalStateException exception) {
            throw new InfoChangeRequestStateConflictException("처리 대기 상태인 신청만 승인할 수 있습니다.");
        } catch (DataIntegrityViolationException exception) {
            if (request.getNewEmail() != null) {
                throw new DuplicateProfileEmailException();
            }
            throw exception;
        }
    }

    @Transactional
    public StudentInfoChangeRequestResponseDTO reject(
            Long requestId,
            InfoChangeRequestRejectRequestDTO rejectDTO,
            CurrentUser currentUser,
            String traceRequestId,
            String ipAddress
    ) {
        validateRole(currentUser, "ADMIN");
        StudentInfoChangeRequest request = getForUpdate(requestId);
        User reviewer = findReviewer(currentUser.id());
        try {
            request.reject(reviewer, rejectDTO.rejectReason().trim(), LocalDateTime.now());
        } catch (IllegalStateException exception) {
            throw new InfoChangeRequestStateConflictException("처리 대기 상태인 신청만 반려할 수 있습니다.");
        }
        StudentInfoChangeRequest saved = requestRepository.saveAndFlush(request);
        recordAudit(
                currentUser.id(),
                "STUDENT_PROFILE_CHANGE_REJECTED",
                saved,
                InfoChangeRequestStatus.REQUESTED,
                InfoChangeRequestStatus.REJECTED,
                traceRequestId,
                ipAddress
        );
        return toDetail(saved);
    }

    @Transactional
    public StudentInfoChangeRequestResponseDTO cancel(
            Long requestId,
            CurrentUser currentUser,
            String traceRequestId,
            String ipAddress
    ) {
        validateRole(currentUser, "STUDENT");
        StudentInfoChangeRequest request = getForUpdate(requestId);
        if (!request.getStudent().getUser().getId().equals(currentUser.id())) {
            throw new InfoChangeRequestAccessDeniedException("본인의 프로필 변경 신청만 취소할 수 있습니다.");
        }
        try {
            request.cancel(LocalDateTime.now());
        } catch (IllegalStateException exception) {
            throw new InfoChangeRequestStateConflictException("처리 대기 상태인 신청만 취소할 수 있습니다.");
        }
        StudentInfoChangeRequest saved = requestRepository.saveAndFlush(request);
        recordAudit(
                currentUser.id(),
                "STUDENT_PROFILE_CHANGE_CANCELLED",
                saved,
                InfoChangeRequestStatus.REQUESTED,
                InfoChangeRequestStatus.CANCELLED,
                traceRequestId,
                ipAddress
        );
        return toDetail(saved);
    }

    private void saveAttachments(StudentInfoChangeRequest request, List<MultipartFile> attachments) {
        if (attachments == null) {
            return;
        }
        for (MultipartFile attachment : attachments) {
            if (attachment == null || attachment.isEmpty()) {
                continue;
            }
            String objectKey = fileStorageService.upload(ATTACHMENT_PATH_PREFIX, attachment);
            fileRepository.save(StudentInfoChangeRequestFile.create(
                    request,
                    attachment.getOriginalFilename(),
                    objectKey,
                    attachment.getContentType(),
                    attachment.getSize()
            ));
        }
    }

    private StudentInfoChangeRequest getForUpdate(Long requestId) {
        return requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(InfoChangeRequestNotFoundException::new);
    }

    private User findReviewer(Long reviewerId) {
        return userRepository.findById(reviewerId)
                .orElseThrow(() -> new InvalidInfoChangeRequestException("검토자 정보를 찾을 수 없습니다."));
    }

    private StudentInfoChangeRequestResponseDTO toDetail(StudentInfoChangeRequest request) {
        String newProfileImageUrl = request.getNewProfileImageKey() == null
                ? null
                : fileStorageService.presignedDownloadUrl(request.getNewProfileImageKey());
        List<StudentInfoChangeRequestFileResponseDTO> files = fileRepository
                .findByRequestIdOrderByIdAsc(request.getId()).stream()
                .map(file -> StudentInfoChangeRequestFileResponseDTO.from(
                        file, fileStorageService.presignedDownloadUrl(file.getObjectKey())
                ))
                .toList();
        return StudentInfoChangeRequestResponseDTO.detail(request, newProfileImageUrl, files);
    }

    private Map<Long, Long> attachmentCounts(List<StudentInfoChangeRequest> requests) {
        if (requests.isEmpty()) {
            return Map.of();
        }
        List<Long> requestIds = requests.stream().map(StudentInfoChangeRequest::getId).toList();
        Map<Long, Long> counts = new HashMap<>();
        fileRepository.findByRequestIdIn(requestIds).forEach(file ->
                counts.merge(file.getRequest().getId(), 1L, Long::sum)
        );
        return counts;
    }

    private void validateReadable(StudentInfoChangeRequest request, CurrentUser currentUser) {
        boolean readable = switch (currentUser.role()) {
            case "STUDENT" -> request.getStudent().getUser().getId().equals(currentUser.id());
            case "ADMIN" -> true;
            default -> false;
        };
        if (!readable) {
            throw new InfoChangeRequestAccessDeniedException("학생 프로필 변경 신청 조회 권한이 없습니다.");
        }
    }

    private void validateRole(CurrentUser currentUser, String role) {
        if (currentUser == null || currentUser.id() == null || !role.equals(currentUser.role())) {
            throw new InfoChangeRequestAccessDeniedException("학생 프로필 변경 신청 처리 권한이 없습니다.");
        }
    }

    private void recordAudit(
            Long actorId,
            String action,
            StudentInfoChangeRequest request,
            InfoChangeRequestStatus beforeStatus,
            InfoChangeRequestStatus afterStatus,
            String requestId,
            String ipAddress
    ) {
        auditLogService.record(
                actorId,
                action,
                TARGET_TYPE,
                request.getId(),
                beforeStatus == null ? null : auditSnapshot(beforeStatus, request),
                auditSnapshot(afterStatus, request),
                null,
                normalizeNullable(requestId),
                normalizeNullable(ipAddress)
        );
    }

    private Map<String, Object> auditSnapshot(
            InfoChangeRequestStatus status,
            StudentInfoChangeRequest request
    ) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("status", status.name());
        snapshot.put("changedFields", changedFields(request));
        return snapshot;
    }

    private List<String> changedFields(StudentInfoChangeRequest request) {
        List<String> fields = new ArrayList<>();
        if (request.getNewName() != null) fields.add("NAME");
        if (request.getNewPhoneNumber() != null) fields.add("PHONE_NUMBER");
        if (request.getNewEmail() != null) fields.add("EMAIL");
        if (request.getNewAddress() != null) fields.add("ADDRESS");
        if (request.getNewProfileImageKey() != null) fields.add("PROFILE_IMAGE");
        return List.copyOf(fields);
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value;
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
}
