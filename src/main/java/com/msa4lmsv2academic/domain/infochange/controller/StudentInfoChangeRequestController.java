package com.msa4lmsv2academic.domain.infochange.controller;

import com.msa4lmsv2academic.domain.infochange.request.InfoChangeRequestRejectRequestDTO;
import com.msa4lmsv2academic.domain.infochange.request.InfoChangeRequestSearchRequestDTO;
import com.msa4lmsv2academic.domain.infochange.request.StudentInfoChangeRequestCreateDTO;
import com.msa4lmsv2academic.domain.infochange.response.StudentInfoChangeRequestResponseDTO;
import com.msa4lmsv2academic.domain.infochange.service.StudentInfoChangeRequestService;
import com.msa4lmsv2academic.global.response.GlobalRes;
import com.msa4lmsv2academic.global.response.PageRes;
import com.msa4lmsv2academic.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@Tag(name = "InfoChangeRequests", description = "학적 정보 변경 신청과 관리자 승인 API")
@Validated
@RestController
@RequestMapping("/api/academic/info-change-requests")
@RequiredArgsConstructor
public class StudentInfoChangeRequestController {

    private final StudentInfoChangeRequestService infoChangeRequestService;

    @Operation(summary = "학적 정보 변경 신청 목록 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<GlobalRes<PageRes<StudentInfoChangeRequestResponseDTO>>> search(
            @ParameterObject @Valid @ModelAttribute InfoChangeRequestSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalRes.success(infoChangeRequestService.search(request, currentUser)));
    }

    @Operation(summary = "학적 정보 변경 신청 상세 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{requestId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<GlobalRes<StudentInfoChangeRequestResponseDTO>> get(
            @Positive @PathVariable Long requestId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalRes.success(infoChangeRequestService.get(requestId, currentUser)));
    }

    @Operation(summary = "학적 정보 변경 신청", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalRes<StudentInfoChangeRequestResponseDTO>> create(
            @Valid @ModelAttribute StudentInfoChangeRequestCreateDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalRes.success(infoChangeRequestService.create(request, currentUser)));
    }

    @Operation(summary = "학적 정보 변경 신청 승인", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/{requestId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalRes<StudentInfoChangeRequestResponseDTO>> approve(
            @Positive @PathVariable Long requestId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalRes.success(infoChangeRequestService.approve(requestId, currentUser)));
    }

    @Operation(summary = "학적 정보 변경 신청 반려", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/{requestId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalRes<StudentInfoChangeRequestResponseDTO>> reject(
            @Positive @PathVariable Long requestId,
            @Valid @RequestBody InfoChangeRequestRejectRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalRes.success(infoChangeRequestService.reject(requestId, request, currentUser)));
    }
}
