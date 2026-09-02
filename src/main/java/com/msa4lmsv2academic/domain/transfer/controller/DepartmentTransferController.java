package com.msa4lmsv2academic.domain.transfer.controller;

import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

import com.msa4lmsv2academic.domain.transfer.entity.TransferDocumentType;
import com.msa4lmsv2academic.domain.transfer.request.*;
import com.msa4lmsv2academic.domain.transfer.response.DepartmentTransferResponseDTO;
import com.msa4lmsv2academic.domain.transfer.service.*;
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
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Department transfer requests", description = "학생 전과 신청·조회·취소와 관리자 승인·반려 API")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/academic/department-transfer-requests")
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
public class DepartmentTransferController {
    private final DepartmentTransferService service;
    private final DepartmentTransferApplicationService applicationService;

    @Operation(operationId = "searchDepartmentTransferRequests", summary = "전과 신청 목록 조회",
            description = "STUDENT는 본인 신청만, ADMIN은 전체 신청을 조회합니다. 상태·적용 학기·희망 학과·학생 필터와 "
                    + "생성 시각 정렬을 지원하며 결과 없음은 items=[]와 totalCount=0입니다.")
    @ApiResponse(responseCode = "200", description = "00: 조회 성공")
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<DepartmentTransferResponseDTO>>> search(
            @ParameterObject @Valid @ModelAttribute DepartmentTransferSearchRequestDTO filter,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.search(filter, actor,
                PageRequest.of(filter.resolvedPage() - 1, filter.resolvedSize()))));
    }

    @Operation(operationId = "getDepartmentTransferRequest", summary = "전과 신청 상세 조회",
            description = "STUDENT 본인 또는 ADMIN만 조회합니다. 신청 당시·희망 소속, 적용 학기, 상태·사유와 "
                    + "필수 PDF 메타데이터를 반환하며 MinIO 저장 키는 노출하지 않습니다.")
    @ApiResponse(responseCode = "200", description = "00: 조회 성공")
    @GetMapping("/{requestId}")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ResponseEntity<GlobalResponseDTO<DepartmentTransferResponseDTO>> get(
            @Parameter(description = "전과 신청 식별자", example = "1")
            @Positive @PathVariable Long requestId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.get(requestId, actor)));
    }

    @Operation(operationId = "createDepartmentTransferRequest", summary = "전과 신청",
            description = "STUDENT 전용. 재학생이 활성 접수 기간에 현재와 다른 활성 학과를 신청합니다. "
                    + "현재 복수전공과 같은 학과는 차단합니다. 자기소개서·학업계획서·성적증명서 PDF가 각각 필수이며 "
                    + "파일당 10MB 이하입니다. 모집요강 확인과 버튼 활성화는 Client가 담당합니다.")
    @ApiResponse(responseCode = "201", description = "00: 신청 생성 또는 저장된 성공 응답 재생")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponseDTO<DepartmentTransferResponseDTO>> create(
            @Parameter(description = "신청 JSON(application/json)", required = true)
            @Valid @RequestPart("request") DepartmentTransferCreateRequestDTO request,
            @Parameter(description = "자기소개서 PDF(필수, 10MB 이하)", required = true,
                    schema = @Schema(type = "string", format = "binary"))
            @RequestPart("selfIntroduction") MultipartFile selfIntroduction,
            @Parameter(description = "학업계획서 PDF(필수, 10MB 이하)", required = true,
                    schema = @Schema(type = "string", format = "binary"))
            @RequestPart("studyPlan") MultipartFile studyPlan,
            @Parameter(description = "성적증명서 PDF(필수, 10MB 이하)", required = true,
                    schema = @Schema(type = "string", format = "binary"))
            @RequestPart("transcript") MultipartFile transcript,
            @Parameter(description = "1~100자의 공백 없는 요청별 키. JSON과 세 파일이 동일한 완료 요청만 24시간 재생",
                    required = true, schema = @Schema(minLength = 1, maxLength = 100), example = "transfer-request-001")
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor,
            HttpServletRequest httpRequest) {
        var response = applicationService.create(request, selfIntroduction, studyPlan, transcript, key, actor,
                DepartmentTransferAuditContext.from(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponseDTO.success(response));
    }

    @Operation(operationId = "cancelDepartmentTransferRequest", summary = "전과 신청 취소",
            description = "STUDENT 본인의 PENDING 신청만 취소합니다. 신청과 PDF는 삭제하지 않고 취소자·시각·사유 및 "
                    + "변경 전후를 보존하며, 취소 후 접수 기간 안에서 재신청할 수 있습니다.")
    @ApiResponse(responseCode = "200", description = "00: 취소 성공 또는 저장된 성공 응답 재생")
    @PatchMapping("/{requestId}/cancellation")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponseDTO<DepartmentTransferResponseDTO>> cancel(
            @Parameter(description = "전과 신청 식별자", example = "1")
            @Positive @PathVariable Long requestId,
            @Valid @RequestBody DepartmentTransferCancelRequestDTO request,
            @Parameter(description = "1~100자의 공백 없는 요청별 키", required = true,
                    schema = @Schema(minLength = 1, maxLength = 100), example = "transfer-cancel-001")
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.cancel(requestId, request, key, actor,
                DepartmentTransferAuditContext.from(httpRequest))));
    }

    @Operation(operationId = "reviewDepartmentTransferRequest", summary = "전과 신청 승인·반려",
            description = "ADMIN 전용. PENDING 신청만 처리합니다. 승인 시 학생이 여전히 재학 중이고 신청 당시 소속과 "
                    + "일치하는지, 희망 학과와 복수전공 충돌이 없는지 다시 확인합니다. 승인 즉시 소속 학과를 "
                    + "변경하고 기존 지도교수를 해제하며, 운영상 현재 학기 종료 후 다음 학기 시작 전에 처리해야 합니다. "
                    + "반려는 사유가 필수이고 학적을 변경하지 않습니다.")
    @ApiResponse(responseCode = "200", description = "00: 승인·반려 성공 또는 저장된 성공 응답 재생")
    @PatchMapping("/{requestId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<DepartmentTransferResponseDTO>> review(
            @Parameter(description = "전과 신청 식별자", example = "1")
            @Positive @PathVariable Long requestId,
            @Valid @RequestBody DepartmentTransferReviewRequestDTO request,
            @Parameter(description = "1~100자의 공백 없는 요청별 키", required = true,
                    schema = @Schema(minLength = 1, maxLength = 100), example = "transfer-review-001")
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.review(requestId, request, key, actor,
                DepartmentTransferAuditContext.from(httpRequest))));
    }

    @Operation(operationId = "downloadDepartmentTransferDocument", summary = "전과 제출 서류 다운로드",
            description = "STUDENT 본인 또는 ADMIN만 신청의 PDF를 다운로드합니다. 승인·반려·취소 후에도 보존하며 "
                    + "Academic이 비공개 MinIO 파일을 전달합니다.")
    @ApiResponse(responseCode = "200", description = "PDF 파일(공통 JSON 응답 미사용)",
            content = @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary")))
    @GetMapping("/{requestId}/documents/{documentType}")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ResponseEntity<byte[]> download(
            @Parameter(description = "전과 신청 식별자", example = "1")
            @Positive @PathVariable Long requestId,
            @Parameter(description = "SELF_INTRODUCTION, STUDY_PLAN, TRANSCRIPT", example = "SELF_INTRODUCTION")
            @PathVariable TransferDocumentType documentType,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor) {
        var download = applicationService.download(requestId, documentType, actor);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.originalName(), StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(download.content());
    }
}
