package com.msa4lmsv2academic.domain.grade.controller;

import com.msa4lmsv2academic.domain.grade.request.GradeCorrectionHistorySearchRequestDTO;
import com.msa4lmsv2academic.domain.grade.request.GradeCorrectionRequestDTO;
import com.msa4lmsv2academic.domain.grade.response.GradeClassResponseDTO;
import com.msa4lmsv2academic.domain.grade.response.GradeCorrectionHistoryResponseDTO;
import com.msa4lmsv2academic.domain.grade.service.GradeCorrectionService;
import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.CustomResponseCode;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Grade Corrections", description = "공개 성적 정정 및 변경 이력 조회 API")
@RestController
@RequestMapping("/api/academic/grades/corrections")
@RequiredArgsConstructor
public class GradeCorrectionController {

    private final GradeCorrectionService gradeCorrectionService;

    @Operation(
            operationId = "correctOpenedGrades",
            summary = "공개 성적 정정",
            description = "담당 교수 또는 관리자가 OPENED 상태의 성적을 정정합니다. "
                    + "변경된 점수와 재계산된 총점·등급의 전후 값, 처리자, 사유와 처리시각을 이력으로 남깁니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "성적 정정 성공 또는 저장된 성공 응답 재생")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.DUPLICATE_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PatchMapping
    @PreAuthorize("hasAnyRole('PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalResponseDTO<GradeClassResponseDTO>> correct(
            @Valid @RequestBody GradeCorrectionRequestDTO request,
            @Parameter(description = "논리적으로 같은 재시도에는 같은 키 사용", required = true,
                    schema = @Schema(type = "string", minLength = 1, maxLength = 100))
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(gradeCorrectionService.correct(
                request,
                idempotencyKey,
                currentUser,
                httpRequest.getHeader("X-Request-Id"),
                httpRequest.getRemoteAddr()
        ));
    }

    @Operation(
            operationId = "getGradeCorrectionHistories",
            summary = "강의별 성적 정정 이력 조회",
            description = "담당 교수 또는 관리자가 강의별 정정 이력을 최신순으로 조회합니다. "
                    + "결과가 없으면 200과 빈 items를 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "성적 정정 이력 조회 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<GradeCorrectionHistoryResponseDTO>>> getHistories(
            @ParameterObject @Valid @ModelAttribute GradeCorrectionHistorySearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        PageRequest pageable = PageRequest.of(
                request.resolvedPage() - 1,
                request.resolvedSize(),
                Sort.by(Sort.Direction.DESC, "createdAt", "id")
        );
        Page<GradeCorrectionHistoryResponseDTO> result = gradeCorrectionService.getHistories(
                request, pageable, currentUser
        );
        return ResponseEntity.ok(GlobalResponseDTO.success(new PageResponseDTO<>(
                result.getContent(), result.getTotalElements(),
                result.getNumber() + 1, result.getSize(), result.hasNext()
        )));
    }
}
