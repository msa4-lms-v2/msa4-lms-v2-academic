package com.msa4lmsv2academic.domain.enrollment.controller;

import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

import com.msa4lmsv2academic.domain.enrollment.request.EnrollmentCreditLimitRuleCreateRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.request.EnrollmentCreditLimitRuleSearchRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.request.EnrollmentCreditLimitRuleStatusRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.request.EnrollmentCreditLimitRuleUpdateRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.response.EnrollmentCreditLimitRuleResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.service.EnrollmentCreditLimitRuleService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Enrollment Credit Limit Rules",
        description = "관리자 학기별 최대 신청학점 기준정보 API"
)
@Validated
@RestController
@RequestMapping("/api/academic/enrollments/credit-limit-rules")
@RequiredArgsConstructor
public class EnrollmentCreditLimitRuleController {

    private final EnrollmentCreditLimitRuleService enrollmentCreditLimitRuleService;

    @Operation(
            operationId = "searchEnrollmentCreditLimitRules",
            summary = "최대 신청학점 규칙 목록 조회",
            description = "ADMIN이 학년도·학기·활성 상태로 최대 신청학점 규칙을 검색하고 정렬합니다. "
                    + "결과가 없으면 빈 items를 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "200", description = "조회 성공 또는 빈 목록")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<EnrollmentCreditLimitRuleResponseDTO>>> search(
            @ParameterObject @Valid @ModelAttribute EnrollmentCreditLimitRuleSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                enrollmentCreditLimitRuleService.search(request, currentUser)
        ));
    }

    @Operation(
            operationId = "getEnrollmentCreditLimitRule",
            summary = "최대 신청학점 규칙 상세 조회",
            description = "ADMIN이 학기별 최대 신청학점, 수강신청 기간과 활성 상태를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "200", description = "상세 조회 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping("/{ruleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<EnrollmentCreditLimitRuleResponseDTO>> get(
            @Parameter(description = "최대 신청학점 규칙 ID", example = "5", required = true)
            @PathVariable @Positive(message = "ruleId는 양수여야 합니다.") Long ruleId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                enrollmentCreditLimitRuleService.get(ruleId, currentUser)
        ));
    }

    @Operation(
            operationId = "createEnrollmentCreditLimitRule",
            summary = "최대 신청학점 규칙 등록",
            description = "ADMIN이 수강신청 시작 전 학기에 1~30학점의 기준을 등록합니다. 신규 규칙은 "
                    + "즉시 활성화되며 같은 학기의 활성·비활성 규칙이 이미 있으면 중복으로 차단합니다. "
                    + "등록 사유와 변경 후 값은 같은 transaction에서 감사 로그에 저장합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "201", description = "등록 성공")
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<EnrollmentCreditLimitRuleResponseDTO>> create(
            @Valid @RequestBody EnrollmentCreditLimitRuleCreateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @Parameter(description = "분산 추적 및 감사 로그 요청 ID", example = "01JABCDEF1234567890")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponseDTO.success(
                enrollmentCreditLimitRuleService.create(
                        request,
                        currentUser,
                        requestId,
                        httpServletRequest.getRemoteAddr()
                )
        ));
    }

    @Operation(
            operationId = "updateEnrollmentCreditLimitRule",
            summary = "최대 신청학점 규칙 전체 수정",
            description = "ADMIN이 수강신청 시작 전에 최대 신청학점과 변경 사유를 전체 수정합니다. "
                    + "semesterId는 변경할 수 없고 동일 값 요청은 DB 변경과 감사 로그 추가 없이 현재 값을 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "200", description = "수정 성공 또는 동일 값 요청")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.DUPLICATE_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PutMapping("/{ruleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<EnrollmentCreditLimitRuleResponseDTO>> update(
            @PathVariable @Positive(message = "ruleId는 양수여야 합니다.") Long ruleId,
            @Valid @RequestBody EnrollmentCreditLimitRuleUpdateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @Parameter(description = "분산 추적 및 감사 로그 요청 ID", example = "01JABCDEF1234567890")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                enrollmentCreditLimitRuleService.update(
                        ruleId,
                        request,
                        currentUser,
                        requestId,
                        httpServletRequest.getRemoteAddr()
                )
        ));
    }

    @Operation(
            operationId = "changeEnrollmentCreditLimitRuleStatus",
            summary = "최대 신청학점 규칙 활성 상태 변경",
            description = "ADMIN이 수강신청 시작 전에 규칙을 물리 삭제하지 않고 활성화·비활성화합니다. "
                    + "동일 상태 요청은 DB 변경과 감사 로그 추가 없이 현재 값을 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "200", description = "상태 변경 성공 또는 동일 상태 요청")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.DUPLICATE_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PatchMapping("/{ruleId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<EnrollmentCreditLimitRuleResponseDTO>> changeStatus(
            @PathVariable @Positive(message = "ruleId는 양수여야 합니다.") Long ruleId,
            @Valid @RequestBody EnrollmentCreditLimitRuleStatusRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @Parameter(description = "분산 추적 및 감사 로그 요청 ID", example = "01JABCDEF1234567890")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                enrollmentCreditLimitRuleService.changeStatus(
                        ruleId,
                        request,
                        currentUser,
                        requestId,
                        httpServletRequest.getRemoteAddr()
                )
        ));
    }
}
