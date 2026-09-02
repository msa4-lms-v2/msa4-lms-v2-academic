package com.msa4lmsv2academic.domain.admission.controller;

import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

import com.msa4lmsv2academic.domain.admission.request.AdmissionCandidateCreateRequestDTO;
import com.msa4lmsv2academic.domain.admission.request.AdmissionCandidateSearchRequestDTO;
import com.msa4lmsv2academic.domain.admission.request.AdmissionCandidateStatusRequestDTO;
import com.msa4lmsv2academic.domain.admission.request.AdmissionCandidateUpdateRequestDTO;
import com.msa4lmsv2academic.domain.admission.response.AdmissionCandidateDetailResponseDTO;
import com.msa4lmsv2academic.domain.admission.response.AdmissionCandidateSummaryResponseDTO;
import com.msa4lmsv2academic.domain.admission.service.AdmissionCandidateService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admission Candidates", description = "관리자 입학 예정자 등록·조회·수정 API")
@Validated
@RestController
@RequestMapping("/api/academic/admission-candidates")
@RequiredArgsConstructor
public class AdmissionCandidateController {

    private final AdmissionCandidateService admissionCandidateService;

    @Operation(
            operationId = "searchAdmissionCandidates",
            summary = "입학 예정자 목록 조회",
            description = "ADMIN만 입학 예정자 목록을 조회합니다. 이름·수험번호 검색, 학과·입학연도·상태 필터와 "
                    + "정렬·1-based 페이징을 지원합니다. 목록에는 생년월일·이메일·전화번호·주소를 포함하지 않으며 "
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
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<AdmissionCandidateSummaryResponseDTO>>> searchCandidates(
            @ParameterObject @Valid @ModelAttribute AdmissionCandidateSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                admissionCandidateService.searchCandidates(request, currentUser)
        ));
    }

    @Operation(
            operationId = "getAdmissionCandidate",
            summary = "입학 예정자 상세 조회",
            description = "ADMIN만 입학 예정자 상세를 조회합니다. 수정 업무에 필요한 생년월일·이메일·전화번호·주소와 "
                    + "학과·입학연도·등록 상태·프로비저닝 연결 결과를 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "200", description = "조회 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping("/{candidateId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<AdmissionCandidateDetailResponseDTO>> getCandidate(
            @Parameter(description = "입학 예정자 ID", example = "15", required = true)
            @PathVariable @Positive(message = "candidateId는 양수여야 합니다.") Long candidateId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                admissionCandidateService.getCandidate(candidateId, currentUser)
        ));
    }

    @Operation(
            operationId = "createAdmissionCandidate",
            summary = "입학 예정자 등록",
            description = "ADMIN만 합격 자료의 고유 수험번호, 이름, 생년월일, 활성 학과와 입학 예정 연도로 "
                    + "입학 예정자를 REGISTERED 상태로 등록합니다. 이메일·전화번호·주소는 선택값이며 "
                    + "이 단계에서는 Auth 계정, Academic 사용자·학생 또는 학번을 생성하지 않습니다.",
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
    public ResponseEntity<GlobalResponseDTO<AdmissionCandidateDetailResponseDTO>> createCandidate(
            @Valid @RequestBody AdmissionCandidateCreateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @Parameter(description = "분산 추적 및 감사 로그 요청 ID", example = "01JABCDEF1234567890")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponseDTO.success(
                admissionCandidateService.createCandidate(
                        request, currentUser, requestId, httpServletRequest.getRemoteAddr()
                )
        ));
    }

    @Operation(
            operationId = "updateAdmissionCandidate",
            summary = "입학 예정자 부분 수정",
            description = "ADMIN만 REGISTERED 상태의 이름·생년월일·이메일·전화번호·주소·활성 학과·입학연도를 "
                    + "부분 수정합니다. applicationNumber는 변경할 수 없습니다. 생략 또는 null인 필드는 유지하고 "
                    + "이메일·전화번호·주소의 공백 문자열은 null로 삭제합니다. 일반 수정에는 사유를 받지 않습니다.",
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
    @PatchMapping("/{candidateId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<AdmissionCandidateDetailResponseDTO>> updateCandidate(
            @Parameter(description = "입학 예정자 ID", example = "15", required = true)
            @PathVariable @Positive(message = "candidateId는 양수여야 합니다.") Long candidateId,
            @Valid @RequestBody AdmissionCandidateUpdateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @Parameter(description = "분산 추적 및 감사 로그 요청 ID", example = "01JABCDEF1234567890")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                admissionCandidateService.updateCandidate(
                        candidateId, request, currentUser, requestId, httpServletRequest.getRemoteAddr()
                )
        ));
    }

    @Operation(
            operationId = "changeAdmissionCandidateStatus",
            summary = "입학 예정자 상태 변경",
            description = "ADMIN만 입학 예정자를 CONFIRMED 또는 CANCELLED로 변경합니다. 상태 변경 사유가 필수이며 "
                    + "동일 상태 재요청은 현재 데이터를 200으로 반환하고 감사 로그를 추가하지 않습니다. "
                    + "PROVISIONED는 향후 Auth 프로비저닝 성공 결과로만 변경할 수 있습니다.",
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
    @PatchMapping("/{candidateId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<AdmissionCandidateDetailResponseDTO>> changeStatus(
            @Parameter(description = "입학 예정자 ID", example = "15", required = true)
            @PathVariable @Positive(message = "candidateId는 양수여야 합니다.") Long candidateId,
            @Valid @RequestBody AdmissionCandidateStatusRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @Parameter(description = "분산 추적 및 감사 로그 요청 ID", example = "01JABCDEF1234567890")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                admissionCandidateService.changeStatus(
                        candidateId, request, currentUser, requestId, httpServletRequest.getRemoteAddr()
                )
        ));
    }
}
