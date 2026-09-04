package com.msa4lmsv2academic.domain.attendance.controller;

import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

import com.msa4lmsv2academic.domain.attendance.request.ExcuseRequestCreateRequestDTO;
import com.msa4lmsv2academic.domain.attendance.request.ExcuseRequestSearchRequestDTO;
import com.msa4lmsv2academic.domain.attendance.request.ExcuseReviewRequestDTO;
import com.msa4lmsv2academic.domain.attendance.response.ExcuseAttachmentResponseDTO;
import com.msa4lmsv2academic.domain.attendance.response.ExcuseRequestResponseDTO;
import com.msa4lmsv2academic.domain.attendance.response.ExcuseRequestStatusResponseDTO;
import com.msa4lmsv2academic.domain.attendance.service.ExcuseAttachmentService;
import com.msa4lmsv2academic.domain.attendance.service.ExcuseRequestQueryService;
import com.msa4lmsv2academic.domain.attendance.service.ExcuseRequestService;
import com.msa4lmsv2academic.domain.attendance.service.ExcuseReviewService;
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
import jakarta.validation.constraints.Positive;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Attendance Excuses", description = "학생 공결 신청 및 처리 API")
@Validated
@RestController
@RequestMapping("/api/academic/attendance/excuses")
@RequiredArgsConstructor
public class ExcuseRequestController {

    private final ExcuseRequestService excuseRequestService;
    private final ExcuseAttachmentService excuseAttachmentService;
    private final ExcuseReviewService excuseReviewService;
    private final ExcuseRequestQueryService excuseRequestQueryService;

    @Operation(
            operationId = "searchExcuseRequests",
            summary = "공결 처리 상태 조회",
            description = "학생은 본인이 신청한 공결, 교수는 본인이 담당하는 강의의 공결, 관리자는 전체 공결을 "
                    + "신청 시각과 ID 내림차순으로 조회합니다. 상태를 생략하면 모든 상태를 조회하며, "
                    + "결과가 없으면 빈 items와 totalCount 0을 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "공결 처리 상태 조회 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<ExcuseRequestStatusResponseDTO>>> search(
            @ParameterObject @Valid @ModelAttribute ExcuseRequestSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(excuseRequestQueryService.search(request, currentUser)));
    }

    @Operation(
            summary = "공결 신청",
            description = "학생이 본인의 활성 수강 수업에 대해 결석 수업일로부터 7일 이내 공결을 신청합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "201", description = "공결 신청 성공")
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
    public ResponseEntity<GlobalResponseDTO<ExcuseRequestResponseDTO>> create(
            @Valid @RequestBody ExcuseRequestCreateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponseDTO.success(excuseRequestService.create(request, currentUser)));
    }

    @Operation(
            operationId = "reviewExcuseRequest",
            summary = "담당 교수 공결 승인·반려",
            description = "담당 교수가 처리 대기 상태인 본인 강의의 공결 신청을 승인하거나 반려합니다. "
                    + "반려 사유, 변경 전후 상태, 처리자와 처리시각을 감사 이력으로 남깁니다. "
                    + "Idempotency-Key는 공백 없는 1~100자이며 논리적으로 같은 처리의 재시도에는 같은 키를 사용합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "공결 처리 성공 또는 저장된 성공 응답 재생")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.DUPLICATE_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PatchMapping("/{requestId}")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<ExcuseRequestResponseDTO>> review(
            @Parameter(description = "공결 신청 ID", example = "301")
            @Positive(message = "requestId는 양수여야 합니다.") @PathVariable Long requestId,
            @Parameter(description = "논리적으로 같은 처리의 재시도에는 같은 키 사용", required = true,
                    example = "06a6de23-1d84-4778-9bf5-9719b163a44f",
                    schema = @Schema(type = "string", minLength = 1, maxLength = 100))
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ExcuseReviewRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(excuseReviewService.review(
                requestId,
                request,
                idempotencyKey,
                currentUser,
                httpRequest.getHeader("X-Request-Id"),
                httpRequest.getRemoteAddr()
        ));
    }

    @Operation(
            operationId = "uploadExcuseAttachment",
            summary = "공결 증빙 등록·교체",
            description = "학생 본인이 처리 대기 상태인 공결 신청에 10MB 이하 PDF 증빙을 등록하거나 교체합니다. "
                    + "확장자, application/pdf MIME 타입과 PDF 시그니처를 모두 검증합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "공결 증빙 등록·교체 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.DUPLICATE_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.FILE_ERROR,
            CustomResponseCode.FILE_SIZE_EXCEEDED,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PutMapping(value = "/{requestId}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponseDTO<ExcuseAttachmentResponseDTO>> uploadAttachment(
            @Parameter(description = "공결 신청 ID", example = "31")
            @Positive @PathVariable Long requestId,
            @Parameter(description = "10MB 이하 PDF", required = true,
                    schema = @Schema(type = "string", format = "binary"))
            @RequestPart("file") MultipartFile file,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(excuseAttachmentService.upload(
                requestId,
                file,
                currentUser,
                httpRequest.getHeader("X-Request-Id"),
                httpRequest.getRemoteAddr()
        )));
    }

    @Operation(
            operationId = "downloadExcuseAttachment",
            summary = "공결 증빙 다운로드",
            description = "학생 본인, 해당 강의 담당 교수 또는 관리자가 권한 확인 후 공결 PDF 증빙을 다운로드합니다. "
                    + "MinIO 저장 키와 내부 URL은 공개하지 않습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(
            responseCode = "200",
            description = "PDF 파일",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE,
                    schema = @Schema(type = "string", format = "binary"))
    )
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.FILE_ERROR,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping("/{requestId}/attachment")
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<byte[]> downloadAttachment(
            @Parameter(description = "공결 신청 ID", example = "31")
            @Positive @PathVariable Long requestId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        var download = excuseAttachmentService.download(requestId, currentUser);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.originalName(), StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(download.content());
    }
}
