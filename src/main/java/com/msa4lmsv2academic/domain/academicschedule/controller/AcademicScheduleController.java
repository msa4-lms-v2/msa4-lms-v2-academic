package com.msa4lmsv2academic.domain.academicschedule.controller;

import com.msa4lmsv2academic.domain.academicschedule.request.AcademicScheduleCreateRequestDTO;
import com.msa4lmsv2academic.domain.academicschedule.request.AcademicScheduleSearchRequestDTO;
import com.msa4lmsv2academic.domain.academicschedule.request.AcademicScheduleStatusRequestDTO;
import com.msa4lmsv2academic.domain.academicschedule.request.AcademicScheduleUpdateRequestDTO;
import com.msa4lmsv2academic.domain.academicschedule.response.AcademicScheduleDetailResponseDTO;
import com.msa4lmsv2academic.domain.academicschedule.response.AcademicScheduleSummaryResponseDTO;
import com.msa4lmsv2academic.domain.academicschedule.service.AcademicScheduleService;
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
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Academic Schedules", description = "학사일정 조회·등록·수정·상태 변경 API")
@Validated
@RestController
@RequestMapping("/api/academic/academic-schedules")
@RequiredArgsConstructor
public class AcademicScheduleController {

    private final AcademicScheduleService academicScheduleService;

    @Operation(
            operationId = "searchAcademicSchedules",
            summary = "학사일정 목록 조회",
            description = "STUDENT와 PROFESSOR는 활성 상태이면서 ALL 또는 본인 역할에 공개된 일정을 조회합니다. "
                    + "ADMIN은 대상 역할과 활성 상태 전체를 검색할 수 있습니다. 기간 조건은 일정과 조회 기간이 "
                    + "하루라도 겹치면 포함하며 시작일과 ID 오름차순으로 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공. 결과가 없으면 빈 items를 반환합니다."),
            @ApiResponse(responseCode = "400", description = "잘못된 페이지·기간·검색 조건",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "403", description = "일반 사용자가 다른 역할 범위를 요청함",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalRes<PageRes<AcademicScheduleSummaryResponseDTO>>> search(
            @ParameterObject @Valid @ModelAttribute AcademicScheduleSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalRes.success(academicScheduleService.search(request, currentUser)));
    }

    @Operation(
            operationId = "getAcademicSchedule",
            summary = "학사일정 상세 조회",
            description = "ADMIN은 모든 일정을 조회합니다. STUDENT와 PROFESSOR는 활성 상태이며 ALL 또는 본인 역할에 "
                    + "공개된 일정만 조회할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 학사일정 ID",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "403", description = "공개 대상 역할 불일치",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "404", description = "일정 없음 또는 일반 사용자에게 숨겨진 비활성 일정",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class)))
    })
    @GetMapping("/{scheduleId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalRes<AcademicScheduleDetailResponseDTO>> get(
            @Parameter(description = "학사일정 ID", example = "1", required = true)
            @PathVariable @Positive(message = "scheduleId는 양수여야 합니다.") Long scheduleId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalRes.success(academicScheduleService.get(scheduleId, currentUser)));
    }

    @Operation(
            operationId = "createAcademicSchedule",
            summary = "학사일정 등록",
            description = "ADMIN만 학사일정을 등록할 수 있습니다. 시작일은 필수이고 종료일을 생략하면 하루 일정입니다. "
                    + "제목·본문·기간·대상 역할이 모두 같은 활성 일정은 중복 등록할 수 없습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "필수값 누락, 길이 또는 기간 순서 오류",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "404", description = "Academic에 동기화된 관리자 정보 없음",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "409", description = "동일한 활성 일정 중복",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalRes<AcademicScheduleDetailResponseDTO>> create(
            @Valid @RequestBody AcademicScheduleCreateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @Parameter(description = "분산 추적 및 감사 로그 요청 ID", example = "01JABCDEF1234567890")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        AcademicScheduleDetailResponseDTO response = academicScheduleService.create(
                request, currentUser, requestId, httpServletRequest.getRemoteAddr()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalRes.success(response));
    }

    @Operation(
            operationId = "updateAcademicSchedule",
            summary = "학사일정 전체 수정",
            description = "ADMIN만 제목·본문·시작일·종료일·대상 역할을 전체 치환할 수 있습니다. 변경 사유가 필수이며 "
                    + "활성 상태는 이 API에서 변경하지 않습니다. 값이 모두 같으면 감사 로그 없이 현재 상태를 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공 또는 동일 값 요청"),
            @ApiResponse(responseCode = "400", description = "필수값 누락, 길이 또는 기간 순서 오류",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "404", description = "학사일정 없음",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "409", description = "수정 결과가 다른 활성 일정과 중복",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class)))
    })
    @PutMapping("/{scheduleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalRes<AcademicScheduleDetailResponseDTO>> update(
            @Parameter(description = "학사일정 ID", example = "1", required = true)
            @PathVariable @Positive(message = "scheduleId는 양수여야 합니다.") Long scheduleId,
            @Valid @RequestBody AcademicScheduleUpdateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @Parameter(description = "분산 추적 및 감사 로그 요청 ID", example = "01JABCDEF1234567890")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(GlobalRes.success(academicScheduleService.update(
                scheduleId, request, currentUser, requestId, httpServletRequest.getRemoteAddr()
        )));
    }

    @Operation(
            operationId = "changeAcademicScheduleStatus",
            summary = "학사일정 활성 상태 변경",
            description = "ADMIN만 학사일정을 비활성화하거나 재활성화할 수 있습니다. 변경 사유가 필수이며 같은 상태를 "
                    + "다시 요청하면 DB와 감사 로그를 변경하지 않고 현재 상태를 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상태 변경 성공 또는 동일 상태 요청"),
            @ApiResponse(responseCode = "400", description = "active 또는 변경 사유 누락",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "404", description = "학사일정 없음",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "409", description = "재활성화 결과가 다른 활성 일정과 중복",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class)))
    })
    @PatchMapping("/{scheduleId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalRes<AcademicScheduleDetailResponseDTO>> changeStatus(
            @Parameter(description = "학사일정 ID", example = "1", required = true)
            @PathVariable @Positive(message = "scheduleId는 양수여야 합니다.") Long scheduleId,
            @Valid @RequestBody AcademicScheduleStatusRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @Parameter(description = "분산 추적 및 감사 로그 요청 ID", example = "01JABCDEF1234567890")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(GlobalRes.success(academicScheduleService.changeStatus(
                scheduleId, request, currentUser, requestId, httpServletRequest.getRemoteAddr()
        )));
    }
}
