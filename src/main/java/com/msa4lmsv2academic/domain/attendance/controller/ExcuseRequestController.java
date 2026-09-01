package com.msa4lmsv2academic.domain.attendance.controller;

import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

import com.msa4lmsv2academic.domain.attendance.request.ExcuseRequestCreateRequestDTO;
import com.msa4lmsv2academic.domain.attendance.response.ExcuseRequestResponseDTO;
import com.msa4lmsv2academic.domain.attendance.service.ExcuseRequestService;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Attendance Excuses", description = "학생 공결 신청 및 처리 API")
@RestController
@RequestMapping("/api/academic/attendance/excuses")
@RequiredArgsConstructor
public class ExcuseRequestController {

    private final ExcuseRequestService excuseRequestService;

    @Operation(
            summary = "공결 신청",
            description = "학생이 본인의 활성 수강 수업에 대해 결석 수업일로부터 7일 이내 공결을 신청합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "201", description = "공결 신청 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.DUPLICATE_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponseDTO<ExcuseRequestResponseDTO>> create(
            @Valid @RequestBody ExcuseRequestCreateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponseDTO.success(excuseRequestService.create(request, currentUser)));
    }
}
