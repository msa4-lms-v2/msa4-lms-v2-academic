package com.msa4lmsv2academic.domain.enrollment.controller;

import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

import com.msa4lmsv2academic.domain.enrollment.request.PrerequisiteRetakeRuleCreateRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.request.PrerequisiteRetakeRuleSearchRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.request.PrerequisiteRetakeRuleStatusRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.request.PrerequisiteRetakeRuleUpdateRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.response.PrerequisiteRetakeRuleCriteriaResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.response.PrerequisiteRetakeRuleQueryResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.service.PrerequisiteRetakeRuleCreateResult;
import com.msa4lmsv2academic.domain.enrollment.service.PrerequisiteRetakeRuleService;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
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
        name = "Prerequisite and Retake Rules",
        description = "선수과목 기준 관리와 학생별 선수과목·재수강 조건 판정 API"
)
@Validated
@RestController
@RequestMapping("/api/academic/enrollments/prerequisite-retake-rules")
@RequiredArgsConstructor
public class PrerequisiteRetakeRuleController {

    private final PrerequisiteRetakeRuleService prerequisiteRetakeRuleService;

    @Operation(
            operationId = "searchPrerequisiteRetakeRules",
            summary = "선수과목 기준·개인별 재수강 조건 조회",
            description = "학생은 courseId로 본인, 교수는 courseId와 studentId로 현재 담당 강의·지도 학생·"
                    + "소속 학과 범위, 관리자는 전체 범위의 조건을 조회합니다. 관리자가 studentId를 "
                    + "생략하면 기준정보 목록만 페이징하고 evaluation은 null입니다. 직접 선수과목만 "
                    + "판정하며 F는 유효 수강 이력에서 제외합니다. C+·C·D+·D는 재수강을 허용하지만 "
                    + "B 이상 성적 이력이 하나라도 있으면 차단합니다. ruleSatisfied는 이 두 조건만의 "
                    + "결과이며 정원·시간표·최대학점·학적 상태까지 포함한 전체 수강 가능 여부는 아닙니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "200", description = "기준정보 및 선택적 개인별 판정 조회 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PrerequisiteRetakeRuleQueryResponseDTO>> search(
            @ParameterObject @Valid @ModelAttribute PrerequisiteRetakeRuleSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                prerequisiteRetakeRuleService.search(request, currentUser)
        ));
    }

    @Operation(
            operationId = "getPrerequisiteRetakeRule",
            summary = "선수과목 기준 상세 조회",
            description = "ADMIN이 대상 교과목과 직접 선수과목의 기준 상세와 활성 상태를 조회합니다.",
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
    public ResponseEntity<GlobalResponseDTO<PrerequisiteRetakeRuleCriteriaResponseDTO>> get(
            @Parameter(description = "선수과목 기준 ID", example = "4", required = true)
            @PathVariable @Positive(message = "ruleId는 양수여야 합니다.") Long ruleId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                prerequisiteRetakeRuleService.get(ruleId, currentUser)
        ));
    }

    @Operation(
            operationId = "createPrerequisiteRetakeRule",
            summary = "선수과목 기준 등록",
            description = "ADMIN이 대상 교과목의 직접 선수과목을 등록합니다. 자기 참조·중복·직접 및 "
                    + "간접 순환 관계를 차단합니다. 동일한 비활성 기준이 있으면 새 행을 만들지 않고 "
                    + "재활성화하며 200을 반환합니다. 신규 등록은 201을 반환하고 모든 변경은 사유와 전후 값을 "
                    + "감사 로그에 남깁니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "200", description = "기존 비활성 기준 재활성화")
            @ApiResponse(responseCode = "201", description = "신규 기준 등록")
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
    public ResponseEntity<GlobalResponseDTO<PrerequisiteRetakeRuleCriteriaResponseDTO>> create(
            @Valid @RequestBody PrerequisiteRetakeRuleCreateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @Parameter(description = "분산 추적 및 감사 로그 요청 ID", example = "01JABCDEF1234567890")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        PrerequisiteRetakeRuleCreateResult result = prerequisiteRetakeRuleService.create(
                request,
                currentUser,
                requestId,
                httpServletRequest.getRemoteAddr()
        );
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(GlobalResponseDTO.success(result.response()));
    }

    @Operation(
            operationId = "updatePrerequisiteRetakeRule",
            summary = "선수과목 기준 전체 수정",
            description = "ADMIN이 기준의 대상·선수 교과목을 전체 수정합니다. 활성 기준은 수정 즉시 "
                    + "순환 관계를 재검증하고, 동일 값 요청은 변경·감사 로그 추가 없이 현재 데이터를 반환합니다.",
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
    public ResponseEntity<GlobalResponseDTO<PrerequisiteRetakeRuleCriteriaResponseDTO>> update(
            @PathVariable @Positive(message = "ruleId는 양수여야 합니다.") Long ruleId,
            @Valid @RequestBody PrerequisiteRetakeRuleUpdateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @Parameter(description = "분산 추적 및 감사 로그 요청 ID", example = "01JABCDEF1234567890")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                prerequisiteRetakeRuleService.update(
                        ruleId,
                        request,
                        currentUser,
                        requestId,
                        httpServletRequest.getRemoteAddr()
                )
        ));
    }

    @Operation(
            operationId = "changePrerequisiteRetakeRuleStatus",
            summary = "선수과목 기준 활성 상태 변경",
            description = "ADMIN이 기준을 물리 삭제하지 않고 활성화·비활성화합니다. 재활성화 시 순환 "
                    + "관계를 다시 검증하고, 동일 상태 요청은 변경·감사 로그 추가 없이 현재 데이터를 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "200", description = "상태 변경 성공 또는 동일 상태 요청")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PatchMapping("/{ruleId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PrerequisiteRetakeRuleCriteriaResponseDTO>> changeStatus(
            @PathVariable @Positive(message = "ruleId는 양수여야 합니다.") Long ruleId,
            @Valid @RequestBody PrerequisiteRetakeRuleStatusRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @Parameter(description = "분산 추적 및 감사 로그 요청 ID", example = "01JABCDEF1234567890")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                prerequisiteRetakeRuleService.changeStatus(
                        ruleId,
                        request,
                        currentUser,
                        requestId,
                        httpServletRequest.getRemoteAddr()
                )
        ));
    }
}
