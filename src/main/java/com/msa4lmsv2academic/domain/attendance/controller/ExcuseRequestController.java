package com.msa4lmsv2academic.domain.attendance.controller;

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
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "공결 신청 성공"),
            @ApiResponse(responseCode = "400", description = "필수값, 신청 기한, 수업일 또는 교시 오류",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "학생 권한이 아님",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "본인 소유 수강신청 없음",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "동일 수강·수업일·교시 중복 신청",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class)))
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
