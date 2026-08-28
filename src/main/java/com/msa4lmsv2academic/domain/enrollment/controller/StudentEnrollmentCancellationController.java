package com.msa4lmsv2academic.domain.enrollment.controller;

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
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "취소 성공"),
            @ApiResponse(responseCode = "400", description = "E21: 수강신청 ID 형식 오류",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "E02/E04: 미인증 또는 잘못된 인증 정보",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "E03: 학생 외 역할의 취소",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "E10: 본인의 수강신청 없음",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "E11: 기간 밖 취소 또는 이미 취소된 수강",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/EnrollmentApplicationError"),
                            examples = @ExampleObject(value = """
                                    {"code":"E11","message":"이미 처리되었거나 현재 상태와 충돌합니다.",
                                     "data":{"reasons":[{"code":"ENROLLMENT_ALREADY_CANCELLED","message":"이미 취소된 수강입니다."}]}}
                                    """)))
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
