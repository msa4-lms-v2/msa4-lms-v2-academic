package com.msa4lmsv2academic.domain.professor.controller;

import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

import com.msa4lmsv2academic.domain.professor.response.ProfessorProfileResponseDTO;
import com.msa4lmsv2academic.domain.professor.service.ProfessorProfileService;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Professor Profiles", description = "교수 본인 프로필 API")
@RestController
@RequestMapping("/api/academic/professors")
@RequiredArgsConstructor
public class ProfessorProfileController {

    private final ProfessorProfileService professorProfileService;

    @Operation(
            operationId = "getMyProfessorProfile",
            summary = "교수 본인 프로필 조회",
            description = "PROFESSOR가 본인의 연락처·주소·소속·임용 연도와 프로필 이미지를 조회합니다. "
                    + "다른 사용자의 프로필은 조회할 수 없습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "200", description = "조회 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping("/me")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<ProfessorProfileResponseDTO>> getMyProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(professorProfileService.getMyProfile(currentUser)));
    }
}
