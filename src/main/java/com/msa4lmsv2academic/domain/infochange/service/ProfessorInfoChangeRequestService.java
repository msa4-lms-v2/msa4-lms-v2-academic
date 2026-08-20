package com.msa4lmsv2academic.domain.infochange.service;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.infochange.entity.InfoChangeRequestStatus;
import com.msa4lmsv2academic.domain.infochange.entity.ProfessorInfoChangeRequest;
import com.msa4lmsv2academic.domain.infochange.entity.ProfessorInfoChangeRequestFile;
import com.msa4lmsv2academic.domain.infochange.repository.InfoChangeRequestSearchCondition;
import com.msa4lmsv2academic.domain.infochange.repository.ProfessorInfoChangeRequestFileRepository;
import com.msa4lmsv2academic.domain.infochange.repository.ProfessorInfoChangeRequestQueryRepository;
import com.msa4lmsv2academic.domain.infochange.repository.ProfessorInfoChangeRequestRepository;
import com.msa4lmsv2academic.domain.infochange.repository.ProfessorInfoChangeRequestSearchResult;
import com.msa4lmsv2academic.domain.infochange.request.InfoChangeRequestRejectRequestDTO;
import com.msa4lmsv2academic.domain.infochange.request.InfoChangeRequestSearchRequestDTO;
import com.msa4lmsv2academic.domain.infochange.request.ProfessorInfoChangeRequestCreateDTO;
import com.msa4lmsv2academic.domain.infochange.response.ProfessorInfoChangeRequestFileResponseDTO;
import com.msa4lmsv2academic.domain.infochange.response.ProfessorInfoChangeRequestResponseDTO;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.professor.repository.ProfessorRepository;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.repository.UserRepository;
import com.msa4lmsv2academic.global.error.DuplicateInfoChangeRequestException;
import com.msa4lmsv2academic.global.error.DuplicateProfileEmailException;
import com.msa4lmsv2academic.global.error.InfoChangeRequestAccessDeniedException;
import com.msa4lmsv2academic.global.error.InfoChangeRequestNotFoundException;
import com.msa4lmsv2academic.global.error.InfoChangeRequestStateConflictException;
import com.msa4lmsv2academic.global.error.InvalidInfoChangeRequestException;
import com.msa4lmsv2academic.global.error.ProfessorNotFoundException;
import com.msa4lmsv2academic.global.file.FileStorageService;
import com.msa4lmsv2academic.global.response.PageRes;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
public class ProfessorInfoChangeRequestService {

    private static final String PROFILE_IMAGE_PATH_PREFIX = "professor-info-change/profile-images";
    private static final String ATTACHMENT_PATH_PREFIX = "professor-info-change/attachments";
    private static final String TARGET_TYPE = "PROFESSOR_PROFILE_CHANGE_REQUEST";

    private final ProfessorInfoChangeRequestRepository requestRepository;
    private final ProfessorInfoChangeRequestQueryRepository requestQueryRepository;
    private final ProfessorInfoChangeRequestFileRepository fileRepository;
    private final ProfessorRepository professorRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ProfileFileValidator profileFileValidator;
    private final ProfileChangeValidator profileChangeValidator;
    private final AuditLogService auditLogService;

    public PageRes<ProfessorInfoChangeRequestResponseDTO> search(
            InfoChangeRequestSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        Long requesterUserId = switch (currentUser.role()) {
            case "PROFESSOR" -> currentUser.id();
            case "ADMIN" -> null;
            default -> throw new InfoChangeRequestAccessDeniedException("교수 프로필 변경 신청 조회 권한이 없습니다.");
        };
        int page = request.resolvedPage();
        int size = request.resolvedSize();
        ProfessorInfoChangeRequestSearchResult result = requestQueryRepository.search(
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
        List<ProfessorInfoChangeRequestResponseDTO> items = result.items().stream()
                .map(ProfessorInfoChangeRequestResponseDTO::summary)
                .toList();
        boolean hasNext = (page - 1L) * size + items.size() < result.totalCount();
        return new PageRes<>(items, result.totalCount(), page, size, hasNext);
    }

    public ProfessorInfoChangeRequestResponseDTO get(Long requestId, CurrentUser currentUser) {
        ProfessorInfoChangeRequest request = requestRepository.findDetailById(requestId)
                .orElseThrow(InfoChangeRequestNotFoundException::new);
        validateReadable(request, currentUser);
        return toDetail(request);
    }

    @Transactional
    public ProfessorInfoChangeRequestResponseDTO create(
            ProfessorInfoChangeRequestCreateDTO createDTO,
            CurrentUser currentUser,
            String requestId,
            String ipAddress
    ) {
        validateRole(currentUser, "PROFESSOR");
        profileFileValidator.validate(createDTO.profileImage(), createDTO.attachments());

        Professor professor = professorRepository.findByUserId(currentUser.id())
                .orElseThrow(ProfessorNotFoundException::new);
        ProfileChangeValues values = profileChangeValidator.resolve(
                professor.getUser(),
                createDTO.newName(),
                createDTO.newPhoneNumber(),
                createDTO.newEmail(),
                createDTO.newAddress(),
                createDTO.profileImage()
        );
        if (requestRepository.existsByProfessorIdAndStatus(professor.getId(), InfoChangeRequestStatus.REQUESTED)) {
            throw new DuplicateInfoChangeRequestException();
        }

        String newProfileImageKey = values.profileImageChanged()
                ? fileStorageService.upload(PROFILE_IMAGE_PATH_PREFIX, createDTO.profileImage())
                : null;
        ProfessorInfoChangeRequest request = requestRepository.saveAndFlush(ProfessorInfoChangeRequest.create(
                professor,
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
                "PROFESSOR_PROFILE_CHANGE_REQUESTED",
                request,
                null,
                InfoChangeRequestStatus.REQUESTED,
                requestId,
                ipAddress
        );
        return toDetail(request);
    }

    @Transactional
    public ProfessorInfoChangeRequestResponseDTO approve(
            Long requestId,
            CurrentUser currentUser,
            String traceRequestId,
            String ipAddress
    ) {
        validateRole(currentUser, "ADMIN");
        ProfessorInfoChangeRequest request = getForUpdate(requestId);
        User reviewer = findReviewer(currentUser.id());
        profileChangeValidator.validateEmailAvailable(request.getProfessor().getUser(), request.getNewEmail());

        try {
            request.approve(reviewer, LocalDateTime.now());
            request.getProfessor().getUser().applyProfileChange(
                    request.getNewName(),
                    request.getNewPhoneNumber(),
                    request.getNewEmail(),
                    request.getNewAddress(),
                    request.getNewProfileImageKey()
            );
            ProfessorInfoChangeRequest saved = requestRepository.saveAndFlush(request);
            recordAudit(
                    currentUser.id(),
                    "PROFESSOR_PROFILE_CHANGE_APPROVED",
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
    public ProfessorInfoChangeRequestResponseDTO reject(
            Long requestId,
            InfoChangeRequestRejectRequestDTO rejectDTO,
            CurrentUser currentUser,
            String traceRequestId,
            String ipAddress
    ) {
        validateRole(currentUser, "ADMIN");
        ProfessorInfoChangeRequest request = getForUpdate(requestId);
        User reviewer = findReviewer(currentUser.id());
        try {
            request.reject(reviewer, rejectDTO.rejectReason().trim(), LocalDateTime.now());
        } catch (IllegalStateException exception) {
            throw new InfoChangeRequestStateConflictException("처리 대기 상태인 신청만 반려할 수 있습니다.");
        }
        ProfessorInfoChangeRequest saved = requestRepository.saveAndFlush(request);
        recordAudit(
                currentUser.id(),
                "PROFESSOR_PROFILE_CHANGE_REJECTED",
                saved,
                InfoChangeRequestStatus.REQUESTED,
                InfoChangeRequestStatus.REJECTED,
                traceRequestId,
                ipAddress
        );
        return toDetail(saved);
    }

    @Transactional
    public ProfessorInfoChangeRequestResponseDTO cancel(
            Long requestId,
            CurrentUser currentUser,
            String traceRequestId,
            String ipAddress
    ) {
        validateRole(currentUser, "PROFESSOR");
        ProfessorInfoChangeRequest request = getForUpdate(requestId);
        if (!request.getProfessor().getUser().getId().equals(currentUser.id())) {
            throw new InfoChangeRequestAccessDeniedException("본인의 프로필 변경 신청만 취소할 수 있습니다.");
        }
        try {
            request.cancel(LocalDateTime.now());
        } catch (IllegalStateException exception) {
            throw new InfoChangeRequestStateConflictException("처리 대기 상태인 신청만 취소할 수 있습니다.");
        }
        ProfessorInfoChangeRequest saved = requestRepository.saveAndFlush(request);
        recordAudit(
                currentUser.id(),
                "PROFESSOR_PROFILE_CHANGE_CANCELLED",
                saved,
                InfoChangeRequestStatus.REQUESTED,
                InfoChangeRequestStatus.CANCELLED,
                traceRequestId,
                ipAddress
        );
        return toDetail(saved);
    }

    private void saveAttachments(ProfessorInfoChangeRequest request, List<MultipartFile> attachments) {
        if (attachments == null) {
            return;
        }
        for (MultipartFile attachment : attachments) {
            if (attachment == null || attachment.isEmpty()) {
                continue;
            }
            String objectKey = fileStorageService.upload(ATTACHMENT_PATH_PREFIX, attachment);
            fileRepository.save(ProfessorInfoChangeRequestFile.create(
                    request,
                    attachment.getOriginalFilename(),
                    objectKey,
                    attachment.getContentType(),
                    attachment.getSize()
            ));
        }
    }

    private ProfessorInfoChangeRequest getForUpdate(Long requestId) {
        return requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(InfoChangeRequestNotFoundException::new);
    }

    private User findReviewer(Long reviewerId) {
        return userRepository.findById(reviewerId)
                .orElseThrow(() -> new InvalidInfoChangeRequestException("검토자 정보를 찾을 수 없습니다."));
    }

    private ProfessorInfoChangeRequestResponseDTO toDetail(ProfessorInfoChangeRequest request) {
        String newProfileImageUrl = request.getNewProfileImageKey() == null
                ? null
                : fileStorageService.presignedDownloadUrl(request.getNewProfileImageKey());
        List<ProfessorInfoChangeRequestFileResponseDTO> files = fileRepository
                .findByRequestIdOrderByIdAsc(request.getId()).stream()
                .map(file -> ProfessorInfoChangeRequestFileResponseDTO.from(
                        file, fileStorageService.presignedDownloadUrl(file.getObjectKey())
                ))
                .toList();
        return ProfessorInfoChangeRequestResponseDTO.detail(request, newProfileImageUrl, files);
    }

    private void validateReadable(ProfessorInfoChangeRequest request, CurrentUser currentUser) {
        boolean readable = switch (currentUser.role()) {
            case "PROFESSOR" -> request.getProfessor().getUser().getId().equals(currentUser.id());
            case "ADMIN" -> true;
            default -> false;
        };
        if (!readable) {
            throw new InfoChangeRequestAccessDeniedException("교수 프로필 변경 신청 조회 권한이 없습니다.");
        }
    }

    private void validateRole(CurrentUser currentUser, String role) {
        if (currentUser == null || currentUser.id() == null || !role.equals(currentUser.role())) {
            throw new InfoChangeRequestAccessDeniedException("교수 프로필 변경 신청 처리 권한이 없습니다.");
        }
    }

    private void recordAudit(
            Long actorId,
            String action,
            ProfessorInfoChangeRequest request,
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
            ProfessorInfoChangeRequest request
    ) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("status", status.name());
        snapshot.put("changedFields", changedFields(request));
        return snapshot;
    }

    private List<String> changedFields(ProfessorInfoChangeRequest request) {
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
}
