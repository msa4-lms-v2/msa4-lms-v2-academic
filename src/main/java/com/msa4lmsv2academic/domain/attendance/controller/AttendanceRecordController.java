package com.msa4lmsv2academic.domain.attendance.controller;

import com.msa4lmsv2academic.domain.attendance.request.AttendanceRecordSearchRequestDTO;
import com.msa4lmsv2academic.domain.attendance.request.AttendanceRecordUpdateRequestDTO;
import com.msa4lmsv2academic.domain.attendance.response.AttendanceRecordResponseDTO;
import com.msa4lmsv2academic.domain.attendance.service.AttendanceRecordService;
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
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Attendance Records", description = "출결 기록 조회·수정 API")
@Validated
@RestController
@RequestMapping("/api/academic/attendance/records")
@RequiredArgsConstructor
public class AttendanceRecordController {

    private final AttendanceRecordService attendanceRecordService;

    @Operation(
            operationId = "searchAttendanceRecords",
            summary = "출결 기록 조회",
            description = "학생은 본인 출결, 교수는 담당 강의 출결, 관리자는 전체 출결을 조건별로 조회합니다. "
                    + "조회 결과가 없으면 빈 items와 totalCount 0을 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "출결 기록 조회 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<AttendanceRecordResponseDTO>>> search(
            @ParameterObject @Valid @ModelAttribute AttendanceRecordSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(attendanceRecordService.search(request, currentUser)));
    }

    @Operation(
            operationId = "updateAttendanceRecord",
            summary = "출결 기록 수정",
            description = "담당 교수 또는 관리자가 출결 상태와 비고를 수정합니다. "
                    + "변경 전후 값, 처리자, 처리시각과 수정 사유를 감사 이력으로 남깁니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "출결 기록 수정 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.DUPLICATE_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PatchMapping("/{attendanceId}")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalResponseDTO<AttendanceRecordResponseDTO>> update(
            @Parameter(description = "출결 기록 ID", example = "501")
            @Positive(message = "attendanceId는 양수여야 합니다.") @PathVariable Long attendanceId,
            @Valid @RequestBody AttendanceRecordUpdateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(attendanceRecordService.update(
                attendanceId,
                request,
                currentUser,
                httpRequest.getHeader("X-Request-Id"),
                httpRequest.getRemoteAddr()
        )));
    }
}
