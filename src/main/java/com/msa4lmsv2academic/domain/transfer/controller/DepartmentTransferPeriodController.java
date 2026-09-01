package com.msa4lmsv2academic.domain.transfer.controller;

import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

import com.msa4lmsv2academic.domain.transfer.request.*;
import com.msa4lmsv2academic.domain.transfer.response.DepartmentTransferPeriodResponseDTO;
import com.msa4lmsv2academic.domain.transfer.service.*;
import com.msa4lmsv2academic.global.response.*;
import com.msa4lmsv2academic.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Department transfer periods", description = "적용 학기별 전과 접수 기간 기준정보 API")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/academic/catalog/department-transfer-periods")
@SecurityRequirement(name = "bearerAuth")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.DUPLICATE_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
public class DepartmentTransferPeriodController {
    private final DepartmentTransferPeriodService service;

    @Operation(operationId = "searchDepartmentTransferPeriods", summary = "전과 접수 기간 조회",
            description = "STUDENT는 활성 기간만, ADMIN은 전체 기간을 조회합니다. 적용 학기·활성 여부 필터와 "
                    + "학년도·학기 내림차순을 제공하며 open은 현재 시각상 접수 가능 여부입니다.")
    @ApiResponse(responseCode = "200", description = "00: 조회 성공")
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<DepartmentTransferPeriodResponseDTO>>> search(
            @ParameterObject @Valid @ModelAttribute DepartmentTransferPeriodSearchRequestDTO filter,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.search(filter, actor,
                PageRequest.of(filter.resolvedPage() - 1, filter.resolvedSize()))));
    }

    @Operation(operationId = "createDepartmentTransferPeriod", summary = "전과 접수 기간 등록",
            description = "ADMIN 전용. 적용 학기별 기간은 비활성을 포함해 하나만 등록하며 변경 사유를 감사 기록합니다.")
    @ApiResponse(responseCode = "200", description = "00: 등록 성공 또는 저장된 성공 응답 재생")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<DepartmentTransferPeriodResponseDTO>> create(
            @Valid @RequestBody DepartmentTransferPeriodSaveRequestDTO request,
            @Parameter(description = "1~100자의 공백 없는 요청별 키", required = true,
                    schema = @Schema(minLength = 1, maxLength = 100), example = "transfer-period-create-001")
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.create(request, key, actor,
                DepartmentTransferAuditContext.from(httpRequest))));
    }

    @Operation(operationId = "updateDepartmentTransferPeriod", summary = "전과 접수 기간 수정",
            description = "ADMIN 전용. 적용 학기는 바꿀 수 없으며 시작·종료·활성 여부와 변경 사유를 저장합니다.")
    @ApiResponse(responseCode = "200", description = "00: 수정 성공 또는 저장된 성공 응답 재생")
    @PutMapping("/{periodId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<DepartmentTransferPeriodResponseDTO>> update(
            @Parameter(description = "전과 접수 기간 식별자", example = "1")
            @Positive @PathVariable Long periodId,
            @Valid @RequestBody DepartmentTransferPeriodSaveRequestDTO request,
            @Parameter(description = "1~100자의 공백 없는 요청별 키", required = true,
                    schema = @Schema(minLength = 1, maxLength = 100), example = "transfer-period-update-001")
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.update(periodId, request, key, actor,
                DepartmentTransferAuditContext.from(httpRequest))));
    }

    @Operation(operationId = "changeDepartmentTransferPeriodStatus", summary = "전과 접수 기간 활성 상태 변경",
            description = "ADMIN 전용. 기존 신청은 보존하면서 신규 신청 접수만 즉시 중단하거나 재개합니다.")
    @ApiResponse(responseCode = "200", description = "00: 상태 변경 성공 또는 저장된 성공 응답 재생")
    @PatchMapping("/{periodId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<DepartmentTransferPeriodResponseDTO>> changeStatus(
            @Parameter(description = "전과 접수 기간 식별자", example = "1")
            @Positive @PathVariable Long periodId,
            @Valid @RequestBody DepartmentTransferPeriodStatusRequestDTO request,
            @Parameter(description = "1~100자의 공백 없는 요청별 키", required = true,
                    schema = @Schema(minLength = 1, maxLength = 100), example = "transfer-period-status-001")
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.changeStatus(periodId, request, key, actor,
                DepartmentTransferAuditContext.from(httpRequest))));
    }
}
