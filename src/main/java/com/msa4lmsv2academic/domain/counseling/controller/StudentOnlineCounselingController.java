package com.msa4lmsv2academic.domain.counseling.controller;

import com.msa4lmsv2academic.domain.counseling.request.OnlineCounselingCreateRequestDTO;
import com.msa4lmsv2academic.domain.counseling.request.StudentCounselingRecordSearchRequestDTO;
import com.msa4lmsv2academic.domain.counseling.response.CounselingRecordResponseDTO;
import com.msa4lmsv2academic.domain.counseling.response.CounselingRecordSummaryResponseDTO;
import com.msa4lmsv2academic.domain.counseling.service.StudentOnlineCounselingService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Student Online Counseling", description = "학생 온라인 상담 신청 및 최근 상담 내역 API")
@Validated
@RestController
@RequestMapping("/api/counseling")
@RequiredArgsConstructor
public class StudentOnlineCounselingController {

    private final StudentOnlineCounselingService studentOnlineCounselingService;

    @Operation(
            summary = "학생 온라인 상담 신청",
            description = "학생이 본인의 지도교수에게 온라인 상담을 신청합니다. 지도교수는 로그인 학생 정보로 자동 결정됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "신청 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 제목 또는 상담 내용", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "403", description = "학생 권한 필요", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "404", description = "학생 또는 지도교수 정보 없음", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "409", description = "답변 대기 중인 온라인 상담 중복", content = @Content(schema = @Schema(implementation = GlobalRes.class)))
    })
    @PostMapping("/online-records")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalRes<CounselingRecordResponseDTO>> createOnline(
            @Valid @RequestBody OnlineCounselingCreateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        CounselingRecordResponseDTO response = studentOnlineCounselingService.createOnline(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalRes.success(response));
    }

    @Operation(
            summary = "학생 최근 상담 내역 조회",
            description = "학생이 본인의 온라인·대면 상담 기록을 최근 수정 순서로 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 검색 조건", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "403", description = "학생 권한 필요", content = @Content(schema = @Schema(implementation = GlobalRes.class)))
    })
    @GetMapping("/my-records")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalRes<PageRes<CounselingRecordSummaryResponseDTO>>> searchMyRecords(
            @ParameterObject @Valid @ModelAttribute StudentCounselingRecordSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalRes.success(
                studentOnlineCounselingService.searchMyRecords(request, currentUser)
        ));
    }

    @Operation(
            summary = "학생 상담 내역 상세 조회",
            description = "학생이 본인의 상담 내용과 지도교수 답변을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 상담 기록 ID", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "403", description = "학생 권한 필요", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "404", description = "본인 상담 기록 없음", content = @Content(schema = @Schema(implementation = GlobalRes.class)))
    })
    @GetMapping("/my-records/{recordId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalRes<CounselingRecordResponseDTO>> getMyRecord(
            @PathVariable @Positive(message = "recordId는 양수여야 합니다.") Long recordId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalRes.success(
                studentOnlineCounselingService.getMyRecord(recordId, currentUser)
        ));
    }
}
