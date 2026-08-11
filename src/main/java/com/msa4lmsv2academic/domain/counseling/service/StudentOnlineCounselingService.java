package com.msa4lmsv2academic.domain.counseling.service;

import com.msa4lmsv2academic.domain.counseling.entity.CounselingMethod;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingRecord;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingStatus;
import com.msa4lmsv2academic.domain.counseling.repository.CounselingRecordRepository;
import com.msa4lmsv2academic.domain.counseling.repository.CounselingRecordSearchResult;
import com.msa4lmsv2academic.domain.counseling.repository.StudentCounselingRecordQueryRepository;
import com.msa4lmsv2academic.domain.counseling.repository.StudentCounselingRecordSearchCondition;
import com.msa4lmsv2academic.domain.counseling.request.OnlineCounselingCreateRequestDTO;
import com.msa4lmsv2academic.domain.counseling.request.StudentCounselingRecordSearchRequestDTO;
import com.msa4lmsv2academic.domain.counseling.response.CounselingRecordResponseDTO;
import com.msa4lmsv2academic.domain.counseling.response.CounselingRecordSummaryResponseDTO;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.global.error.CounselingAccessDeniedException;
import com.msa4lmsv2academic.global.error.CounselingParticipantNotFoundException;
import com.msa4lmsv2academic.global.error.CounselingRecordNotFoundException;
import com.msa4lmsv2academic.global.error.DuplicateCounselingRequestException;
import com.msa4lmsv2academic.global.error.InvalidCounselingRecordException;
import com.msa4lmsv2academic.global.response.PageRes;
import com.msa4lmsv2academic.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentOnlineCounselingService {

    private static final int MAX_TITLE_LENGTH = 150;
    private static final int MAX_CONTENT_LENGTH = 5000;

    private final CounselingRecordRepository counselingRecordRepository;
    private final StudentCounselingRecordQueryRepository studentCounselingRecordQueryRepository;

    public PageRes<CounselingRecordSummaryResponseDTO> searchMyRecords(
            StudentCounselingRecordSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        validateStudent(currentUser);

        int page = request.resolvedPage();
        int size = request.resolvedSize();
        long offset = (page - 1L) * size;
        StudentCounselingRecordSearchCondition condition = new StudentCounselingRecordSearchCondition(
                offset,
                size,
                currentUser.id(),
                request.counselingMethod(),
                request.status()
        );

        CounselingRecordSearchResult result = studentCounselingRecordQueryRepository.search(condition);
        List<CounselingRecordSummaryResponseDTO> items = result.items().stream()
                .map(CounselingRecordSummaryResponseDTO::from)
                .toList();
        boolean hasNext = offset + items.size() < result.totalCount();

        return new PageRes<>(items, result.totalCount(), page, size, hasNext);
    }

    public CounselingRecordResponseDTO getMyRecord(Long recordId, CurrentUser currentUser) {
        validateStudent(currentUser);
        CounselingRecord record = getStudentRecord(recordId, currentUser.id());
        return CounselingRecordResponseDTO.from(record);
    }

    @Transactional
    public CounselingRecordResponseDTO createOnline(
            OnlineCounselingCreateRequestDTO request,
            CurrentUser currentUser
    ) {
        validateStudent(currentUser);

        Student student = studentCounselingRecordQueryRepository.findStudentWithAdvisorByUserId(currentUser.id())
                .orElseThrow(() -> new CounselingParticipantNotFoundException("학생 정보를 찾을 수 없습니다."));
        Professor advisor = student.getAdvisor();
        if (advisor == null) {
            throw new CounselingParticipantNotFoundException("지도교수가 지정되지 않았습니다.");
        }

        boolean pendingRequestExists = counselingRecordRepository
                .existsByStudentIdAndProfessorIdAndCounselingMethodAndStatus(
                        student.getId(),
                        advisor.getId(),
                        CounselingMethod.ONLINE,
                        CounselingStatus.PENDING
                );
        if (pendingRequestExists) {
            throw new DuplicateCounselingRequestException();
        }

        CounselingRecord record = CounselingRecord.requestOnline(
                student,
                advisor,
                normalize(request.title(), "title", MAX_TITLE_LENGTH),
                normalize(request.content(), "content", MAX_CONTENT_LENGTH)
        );
        return CounselingRecordResponseDTO.from(counselingRecordRepository.saveAndFlush(record));
    }

    private CounselingRecord getStudentRecord(Long recordId, Long studentUserId) {
        if (recordId == null || recordId <= 0) {
            throw new InvalidCounselingRecordException("recordId는 양수여야 합니다.");
        }
        return studentCounselingRecordQueryRepository.findById(recordId, studentUserId)
                .orElseThrow(CounselingRecordNotFoundException::new);
    }

    private void validateStudent(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || !"STUDENT".equals(currentUser.role())) {
            throw new CounselingAccessDeniedException("학생만 온라인 상담을 신청하고 본인 기록을 조회할 수 있습니다.");
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
