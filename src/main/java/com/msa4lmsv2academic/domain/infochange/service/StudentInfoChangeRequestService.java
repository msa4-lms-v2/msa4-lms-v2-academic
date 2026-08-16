package com.msa4lmsv2academic.domain.infochange.service;

import com.msa4lmsv2academic.domain.infochange.entity.InfoChangeRequestStatus;
import com.msa4lmsv2academic.domain.infochange.entity.StudentInfoChangeRequest;
import com.msa4lmsv2academic.domain.infochange.entity.StudentInfoChangeRequestFile;
import com.msa4lmsv2academic.domain.infochange.repository.StudentInfoChangeRequestFileRepository;
import com.msa4lmsv2academic.domain.infochange.repository.StudentInfoChangeRequestRepository;
import com.msa4lmsv2academic.domain.infochange.request.InfoChangeRequestRejectRequestDTO;
import com.msa4lmsv2academic.domain.infochange.request.InfoChangeRequestSearchRequestDTO;
import com.msa4lmsv2academic.domain.infochange.request.StudentInfoChangeRequestCreateDTO;
import com.msa4lmsv2academic.domain.infochange.response.StudentInfoChangeRequestFileResponseDTO;
import com.msa4lmsv2academic.domain.infochange.response.StudentInfoChangeRequestResponseDTO;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.student.repository.StudentRepository;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.repository.UserRepository;
import com.msa4lmsv2academic.global.error.DuplicateInfoChangeRequestException;
import com.msa4lmsv2academic.global.error.InfoChangeRequestAccessDeniedException;
import com.msa4lmsv2academic.global.error.InfoChangeRequestNotFoundException;
import com.msa4lmsv2academic.global.error.InvalidInfoChangeRequestException;
import com.msa4lmsv2academic.global.error.StudentNotFoundException;
import com.msa4lmsv2academic.global.file.FileStorageService;
import com.msa4lmsv2academic.global.response.PageRes;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentInfoChangeRequestService {

    private static final String PROFILE_IMAGE_PATH_PREFIX = "student-info-change/profile-images";
    private static final String ATTACHMENT_PATH_PREFIX = "student-info-change/attachments";

    private final StudentInfoChangeRequestRepository requestRepository;
    private final StudentInfoChangeRequestFileRepository fileRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public PageRes<StudentInfoChangeRequestResponseDTO> search(
            InfoChangeRequestSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        int page = request.resolvedPage();
        int size = request.resolvedSize();
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<StudentInfoChangeRequest> result = switch (currentUser.role()) {
            case "STUDENT" -> requestRepository.findByStudentUserId(currentUser.id(), pageable);
            case "ADMIN" -> requestRepository.findAll(pageable);
            default -> throw new InfoChangeRequestAccessDeniedException("학적 정보 변경 신청 조회 권한이 없습니다.");
        };
        List<StudentInfoChangeRequestResponseDTO> items = result.getContent().stream()
                .map(StudentInfoChangeRequestResponseDTO::summary)
                .toList();
        return new PageRes<>(items, result.getTotalElements(), page, size, result.hasNext());
    }

    public StudentInfoChangeRequestResponseDTO get(Long requestId, CurrentUser currentUser) {
        StudentInfoChangeRequest request = requestRepository.findDetailById(requestId)
                .orElseThrow(InfoChangeRequestNotFoundException::new);
        validateReadable(request, currentUser);
        return toDetail(request);
    }

    @Transactional
    public StudentInfoChangeRequestResponseDTO create(StudentInfoChangeRequestCreateDTO createDTO, CurrentUser currentUser) {
        validateRole(currentUser, "STUDENT");
        validateHasAnyChange(createDTO);

        Student student = studentRepository.findByUserId(currentUser.id())
                .orElseThrow(StudentNotFoundException::new);
        if (requestRepository.existsByStudentIdAndStatus(student.getId(), InfoChangeRequestStatus.REQUESTED)) {
            throw new DuplicateInfoChangeRequestException();
        }

        String newProfileImageKey = hasContent(createDTO.profileImage())
                ? fileStorageService.upload(PROFILE_IMAGE_PATH_PREFIX, createDTO.profileImage())
                : null;

        StudentInfoChangeRequest request = StudentInfoChangeRequest.create(
                student,
                StringUtils.hasText(createDTO.newName()) ? createDTO.newName().trim() : null,
                StringUtils.hasText(createDTO.newPhoneNumber()) ? createDTO.newPhoneNumber().trim() : null,
                StringUtils.hasText(createDTO.newEmail()) ? createDTO.newEmail().trim() : null,
                StringUtils.hasText(createDTO.newAddress()) ? createDTO.newAddress().trim() : null,
                newProfileImageKey,
                createDTO.reason().trim()
        );
        requestRepository.saveAndFlush(request);

        if (createDTO.attachments() != null) {
            for (MultipartFile attachment : createDTO.attachments()) {
                if (!hasContent(attachment)) {
                    continue;
                }
                String objectKey = fileStorageService.upload(ATTACHMENT_PATH_PREFIX, attachment);
                fileRepository.save(StudentInfoChangeRequestFile.create(
                        request, attachment.getOriginalFilename(), objectKey
                ));
            }
        }

        return toDetail(request);
    }

    @Transactional
    public StudentInfoChangeRequestResponseDTO approve(Long requestId, CurrentUser currentUser) {
        validateRole(currentUser, "ADMIN");
        StudentInfoChangeRequest request = getForUpdate(requestId);
        User reviewer = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new InvalidInfoChangeRequestException("검토자 정보를 찾을 수 없습니다."));
        try {
            request.approve(reviewer, LocalDateTime.now());
        } catch (IllegalStateException exception) {
            throw new InvalidInfoChangeRequestException("처리 대기 상태인 신청만 승인할 수 있습니다.");
        }
        request.getStudent().getUser().applyProfileChange(
                request.getNewName(),
                request.getNewPhoneNumber(),
                request.getNewEmail(),
                request.getNewAddress(),
                request.getNewProfileImageKey()
        );
        return toDetail(requestRepository.saveAndFlush(request));
    }

    @Transactional
    public StudentInfoChangeRequestResponseDTO reject(
            Long requestId,
            InfoChangeRequestRejectRequestDTO rejectDTO,
            CurrentUser currentUser
    ) {
        validateRole(currentUser, "ADMIN");
        StudentInfoChangeRequest request = getForUpdate(requestId);
        User reviewer = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new InvalidInfoChangeRequestException("검토자 정보를 찾을 수 없습니다."));
        try {
            request.reject(reviewer, rejectDTO.rejectReason().trim(), LocalDateTime.now());
        } catch (IllegalStateException exception) {
            throw new InvalidInfoChangeRequestException("처리 대기 상태인 신청만 반려할 수 있습니다.");
        }
        return toDetail(requestRepository.saveAndFlush(request));
    }

    private StudentInfoChangeRequest getForUpdate(Long requestId) {
        return requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(InfoChangeRequestNotFoundException::new);
    }

    private StudentInfoChangeRequestResponseDTO toDetail(StudentInfoChangeRequest request) {
        String newProfileImageUrl = request.getNewProfileImageKey() == null
                ? null
                : fileStorageService.presignedDownloadUrl(request.getNewProfileImageKey());
        List<StudentInfoChangeRequestFileResponseDTO> files = fileRepository.findByRequestId(request.getId()).stream()
                .map(file -> StudentInfoChangeRequestFileResponseDTO.from(
                        file, fileStorageService.presignedDownloadUrl(file.getObjectKey())
                ))
                .toList();
        return StudentInfoChangeRequestResponseDTO.detail(request, newProfileImageUrl, files);
    }

    private void validateReadable(StudentInfoChangeRequest request, CurrentUser currentUser) {
        boolean readable = switch (currentUser.role()) {
            case "STUDENT" -> request.getStudent().getUser().getId().equals(currentUser.id());
            case "ADMIN" -> true;
            default -> false;
        };
        if (!readable) {
            throw new InfoChangeRequestAccessDeniedException("학적 정보 변경 신청 조회 권한이 없습니다.");
        }
    }

    private void validateRole(CurrentUser currentUser, String role) {
        if (currentUser == null || currentUser.id() == null || !role.equals(currentUser.role())) {
            throw new InfoChangeRequestAccessDeniedException("학적 정보 변경 신청 처리 권한이 없습니다.");
        }
    }

    private void validateHasAnyChange(StudentInfoChangeRequestCreateDTO createDTO) {
        boolean hasAnyChange = StringUtils.hasText(createDTO.newName())
                || StringUtils.hasText(createDTO.newPhoneNumber())
                || StringUtils.hasText(createDTO.newEmail())
                || StringUtils.hasText(createDTO.newAddress())
                || hasContent(createDTO.profileImage());
        if (!hasAnyChange) {
            throw new InvalidInfoChangeRequestException("변경할 항목을 하나 이상 입력해야 합니다.");
        }
    }

    private boolean hasContent(MultipartFile file) {
        return file != null && !file.isEmpty();
    }
}
