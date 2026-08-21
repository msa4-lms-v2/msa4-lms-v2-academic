package com.msa4lmsv2academic.domain.professor.controller;

import com.msa4lmsv2academic.domain.professor.request.ProfessorSearchRequestDTO;
import com.msa4lmsv2academic.domain.professor.request.ProfessorUpdateRequestDTO;
import com.msa4lmsv2academic.domain.professor.response.ProfessorDetailResponseDTO;
import com.msa4lmsv2academic.domain.professor.response.ProfessorSummaryResponseDTO;
import com.msa4lmsv2academic.domain.professor.service.ProfessorManagementService;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Faculty Management", description = "관리자 교수 인사정보 조회·수정 API")
@Validated
@RestController
@RequestMapping("/api/academic/faculty-management")
@RequiredArgsConstructor
public class ProfessorManagementController {

    private final ProfessorManagementService professorManagementService;

    @Operation(
            summary = "교수 목록 조회",
            description = "ADMIN만 교수 목록을 페이지 조회합니다. 학과·임용 연도·동기화 계정 상태로 필터링하고 "
                    + "이름 또는 이메일을 부분 검색합니다. status는 Auth에서 동기화된 읽기 전용 복제본이므로 "
                    + "동기화 실패 시 Auth 원본과 일시적으로 다를 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공. 결과가 없으면 빈 items를 반환합니다."),
            @ApiResponse(responseCode = "400", description = "잘못된 페이지·검색 조건",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class)))
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<ProfessorSummaryResponseDTO>>> searchProfessors(
            @ParameterObject @Valid @ModelAttribute ProfessorSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                professorManagementService.searchProfessors(request, currentUser)
        ));
    }

    @Operation(
            summary = "교수 상세 조회",
            description = "ADMIN만 Professor 엔티티 ID로 교수 상세 인사정보를 조회합니다. 계정 상태는 Auth에서 "
                    + "동기화된 읽기 전용 복제본이며 Academic에서는 변경할 수 없습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 교수 ID",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "교수 없음",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class)))
    })
    @GetMapping("/{professorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<ProfessorDetailResponseDTO>> getProfessor(
            @Parameter(description = "Professor 엔티티 ID", example = "10", required = true)
            @PathVariable @Positive(message = "professorId는 양수여야 합니다.") Long professorId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                professorManagementService.getProfessor(professorId, currentUser)
        ));
    }

    @Operation(
            summary = "교수 인사정보 부분 수정",
            description = "ADMIN만 Professor 엔티티 ID로 활성 학과와 임용 연도를 부분 수정합니다. "
                    + "departmentId와 hireYear 중 최소 한 필드와 변경 사유가 필요합니다. 변경 시 관리자, 사유, "
                    + "변경 전후 인사정보를 감사 로그로 기록하며 같은 값이면 현재 상태만 반환하고 로그를 남기지 않습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공 또는 동일 값 요청"),
            @ApiResponse(responseCode = "400", description = "빈 PATCH, 비활성 학과, 잘못된 임용 연도 또는 사유",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "교수 또는 변경할 학과 없음",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class)))
    })
    @PatchMapping("/{professorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<ProfessorDetailResponseDTO>> updateProfessor(
            @Parameter(description = "Professor 엔티티 ID", example = "10", required = true)
            @PathVariable @Positive(message = "professorId는 양수여야 합니다.") Long professorId,
            @Valid @RequestBody ProfessorUpdateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @Parameter(description = "분산 추적 및 감사 로그 요청 ID", example = "01JABCDEF1234567890")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(professorManagementService.updateProfessor(
                professorId,
                request,
                currentUser,
                requestId,
                httpServletRequest.getRemoteAddr()
        )));
    }
}
