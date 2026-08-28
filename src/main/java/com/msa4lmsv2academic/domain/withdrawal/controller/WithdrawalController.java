package com.msa4lmsv2academic.domain.withdrawal.controller;

import com.msa4lmsv2academic.domain.withdrawal.request.AdvisorWithdrawalReviewRequestDTO;
import com.msa4lmsv2academic.domain.withdrawal.request.FinalWithdrawalReviewRequestDTO;
import com.msa4lmsv2academic.domain.withdrawal.request.WithdrawalCreateRequestDTO;
import com.msa4lmsv2academic.domain.withdrawal.request.WithdrawalCancelRequestDTO;
import com.msa4lmsv2academic.domain.withdrawal.request.WithdrawalSearchRequestDTO;
import com.msa4lmsv2academic.domain.withdrawal.response.WithdrawalResponseDTO;
import com.msa4lmsv2academic.domain.withdrawal.service.WithdrawalService;
import com.msa4lmsv2academic.domain.withdrawal.service.WithdrawalAuditContext;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Withdrawals", description = "자퇴 신청·취소와 2단계 승인. 변경 요청의 완료 응답은 같은 사용자·경로·본문·키에 한해 24시간 재생합니다. "
        + "재생은 업무·감사를 다시 실행하지 않으며 이후 상태는 GET으로 확인합니다. 실패는 전체 rollback하고, 만료 후 같은 키도 새 요청으로 검사합니다.")
@Validated
@RestController
@RequestMapping("/api/academic/withdrawals")
@RequiredArgsConstructor
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "E21: 잘못된 ID·사유·본문·멱등 키·페이지·적용일",
                content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "E02/E04: 인증 필요 또는 유효하지 않은 인증 정보",
                content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "E03: 역할·본인·지도교수 범위 위반",
                content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "E10: 자퇴 신청 없음",
                content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
        @ApiResponse(responseCode = "409", description = "E11: 진행 중 신청 중복·상태 충돌·멱등 키 충돌·희망일 이전 승인·학적 변경 불가",
                content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "E80/E99: DB·시스템 오류. 변경 요청은 감사·멱등 응답과 함께 rollback",
                content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class)))
})
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    @Operation(operationId = "searchWithdrawals", summary = "자퇴 신청 목록 조회",
            description = "STUDENT는 본인, PROFESSOR는 현재 배정 지도학생, ADMIN은 전체 신청을 조회합니다. "
                    + "취소·반려를 포함한 모든 상태를 생성 시각·ID 내림차순으로 조회합니다. 결과 없음은 빈 items와 totalCount 0입니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "00: 조회 성공")
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<WithdrawalResponseDTO>>> search(
            @ParameterObject @Valid @ModelAttribute WithdrawalSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        PageRequest pageable = PageRequest.of(request.resolvedPage() - 1, request.resolvedSize(),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return ResponseEntity.ok(GlobalResponseDTO.success(withdrawalService.search(request, currentUser, pageable)));
    }

    @Operation(operationId = "getWithdrawal", summary = "자퇴 신청 상세 조회",
            description = "학생 본인, 현재 배정 지도교수, ADMIN만 조회합니다. 교수/관리자의 ID는 Academic users.id입니다. "
                    + "Payment에 필요한 id·studentId·status·effectiveDate 계약은 유지하며 시스템 호출 인증은 별도 연동 항목입니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "00: 조회 성공")
    @GetMapping("/{withdrawalId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalResponseDTO<WithdrawalResponseDTO>> get(
            @Parameter(description = "자퇴 신청 ID", example = "1") @Positive @PathVariable Long withdrawalId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(withdrawalService.get(withdrawalId, currentUser)));
    }

    @Operation(operationId = "createWithdrawal", summary = "자퇴 신청",
            description = "STUDENT 본인만 연중 신청합니다. 재학/휴학 상태, 지도교수 배정, 진행 중 신청 없음이 필요합니다. "
                    + "희망일은 선택이며 입력 시 신청 당일(KST) 또는 미래 날짜입니다. 성공 시 PENDING, 학적은 유지합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "201", description = "00: 신청 성공 또는 저장된 성공 응답 재생")
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponseDTO<WithdrawalResponseDTO>> create(
            @Valid @RequestBody WithdrawalCreateRequestDTO request,
            @Parameter(description = "요청별 고유 키. 1~100자, 공백 불가. 같은 요청 재전송에는 같은 키 사용",
                    required = true, schema = @Schema(minLength = 1, maxLength = 100), example = "withdrawal-create-001")
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponseDTO.success(
                withdrawalService.create(request, key, currentUser, WithdrawalAuditContext.from(httpRequest))
        ));
    }

    @Operation(operationId = "reviewWithdrawalByAdvisor", summary = "지도교수 자퇴 신청 검토",
            description = "현재 배정 지도교수(PROFESSOR)만 PENDING 신청을 ADVISOR_APPROVED 또는 ADVISOR_REJECTED로 처리합니다. "
                    + "반려 사유는 필수입니다. 희망일 전에도 검토할 수 있으며 학생 학적은 바꾸지 않습니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "00: 정상 처리 또는 저장된 성공 응답 재생")
    @PatchMapping("/{withdrawalId}/advisor-review")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<WithdrawalResponseDTO>> reviewByAdvisor(
            @Parameter(description = "자퇴 신청 ID", example = "1") @Positive @PathVariable Long withdrawalId,
            @Valid @RequestBody AdvisorWithdrawalReviewRequestDTO request,
            @Parameter(description = "요청별 고유 키. 1~100자, 공백 불가. 같은 요청 재전송에는 같은 키 사용",
                    required = true, schema = @Schema(minLength = 1, maxLength = 100), example = "withdrawal-advisor-001")
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                withdrawalService.reviewByAdvisor(withdrawalId, request, key, currentUser, WithdrawalAuditContext.from(httpRequest))
        ));
    }

    @Operation(operationId = "reviewWithdrawalByAdmin", summary = "관리자 자퇴 신청 최종 검토",
            description = "ADMIN만 ADVISOR_APPROVED 신청을 APPROVED 또는 REJECTED로 처리합니다. 승인 직전에도 재학/휴학 상태를 확인합니다. "
                    + "승인은 희망일 당일 이후에만 가능하고 effectiveDate는 실제 승인 당일(KST)이어야 합니다. "
                    + "승인 시 학적을 WITHDRAWN으로 변경하고 학적 이력·감사·멱등 응답을 같은 transaction에 저장합니다. "
                    + "같은 학생의 PENDING 휴·복학 신청은 자동 취소하며 자동 취소·감사 실패 시 자퇴 승인도 함께 롤백합니다. "
                    + "이미 승인·반려·취소된 휴·복학 신청과 증빙은 보존합니다. "
                    + "같은 학생의 PENDING 제적 후보가 있으면 최종 승인을 차단합니다. 후보 취소 후 다시 승인해야 합니다. "
                    + "반려 사유는 필수이며 희망일 전 반려는 가능합니다. 자동 승인·소급·일반 승인 취소는 제공하지 않습니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "00: 정상 처리 또는 저장된 성공 응답 재생")
    @PatchMapping("/{withdrawalId}/final-review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<WithdrawalResponseDTO>> reviewByAdmin(
            @Parameter(description = "자퇴 신청 ID", example = "1") @Positive @PathVariable Long withdrawalId,
            @Valid @RequestBody FinalWithdrawalReviewRequestDTO request,
            @Parameter(description = "요청별 고유 키. 1~100자, 공백 불가. 같은 요청 재전송에는 같은 키 사용",
                    required = true, schema = @Schema(minLength = 1, maxLength = 100), example = "withdrawal-final-001")
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                withdrawalService.reviewByAdmin(withdrawalId, request, key, currentUser, WithdrawalAuditContext.from(httpRequest))
        ));
    }

    @Operation(operationId = "cancelWithdrawal", summary = "본인 자퇴 신청 취소",
            description = "STUDENT 본인의 PENDING/ADVISOR_APPROVED 신청만 CANCELLED로 변경합니다. 취소 사유는 필수입니다. "
                    + "지도교수 검토 이력은 보존하며 학생 학적과 학적 변경 이력은 변경하지 않습니다. 승인·반려 후 취소는 불가합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "00: 취소 성공 또는 저장된 성공 응답 재생")
    @PatchMapping("/{withdrawalId}/status")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponseDTO<WithdrawalResponseDTO>> cancel(
            @Parameter(description = "자퇴 신청 ID", example = "1") @Positive @PathVariable Long withdrawalId,
            @Valid @RequestBody WithdrawalCancelRequestDTO request,
            @Parameter(description = "요청별 고유 키. 1~100자, 공백 불가. 같은 요청 재전송에는 같은 키 사용",
                    required = true, schema = @Schema(minLength = 1, maxLength = 100), example = "withdrawal-cancel-001")
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(withdrawalService.cancel(
                withdrawalId, request, key, currentUser, WithdrawalAuditContext.from(httpRequest))));
    }
}
