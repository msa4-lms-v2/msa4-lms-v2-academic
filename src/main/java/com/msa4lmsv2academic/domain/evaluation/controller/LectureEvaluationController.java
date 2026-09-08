package com.msa4lmsv2academic.domain.evaluation.controller;

import com.msa4lmsv2academic.domain.evaluation.request.LectureEvaluationSubmitRequestDTO;
import com.msa4lmsv2academic.domain.evaluation.request.ProfessorLectureEvaluationSearchRequestDTO;
import com.msa4lmsv2academic.domain.evaluation.response.LectureEvaluationResponseDTO;
import com.msa4lmsv2academic.domain.evaluation.response.ProfessorLectureEvaluationResponseDTO;
import com.msa4lmsv2academic.domain.evaluation.service.LectureEvaluationService;
import com.msa4lmsv2academic.domain.evaluation.service.ProfessorLectureEvaluationQueryService;
import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.CustomResponseCode;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Lecture Evaluations", description = "학생 강의평가 제출 및 교수 결과 조회 API")
@RestController
@RequestMapping("/api/academic/evaluations")
@RequiredArgsConstructor
public class LectureEvaluationController {

    private final LectureEvaluationService lectureEvaluationService;
    private final ProfessorLectureEvaluationQueryService professorLectureEvaluationQueryService;

    @Operation(
            operationId = "getMyLectureEvaluationResults",
            summary = "교수 강의평가 결과 조회",
            description = "교수가 본인의 담당 강의에 제출된 평가를 익명 통계와 서술형 의견으로 조회합니다. "
                    + "학생·수강 식별정보와 제출 시각은 노출하지 않으며 결과가 없으면 빈 통계를 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "강의평가 결과 조회 성공")
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
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<ProfessorLectureEvaluationResponseDTO>>> getMyResults(
            @Valid @ModelAttribute ProfessorLectureEvaluationSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                professorLectureEvaluationQueryService.getMyResults(request, currentUser)
        ));
    }

    @Operation(
            operationId = "submitLectureEvaluation",
            summary = "학생 강의평가 제출",
            description = "학생이 본인의 활성 수강 강의를 학기에 설정된 강의평가 기간 안에 한 번 평가합니다. "
                    + "점수는 문항별 1~5점이며 제출 후 수정하거나 다시 제출할 수 없습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "201", description = "강의평가 제출 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.DUPLICATE_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponseDTO<LectureEvaluationResponseDTO>> submit(
            @Valid @RequestBody LectureEvaluationSubmitRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponseDTO.success(lectureEvaluationService.submit(request, currentUser)));
    }
}
