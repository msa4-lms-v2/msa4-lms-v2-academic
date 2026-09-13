package com.msa4lmsv2academic.domain.infochange.controller;

import com.msa4lmsv2academic.domain.infochange.request.AdminInfoChangeRequestSearchRequestDTO;
import com.msa4lmsv2academic.domain.infochange.response.AdminInfoChangeRequestSummaryResponseDTO;
import com.msa4lmsv2academic.domain.infochange.service.AdminInfoChangeRequestService;
import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.CustomResponseCode;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Profile Change Requests", description = "관리자용 학생·교수 통합 프로필 변경 신청 조회 API")
@RestController
@RequestMapping("/api/academic/admin/info-change-requests")
@RequiredArgsConstructor
public class AdminInfoChangeRequestController {

    private final AdminInfoChangeRequestService adminInfoChangeRequestService;

    @Operation(
            operationId = "searchAdminProfileChangeRequests",
            summary = "학생·교수 통합 프로필 변경 신청 목록 조회",
            description = "ADMIN이 학생과 교수의 프로필 변경 신청을 유형·신청자 이름·처리 상태·신청일로 검색하고, "
                    + "신청일 내림차순으로 통합 페이지 조회합니다. 상세·승인·반려는 응답 requesterType에 맞는 "
                    + "기존 학생 또는 교수 리소스를 사용합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<AdminInfoChangeRequestSummaryResponseDTO>>> search(
            @ParameterObject @Valid @ModelAttribute AdminInfoChangeRequestSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(adminInfoChangeRequestService.search(request, currentUser)));
    }
}
