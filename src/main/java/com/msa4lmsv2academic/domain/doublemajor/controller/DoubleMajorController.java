package com.msa4lmsv2academic.domain.doublemajor.controller;

import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

import com.msa4lmsv2academic.domain.doublemajor.request.*;
import com.msa4lmsv2academic.domain.doublemajor.response.DoubleMajorResponseDTO;
import com.msa4lmsv2academic.domain.doublemajor.service.*;
import com.msa4lmsv2academic.domain.transfer.request.AdminAcademicChangeRejectionRequestDTO;
import com.msa4lmsv2academic.domain.transfer.service.DepartmentTransferAuditContext;
import com.msa4lmsv2academic.domain.transfer.service.DepartmentTransferTemplateService;
import com.msa4lmsv2academic.global.response.*;
import com.msa4lmsv2academic.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
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

@Tag(name = "Double major requests",
        description = "학생 신청 → 지도교수 검토 → 학장 오프라인 날인 → 관리자 학적 반영 API")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/academic/double-major-requests")
@SecurityRequirement(name = "bearerAuth")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.DUPLICATE_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.FILE_SIZE_EXCEEDED,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
public class DoubleMajorController {
    private final DoubleMajorService service;
    private final DoubleMajorApplicationService applicationService;
    private final DepartmentTransferTemplateService templateService;

    @Operation(operationId = "searchDoubleMajorRequests", summary = "복수전공 신청 목록 조회",
            description = "STUDENT는 본인 신청, PROFESSOR는 현재 지도학생 신청, ADMIN은 전체 신청을 조회합니다. 상태·모집 회차·희망 학과·학생 "
                    + "필터와 생성 시각 정렬을 지원하며 결과 없음은 items=[]와 totalCount=0입니다.")
    @ApiResponse(responseCode = "200", description = "00: 조회 성공")
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT','PROFESSOR','ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<DoubleMajorResponseDTO>>> search(
            @ParameterObject @Valid @ModelAttribute DoubleMajorSearchRequestDTO filter,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.search(filter, actor,
                PageRequest.of(filter.resolvedPage() - 1, filter.resolvedSize()))));
    }

    @Operation(operationId = "getDoubleMajorRequest", summary = "복수전공 신청 상세 조회",
            description = "학생 본인, 현재 담당 지도교수 또는 관리자가 조회합니다. 신청 당시 주전공, 희망 복수전공, 모집 회차, "
                    + "처리 상태·사유와 HWP/HWPX 메타데이터를 반환하며 MinIO 저장 키는 노출하지 않습니다.")
    @ApiResponse(responseCode = "200", description = "00: 조회 성공")
    @GetMapping("/{requestId}")
    @PreAuthorize("hasAnyRole('STUDENT','PROFESSOR','ADMIN')")
    public ResponseEntity<GlobalResponseDTO<DoubleMajorResponseDTO>> get(
            @Parameter(description = "복수전공 신청 식별자", example = "1")
            @Positive @PathVariable Long requestId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.get(requestId, actor)));
    }

    @Operation(operationId = "createDoubleMajorRequest", summary = "복수전공 신청",
            description = "STUDENT 전용. 정규학기 2개 이상 이수하고 33학점 이상 취득한 재학생이 현재 열린 단일 모집 회차에서 "
                    + "현재 소속과 다른 활성 학과를 신청합니다. 파일명·순서·문서 종류를 판별하지 않는 HWP/HWPX 파일을 "
                    + "정확히 2개 받으며 파일당 10MB 이하입니다.")
    @ApiResponse(responseCode = "201", description = "00: 신청 생성 또는 저장된 성공 응답 재생")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponseDTO<DoubleMajorResponseDTO>> create(
            @Parameter(description = "신청 JSON(application/json)", required = true)
            @Valid @RequestPart("request") DoubleMajorCreateRequestDTO request,
            @Parameter(description = "HWP/HWPX 첨부파일 정확히 2개", required = true)
            @RequestPart("files") List<MultipartFile> files,
            @Parameter(description = "1~100자의 공백 없는 요청별 키. JSON과 두 파일이 동일한 완료 요청만 24시간 재생",
                    required = true, schema = @Schema(minLength = 1, maxLength = 100),
                    example = "double-major-request-001")
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor,
            HttpServletRequest httpRequest) {
        var response = applicationService.create(request, files, key, actor,
                DepartmentTransferAuditContext.from(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponseDTO.success(response));
    }

    @Operation(operationId = "advisorReviewDoubleMajorRequest", summary = "지도교수 복수전공 검토",
            description = "현재 담당 지도교수만 PENDING 신청을 승인하거나 사유와 함께 반려합니다. 승인된 두 문서는 학장에게 전달합니다.")
    @PatchMapping("/{requestId}/advisor-review")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<DoubleMajorResponseDTO>> advisorReview(
            @Parameter(description = "복수전공 신청 식별자", example = "1") @Positive @PathVariable Long requestId,
            @Valid @RequestBody AdvisorDoubleMajorReviewRequestDTO request,
            @Parameter(description = "1~100자의 공백 없는 요청별 키", required = true,
                    schema = @Schema(minLength = 1, maxLength = 100), example = "double-major-advisor-review-001")
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.reviewByAdvisor(requestId, request, key, actor,
                DepartmentTransferAuditContext.from(httpRequest))));
    }

    @Operation(operationId = "applyDoubleMajorRequest", summary = "관리자 복수전공 학적 반영",
            description = "ADMIN 전용. 지도교수 승인 후 학장이 두 HWP/HWPX 문서 모두에 날인한 경우에만 호출합니다. "
                    + "날인본 2개로 기존 서류 묶음을 교체하고 복수전공 배정과 APPLIED 전이를 한 번에 처리합니다. "
                    + "파일명·순서·문서 종류는 판별하지 않습니다.")
    @PatchMapping(value = "/{requestId}/application", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<DoubleMajorResponseDTO>> apply(
            @Positive @PathVariable Long requestId,
            @Parameter(description = "학장 날인이 포함된 HWP/HWPX 파일 정확히 2개", required = true)
            @RequestPart("files") List<MultipartFile> files,
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(GlobalResponseDTO.success(applicationService.apply(requestId, files, key, actor,
                DepartmentTransferAuditContext.from(httpRequest))));
    }

    @Operation(operationId = "rejectDoubleMajorRequestByAdmin", summary = "관리자 복수전공 반려",
            description = "ADMIN 전용. 지도교수 승인 후 전달된 두 문서에 학장 날인이 확인되지 않으면 "
                    + "사유를 기록하고 REJECTED로 종료합니다. 기존 학생 제출 파일은 교체하지 않습니다.")
    @PatchMapping("/{requestId}/rejection")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<DoubleMajorResponseDTO>> reject(
            @Positive @PathVariable Long requestId,
            @Valid @RequestBody AdminAcademicChangeRejectionRequestDTO request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.rejectByAdmin(requestId, request, key, actor,
                DepartmentTransferAuditContext.from(httpRequest))));
    }

    @Operation(operationId = "downloadDoubleMajorDocument", summary = "복수전공 제출 서류 다운로드",
            description = "학생 본인, 현재 담당 지도교수 또는 관리자가 신청의 HWP/HWPX를 다운로드합니다. "
                    + "APPLIED이면 학장 날인본을 반환하며 "
                    + "Academic이 비공개 MinIO 파일을 전달합니다.")
    @ApiResponse(responseCode = "200", description = "HWP/HWPX 파일(공통 JSON 응답 미사용)",
            content = @Content(mediaType = "application/octet-stream",
                    schema = @Schema(type = "string", format = "binary")))
    @GetMapping("/{requestId}/files/{fileId}")
    @PreAuthorize("hasAnyRole('STUDENT','PROFESSOR','ADMIN')")
    public ResponseEntity<byte[]> download(
            @Parameter(description = "복수전공 신청 식별자", example = "1") @Positive @PathVariable Long requestId,
            @Parameter(description = "첨부파일 ID", example = "1") @Positive @PathVariable Long fileId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor) {
        var download = applicationService.download(requestId, fileId, actor);
        return attachment(download.content(), download.originalName(), download.contentType());
    }

    @Operation(operationId = "downloadDoubleMajorStudyPlanTemplate", summary = "학업계획서 HWP 양식 다운로드")
    @GetMapping("/templates/study-plan")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<byte[]> studyPlanTemplate() {
        var template = templateService.studyPlan();
        return attachment(template.content(), template.filename(), "application/x-hwp");
    }

    @Operation(operationId = "downloadDoubleMajorSelfIntroductionTemplate", summary = "자기소개서 HWP 양식 다운로드")
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
