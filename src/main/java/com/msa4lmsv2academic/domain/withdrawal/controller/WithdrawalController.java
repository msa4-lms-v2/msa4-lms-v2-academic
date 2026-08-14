package com.msa4lmsv2academic.domain.withdrawal.controller;

import com.msa4lmsv2academic.domain.withdrawal.request.AdvisorWithdrawalReviewRequestDTO;
import com.msa4lmsv2academic.domain.withdrawal.request.FinalWithdrawalReviewRequestDTO;
import com.msa4lmsv2academic.domain.withdrawal.request.WithdrawalCreateRequestDTO;
import com.msa4lmsv2academic.domain.withdrawal.request.WithdrawalSearchRequestDTO;
import com.msa4lmsv2academic.domain.withdrawal.response.WithdrawalResponseDTO;
import com.msa4lmsv2academic.domain.withdrawal.service.WithdrawalService;
import com.msa4lmsv2academic.global.response.GlobalRes;
import com.msa4lmsv2academic.global.response.PageRes;
import com.msa4lmsv2academic.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Withdrawals", description = "학생 자퇴 신청과 2단계 승인 API")
@Validated
@RestController
@RequestMapping("/api/academic/withdrawals")
@RequiredArgsConstructor
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    @Operation(summary = "자퇴 신청 목록 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalRes<PageRes<WithdrawalResponseDTO>>> search(
            @ParameterObject @Valid @ModelAttribute WithdrawalSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalRes.success(withdrawalService.search(request, currentUser)));
    }

    @Operation(
            summary = "자퇴 신청 상세 조회",
            description = "학생 본인, 배정 지도교수, ADMIN이 조회합니다. Payment는 Internal SCG의 ADMIN 서비스 주체로 호출합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "조회 권한 없음"),
            @ApiResponse(responseCode = "404", description = "자퇴 신청 없음")
    })
    @GetMapping("/{withdrawalId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalRes<WithdrawalResponseDTO>> get(
            @Positive @PathVariable Long withdrawalId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalRes.success(withdrawalService.get(withdrawalId, currentUser)));
    }

    @Operation(summary = "자퇴 신청", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "201", description = "신청 성공")
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalRes<WithdrawalResponseDTO>> create(
            @Valid @RequestBody WithdrawalCreateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalRes.success(withdrawalService.create(request, currentUser)));
    }

    @Operation(summary = "지도교수 자퇴 신청 검토", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/{withdrawalId}/advisor-review")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalRes<WithdrawalResponseDTO>> reviewByAdvisor(
            @Positive @PathVariable Long withdrawalId,
            @Valid @RequestBody AdvisorWithdrawalReviewRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalRes.success(
                withdrawalService.reviewByAdvisor(withdrawalId, request, currentUser)
        ));
    }

    @Operation(summary = "관리자 자퇴 신청 최종 검토", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/{withdrawalId}/final-review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalRes<WithdrawalResponseDTO>> reviewByAdmin(
            @Positive @PathVariable Long withdrawalId,
            @Valid @RequestBody FinalWithdrawalReviewRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalRes.success(
                withdrawalService.reviewByAdmin(withdrawalId, request, currentUser)
        ));
    }
}
