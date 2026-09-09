package com.msa4lmsv2academic.domain.transfer.controller;

import com.msa4lmsv2academic.domain.transfer.request.*;
import com.msa4lmsv2academic.domain.transfer.response.DepartmentTransferResponseDTO;
import com.msa4lmsv2academic.domain.transfer.service.*;
import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.*;
import com.msa4lmsv2academic.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Department transfer requests",
        description = "학생 신청 → 지도교수 검토 → 학장 오프라인 날인 → 관리자 학적 반영 API")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/academic/department-transfer-requests")
@SecurityRequirement(name = "bearerAuth")
@CustomApiResponse({CustomResponseCode.UNAUTHENTICATED, CustomResponseCode.ACCESS_DENIED,
        CustomResponseCode.NOT_FOUND_DATA, CustomResponseCode.DUPLICATE_DATA,
        CustomResponseCode.INVALID_PARAMETER, CustomResponseCode.FILE_SIZE_EXCEEDED,
        CustomResponseCode.DATABASE_ERROR, CustomResponseCode.SYSTEM_ERROR})
public class DepartmentTransferController {
    private final DepartmentTransferService service;
    private final DepartmentTransferApplicationService applicationService;
    private final DepartmentTransferTemplateService templateService;

    @Operation(operationId = "searchDepartmentTransferRequests", summary = "전과 신청 목록 조회",
            description = "STUDENT는 본인 신청, PROFESSOR는 현재 지도학생 신청, ADMIN은 전체 신청을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "00: 조회 성공")
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT','PROFESSOR','ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<DepartmentTransferResponseDTO>>> search(
            @ParameterObject @Valid @ModelAttribute DepartmentTransferSearchRequestDTO filter,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.search(filter, actor,
                PageRequest.of(filter.resolvedPage() - 1, filter.resolvedSize()))));
    }

    @Operation(operationId = "getDepartmentTransferRequest", summary = "전과 신청 상세 조회",
            description = "학생 본인, 현재 담당 지도교수 또는 관리자가 조회합니다.")
    @ApiResponse(responseCode = "200", description = "00: 조회 성공")
    @GetMapping("/{requestId}")
    @PreAuthorize("hasAnyRole('STUDENT','PROFESSOR','ADMIN')")
    public ResponseEntity<GlobalResponseDTO<DepartmentTransferResponseDTO>> get(
            @Positive @PathVariable Long requestId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.get(requestId, actor)));
    }

    @Operation(operationId = "createDepartmentTransferRequest", summary = "전과 신청",
            description = "STUDENT 전용. 1학년 1학기 이상 이수, 3학년 미만, 승인 전과 이력 없음 요건을 검사합니다. "
                    + "HWP/HWPX 파일을 정확히 2개 받으며 파일명과 순서는 판별하지 않고 파일당 10MB로 제한합니다.")
    @ApiResponse(responseCode = "201", description = "00: 신청 생성 또는 저장된 성공 응답 재생")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponseDTO<DepartmentTransferResponseDTO>> create(
            @Valid @RequestPart("request") DepartmentTransferCreateRequestDTO request,
            @Parameter(description = "HWP/HWPX 첨부파일 정확히 2개", required = true)
            @RequestPart("files") List<MultipartFile> files,
            @Parameter(description = "1~100자의 공백 없는 요청별 키", required = true)
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor,
            HttpServletRequest httpRequest) {
        var response = applicationService.create(request, files, key, actor,
                DepartmentTransferAuditContext.from(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponseDTO.success(response));
    }

    @Operation(operationId = "cancelDepartmentTransferRequest", summary = "전과 신청 취소",
            description = "STUDENT 본인이 PENDING 또는 ADVISOR_APPROVED 상태의 신청을 취소합니다.")
    @PatchMapping("/{requestId}/cancellation")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponseDTO<DepartmentTransferResponseDTO>> cancel(
            @Positive @PathVariable Long requestId,
            @Valid @RequestBody DepartmentTransferCancelRequestDTO request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.cancel(requestId, request, key, actor,
                DepartmentTransferAuditContext.from(httpRequest))));
    }

    @Operation(operationId = "advisorReviewDepartmentTransferRequest", summary = "지도교수 전과 검토",
            description = "현재 담당 지도교수만 PENDING 신청을 승인하거나 사유와 함께 반려합니다.")
    @PatchMapping("/{requestId}/advisor-review")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<DepartmentTransferResponseDTO>> advisorReview(
            @Positive @PathVariable Long requestId,
            @Valid @RequestBody AdvisorDepartmentTransferReviewRequestDTO request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.reviewByAdvisor(requestId, request, key, actor,
                DepartmentTransferAuditContext.from(httpRequest))));
    }

    @Operation(operationId = "applyDepartmentTransferRequest", summary = "관리자 전과 학적 반영",
            description = "ADMIN 전용. 지도교수가 승인하고 학장이 두 HWP/HWPX 문서 모두에 날인한 경우에만 호출합니다. "
                    + "날인본 2개로 기존 서류 묶음을 교체하고 학과 변경과 APPLIED 전이를 한 번에 처리한 뒤 "
                    + "StudentSnapshotChanged를 발행합니다. 파일명·순서·문서 종류는 판별하지 않습니다.")
    @PatchMapping(value = "/{requestId}/application", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<DepartmentTransferResponseDTO>> apply(
            @Positive @PathVariable Long requestId,
            @Parameter(description = "학장 날인이 포함된 HWP/HWPX 파일 정확히 2개", required = true)
            @RequestPart("files") List<MultipartFile> files,
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(GlobalResponseDTO.success(applicationService.apply(requestId, files, key, actor,
                DepartmentTransferAuditContext.from(httpRequest))));
    }

    @Operation(operationId = "rejectDepartmentTransferRequestByAdmin", summary = "관리자 전과 반려",
            description = "ADMIN 전용. 지도교수 승인 후 전달된 두 문서에 학장 날인이 확인되지 않으면 "
                    + "사유를 기록하고 REJECTED로 종료합니다. 기존 학생 제출 파일은 교체하지 않습니다.")
    @PatchMapping("/{requestId}/rejection")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<DepartmentTransferResponseDTO>> reject(
            @Positive @PathVariable Long requestId,
            @Valid @RequestBody AdminAcademicChangeRejectionRequestDTO request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.rejectByAdmin(requestId, request, key, actor,
                DepartmentTransferAuditContext.from(httpRequest))));
    }

    @Operation(operationId = "downloadDepartmentTransferFile", summary = "전과 첨부파일 다운로드")
    @GetMapping("/{requestId}/files/{fileId}")
    @PreAuthorize("hasAnyRole('STUDENT','PROFESSOR','ADMIN')")
    public ResponseEntity<byte[]> download(@Positive @PathVariable Long requestId,
                                           @Positive @PathVariable Long fileId,
                                           @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor) {
        var download = applicationService.download(requestId, fileId, actor);
        return attachment(download.content(), download.originalName(), download.contentType());
    }

    @Operation(operationId = "downloadDepartmentTransferStudyPlanTemplate", summary = "학업계획서 HWP 양식 다운로드")
    @GetMapping("/templates/study-plan")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<byte[]> studyPlanTemplate() {
        var template = templateService.studyPlan();
        return attachment(template.content(), template.filename(), "application/x-hwp");
    }

    @Operation(operationId = "downloadDepartmentTransferSelfIntroductionTemplate", summary = "자기소개서 HWP 양식 다운로드")
    @GetMapping("/templates/self-introduction")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<byte[]> selfIntroductionTemplate() {
        var template = templateService.selfIntroduction();
        return attachment(template.content(), template.filename(), "application/x-hwp");
    }

    private ResponseEntity<byte[]> attachment(byte[] content, String filename, String contentType) {
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException exception) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok().contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff").body(content);
    }
}
