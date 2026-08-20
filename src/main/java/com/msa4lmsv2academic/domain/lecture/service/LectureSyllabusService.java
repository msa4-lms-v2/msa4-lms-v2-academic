package com.msa4lmsv2academic.domain.lecture.service;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.lecture.entity.LectureStatus;
import com.msa4lmsv2academic.domain.lecture.repository.LectureRepository;
import com.msa4lmsv2academic.domain.lecture.request.LectureSyllabusUpdateRequestDTO;
import com.msa4lmsv2academic.domain.lecture.response.LectureSyllabusResponseDTO;
import com.msa4lmsv2academic.global.error.InvalidLectureSyllabusRequestException;
import com.msa4lmsv2academic.global.error.LectureSyllabusAccessDeniedException;
import com.msa4lmsv2academic.global.error.LectureSyllabusConflictException;
import com.msa4lmsv2academic.global.error.LectureSyllabusNotFoundException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LectureSyllabusService {

    private static final String AUDIT_TARGET_TYPE = "LECTURE";
    private static final String CREATE_ACTION = "LECTURE_SYLLABUS_CREATED";
    private static final String UPDATE_ACTION = "LECTURE_SYLLABUS_UPDATED";

    private final LectureRepository lectureRepository;
    private final AuditLogService auditLogService;

    public LectureSyllabusResponseDTO get(Long classId, CurrentUser currentUser) {
        validateReadableRole(currentUser);
        Lecture lecture = lectureRepository.findSyllabusById(classId)
                .orElseThrow(LectureSyllabusNotFoundException::new);
        validateReadable(lecture, currentUser);
        return LectureSyllabusResponseDTO.from(lecture);
    }

    @Transactional
    public LectureSyllabusResponseDTO update(
            Long classId,
            LectureSyllabusUpdateRequestDTO request,
            CurrentUser currentUser,
            String requestId,
            String ipAddress
    ) {
        validateProfessor(currentUser);
        String syllabus = normalizeSyllabus(request);
        Lecture lecture = lectureRepository.findSyllabusByIdForUpdate(classId)
                .orElseThrow(LectureSyllabusNotFoundException::new);
        validateOwner(lecture, currentUser);
        validateEditableStatus(lecture);

        if (Objects.equals(lecture.getSyllabus(), syllabus)) {
            return LectureSyllabusResponseDTO.from(lecture);
        }

        Map<String, Object> beforeValue = snapshot(lecture);
        boolean creating = lecture.getSyllabus() == null || lecture.getSyllabus().isBlank();
        lecture.updateSyllabus(syllabus);
        Lecture saved = lectureRepository.saveAndFlush(lecture);
        auditLogService.record(
                currentUser.id(),
                creating ? CREATE_ACTION : UPDATE_ACTION,
                AUDIT_TARGET_TYPE,
                saved.getId(),
                beforeValue,
                snapshot(saved),
                null,
                normalizeNullable(requestId),
                normalizeNullable(ipAddress)
        );
        return LectureSyllabusResponseDTO.from(saved);
    }

    private void validateReadableRole(CurrentUser currentUser) {
        validateAuthenticated(currentUser);
        if (!"PROFESSOR".equals(currentUser.role()) && !currentUser.isAdmin()) {
            throw new LectureSyllabusAccessDeniedException("강의계획서를 조회할 권한이 없습니다.");
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
            throw new LectureSyllabusAccessDeniedException("교수만 강의계획서를 작성하거나 수정할 수 있습니다.");
        }
    }

    private void validateAuthenticated(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || currentUser.role() == null) {
            throw new LectureSyllabusAccessDeniedException("인증된 사용자만 강의계획서를 사용할 수 있습니다.");
        }
    }

    private void validateOwner(Lecture lecture, CurrentUser currentUser) {
        if (!lecture.getProfessor().getUser().getId().equals(currentUser.id())) {
            throw new LectureSyllabusAccessDeniedException("본인이 담당하는 강의계획서만 조회·수정할 수 있습니다.");
        }
    }

    private void validateEditableStatus(Lecture lecture) {
        if (lecture.getStatus() != LectureStatus.OPEN) {
            throw new LectureSyllabusConflictException("개설 상태인 강의의 강의계획서만 수정할 수 있습니다.");
        }
    }

    private String normalizeSyllabus(LectureSyllabusUpdateRequestDTO request) {
        if (request == null || request.syllabus() == null || request.syllabus().isBlank()) {
            throw new InvalidLectureSyllabusRequestException("강의계획서는 필수입니다.");
        }
        String normalized = request.syllabus().strip();
        if (normalized.length() > 65535) {
            throw new InvalidLectureSyllabusRequestException("강의계획서는 65535자 이하여야 합니다.");
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private Map<String, Object> snapshot(Lecture lecture) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("classId", lecture.getId());
        snapshot.put("professorId", lecture.getProfessor().getId());
        snapshot.put("status", lecture.getStatus().name());
        snapshot.put("syllabus", lecture.getSyllabus());
        return snapshot;
    }
}
