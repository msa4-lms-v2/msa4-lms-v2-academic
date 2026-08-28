package com.msa4lmsv2academic.domain.grade.controller;

import com.msa4lmsv2academic.domain.grade.request.RetakeGradeReflectionRequestDTO;
import com.msa4lmsv2academic.domain.grade.response.RetakeGradeReflectionResponseDTO;
import com.msa4lmsv2academic.domain.grade.service.RetakeGradeReflectionService;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Grades", description = "성적 및 재수강 성적 반영 API")
@Validated
@RestController
@RequestMapping("/api/academic/grades")
@RequiredArgsConstructor
public class RetakeGradeReflectionController {

    private final RetakeGradeReflectionService reflectionService;

    @Operation(
            operationId = "reflectRetakeGrade",
            summary = "재수강 성적 반영",
            description = "ADMIN이 같은 학생·교과목의 가장 최근 ACTIVE·OPENED 성적을 재수강 성적으로 반영합니다. "
                    + "기존 수강 성적은 보존하고 최신 성적만 학점·GPA 계산에 사용하며, 학기별 요약과 변경 이력을 "
                    + "같은 트랜잭션에 저장합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재수강 성적 반영 성공"),
            @ApiResponse(responseCode = "400", description = "E21: ID 또는 사유 입력 오류",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "E02/E04: 미인증 또는 잘못된 인증 정보",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "E03: 관리자 외 역할의 요청",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "E10: 수강 또는 관리자 정보 없음",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "E11: 상태·성적·최신 수강·중복 반영 충돌",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class)))
    })
    @PatchMapping("/{enrollmentId}/retake-reflection")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<RetakeGradeReflectionResponseDTO>> reflect(
            @Parameter(description = "재수강 성적을 반영할 수강 ID", example = "302", required = true)
            @Positive @PathVariable Long enrollmentId,
            @Valid @RequestBody RetakeGradeReflectionRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                reflectionService.reflect(enrollmentId, request, currentUser)
        ));
    }
}
