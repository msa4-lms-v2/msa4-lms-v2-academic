package com.msa4lmsv2academic.domain.counseling.controller;

import com.msa4lmsv2academic.domain.counseling.entity.CounselingMethod;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingStatus;
import com.msa4lmsv2academic.domain.counseling.request.CounselingRecordSearchRequestDTO;
import com.msa4lmsv2academic.domain.counseling.request.InPersonCounselingCreateRequestDTO;
import com.msa4lmsv2academic.domain.counseling.request.OnlineCounselingResponseUpdateRequestDTO;
import com.msa4lmsv2academic.domain.counseling.response.CounselingRecordResponseDTO;
import com.msa4lmsv2academic.domain.counseling.response.CounselingRecordSummaryResponseDTO;
import com.msa4lmsv2academic.domain.counseling.service.ProfessorCounselingRecordService;
import com.msa4lmsv2academic.global.response.GlobalRes;
import com.msa4lmsv2academic.global.response.PageRes;
import com.msa4lmsv2academic.global.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfessorCounselingRecordControllerTest {

    private final ProfessorCounselingRecordService counselingRecordService =
            mock(ProfessorCounselingRecordService.class);
    private final ProfessorCounselingRecordController counselingRecordController =
            new ProfessorCounselingRecordController(counselingRecordService);
    private final CurrentUser professor = new CurrentUser(2001L, "PROFESSOR");

    @Test
    void searchReturnsGlobalPagedResponse() {
        CounselingRecordSearchRequestDTO request = new CounselingRecordSearchRequestDTO(
                null,
                null,
                null,
                null,
                null
        );
        PageRes<CounselingRecordSummaryResponseDTO> page =
                new PageRes<>(List.of(), 0L, 1, 20, false);
        when(counselingRecordService.search(request, professor)).thenReturn(page);

        ResponseEntity<?> response = counselingRecordController.search(request, professor);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        GlobalRes<?> body = (GlobalRes<?>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("00");
        assertThat(body.data()).isEqualTo(page);
    }

    @Test
    void createInPersonReturnsCreated() {
        LocalDateTime counseledAt = LocalDateTime.of(2026, 8, 10, 14, 0);
        InPersonCounselingCreateRequestDTO request = new InPersonCounselingCreateRequestDTO(
                101L,
                "진로 상담",
                "진로 방향을 논의했습니다.",
                counseledAt
        );
        CounselingRecordResponseDTO record = response(CounselingMethod.IN_PERSON, counseledAt);
        when(counselingRecordService.createInPerson(request, professor)).thenReturn(record);

        ResponseEntity<GlobalRes<CounselingRecordResponseDTO>> response =
                counselingRecordController.createInPerson(request, professor);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(record);
    }

    @Test
    void updateOnlineResponseReturnsCompletedRecord() {
        OnlineCounselingResponseUpdateRequestDTO request =
                new OnlineCounselingResponseUpdateRequestDTO("졸업요건을 안내했습니다.");
        CounselingRecordResponseDTO record = response(
                CounselingMethod.ONLINE,
                LocalDateTime.of(2026, 8, 11, 10, 0)
        );
        when(counselingRecordService.updateOnlineResponse(11L, request, professor)).thenReturn(record);

        ResponseEntity<GlobalRes<CounselingRecordResponseDTO>> response =
                counselingRecordController.updateOnlineResponse(11L, request, professor);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(record);
    }

    private CounselingRecordResponseDTO response(CounselingMethod method, LocalDateTime counseledAt) {
        return new CounselingRecordResponseDTO(
                11L,
                101L,
                "이학생",
                301L,
                "김교수",
                method,
                CounselingStatus.COMPLETED,
                "상담 제목",
                method == CounselingMethod.ONLINE ? "학생 문의" : null,
                "교수 답변",
                LocalDateTime.of(2026, 8, 10, 9, 0),
                counseledAt,
                method == CounselingMethod.ONLINE ? counseledAt : null,
                counseledAt
        );
    }
}
