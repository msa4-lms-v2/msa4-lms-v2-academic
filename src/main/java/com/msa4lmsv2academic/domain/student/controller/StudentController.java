package com.msa4lmsv2academic.domain.student.controller;

import com.msa4lmsv2academic.domain.student.request.StudentSearchRequestDTO;
import com.msa4lmsv2academic.domain.student.response.StudentProfileResponseDTO;
import com.msa4lmsv2academic.domain.student.response.StudentSummaryResponseDTO;
import com.msa4lmsv2academic.domain.student.service.StudentDirectoryService;
import com.msa4lmsv2academic.domain.student.service.StudentService;
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
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Students", description = "학생 본인 학적과 권한 범위별 학생 목록 조회 API")
@RestController
@RequestMapping("/api/academic/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final StudentDirectoryService studentDirectoryService;

    @Operation(
            operationId = "searchStudents",
            summary = "학생 목록 검색",
            description = "PROFESSOR는 재학·휴학 상태인 지도학생, 현재 담당 강의의 정상 수강생, "
                    + "같은 학과 학생의 합집합만 조회합니다. ADMIN은 전체 학생을 조회합니다. "
                    + "검색 결과가 없으면 200 응답과 빈 items를 반환하며 이메일·전화번호·주소·학번·변경 사유는 "
                    + "목록에 포함하지 않습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공. 결과가 없으면 빈 items 반환"),
            @ApiResponse(responseCode = "400", description = "잘못된 페이지·필터·정렬 조건",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "학생 역할이거나 교수가 종결 학적 상태를 요청함",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "인증 교수에 대응하는 Academic 교수 정보 없음",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<StudentSummaryResponseDTO>>> searchStudents(
            @ParameterObject @Valid @ModelAttribute StudentSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                studentDirectoryService.searchStudents(request, currentUser)
        ));
    }

    @Operation(
            operationId = "getMyStudentProfile",
            summary = "학생 본인 프로필 조회",
            description = "STUDENT가 본인의 연락처·주소·소속·학적 상태와 프로필 이미지를 조회합니다. "
                    + "다른 사용자의 프로필은 조회할 수 없습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "STUDENT 권한 필요",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "학생 프로필 없음",
                    content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class)))
    })
    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponseDTO<StudentProfileResponseDTO>> getMyProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(studentService.getMyProfile(currentUser)));
    }
}
