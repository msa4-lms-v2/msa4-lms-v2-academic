package com.msa4lmsv2academic.domain.lecture.controller;

import com.msa4lmsv2academic.domain.lecture.request.LectureSyllabusUpdateRequestDTO;
import com.msa4lmsv2academic.domain.lecture.response.LectureSyllabusResponseDTO;
import com.msa4lmsv2academic.domain.lecture.service.LectureSyllabusService;
import com.msa4lmsv2academic.global.response.GlobalRes;
import com.msa4lmsv2academic.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/academic/classes")
@Tag(name = "LectureSyllabus", description = "교수 강의계획서 작성·수정 및 관리자 조회 API")
public class LectureSyllabusController {

    private final LectureSyllabusService lectureSyllabusService;

    @Operation(
            summary = "강의계획서 조회",
            description = "담당 교수는 본인 강의계획서를 조회하고 관리자는 모든 강의계획서를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 강의 ID",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "403", description = "조회 권한 없음",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "404", description = "강의 없음",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class)))
    })
    @GetMapping("/{classId}/syllabus")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalRes<LectureSyllabusResponseDTO>> get(
            @Parameter(description = "강의 ID", example = "101", required = true)
            @PathVariable @Positive(message = "classId는 양수여야 합니다.") Long classId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalRes.success(lectureSyllabusService.get(classId, currentUser)));
    }

    @Operation(
            summary = "강의계획서 작성·수정",
            description = "담당 교수가 개설 상태인 본인 강의의 강의계획서를 작성하거나 전체 수정합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "작성·수정 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "403", description = "담당 교수 권한 없음",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "404", description = "강의 없음",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "409", description = "수정할 수 없는 강의 상태",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class)))
    })
    @PutMapping("/{classId}/syllabus")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalRes<LectureSyllabusResponseDTO>> update(
            @Parameter(description = "강의 ID", example = "101", required = true)
            @PathVariable @Positive(message = "classId는 양수여야 합니다.") Long classId,
            @Valid @RequestBody LectureSyllabusUpdateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @Parameter(description = "분산 추적 및 감사 로그 요청 ID", example = "01JABCDEF1234567890")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(GlobalRes.success(lectureSyllabusService.update(
                classId,
                request,
                currentUser,
                requestId,
                httpServletRequest.getRemoteAddr()
        )));
    }
}
