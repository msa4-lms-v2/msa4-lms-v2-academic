package com.msa4lmsv2academic.domain.enrollment.controller;

import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

import com.msa4lmsv2academic.domain.enrollment.request.EnrollmentCartCreateRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.request.EnrollmentCartSearchRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.response.EnrollmentCartCreateResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.response.EnrollmentCartSummaryResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.service.EnrollmentCartService;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Enrollment Cart", description = "학생 본인 수강 장바구니 API")
@Validated
@RestController
@RequestMapping("/api/academic/enrollment-cart-items")
@RequiredArgsConstructor
public class EnrollmentCartController {

    private final EnrollmentCartService cartService;

    @Operation(
            operationId = "getMyEnrollmentCart",
            summary = "수강 장바구니 조회",
            description = "수강신청 기간과 관계없이 본인의 장바구니 강의, 예상 시간표와 예상 신청학점 합계를 조회합니다. 결과가 없으면 빈 목록과 0학점을 반환합니다.",
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
    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponseDTO<EnrollmentCartSummaryResponseDTO>> getMyCart(
            @Valid @ModelAttribute EnrollmentCartSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(cartService.getMyCart(request, currentUser)));
    }

    @Operation(
            operationId = "addMyEnrollmentCartItem",
            summary = "수강 장바구니 추가",
            description = "수강신청 기간에 본인의 장바구니에 개설 강의를 추가합니다. 좌석을 예약하지 않으며 실제 수강신청 때 모든 신청 규칙을 다시 검증합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "201", description = "추가 성공")
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
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponseDTO<EnrollmentCartCreateResponseDTO>> add(
            @Valid @RequestBody EnrollmentCartCreateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponseDTO.success(cartService.add(request, currentUser)));
    }

    @Operation(
            operationId = "removeMyEnrollmentCartItem",
            summary = "수강 장바구니 삭제",
            description = "수강신청 기간에 본인이 담은 장바구니 항목만 삭제합니다. 다른 학생의 항목은 노출하지 않고 E10으로 처리합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "200", description = "삭제 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.DUPLICATE_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @DeleteMapping("/{cartItemId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponseDTO<Void>> remove(
            @Parameter(description = "삭제할 본인 장바구니 항목 ID", example = "301", required = true)
            @Positive @PathVariable Long cartItemId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        cartService.remove(cartItemId, currentUser);
        return ResponseEntity.ok(GlobalResponseDTO.success());
    }
}
