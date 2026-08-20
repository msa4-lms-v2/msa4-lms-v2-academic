package com.msa4lmsv2academic.domain.lecture.service;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.lecture.entity.LectureStatus;
import com.msa4lmsv2academic.domain.lecture.entity.SyllabusFile;
import com.msa4lmsv2academic.domain.lecture.repository.SyllabusFileReferenceQueryRepository;
import com.msa4lmsv2academic.domain.lecture.repository.SyllabusFileRepository;
import com.msa4lmsv2academic.domain.lecture.response.SyllabusFileDownloadTarget;
import com.msa4lmsv2academic.domain.lecture.response.SyllabusFileResponseDTO;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.global.error.DuplicateSyllabusFileException;
import com.msa4lmsv2academic.global.error.SyllabusFileAccessDeniedException;
import com.msa4lmsv2academic.global.error.SyllabusFileConflictException;
import com.msa4lmsv2academic.global.error.SyllabusFileNotFoundException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SyllabusFileTransactionService {

    private static final String AUDIT_ACTION = "SYLLABUS_FILE_UPLOADED";
    private static final String AUDIT_TARGET_TYPE = "SYLLABUS_FILE";

    private final SyllabusFileRepository syllabusFileRepository;
    private final SyllabusFileReferenceQueryRepository referenceQueryRepository;
    private final AuditLogService auditLogService;

    public void validateUploadTarget(
            Long classId,
            String originalName,
            long size,
            CurrentUser currentUser
    ) {
        validateProfessor(currentUser);
        Lecture lecture = findLecture(classId);
        validateOwner(lecture, currentUser);
        validateOpen(lecture);
        validateNoDuplicate(classId, originalName, size);
    }

    @Transactional
    public SyllabusFileResponseDTO register(
            Long classId,
            String originalName,
            String storedName,
            String contentType,
            long size,
            CurrentUser currentUser,
            String requestId,
            String ipAddress
    ) {
        validateProfessor(currentUser);
        Lecture lecture = referenceQueryRepository.findLectureForUpdate(classId)
                .orElseThrow(() -> new SyllabusFileNotFoundException("강의를 찾을 수 없습니다."));
        validateOwner(lecture, currentUser);
        validateOpen(lecture);
        validateNoDuplicate(classId, originalName, size);
        User uploader = referenceQueryRepository.findUser(currentUser.id())
                .orElseThrow(() -> new SyllabusFileNotFoundException("업로드 사용자 정보를 찾을 수 없습니다."));

        SyllabusFile saved = syllabusFileRepository.saveAndFlush(SyllabusFile.create(
                lecture,
                originalName,
                storedName,
                contentType,
                size,
                uploader
        ));
        auditLogService.record(
                currentUser.id(),
                AUDIT_ACTION,
                AUDIT_TARGET_TYPE,
                saved.getId(),
                null,
                snapshot(saved),
                null,
                normalizeNullable(requestId),
                normalizeNullable(ipAddress)
        );
        return SyllabusFileResponseDTO.from(saved);
    }

    public List<SyllabusFileResponseDTO> list(Long classId, CurrentUser currentUser) {
        validateReadableRole(currentUser);
        Lecture lecture = findLecture(classId);
        validateReadable(lecture, currentUser);
        return syllabusFileRepository.findAllByClassId(classId).stream()
                .map(SyllabusFileResponseDTO::from)
                .toList();
    }

    public SyllabusFileDownloadTarget getDownloadTarget(Long fileId, CurrentUser currentUser) {
        validateReadableRole(currentUser);
        SyllabusFile syllabusFile = syllabusFileRepository.findDetailById(fileId)
                .orElseThrow(() -> new SyllabusFileNotFoundException("강의계획서 파일을 찾을 수 없습니다."));
        validateReadable(syllabusFile.getLecture(), currentUser);
        return new SyllabusFileDownloadTarget(
                syllabusFile.getOriginalName(),
                syllabusFile.getStoredName(),
                syllabusFile.getContentType()
        );
    }

    private Lecture findLecture(Long classId) {
        return referenceQueryRepository.findLecture(classId)
                .orElseThrow(() -> new SyllabusFileNotFoundException("강의를 찾을 수 없습니다."));
    }

    private void validateNoDuplicate(Long classId, String originalName, long size) {
        if (syllabusFileRepository.existsByLectureIdAndOriginalNameAndSize(classId, originalName, size)) {
            throw new DuplicateSyllabusFileException();
        }
    }

    private void validateOpen(Lecture lecture) {
        if (lecture.getStatus() != LectureStatus.OPEN) {
            throw new SyllabusFileConflictException("개설 상태인 강의에만 강의계획서 파일을 등록할 수 있습니다.");
        }
    }

    private void validateReadableRole(CurrentUser currentUser) {
        validateAuthenticated(currentUser);
        if (!"PROFESSOR".equals(currentUser.role()) && !currentUser.isAdmin()) {
            throw new SyllabusFileAccessDeniedException("강의계획서 파일을 조회할 권한이 없습니다.");
        }
    }

    private void validateReadable(Lecture lecture, CurrentUser currentUser) {
        if (currentUser.isAdmin()) {
            return;
        }
        validateOwner(lecture, currentUser);
    }

    private void validateProfessor(CurrentUser currentUser) {
        validateAuthenticated(currentUser);
        if (!"PROFESSOR".equals(currentUser.role())) {
            throw new SyllabusFileAccessDeniedException("교수만 강의계획서 파일을 업로드할 수 있습니다.");
        }
    }

    private void validateAuthenticated(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || currentUser.role() == null) {
            throw new SyllabusFileAccessDeniedException("인증된 사용자만 강의계획서 파일을 사용할 수 있습니다.");
        }
    }

    private void validateOwner(Lecture lecture, CurrentUser currentUser) {
        if (!lecture.getProfessor().getUser().getId().equals(currentUser.id())) {
            throw new SyllabusFileAccessDeniedException("본인이 담당하는 강의계획서 파일만 사용할 수 있습니다.");
        }
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private Map<String, Object> snapshot(SyllabusFile syllabusFile) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("fileId", syllabusFile.getId());
        snapshot.put("classId", syllabusFile.getLecture().getId());
        snapshot.put("originalName", syllabusFile.getOriginalName());
        snapshot.put("storedName", syllabusFile.getStoredName());
        snapshot.put("contentType", syllabusFile.getContentType());
        snapshot.put("size", syllabusFile.getSize());
        snapshot.put("uploadedBy", syllabusFile.getUploadedBy().getId());
        return snapshot;
    }
}
