package com.msa4lmsv2academic.domain.counseling.controller;

import com.msa4lmsv2academic.domain.counseling.request.CounselingRecordSearchRequestDTO;
import com.msa4lmsv2academic.domain.counseling.request.InPersonCounselingCreateRequestDTO;
import com.msa4lmsv2academic.domain.counseling.request.OnlineCounselingResponseUpdateRequestDTO;
import com.msa4lmsv2academic.domain.counseling.response.CounselingRecordResponseDTO;
import com.msa4lmsv2academic.domain.counseling.response.CounselingRecordSummaryResponseDTO;
import com.msa4lmsv2academic.domain.counseling.service.ProfessorCounselingRecordService;
import com.msa4lmsv2academic.global.response.GlobalRes;
import com.msa4lmsv2academic.global.response.PageRes;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Professor Counseling Records", description = "교수 상담 답변 및 온라인·대면 상담 기록 API")
@Validated
@RestController
@RequestMapping("/api/counseling")
@RequiredArgsConstructor
public class ProfessorCounselingRecordController {

    private final ProfessorCounselingRecordService professorCounselingRecordService;

    @Operation(
            summary = "교수 상담 기록 목록 조회",
            description = "교수가 본인의 담당 학생에 대한 온라인·대면 상담 기록을 조건별로 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 검색 조건", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "403", description = "교수 권한 필요", content = @Content(schema = @Schema(implementation = GlobalRes.class)))
    })
    @GetMapping("/records")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalRes<PageRes<CounselingRecordSummaryResponseDTO>>> search(
            @ParameterObject @Valid @ModelAttribute CounselingRecordSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalRes.success(professorCounselingRecordService.search(request, currentUser)));
    }

    @Operation(
            summary = "교수 상담 기록 상세 조회",
            description = "교수가 본인에게 배정된 상담 기록과 학생의 온라인 상담 내용, 교수 답변을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 상담 기록 ID", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "403", description = "교수 권한 필요", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "404", description = "상담 기록 없음", content = @Content(schema = @Schema(implementation = GlobalRes.class)))
    })
    @GetMapping("/records/{recordId}")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalRes<CounselingRecordResponseDTO>> get(
            @PathVariable @Positive(message = "recordId는 양수여야 합니다.") Long recordId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalRes.success(professorCounselingRecordService.get(recordId, currentUser)));
    }

    @Operation(
            summary = "대면 상담 기록 등록",
            description = "교수가 실제 대면 상담을 완료한 뒤 담당 학생의 상담 내용과 결과를 기록합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 상담 기록", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "403", description = "교수 권한 또는 담당 학생 확인 실패", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "404", description = "교수 정보 없음", content = @Content(schema = @Schema(implementation = GlobalRes.class)))
    })
    @PostMapping("/in-person-records")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalRes<CounselingRecordResponseDTO>> createInPerson(
            @Valid @RequestBody InPersonCounselingCreateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        CounselingRecordResponseDTO response = professorCounselingRecordService.createInPerson(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalRes.success(response));
    }

    @Operation(
            summary = "온라인 상담 교수 답변 등록·수정",
            description = "교수가 본인에게 신청된 온라인 상담의 답변 전체를 등록하거나 수정합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "답변 저장 성공"),
            @ApiResponse(responseCode = "400", description = "대면 또는 취소 상담, 잘못된 답변", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "403", description = "교수 권한 필요", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "404", description = "상담 기록 없음 또는 다른 교수의 상담", content = @Content(schema = @Schema(implementation = GlobalRes.class)))
    })
    @PutMapping("/records/{recordId}/response")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalRes<CounselingRecordResponseDTO>> updateOnlineResponse(
            @PathVariable @Positive(message = "recordId는 양수여야 합니다.") Long recordId,
            @Valid @RequestBody OnlineCounselingResponseUpdateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalRes.success(
                professorCounselingRecordService.updateOnlineResponse(recordId, request, currentUser)
        ));
    }
}
