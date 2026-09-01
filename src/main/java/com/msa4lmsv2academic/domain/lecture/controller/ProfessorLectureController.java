package com.msa4lmsv2academic.domain.lecture.controller;

import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

import com.msa4lmsv2academic.domain.lecture.request.ProfessorLectureSearchRequestDTO;
import com.msa4lmsv2academic.domain.lecture.response.ProfessorLectureResponseDTO;
import com.msa4lmsv2academic.domain.lecture.service.ProfessorLectureQueryService;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

@Tag(name = "Classes", description = "강의 조회 API")
@RestController
@RequestMapping("/api/academic/classes")
@RequiredArgsConstructor
public class ProfessorLectureController {

    private final ProfessorLectureQueryService professorLectureQueryService;

    @Operation(
            summary = "교수 담당 강의 조회",
            description = "로그인한 교수 본인의 담당 강의를 조회합니다. 결과가 없으면 빈 페이지를 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "200", description = "조회 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<ProfessorLectureResponseDTO>>> getMyLectures(
            @Valid @ModelAttribute ProfessorLectureSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                professorLectureQueryService.getMyLectures(request, currentUser)
        ));
    }
}
