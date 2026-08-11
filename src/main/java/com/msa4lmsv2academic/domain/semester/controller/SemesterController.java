package com.msa4lmsv2academic.domain.semester.controller;

import com.msa4lmsv2academic.domain.semester.request.SemesterCreateRequestDTO;
import com.msa4lmsv2academic.domain.semester.request.SemesterSearchRequestDTO;
import com.msa4lmsv2academic.domain.semester.response.SemesterResponseDTO;
import com.msa4lmsv2academic.domain.semester.service.SemesterService;
import com.msa4lmsv2academic.global.response.GlobalRes;
import com.msa4lmsv2academic.global.response.PageRes;
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
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Semesters", description = "학기 등록·조회 API")
@Validated
@RestController
@RequestMapping("/api/academic/catalog/semesters")
@RequiredArgsConstructor
public class SemesterController {

    private final SemesterService semesterService;

    @Operation(
            summary = "학기 목록 조회",
            description = "STUDENT, PROFESSOR, ADMIN이 학년도, 학기 구분, 현재 학기 여부로 학기를 페이지 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공. 결과가 없으면 빈 items를 반환합니다."),
            @ApiResponse(responseCode = "400", description = "잘못된 검색 조건",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "403", description = "허용되지 않은 역할",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalRes<PageRes<SemesterResponseDTO>>> searchSemesters(
            @ParameterObject @Valid @ModelAttribute SemesterSearchRequestDTO request
    ) {
        return ResponseEntity.ok(GlobalRes.success(semesterService.searchSemesters(request)));
    }

    @Operation(
            summary = "학기 등록",
            description = "ADMIN만 학기를 등록할 수 있습니다. 동일 학년도·학기는 중복 등록할 수 없으며, "
                    + "현재 학기로 등록하면 기존 현재 학기를 자동 해제하고 모든 변경을 감사 로그에 기록합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "필수값 누락 또는 기간 순서 오류",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "409", description = "동일 학년도·학기 중복",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalRes<SemesterResponseDTO>> createSemester(
            @Valid @RequestBody SemesterCreateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @Parameter(description = "분산 추적 및 감사 로그 요청 ID", example = "01JABCDEF1234567890")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        SemesterResponseDTO response = semesterService.createSemester(
                request,
                currentUser,
                requestId,
                httpServletRequest.getRemoteAddr()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalRes.success(response));
    }
}
