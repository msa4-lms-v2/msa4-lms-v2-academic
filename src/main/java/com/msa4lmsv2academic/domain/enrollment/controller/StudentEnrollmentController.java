package com.msa4lmsv2academic.domain.enrollment.controller;

import com.msa4lmsv2academic.domain.enrollment.request.StudentEnrollmentSearchRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.response.StudentEnrollmentResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.service.StudentEnrollmentQueryService;
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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Enrollments", description = "학생 본인 수강 조회 API")
@RestController
@RequestMapping("/api/academic/enrollments")
@RequiredArgsConstructor
public class StudentEnrollmentController {

    private final StudentEnrollmentQueryService studentEnrollmentQueryService;

    @Operation(
            summary = "학생 본인 강의 조회",
            description = "로그인한 학생의 활성 수강 내역을 학년도와 학기 조건으로 조회합니다. 결과가 없으면 빈 목록을 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "조회 조건 오류",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "학생 권한이 아님",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "학생 정보 없음",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class)))
    })
    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponseDTO<List<StudentEnrollmentResponseDTO>>> getMyEnrollments(
            @Valid @ModelAttribute StudentEnrollmentSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                studentEnrollmentQueryService.getMyEnrollments(request, currentUser)
        ));
    }
}
