package com.msa4lmsv2academic.domain.enrollment.controller;

import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

import com.msa4lmsv2academic.domain.enrollment.request.StudentEnrollmentCreateRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.response.StudentEnrollmentCreateResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.service.StudentEnrollmentApplicationService;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Enrollments")
@RestController
@RequestMapping("/api/academic/enrollments")
@RequiredArgsConstructor
public class StudentEnrollmentApplicationController {
    private final StudentEnrollmentApplicationService applicationService;

    @Operation(operationId = "createMyEnrollment", summary = "학생 본인 수강신청",
            description = """
                    STUDENT 본인만 신청합니다. 학생 ID는 인증 정보로 결정합니다.
                    재학 상태, 개설 여부, 해당 학기 신청 기간, 활성 중복, 정원, 시간표,
                    활성 최대학점 규칙과 선수과목·재수강 조건을 검증한 뒤 신청·성공 이력·멱등 응답을 원자적으로 저장합니다.
                    취소된 과거 수강은 수정하지 않고 새 수강 ID를 생성합니다.
                    Idempotency-Key는 필수이며 공백 없는 1~100자입니다.
                    성공 키는 생성 시점부터 24시간 보존합니다. 동일 사용자·경로·본문은 저장된 성공 응답을 재생하며
                    다른 사용자·경로·본문 또는 처리 중 키 재사용은 E11로 거절합니다.
                    실패 시 신규 키와 신청·이력을 모두 롤백하므로 같은 키로 현재 조건을 다시 검증할 수 있습니다.
                    만료 후 과거 응답 재생은 보장하지 않으며 활성 수강 중복 검사는 계속 적용합니다.
                    E11의 data.reasons는 확인된 거절 사유이며 전체 검증 결과를 일괄 수집하지 않습니다.
                    """, security = @SecurityRequirement(name = "bearerAuth"))
            @ApiResponse(responseCode = "200", description = "신청 성공 또는 보존된 성공 응답 재생")
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
    public ResponseEntity<GlobalResponseDTO<StudentEnrollmentCreateResponseDTO>> create(
            @Parameter(description = "논리적 신청마다 새 키 사용. 재시도에는 동일 키 사용", required = true,
                    example = "7a811872-83b8-4ea4-9a00-e095a98f17e9",
                    schema = @Schema(type = "string", minLength = 1, maxLength = 100))
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody StudentEnrollmentCreateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(applicationService.create(request, idempotencyKey, currentUser));
    }
}
