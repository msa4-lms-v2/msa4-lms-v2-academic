package com.msa4lmsv2academic.domain.grade.controller;

import com.msa4lmsv2academic.domain.grade.request.GradeFinalizeRequestDTO;
import com.msa4lmsv2academic.domain.grade.request.GradeSaveRequestDTO;
import com.msa4lmsv2academic.domain.grade.response.GradeClassResponseDTO;
import com.msa4lmsv2academic.domain.grade.service.GradeManagementService;
import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.CustomResponseCode;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Grades", description = "교수·관리자 성적 입력 및 확정 API")
@Validated
@RestController
@RequestMapping("/api/academic/grades")
@RequiredArgsConstructor
public class GradeManagementController {

    private final GradeManagementService gradeManagementService;

    @Operation(
            operationId = "getManagedGrades",
            summary = "강의 성적 입력 현황 조회",
            description = "담당 교수 또는 관리자가 활성 수강생의 점수, 계산된 총점, 등급과 확정 상태를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "성적 입력 현황 조회 성공")
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
    public ResponseEntity<GlobalResponseDTO<GradeClassResponseDTO>> getGrades(
            @Parameter(description = "강의 ID", example = "101")
            @Positive @RequestParam Long classId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                gradeManagementService.getGrades(classId, currentUser)
        ));
    }

    @Operation(
            operationId = "createGradeDraft",
            summary = "성적 최초 입력 및 임시저장",
            description = "아직 성적이 없는 활성 수강생에게 일부 또는 전체 점수를 최초 입력합니다. "
                    + "네 점수가 모두 입력되면 강의 반영 비율로 총점과 등급을 계산하지만 상태는 DRAFT로 유지합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "201", description = "성적 임시저장 성공 또는 저장된 성공 응답 재생")
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
    @PreAuthorize("hasAnyRole('PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalResponseDTO<GradeClassResponseDTO>> createDraft(
            @Valid @RequestBody GradeSaveRequestDTO request,
            @Parameter(description = "논리적으로 같은 재시도에는 같은 키 사용", required = true,
                    schema = @Schema(type = "string", minLength = 1, maxLength = 100))
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gradeManagementService.createDraft(
                request, idempotencyKey, currentUser,
                httpRequest.getHeader("X-Request-Id"), httpRequest.getRemoteAddr()
        ));
    }

    @Operation(
            operationId = "updateGradeDraft",
            summary = "임시저장 성적 수정",
            description = "DRAFT 상태의 기존 성적을 수정합니다. null 점수는 해당 항목을 비운 것으로 저장되며, "
                    + "확정된 OPENED 성적은 이 API로 수정할 수 없습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "임시저장 성적 수정 성공 또는 저장된 성공 응답 재생")
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
    public ResponseEntity<GlobalResponseDTO<GradeClassResponseDTO>> updateDraft(
            @Valid @RequestBody GradeSaveRequestDTO request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(gradeManagementService.updateDraft(
                request, idempotencyKey, currentUser,
                httpRequest.getHeader("X-Request-Id"), httpRequest.getRemoteAddr()
        ));
    }

    @Operation(
            operationId = "finalizeGrades",
            summary = "강의 성적 확정",
            description = "모든 활성 수강생의 네 점수가 입력된 강의를 DRAFT에서 OPENED로 확정합니다. "
                    + "확정 후 일반 수정은 차단되며 변경 전후 상태와 처리자를 감사 로그에 기록합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "성적 확정 성공 또는 저장된 성공 응답 재생")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.DUPLICATE_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PatchMapping("/classes/{classId}/status")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalResponseDTO<GradeClassResponseDTO>> finalizeGrades(
            @Positive @PathVariable Long classId,
            @Valid @RequestBody GradeFinalizeRequestDTO request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(gradeManagementService.finalizeGrades(
                classId, request, idempotencyKey, currentUser,
                httpRequest.getHeader("X-Request-Id"), httpRequest.getRemoteAddr()
        ));
    }
}
