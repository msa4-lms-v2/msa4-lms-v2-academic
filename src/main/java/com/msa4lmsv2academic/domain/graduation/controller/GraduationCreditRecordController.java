package com.msa4lmsv2academic.domain.graduation.controller;

import com.msa4lmsv2academic.domain.graduation.request.GraduationCreditRecordSearchRequestDTO;
import com.msa4lmsv2academic.domain.graduation.response.GraduationCreditRecordResponseDTO;
import com.msa4lmsv2academic.domain.graduation.service.GraduationCreditRecordService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Graduation Credit Records", description = "졸업요건 진단용 과목별 수강·성적 근거 API")
@Validated
@RestController
@RequestMapping("/api/academic/students/{studentId}/graduation-credit-records")
@RequiredArgsConstructor
public class GraduationCreditRecordController {

    private final GraduationCreditRecordService graduationCreditRecordService;

    @Operation(
            operationId = "searchGraduationCreditRecords",
            summary = "졸업학점 수강·성적 근거 조회",
            description = "학생은 본인, 교수는 현재 담당 강의·지도·소속 학과의 재학·휴학 학생, 관리자는 전체 "
                    + "학생의 과목별 수강·공개 성적과 졸업학점 반영 결과를 조회합니다. 취소·미공개·미입력·F·비정상 "
                    + "성적·재수강 중복 기록은 표준 제외 사유와 함께 반환하며 결과가 없으면 빈 items를 반환합니다. "
                    + "DRAFT 성적값은 모든 역할에서 노출하지 않습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공 또는 빈 목록"),
            @ApiResponse(responseCode = "400", description = "학생 ID 또는 검색·정렬·페이징 조건 오류",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "역할 또는 학생 관리 범위 접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "학생 정보 없음",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<GraduationCreditRecordResponseDTO>>> search(
            @Parameter(description = "조회 대상 학생 ID", example = "1001", required = true)
            @PathVariable @Positive(message = "studentId는 양수여야 합니다.") Long studentId,
            @ParameterObject @Valid @ModelAttribute GraduationCreditRecordSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                graduationCreditRecordService.search(studentId, request, currentUser)
        ));
    }
}
