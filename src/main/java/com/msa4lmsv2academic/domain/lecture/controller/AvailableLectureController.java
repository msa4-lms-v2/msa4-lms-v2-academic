package com.msa4lmsv2academic.domain.lecture.controller;

import com.msa4lmsv2academic.domain.lecture.request.AvailableLectureSearchRequestDTO;
import com.msa4lmsv2academic.domain.lecture.response.ProfessorLectureResponseDTO;
import com.msa4lmsv2academic.domain.lecture.service.AvailableLectureQueryService;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
public class AvailableLectureController {

    private final AvailableLectureQueryService service;

    @Operation(
            summary = "학생 수강신청 대상 강의 조회",
            description = "학생이 학기와 검색 조건에 맞는 개설 강의를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/available")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<ProfessorLectureResponseDTO>>> search(
            @Valid @ModelAttribute AvailableLectureSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.search(request, currentUser)));
    }
}
