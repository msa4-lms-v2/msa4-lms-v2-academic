package com.msa4lmsv2academic.domain.infochange.controller;

import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

import com.msa4lmsv2academic.domain.infochange.request.InfoChangeRequestRejectRequestDTO;
import com.msa4lmsv2academic.domain.infochange.request.InfoChangeRequestSearchRequestDTO;
import com.msa4lmsv2academic.domain.infochange.request.ProfessorInfoChangeRequestCreateDTO;
import com.msa4lmsv2academic.domain.infochange.response.ProfessorInfoChangeRequestResponseDTO;
import com.msa4lmsv2academic.domain.infochange.service.ProfessorInfoChangeRequestService;
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

@Tag(name = "Professor Profile Change Requests", description = "교수 프로필 변경 신청·조회·취소와 관리자 심사 API")
@Validated
@RestController
@RequestMapping("/api/academic/professor-info-change-requests")
@RequiredArgsConstructor
public class ProfessorInfoChangeRequestController {

    private final ProfessorInfoChangeRequestService infoChangeRequestService;

    @Operation(
            operationId = "searchProfessorProfileChangeRequests",
            summary = "교수 프로필 변경 신청 목록 조회",
            description = "PROFESSOR는 본인 신청만, ADMIN은 전체 교수 신청을 조회합니다. 신청자 이름·상태·학과로 "
                    + "필터링하고 생성 시각 정렬 방향을 선택할 수 있으며 결과가 없으면 빈 items를 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "200", description = "조회 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<ProfessorInfoChangeRequestResponseDTO>>> search(
            @ParameterObject @Valid @ModelAttribute InfoChangeRequestSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(infoChangeRequestService.search(request, currentUser)));
    }

    @Operation(
            operationId = "getProfessorProfileChangeRequest",
            summary = "교수 프로필 변경 신청 상세 조회",
            description = "PROFESSOR 본인 또는 ADMIN이 신청 상세와 증빙 PDF·래스터 이미지·HWP/HWPX의 "
                    + "1일 유효 임시 URL을 조회합니다.",
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
    @GetMapping("/{requestId}")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalResponseDTO<ProfessorInfoChangeRequestResponseDTO>> get(
            @Parameter(description = "신청 ID", example = "1", required = true)
            @Positive @PathVariable Long requestId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(infoChangeRequestService.get(requestId, currentUser)));
    }

    @Operation(
            operationId = "createProfessorProfileChangeRequest",
            summary = "교수 프로필 변경 신청",
            description = "PROFESSOR가 이름·전화번호·이메일·주소·프로필 이미지 중 실제로 달라지는 항목을 신청합니다. "
                    + "프로필 이미지는 JPEG/PNG/GIF/WebP 5MB 이하, 증빙은 같은 이미지와 PDF/HWP/HWPX "
                    + "파일당 10MB 이하·최대 5개이며 전체 요청은 "
                    + "20MB 이하여야 합니다. 확장자·MIME 타입·실제 파일 형식을 모두 검증하고, 처리 대기 신청은 "
                    + "한 건만 허용됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "201", description = "신청 생성 성공")
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
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<ProfessorInfoChangeRequestResponseDTO>> create(
            @Valid @ModelAttribute ProfessorInfoChangeRequestCreateDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @Parameter(description = "분산 추적 및 감사 로그 요청 ID")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponseDTO.success(
                infoChangeRequestService.create(
                        request, currentUser, requestId, httpServletRequest.getRemoteAddr()
                )
        ));
    }

    @Operation(
            operationId = "approveProfessorProfileChangeRequest",
            summary = "교수 프로필 변경 신청 승인",
            description = "ADMIN이 REQUESTED 신청을 승인하고 Academic 사용자 프로필에 반영합니다. 이메일은 승인 "
                    + "직전에 다시 중복 검사하며 충돌하면 신청 상태를 유지합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "200", description = "승인 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.DUPLICATE_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PatchMapping("/{requestId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<ProfessorInfoChangeRequestResponseDTO>> approve(
            @Positive @PathVariable Long requestId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @RequestHeader(value = "X-Request-Id", required = false) String traceRequestId,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(infoChangeRequestService.approve(
                requestId, currentUser, traceRequestId, httpServletRequest.getRemoteAddr()
        )));
    }

    @Operation(
            operationId = "rejectProfessorProfileChangeRequest",
            summary = "교수 프로필 변경 신청 반려",
            description = "ADMIN이 REQUESTED 신청을 필수 반려 사유와 함께 반려합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "200", description = "반려 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.DUPLICATE_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PatchMapping("/{requestId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<ProfessorInfoChangeRequestResponseDTO>> reject(
            @Positive @PathVariable Long requestId,
            @Valid @RequestBody InfoChangeRequestRejectRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @RequestHeader(value = "X-Request-Id", required = false) String traceRequestId,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(infoChangeRequestService.reject(
                requestId, request, currentUser, traceRequestId, httpServletRequest.getRemoteAddr()
        )));
    }

    @Operation(
            operationId = "cancelProfessorProfileChangeRequest",
            summary = "교수 프로필 변경 신청 취소",
            description = "PROFESSOR가 본인의 REQUESTED 신청을 취소합니다. 신청과 파일은 삭제하지 않고 CANCELLED "
                    + "상태와 취소 시각을 보존합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "200", description = "취소 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.DUPLICATE_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PatchMapping("/{requestId}/cancel")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<ProfessorInfoChangeRequestResponseDTO>> cancel(
            @Positive @PathVariable Long requestId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @RequestHeader(value = "X-Request-Id", required = false) String traceRequestId,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(infoChangeRequestService.cancel(
                requestId, currentUser, traceRequestId, httpServletRequest.getRemoteAddr()
        )));
    }
}
