package com.msa4lmsv2academic.domain.counseling.service;

import com.msa4lmsv2academic.domain.counseling.entity.Counseling;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingStatus;
import com.msa4lmsv2academic.domain.counseling.repository.CounselingParticipantQueryRepository;
import com.msa4lmsv2academic.domain.counseling.repository.CounselingRepository;
import com.msa4lmsv2academic.domain.counseling.request.CounselingAnswerRequestDTO;
import com.msa4lmsv2academic.domain.counseling.request.CounselingCreateRequestDTO;
import com.msa4lmsv2academic.domain.counseling.request.CounselingSearchRequestDTO;
import com.msa4lmsv2academic.domain.counseling.response.CounselingResponseDTO;
import com.msa4lmsv2academic.domain.counseling.response.CounselingProfessorResponseDTO;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.global.error.CounselingAccessDeniedException;
import com.msa4lmsv2academic.global.error.CounselingNotFoundException;
import com.msa4lmsv2academic.global.error.CounselingParticipantNotFoundException;
import com.msa4lmsv2academic.global.error.InvalidCounselingRequestException;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CounselingService {
    private final CounselingRepository repository;
    private final CounselingParticipantQueryRepository participants;
    private final CounselingNotificationService notifications;
    private final CounselingPolicy policy;

    public CounselingProfessorResponseDTO professor(CurrentUser user) {
        requireRole(user, "STUDENT", "학생만 상담 대상 교수를 조회할 수 있습니다.");
        return participants.findAdvisorByStudentUserId(user.id())
                .map(CounselingProfessorResponseDTO::from)
                .orElseThrow(() -> new CounselingParticipantNotFoundException("배정된 담당 교수를 찾을 수 없습니다."));
    }

    public PageResponseDTO<CounselingResponseDTO> search(CounselingSearchRequestDTO request, CurrentUser user) {
        requireReadable(user);
        Set<CounselingStatus> statuses = request.status() == null
                ? EnumSet.allOf(CounselingStatus.class) : EnumSet.of(request.status());
        Pageable pageable = PageRequest.of(request.resolvedPage() - 1, request.resolvedSize(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<Counseling> result = switch (user.role()) {
            case "STUDENT" -> repository.findByStudentUserIdAndStatusIn(user.id(), statuses, pageable);
            case "PROFESSOR" -> repository.findByProfessorUserIdAndStatusIn(user.id(), statuses, pageable);
            case "ADMIN" -> repository.findByStatusIn(statuses, pageable);
            default -> throw new CounselingAccessDeniedException("상담을 조회할 권한이 없습니다.");
        };
        return new PageResponseDTO<>(result.map(CounselingResponseDTO::from).getContent(),
                result.getTotalElements(), request.resolvedPage(), request.resolvedSize(), result.hasNext());
    }

    public CounselingResponseDTO get(Long id, CurrentUser user) {
        requireReadable(user);
        Counseling counseling = find(id);
        if (!"ADMIN".equals(user.role()) && !isParticipant(counseling, user.id())) {
            throw new CounselingAccessDeniedException("해당 상담을 조회할 권한이 없습니다.");
        }
        return CounselingResponseDTO.from(counseling);
    }

    @Transactional
    public CounselingResponseDTO create(CounselingCreateRequestDTO request, CurrentUser user) {
        requireRole(user, "STUDENT", "학생만 상담을 신청할 수 있습니다.");
        Student student = participants.findStudentByUserIdForUpdate(user.id())
                .orElseThrow(() -> new CounselingParticipantNotFoundException("학생 정보를 찾을 수 없습니다."));
        policy.requireCounselingAllowed(student.getAcademicStatus());
        Professor professor = student.getAdvisor();
        if (professor == null) {
            throw new CounselingParticipantNotFoundException("배정된 담당 교수를 찾을 수 없습니다.");
        }
        if (!professor.getId().equals(request.professorId())) {
            throw new CounselingAccessDeniedException("배정된 담당 교수에게만 상담을 신청할 수 있습니다.");
        }
        Counseling saved = repository.saveAndFlush(Counseling.create(student, professor,
                normalize(request.title(), 200, "상담 제목"),
                normalize(request.question(), 10000, "상담 내용")));
        notifications.createRequested(saved);
        return CounselingResponseDTO.from(saved);
    }

    @Transactional
    public CounselingResponseDTO answer(Long id, CounselingAnswerRequestDTO request, CurrentUser user) {
        requireRole(user, "PROFESSOR", "교수만 상담에 답변할 수 있습니다.");
        Counseling counseling = findForUpdate(id);
        if (!counseling.getProfessor().getUser().getId().equals(user.id())) {
            throw new CounselingAccessDeniedException("본인에게 신청된 상담에만 답변할 수 있습니다.");
        }
        CounselingStatus previous = counseling.getStatus();
        boolean updated = counseling.hasAnswer();
        counseling.answer(normalize(request.answer(), 10000, "상담 답변"),
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        Counseling saved = repository.saveAndFlush(counseling);
        notifications.createAnswered(saved, previous, updated);
        return CounselingResponseDTO.from(saved);
    }

    private Counseling find(Long id) {
        validateId(id);
        return repository.findById(id).orElseThrow(CounselingNotFoundException::new);
    }

    private Counseling findForUpdate(Long id) {
        validateId(id);
        return repository.findByIdForUpdate(id).orElseThrow(CounselingNotFoundException::new);
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new InvalidCounselingRequestException("counselingId는 양수여야 합니다.");
        }
    }

    private void requireReadable(CurrentUser user) {
        if (user == null || user.id() == null || !Set.of("STUDENT", "PROFESSOR", "ADMIN").contains(user.role())) {
            throw new CounselingAccessDeniedException("상담을 조회할 권한이 없습니다.");
        }
    }

    private void requireRole(CurrentUser user, String role, String message) {
        if (user == null || user.id() == null || !role.equals(user.role())) {
            throw new CounselingAccessDeniedException(message);
        }
    }

    private boolean isParticipant(Counseling counseling, Long userId) {
        return counseling.getStudent().getUser().getId().equals(userId)
                || counseling.getProfessor().getUser().getId().equals(userId);
    }

    private String normalize(String value, int max, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidCounselingRequestException(field + "은 필수입니다.");
        }
        String normalized = value.trim();
        if (normalized.length() > max) {
            throw new InvalidCounselingRequestException(field + "이 너무 깁니다.");
        }
        return normalized;
    }
}
