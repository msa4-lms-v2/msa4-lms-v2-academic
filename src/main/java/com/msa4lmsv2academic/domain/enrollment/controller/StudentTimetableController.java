package com.msa4lmsv2academic.domain.enrollment.controller;

import com.msa4lmsv2academic.domain.enrollment.request.StudentTimetableSearchRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.response.StudentTimetableResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.service.StudentTimetableQueryService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Timetables", description = "학생 본인 시간표 조회 API")
@RestController
@RequestMapping("/api/academic/timetables")
@RequiredArgsConstructor
public class StudentTimetableController {

    private final StudentTimetableQueryService timetableQueryService;

    @Operation(
            summary = "학생 본인 시간표 조회",
            description = "로그인한 학생의 지정 학년도·학기 활성 수강 강의와 요일·교시를 조회합니다. "
                    + "결과가 없으면 총 학점 0과 빈 목록을 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "학년도·학기 조회 조건 오류",
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
    public ResponseEntity<GlobalResponseDTO<StudentTimetableResponseDTO>> getMyTimetable(
            @Valid @ModelAttribute StudentTimetableSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                timetableQueryService.getMyTimetable(request, currentUser)
        ));
    }
}
