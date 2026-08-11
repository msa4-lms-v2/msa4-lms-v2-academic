package com.msa4lmsv2academic.domain.counseling.service;

import com.msa4lmsv2academic.domain.counseling.entity.CounselingMethod;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingRecord;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingStatus;
import com.msa4lmsv2academic.domain.counseling.repository.CounselingRecordRepository;
import com.msa4lmsv2academic.domain.counseling.repository.CounselingRecordSearchCondition;
import com.msa4lmsv2academic.domain.counseling.repository.CounselingRecordSearchResult;
import com.msa4lmsv2academic.domain.counseling.repository.ProfessorCounselingRecordQueryRepository;
import com.msa4lmsv2academic.domain.counseling.request.CounselingRecordSearchRequestDTO;
import com.msa4lmsv2academic.domain.counseling.request.InPersonCounselingCreateRequestDTO;
import com.msa4lmsv2academic.domain.counseling.request.OnlineCounselingResponseUpdateRequestDTO;
import com.msa4lmsv2academic.domain.counseling.response.CounselingRecordResponseDTO;
import com.msa4lmsv2academic.domain.counseling.response.CounselingRecordSummaryResponseDTO;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.global.error.CounselingAccessDeniedException;
import com.msa4lmsv2academic.global.error.CounselingParticipantNotFoundException;
import com.msa4lmsv2academic.global.error.CounselingRecordNotFoundException;
import com.msa4lmsv2academic.global.error.InvalidCounselingRecordException;
import com.msa4lmsv2academic.global.response.PageRes;
import com.msa4lmsv2academic.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfessorCounselingRecordService {

    private static final int MAX_TITLE_LENGTH = 150;
    private static final int MAX_CONTENT_LENGTH = 5000;

    private final CounselingRecordRepository counselingRecordRepository;
    private final ProfessorCounselingRecordQueryRepository counselingRecordQueryRepository;

    public PageRes<CounselingRecordSummaryResponseDTO> search(
            CounselingRecordSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        validateProfessor(currentUser);

        int page = request.resolvedPage();
        int size = request.resolvedSize();
        long offset = (page - 1L) * size;
        CounselingRecordSearchCondition condition = new CounselingRecordSearchCondition(
                offset,
                size,
                currentUser.id(),
                request.studentId(),
                request.counselingMethod(),
                request.status()
        );

        CounselingRecordSearchResult result = counselingRecordQueryRepository.search(condition);
        List<CounselingRecordSummaryResponseDTO> items = result.items().stream()
                .map(CounselingRecordSummaryResponseDTO::from)
                .toList();
        boolean hasNext = offset + items.size() < result.totalCount();

        return new PageRes<>(items, result.totalCount(), page, size, hasNext);
    }

    public CounselingRecordResponseDTO get(Long recordId, CurrentUser currentUser) {
        validateProfessor(currentUser);
        CounselingRecord record = getProfessorRecord(recordId, currentUser.id());
        return CounselingRecordResponseDTO.from(record);
    }

    @Transactional
    public CounselingRecordResponseDTO createInPerson(
            InPersonCounselingCreateRequestDTO request,
            CurrentUser currentUser
    ) {
        validateProfessor(currentUser);
        validatePastOrPresent(request.counseledAt());

        Professor professor = counselingRecordQueryRepository.findProfessorByUserId(currentUser.id())
                .orElseThrow(() -> new CounselingParticipantNotFoundException("교수 정보를 찾을 수 없습니다."));
        Student student = counselingRecordQueryRepository
                .findAdvisedStudent(request.studentId(), professor.getId())
                .orElseThrow(() -> new CounselingAccessDeniedException("담당 학생의 상담 기록만 등록할 수 있습니다."));

        CounselingRecord record = CounselingRecord.recordInPerson(
                student,
                professor,
                normalize(request.title(), "title", MAX_TITLE_LENGTH),
                normalize(request.content(), "content", MAX_CONTENT_LENGTH),
                request.counseledAt()
        );
        return CounselingRecordResponseDTO.from(counselingRecordRepository.saveAndFlush(record));
    }

    @Transactional
    public CounselingRecordResponseDTO updateOnlineResponse(
            Long recordId,
            OnlineCounselingResponseUpdateRequestDTO request,
            CurrentUser currentUser
    ) {
        validateProfessor(currentUser);
        CounselingRecord record = getProfessorRecord(recordId, currentUser.id());

        if (record.getCounselingMethod() != CounselingMethod.ONLINE) {
            throw new InvalidCounselingRecordException("온라인 상담에만 답변을 등록할 수 있습니다.");
        }
        if (record.getStatus() == CounselingStatus.CANCELLED) {
            throw new InvalidCounselingRecordException("취소된 상담에는 답변을 등록할 수 없습니다.");
        }

        record.answer(
                normalize(request.response(), "response", MAX_CONTENT_LENGTH),
                LocalDateTime.now()
        );
        return CounselingRecordResponseDTO.from(counselingRecordRepository.saveAndFlush(record));
    }

    private CounselingRecord getProfessorRecord(Long recordId, Long professorUserId) {
        if (recordId == null || recordId <= 0) {
            throw new InvalidCounselingRecordException("recordId는 양수여야 합니다.");
        }
        return counselingRecordQueryRepository.findByIdForProfessor(recordId, professorUserId)
                .orElseThrow(CounselingRecordNotFoundException::new);
    }

    private void validateProfessor(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || !"PROFESSOR".equals(currentUser.role())) {
            throw new CounselingAccessDeniedException("교수만 상담 기록을 관리할 수 있습니다.");
        }
    }

    private void validatePastOrPresent(LocalDateTime counseledAt) {
        if (counseledAt == null || counseledAt.isAfter(LocalDateTime.now())) {
            throw new InvalidCounselingRecordException("counseledAt은 현재보다 미래일 수 없습니다.");
        }
    }

    private String normalize(String value, String fieldName, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidCounselingRecordException(fieldName + "은(는) 필수입니다.");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new InvalidCounselingRecordException(fieldName + "은(는) " + maxLength + "자 이하여야 합니다.");
        }
        return normalized;
    }
}
