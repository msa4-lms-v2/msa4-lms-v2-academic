package com.msa4lmsv2academic.domain.enrollment.controller;

import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

import com.msa4lmsv2academic.domain.enrollment.response.StudentEnrollmentCancellationResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.service.StudentEnrollmentCancellationService;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Enrollments", description = "학생 본인 수강신청·취소 API")
@Validated
@RestController
@RequestMapping("/api/academic/enrollments")
@RequiredArgsConstructor
public class StudentEnrollmentCancellationController {

    private final StudentEnrollmentCancellationService cancellationService;

    @Operation(
            operationId = "cancelMyEnrollment",
            summary = "학생 본인 수강 취소",
            description = "STUDENT 본인이 신청한 ACTIVE 수강을 해당 학기의 수강신청 기간 안에 취소합니다. 수강 상태를 CANCELLED로 변경하고 CANCEL 이력을 같은 트랜잭션에 저장합니다.",
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
    @DeleteMapping("/{enrollmentId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponseDTO<StudentEnrollmentCancellationResponseDTO>> cancel(
            @Parameter(description = "취소할 본인 수강신청 ID", example = "101", required = true)
            @Positive @PathVariable Long enrollmentId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                cancellationService.cancel(enrollmentId, currentUser)
        ));
    }
}
