package com.msa4lmsv2academic.domain.graduation.controller;

import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

import com.msa4lmsv2academic.domain.graduation.request.GraduationRequirementCreateRequestDTO;
import com.msa4lmsv2academic.domain.graduation.request.GraduationRequirementSearchRequestDTO;
import com.msa4lmsv2academic.domain.graduation.request.GraduationRequirementUpdateRequestDTO;
import com.msa4lmsv2academic.domain.graduation.response.GraduationRequirementResponseDTO;
import com.msa4lmsv2academic.domain.graduation.service.GraduationRequirementService;
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

@Tag(name = "Graduation Requirements", description = "관리자 졸업 학점요건 기준정보 API")
@Validated
@RestController
@RequestMapping("/api/academic/catalog/graduation-requirements")
@RequiredArgsConstructor
public class GraduationRequirementController {

    private final GraduationRequirementService graduationRequirementService;

    @Operation(
            operationId = "searchGraduationRequirements",
            summary = "졸업 학점요건 목록 조회",
            description = "ADMIN이 학과명·코드, 학과 ID, 입학연도로 졸업 학점요건을 검색하고 정렬합니다. "
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
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<GraduationRequirementResponseDTO>>> search(
            @ParameterObject @Valid @ModelAttribute GraduationRequirementSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                graduationRequirementService.search(request, currentUser)
        ));
    }

    @Operation(
            operationId = "getGraduationRequirement",
            summary = "졸업 학점요건 상세 조회",
            description = "ADMIN이 학과·입학연도별 전공·교양·총학점 기준을 조회합니다.",
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
    @GetMapping("/{requirementId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<GraduationRequirementResponseDTO>> get(
            @Parameter(description = "졸업요건 ID", example = "10", required = true)
            @PathVariable @Positive(message = "requirementId는 양수여야 합니다.") Long requirementId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                graduationRequirementService.get(requirementId, currentUser)
        ));
    }

    @Operation(
            operationId = "createGraduationRequirement",
            summary = "졸업 학점요건 등록",
            description = "ADMIN이 학과·입학연도별 전공·교양·총학점 기준을 등록합니다. 동일 학과·입학연도에는 "
                    + "한 건만 등록할 수 있으며 전공과 교양 기준의 합은 총학점을 초과할 수 없습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "201", description = "등록 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.DUPLICATE_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<GraduationRequirementResponseDTO>> create(
            @Valid @RequestBody GraduationRequirementCreateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @Parameter(description = "분산 추적 및 감사 로그 요청 ID", example = "01JABCDEF1234567890")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponseDTO.success(
                graduationRequirementService.create(
                        request, currentUser, requestId, httpServletRequest.getRemoteAddr()
                )
        ));
    }

    @Operation(
            operationId = "updateGraduationRequirement",
            summary = "졸업 학점요건 부분 수정",
            description = "ADMIN이 학과·입학연도와 전공·교양·총학점 기준을 부분 수정합니다. 변경 사유가 필수이고 "
                    + "requiredCourses는 별도 필수과목 진단 범위이므로 변경하지 않습니다. 동일 값 요청은 감사 로그를 "
                    + "추가하지 않고 현재 데이터를 반환합니다.",
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
    @PatchMapping("/{requirementId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<GraduationRequirementResponseDTO>> update(
            @Parameter(description = "졸업요건 ID", example = "10", required = true)
            @PathVariable @Positive(message = "requirementId는 양수여야 합니다.") Long requirementId,
            @Valid @RequestBody GraduationRequirementUpdateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @Parameter(description = "분산 추적 및 감사 로그 요청 ID", example = "01JABCDEF1234567890")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                graduationRequirementService.update(
                        requirementId, request, currentUser, requestId, httpServletRequest.getRemoteAddr()
                )
        ));
    }
}
