package com.msa4lmsv2academic.domain.lecture.controller;

import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

import com.msa4lmsv2academic.domain.lecture.request.LectureOpeningCreateRequestDTO;
import com.msa4lmsv2academic.domain.lecture.request.LectureOpeningCorrectionRequestDTO;
import com.msa4lmsv2academic.domain.lecture.request.LectureOpeningReviewRequestDTO;
import com.msa4lmsv2academic.domain.lecture.request.LectureOpeningSearchRequestDTO;
import com.msa4lmsv2academic.domain.lecture.response.LectureOpeningResponseDTO;
import com.msa4lmsv2academic.domain.lecture.service.LectureOpeningService;
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
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "LectureOpeningRequests", description = "교수 강의 개설 신청과 관리자 승인·반려 API")
@Validated
@RestController
@RequestMapping("/api/academic/classes")
@RequiredArgsConstructor
public class LectureOpeningController {

    private final LectureOpeningService lectureOpeningService;

    @Operation(
            summary = "강의 개설 신청 목록 조회",
            description = "교수는 본인의 신청만, 관리자는 전체 신청을 상태별로 조회합니다.",
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
    @GetMapping("/opening-requests")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<LectureOpeningResponseDTO>>> search(
            @ParameterObject @Valid @ModelAttribute LectureOpeningSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        LectureOpeningSearchRequestDTO resolved = request == null
                ? new LectureOpeningSearchRequestDTO(null, null, null)
                : request;
        int page = resolved.resolvedPage();
        int size = resolved.resolvedSize();
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<LectureOpeningResponseDTO> result = lectureOpeningService.search(
                resolved.status(),
                pageable,
                currentUser
        );
        PageResponseDTO<LectureOpeningResponseDTO> response = new PageResponseDTO<>(
                result.getContent(),
                result.getTotalElements(),
                page,
                size,
                result.hasNext()
        );
        return ResponseEntity.ok(GlobalResponseDTO.success(response));
    }

    @Operation(
            summary = "강의 개설 신청 상세 조회",
            description = "교수는 본인의 신청만, 관리자는 모든 신청을 조회합니다.",
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
    @GetMapping("/opening-requests/{requestId}")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalResponseDTO<LectureOpeningResponseDTO>> get(
            @Positive(message = "requestId는 양수여야 합니다.") @PathVariable Long requestId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(lectureOpeningService.get(requestId, currentUser)));
    }

    @Operation(
            summary = "교수 강의 개설 신청",
            description = "교수가 담당 예정 교과목의 학기·분반·시간·강의실·정원·평가비율·강의계획서를 입력해 신청합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "201", description = "신청 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.DUPLICATE_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PostMapping("/opening-requests")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<LectureOpeningResponseDTO>> create(
            @Valid @RequestBody LectureOpeningCreateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponseDTO.success(lectureOpeningService.create(request, currentUser)));
    }

    @Operation(
            summary = "교수 강의 개설 신청 보완",
            description = "교수가 본인의 처리 대기 강의 개설 신청 내용을 보완합니다. 승인 또는 반려된 신청은 변경할 수 없습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "200", description = "보완 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.DUPLICATE_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PatchMapping("/opening-requests/{requestId}")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<LectureOpeningResponseDTO>> update(
            @Positive(message = "requestId는 양수여야 합니다.") @PathVariable Long requestId,
            @Valid @RequestBody LectureOpeningCorrectionRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                lectureOpeningService.update(requestId, request, currentUser)
        ));
    }

    @Operation(
            summary = "관리자 강의 개설 승인·반려",
            description = "관리자가 처리 대기 신청을 검토하고 필요하면 개설 정보를 보정한 뒤 승인하거나 반려합니다. "
                    + "승인 시 강의와 시간표가 생성됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "200", description = "처리 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.DUPLICATE_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PatchMapping("/opening-approvals")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<LectureOpeningResponseDTO>> review(
            @Valid @RequestBody LectureOpeningReviewRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(lectureOpeningService.review(request, currentUser)));
    }
}
