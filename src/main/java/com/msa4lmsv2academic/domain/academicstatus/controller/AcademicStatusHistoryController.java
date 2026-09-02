package com.msa4lmsv2academic.domain.academicstatus.controller;

import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

import com.msa4lmsv2academic.domain.academicstatus.request.AcademicStatusHistorySearchRequestDTO;
import com.msa4lmsv2academic.domain.academicstatus.response.AcademicStatusHistoryResponseDTO;
import com.msa4lmsv2academic.domain.academicstatus.service.AcademicStatusHistoryService;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Academic Status Histories", description = "권한 범위별 확정 학적 변경 이력 조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/academic/status-histories")
public class AcademicStatusHistoryController {

    private final AcademicStatusHistoryService historyService;

    @Operation(operationId = "searchAcademicStatusHistories", summary = "학적 변경 이력 검색",
            description = "STUDENT는 본인, ADMIN은 전체, PROFESSOR는 현재 지도학생·현재 같은 학과 학생·"
                    + "현재 학기 담당 강의의 ACTIVE 수강생 합집합만 조회합니다. 교수는 관계가 유지되면 자퇴·졸업·퇴학 학생도 조회할 수 있습니다. "
                    + "단, users.deleted_at이 설정된 학생 계정은 기존 논리 삭제 조회 정책에 따라 제외합니다. "
                    + "과거 담당 관계로 조회 권한을 부여하지 않습니다. 모든 필터와 totalCount에 동일한 권한 범위를 적용하며 "
                    + "범위 밖 studentId나 결과 없음은 200과 빈 items를 반환합니다. "
                    + "academic_status_histories에 기록된 확정 전이만 반환하며 신청 대기·반려 내역은 포함하지 않습니다. "
                    + "이름·학과는 현재 값이고 reason·sourceId는 null일 수 있습니다. 조회는 이력을 생성·수정하지 않습니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
            @ApiResponse(responseCode = "200", description = "00: 조회 성공. 결과 없음은 빈 items 및 totalCount 0")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<AcademicStatusHistoryResponseDTO>>> search(
            @ParameterObject @Valid @ModelAttribute AcademicStatusHistorySearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        Sort.Direction direction = "asc".equals(request.sortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(request.resolvedPage() - 1, request.resolvedSize(),
                Sort.by(direction, "createdAt", "id"));
        Page<AcademicStatusHistoryResponseDTO> result = historyService.search(request, pageable, currentUser);
        return ResponseEntity.ok(GlobalResponseDTO.success(new PageResponseDTO<>(
                result.getContent(), result.getTotalElements(), result.getNumber() + 1, result.getSize(), result.hasNext()
        )));
    }
}
