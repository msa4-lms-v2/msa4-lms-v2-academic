package com.msa4lmsv2academic.domain.graduation.controller;

import com.msa4lmsv2academic.domain.graduation.request.CreditDiagnosisSearchRequestDTO;
import com.msa4lmsv2academic.domain.graduation.response.CreditDiagnosisResponseDTO;
import com.msa4lmsv2academic.domain.graduation.response.CreditDiagnosisSummaryResponseDTO;
import com.msa4lmsv2academic.domain.graduation.service.GraduationCreditDiagnosisService;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Graduation Credit Diagnosis", description = "전공·교양·필수·선택 학점 진단 API")
@Validated
@RestController
@RequestMapping("/api/academic")
@RequiredArgsConstructor
public class GraduationCreditDiagnosisController {

    private final GraduationCreditDiagnosisService graduationCreditDiagnosisService;

    @Operation(
            operationId = "getCreditRequirementDiagnosis",
            summary = "졸업 학점 진단",
            description = "공개된 성적 중 통과한 과목을 기준으로 전공·교양·필수·선택 취득 학점과 전공·교양·총 "
                    + "부족 학점을 진단합니다. 학생은 본인, 교수는 현재 담당 강의·지도·소속 학과 학생, "
                    + "관리자는 전체 학생을 조회할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "진단 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 학생 ID", content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "학생 접근 권한 없음", content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "학생 또는 졸업요건 없음", content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class)))
    })
    @GetMapping("/credit-requirement-diagnosis")
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalResponseDTO<CreditDiagnosisResponseDTO>> diagnose(
            @RequestParam
            @Positive(message = "studentId는 양수여야 합니다.")
            @Parameter(description = "진단 대상 학생 ID", example = "1001")
            Long studentId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        CreditDiagnosisResponseDTO response = graduationCreditDiagnosisService.diagnose(studentId, currentUser);
        return ResponseEntity.ok(GlobalResponseDTO.success(response));
    }

    @Operation(
            operationId = "searchCreditRequirementDiagnoses",
            summary = "역할 범위별 학점 진단 현황 조회",
            description = "학생은 본인, 교수는 현재 담당 강의·지도·소속 학과 학생, 관리자는 전체 학생의 학점 "
                    + "진단 현황을 검색·필터·정렬합니다. 졸업요건이 없는 학생도 REQUIREMENT_NOT_CONFIGURED "
                    + "상태와 사유로 포함하며 결과가 없으면 빈 items를 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공 또는 빈 목록"),
            @ApiResponse(responseCode = "400", description = "잘못된 검색·정렬·페이징 조건",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "역할 또는 소유 범위 접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class)))
    })
    @GetMapping("/credit-requirement-diagnoses")
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<CreditDiagnosisSummaryResponseDTO>>> search(
            @ParameterObject @Valid @ModelAttribute CreditDiagnosisSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                graduationCreditDiagnosisService.search(request, currentUser)
        ));
    }
}
