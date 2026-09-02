package com.msa4lmsv2academic.domain.dismissal.controller;

import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

import com.msa4lmsv2academic.domain.dismissal.request.*;
import com.msa4lmsv2academic.domain.dismissal.response.DismissalResponseDTO;
import com.msa4lmsv2academic.domain.dismissal.service.DismissalAuditContext;
import com.msa4lmsv2academic.domain.dismissal.service.DismissalService;
import com.msa4lmsv2academic.global.response.*;
import com.msa4lmsv2academic.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/academic/dismissals")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Dismissals", description = "관리자 제적. 학생 자퇴와 별도 관리")
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
public class DismissalController {
    private final DismissalService service;
    private static final String KEY_DESCRIPTION = "1~100자의 공백 없는 필수 키. 동일 사용자·경로·정규화된 본문·키의 성공 응답을 24시간 재생합니다. "
            + "수정된 본문/버전에는 새 키를 사용합니다. 재생은 감사·자동 취소를 반복하지 않으며 현재 권한 검증 후 버전 비교보다 먼저 수행합니다.";

    @Operation(operationId = "searchDismissals", summary = "제적 후보 목록",
            description = "ADMIN 전용. 학생 ID·현재 학과·이름·제적 종류·처리 상태 필터 및 생성 시각/ID 정렬. "
                    + "page 기본 1, size 기본 20/최대 100. 필터 미지정은 전체, 결과 없음은 빈 items와 totalCount=0. "
                    + "학생/교수용 원본 조회는 제공하지 않으며 민감한 상세 근거는 ADMIN에게만 반환합니다.")
    @ApiResponse(responseCode = "200", description = "00: 페이지 조회 성공")
    @GetMapping
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<DismissalResponseDTO>>> search(
            @ParameterObject @Valid @ModelAttribute DismissalSearchRequestDTO filter,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.search(filter, actor,
                PageRequest.of(filter.resolvedPage() - 1, filter.resolvedSize()))));
    }

    @Operation(operationId = "getDismissal", summary = "제적 후보 상세",
            description = "ADMIN 전용. 대기/확정/취소 원본 및 최신 version을 제공합니다. 학적 이력의 sourceId를 알아도 학생/교수는 조회할 수 없습니다.")
    @ApiResponse(responseCode = "200", description = "00: 상세 조회 성공")
    @GetMapping("/{id}")
    public ResponseEntity<GlobalResponseDTO<DismissalResponseDTO>> get(
            @Parameter(description = "제적 후보 ID", example = "1") @Positive @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.get(id, actor)));
    }

    @Operation(operationId = "createDismissal", summary = "제적 후보 등록",
            description = "ADMIN이 실제 근거를 확인한 뒤 등록합니다. 학생당 종류와 무관하게 PENDING 1건만 허용합니다. "
                    + "휴학만료는 ON_LEAVE, 나머지 종류는 ENROLLED/ON_LEAVE만 가능합니다. "
                    + "납부·경고·징계·기한의 전체 자동 판정은 하지 않습니다. 성공 시 PENDING이며 학적과 다른 신청은 변경하지 않습니다.")
    @ApiResponse(responseCode = "200", description = "00: 등록 또는 저장된 성공 응답 재생")
    @PostMapping
    public ResponseEntity<GlobalResponseDTO<DismissalResponseDTO>> create(
            @Valid @RequestBody DismissalCreateRequestDTO body,
            @Parameter(description = KEY_DESCRIPTION, required = true, example = "dismissal-create-001",
                    schema = @Schema(minLength = 1, maxLength = 100, pattern = "^\\S{1,100}$"))
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor, HttpServletRequest request) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.create(body, key, actor, DismissalAuditContext.from(request))));
    }

    @Operation(operationId = "updateDismissal", summary = "제적 후보 근거 수정",
            description = "ADMIN 전용. PENDING의 reasonType/reason 전체와 조회한 version을 전달합니다. "
                    + "학생·최초 등록자는 변경하지 않습니다. 변경할 종류의 현재 학적 조건을 검증하고 변경 전후를 감사에 남깁니다. "
                    + "확정·취소 후보 수정 또는 오래된 version의 새로운 요청은 E11입니다.")
    @ApiResponse(responseCode = "200", description = "00: 수정 또는 저장된 성공 응답 재생")
    @PutMapping("/{id}")
    public ResponseEntity<GlobalResponseDTO<DismissalResponseDTO>> update(
            @Parameter(description = "제적 후보 ID", example = "1") @Positive @PathVariable Long id,
            @Valid @RequestBody DismissalUpdateRequestDTO body,
            @Parameter(description = KEY_DESCRIPTION, required = true, example = "dismissal-update-001",
                    schema = @Schema(minLength = 1, maxLength = 100, pattern = "^\\S{1,100}$"))
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor, HttpServletRequest request) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.update(id, body, key, actor, DismissalAuditContext.from(request))));
    }

    @Operation(operationId = "changeDismissalStatus", summary = "제적 확정·후보 취소",
            description = "ADMIN 전용. 조회한 version과 CONFIRMED/CANCELLED를 전달합니다. PENDING만 변경 가능합니다. "
                    + "확정 직전 학적을 재검증하고 즉시 DISMISSED로 전이합니다. 학적 이력에는 확정 사실만 남깁니다. "
                    + "같은 학생의 PENDING 휴복학과 PENDING/ADVISOR_APPROVED 자퇴는 자동 취소하고 원본 사유·증빙·지도교수 검토를 보존합니다. "
                    + "확정·학적·이력·자동 취소·감사·멱등 응답은 하나의 transaction입니다. "
                    + "후보 취소는 사유 필수이며 학적이 바뀌었어도 가능합니다. 후보 취소 자체는 학적 이력을 만들지 않습니다. "
                    + "예약·소급·확정 후 복구 및 Auth 로그인/세션 제한은 제공하지 않습니다.")
    @ApiResponse(responseCode = "200", description = "00: 처리 또는 저장된 성공 응답 재생")
    @PatchMapping("/{id}/status")
    public ResponseEntity<GlobalResponseDTO<DismissalResponseDTO>> changeStatus(
            @Parameter(description = "제적 후보 ID", example = "1") @Positive @PathVariable Long id,
            @Valid @RequestBody DismissalStatusRequestDTO body,
            @Parameter(description = KEY_DESCRIPTION, required = true, example = "dismissal-confirm-001",
                    schema = @Schema(minLength = 1, maxLength = 100, pattern = "^\\S{1,100}$"))
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor, HttpServletRequest request) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.changeStatus(id, body, key, actor, DismissalAuditContext.from(request))));
    }
}
