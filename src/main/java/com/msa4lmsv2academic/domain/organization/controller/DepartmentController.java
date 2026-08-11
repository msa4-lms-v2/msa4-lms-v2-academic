package com.msa4lmsv2academic.domain.organization.controller;

import com.msa4lmsv2academic.domain.organization.request.DepartmentCreateRequestDTO;
import com.msa4lmsv2academic.domain.organization.request.DepartmentSearchRequestDTO;
import com.msa4lmsv2academic.domain.organization.request.DepartmentUpdateRequestDTO;
import com.msa4lmsv2academic.domain.organization.response.DepartmentResponseDTO;
import com.msa4lmsv2academic.domain.organization.service.DepartmentService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Departments", description = "학과 조회·등록·수정 API")
@Validated
@RestController
@RequestMapping("/api/academic/catalog/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(
            summary = "학과 목록 조회",
            description = "STUDENT와 PROFESSOR는 활성 단과대의 활성 학과만, ADMIN은 활성 여부에 따라 전체 학과를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 검색 조건", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "403", description = "권한 부족", content = @Content(schema = @Schema(implementation = GlobalRes.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalRes<PageRes<DepartmentResponseDTO>>> searchDepartments(
            @ParameterObject @Valid @ModelAttribute DepartmentSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalRes.success(departmentService.searchDepartments(request, currentUser)));
    }

    @Operation(
            summary = "학과 상세 조회",
            description = "ADMIN은 비활성 학과도 조회할 수 있습니다. STUDENT와 PROFESSOR에게 비활성 조직은 404로 처리합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 학과 ID", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "403", description = "권한 부족", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "404", description = "학과 없음 또는 일반 사용자에게 숨겨진 비활성 조직", content = @Content(schema = @Schema(implementation = GlobalRes.class)))
    })
    @GetMapping("/{departmentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalRes<DepartmentResponseDTO>> getDepartment(
            @PathVariable @Positive(message = "departmentId는 양수여야 합니다.") Long departmentId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalRes.success(departmentService.getDepartment(departmentId, currentUser)));
    }

    @Operation(
            summary = "학과 등록",
            description = "ADMIN만 학과를 등록할 수 있습니다. collegeId는 선택값이며 지정한 경우 활성 단과대여야 합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "404", description = "단과대 없음", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "409", description = "학과 코드 중복", content = @Content(schema = @Schema(implementation = GlobalRes.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalRes<DepartmentResponseDTO>> createDepartment(
            @Valid @RequestBody DepartmentCreateRequestDTO request
    ) {
        DepartmentResponseDTO response = departmentService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalRes.success(response));
    }

    @Operation(
            summary = "학과 부분 수정",
            description = "ADMIN만 name과 active를 부분 수정할 수 있으며 code와 collegeId는 변경할 수 없습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "빈 PATCH 또는 잘못된 요청", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요", content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "404", description = "학과 없음", content = @Content(schema = @Schema(implementation = GlobalRes.class)))
    })
    @PatchMapping("/{departmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalRes<DepartmentResponseDTO>> updateDepartment(
            @PathVariable @Positive(message = "departmentId는 양수여야 합니다.") Long departmentId,
            @Valid @RequestBody DepartmentUpdateRequestDTO request
    ) {
        return ResponseEntity.ok(GlobalRes.success(departmentService.updateDepartment(departmentId, request)));
    }
}
