package com.msa4lmsv2academic.domain.leaverequest.controller;

import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

import com.msa4lmsv2academic.domain.leaverequest.request.LeavePeriodSaveRequestDTO;
import com.msa4lmsv2academic.domain.leaverequest.request.LeavePeriodSearchRequestDTO;
import com.msa4lmsv2academic.domain.leaverequest.response.LeavePeriodResponseDTO;
import com.msa4lmsv2academic.domain.leaverequest.service.LeaveAuditContext;
import com.msa4lmsv2academic.domain.leaverequest.service.LeavePeriodService;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

@Tag(name = "Leave request periods", description = "적용 학기·유형별 접수/승인 기간. KST 기준 시작·종료 시각 포함")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/academic/leave-request-periods")
@SecurityRequirement(name = "bearerAuth")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.DUPLICATE_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.FILE_SIZE_EXCEEDED,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
public class LeavePeriodController {
    private final LeavePeriodService service;

    @Operation(operationId = "searchLeaveRequestPeriods", summary = "휴·복학 기간 조회",
            description = "STUDENT는 활성 설정만, ADMIN은 전체 설정을 조회합니다. 학기 ID·유형·활성 여부 필터, "
                    + "학년도/학기 내림차순·유형 오름차순·ID 내림차순입니다. 빈 결과는 items=[], totalCount=0. "
                    + "open은 시간상 접수 가능 여부이며 학생 개인의 신청 자격을 보장하지 않습니다.")
    @ApiResponse(responseCode = "200", description = "00: 조회 성공")
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<LeavePeriodResponseDTO>>> search(
            @ParameterObject @Valid @ModelAttribute LeavePeriodSearchRequestDTO filter,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.search(filter, actor,
                PageRequest.of(filter.resolvedPage() - 1, filter.resolvedSize()))));
    }

    @Operation(operationId = "createLeaveRequestPeriod", summary = "휴·복학 기간 등록",
            description = "ADMIN 전용. semesterId는 적용 학기이며 사전 등록되어야 합니다. 같은 학기·유형은 비활성 포함 "
                    + "한 설정만 허용합니다. 접수/승인은 각각 시작<종료여야 하며 서로 겹쳐도 됩니다. 변경 사유와 감사 필수.")
    @ApiResponse(responseCode = "200", description = "00: 등록 성공 또는 저장된 성공 응답 재생")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<LeavePeriodResponseDTO>> create(
            @Valid @RequestBody LeavePeriodSaveRequestDTO request,
            @Parameter(description = "1~100자의 공백 없는 요청별 키. 같은 사용자·경로·본문·파일의 완료 응답은 24시간 재생. 재생 시 업무·감사 재실행 없음",
                    required = true, schema = @Schema(minLength = 1, maxLength = 100), example = "leave-operation-001")
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.create(request, key, actor, LeaveAuditContext.from(httpRequest))));
    }

    @Operation(operationId = "updateLeaveRequestPeriod", summary = "휴·복학 기간 수정",
            description = "ADMIN 전용. 기존 학기·유형은 유지하고 기간·활성 여부를 교체합니다. 연장·재개도 기존 행을 수정합니다. "
                    + "수정으로 기존 신청을 취소하지 않습니다. 변경 사유·변경 전후·관리자를 감사에 기록합니다.")
    @ApiResponse(responseCode = "200", description = "00: 수정 성공 또는 저장된 성공 응답 재생")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<LeavePeriodResponseDTO>> update(
            @Parameter(description = "기간 설정 ID", example = "1") @Positive @PathVariable Long id,
            @Valid @RequestBody LeavePeriodSaveRequestDTO request,
            @Parameter(description = "1~100자의 공백 없는 요청별 키. 같은 사용자·경로·본문·파일의 완료 응답은 24시간 재생. 재생 시 업무·감사 재실행 없음",
                    required = true, schema = @Schema(minLength = 1, maxLength = 100), example = "leave-operation-001")
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.update(id, request, key, actor, LeaveAuditContext.from(httpRequest))));
    }
}
