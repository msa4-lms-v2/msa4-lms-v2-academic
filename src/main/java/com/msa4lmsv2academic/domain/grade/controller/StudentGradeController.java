package com.msa4lmsv2academic.domain.grade.controller;

import com.msa4lmsv2academic.domain.grade.request.StudentGradeSearchRequestDTO;
import com.msa4lmsv2academic.domain.grade.response.StudentGradeResponseDTO;
import com.msa4lmsv2academic.domain.grade.service.StudentGradeQueryService;
import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.CustomResponseCode;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@Tag(name = "Grades", description = "성적 관리 및 학생 본인 성적 조회 API")
@RestController
@RequestMapping("/api/academic/grades")
@RequiredArgsConstructor
public class StudentGradeController {

    private final StudentGradeQueryService studentGradeQueryService;

    @Operation(
            operationId = "getMyGrades",
            summary = "학생 본인 성적 조회",
            description = "로그인한 학생의 활성 수강 중 성적이 확정되고 해당 강의평가를 제출한 성적만 조회합니다. "
                    + "학년도·학기·교과목명 필터와 정렬을 지원하며 결과가 없으면 0점 요약과 빈 목록을 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "성적 조회 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponseDTO<StudentGradeResponseDTO>> getMyGrades(
            @Valid @ModelAttribute StudentGradeSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                studentGradeQueryService.getMyGrades(request, currentUser)
        ));
    }
}
