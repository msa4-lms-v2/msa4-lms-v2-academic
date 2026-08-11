package com.msa4lmsv2academic.domain.counseling.controller;

import com.msa4lmsv2academic.domain.counseling.entity.CounselingMethod;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingStatus;
import com.msa4lmsv2academic.domain.counseling.request.OnlineCounselingCreateRequestDTO;
import com.msa4lmsv2academic.domain.counseling.request.StudentCounselingRecordSearchRequestDTO;
import com.msa4lmsv2academic.domain.counseling.response.CounselingRecordResponseDTO;
import com.msa4lmsv2academic.domain.counseling.response.CounselingRecordSummaryResponseDTO;
import com.msa4lmsv2academic.domain.counseling.service.CounselingRecordService;
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

class StudentCounselingRecordControllerTest {

    private final CounselingRecordService counselingRecordService = mock(CounselingRecordService.class);
    private final CounselingRecordController counselingRecordController =
            new CounselingRecordController(counselingRecordService);
    private final CurrentUser student = new CurrentUser(1001L, "STUDENT");

    @Test
    void createOnlineReturnsCreated() {
        OnlineCounselingCreateRequestDTO request = new OnlineCounselingCreateRequestDTO(
                "졸업 상담",
                "졸업 학점을 확인하고 싶습니다."
        );
        CounselingRecordResponseDTO record = response();
        when(counselingRecordService.createOnline(request, student)).thenReturn(record);

        ResponseEntity<GlobalRes<CounselingRecordResponseDTO>> response =
                counselingRecordController.createOnline(request, student);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(record);
    }

    @Test
    void searchMyRecordsReturnsGlobalPagedResponse() {
        StudentCounselingRecordSearchRequestDTO request =
                new StudentCounselingRecordSearchRequestDTO(null, null, null, null);
        PageRes<CounselingRecordSummaryResponseDTO> page =
                new PageRes<>(List.of(), 0L, 1, 20, false);
        when(counselingRecordService.searchMyRecords(request, student)).thenReturn(page);

        ResponseEntity<?> response = counselingRecordController.searchMyRecords(request, student);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        GlobalRes<?> body = (GlobalRes<?>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.data()).isEqualTo(page);
    }

    @Test
    void getMyRecordReturnsProfessorResponse() {
        CounselingRecordResponseDTO record = response();
        when(counselingRecordService.getMyRecord(11L, student)).thenReturn(record);

        ResponseEntity<GlobalRes<CounselingRecordResponseDTO>> response =
                counselingRecordController.getMyRecord(11L, student);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().professorResponse()).isEqualTo("전공필수 6학점이 더 필요합니다.");
    }

    private CounselingRecordResponseDTO response() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 11, 11, 0);
        return new CounselingRecordResponseDTO(
                11L,
                101L,
                "이학생",
                301L,
                "김교수",
                CounselingMethod.ONLINE,
                CounselingStatus.COMPLETED,
                "졸업 상담",
                "졸업 학점을 확인하고 싶습니다.",
                "전공필수 6학점이 더 필요합니다.",
                now.minusDays(1),
                now,
                now,
                now
        );
    }
}
