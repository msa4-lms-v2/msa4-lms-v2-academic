package com.msa4lmsv2academic.domain.leaverequest.controller;

import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

import com.msa4lmsv2academic.domain.leaverequest.request.*;
import com.msa4lmsv2academic.domain.leaverequest.response.LeaveRequestResponseDTO;
import com.msa4lmsv2academic.domain.leaverequest.service.*;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Leave requests", description = "일반휴학·군휴학·일반복학·군복학 신청. 외부 호출은 Gateway 8080 경유")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/academic/leave-requests")
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
public class LeaveRequestController {
    private final LeaveRequestService service;
    private final LeaveRequestApplicationService applicationService;

    @Operation(operationId = "searchLeaveRequests", summary = "휴·복학 신청 목록",
            description = "STUDENT 본인, ADMIN 전체. 유형·상태·적용 학기·학생 ID 필터와 생성시각/ID 정렬을 제공합니다. "
                    + "페이지는 1부터, size 최대 100. 결과 없음은 items=[]와 totalCount=0입니다.")
    @ApiResponse(responseCode = "200", description = "00: 목록 조회 성공")
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<LeaveRequestResponseDTO>>> search(
            @ParameterObject @Valid @ModelAttribute LeaveRequestSearchRequestDTO filter,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.search(filter, actor,
                PageRequest.of(filter.resolvedPage() - 1, filter.resolvedSize()))));
    }

    @Operation(operationId = "getLeaveRequest", summary = "휴·복학 신청 상세",
            description = "STUDENT 본인 또는 ADMIN만 조회합니다. 원본 신청·반려·취소 사유와 증빙 메타데이터를 제공합니다. "
                    + "updatedAt은 처리시각이 아니며 처리자·시각은 별도 감사 로그에 보존합니다.")
    @ApiResponse(responseCode = "200", description = "00: 상세 조회 성공")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ResponseEntity<GlobalResponseDTO<LeaveRequestResponseDTO>> get(
            @Parameter(description = "신청 ID", example = "1") @Positive @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.get(id, actor)));
    }

    @Operation(operationId = "createLeaveRequest", summary = "휴·복학 신청",
            description = "STUDENT 본인만 multipart request(JSON)와 file(PDF)을 제출합니다. 일반휴학은 사유 필수·PDF 선택, "
                    + "군휴학은 입영통지서 PDF 필수(사유 미입력 시 군입대), 복학 사유 미입력 시 복학입니다. PDF는 10MB 이하입니다. "
                    + "일반휴학은 현재 학기의 다음 학기부터, 복학 예정은 그보다 뒤의 학기를 선택합니다. "
                    + "군휴학 복학 예정은 유일한 현재 학기+4학기이며 승인 시 재계산하지 않습니다. "
                    + "복학 유형은 실제 휴학 승인 근거에서 결정하며 대상 학기는 원본 복학 예정과 같아야 합니다. "
                    + "휴학은 ENROLLED, 복학은 ON_LEAVE만 가능하고 활성 접수 기간(KST, 양 끝 포함)이 필요합니다. "
                    + "학생당 PENDING 한 건, 군휴학 승인은 평생 한 번. 성공 시 PENDING이고 학적은 유지합니다. "
                    + "자퇴와 동시 대기는 허용하되 자퇴 최종 승인 시 PENDING 신청은 자동 취소됩니다.")
    @ApiResponse(responseCode = "200", description = "00: 신청 성공 또는 저장된 성공 응답 재생")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponseDTO<LeaveRequestResponseDTO>> create(
            @Parameter(description = "신청 JSON. 이 파트의 Content-Type은 application/json", required = true)
            @Valid @RequestPart("request") LeaveRequestCreateRequestDTO request,
            @Parameter(description = "10MB 이하 PDF. 군휴학 필수, 나머지 선택", schema = @Schema(type = "string", format = "binary"))
            @RequestPart(value = "file", required = false) MultipartFile file,
            @Parameter(description = "1~100자의 공백 없는 요청별 키. 같은 사용자·경로·본문·파일의 완료 응답은 24시간 재생. 재생 시 업무·감사 재실행 없음",
                    required = true, schema = @Schema(minLength = 1, maxLength = 100), example = "leave-operation-001")
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(GlobalResponseDTO.success(applicationService.create(request, file, key, actor,
                LeaveAuditContext.from(httpRequest))));
    }

    @Operation(operationId = "changeLeaveRequestStatus", summary = "휴·복학 승인·반려·취소",
            description = "STUDENT 본인은 PENDING→CANCELLED만, ADMIN은 PENDING→APPROVED/REJECTED만 가능합니다. "
                    + "취소·반려 사유 필수(500자 이하). 승인에만 별도 활성 승인 기간(KST, 양 끝 포함)을 적용합니다. "
                    + "승인은 현재 학적과 승인 근거를 재검증하고 즉시 학적·학적 이력·감사를 함께 저장합니다. "
                    + "취소·반려는 학적을 변경하지 않습니다. 종결 상태 재처리·승인 후 취소·예약 실행은 제공하지 않습니다.")
    @ApiResponse(responseCode = "200", description = "00: 처리 성공 또는 저장된 성공 응답 재생")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ResponseEntity<GlobalResponseDTO<LeaveRequestResponseDTO>> changeStatus(
            @Parameter(description = "신청 ID", example = "1") @Positive @PathVariable Long id,
            @Valid @RequestBody LeaveRequestStatusChangeRequestDTO request,
            @Parameter(description = "1~100자의 공백 없는 요청별 키. 같은 사용자·경로·본문·파일의 완료 응답은 24시간 재생. 재생 시 업무·감사 재실행 없음",
                    required = true, schema = @Schema(minLength = 1, maxLength = 100), example = "leave-operation-001")
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.changeStatus(id, request, key, actor,
                LeaveAuditContext.from(httpRequest))));
    }

    @Operation(operationId = "downloadLeaveRequestAttachment", summary = "휴·복학 증빙 다운로드",
            description = "STUDENT 본인 또는 ADMIN만 조회합니다. 권한 확인 후 Academic이 MinIO 파일을 전달합니다. "
                    + "MinIO URL·저장 키는 공개하지 않으며 취소·반려 후에도 증빙을 보존합니다. 첨부 없으면 E10.")
    @ApiResponse(responseCode = "200", description = "PDF 파일(공통 JSON envelope 미사용)",
            content = @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary")))
    @GetMapping("/{id}/attachment")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ResponseEntity<byte[]> download(
            @Parameter(description = "신청 ID", example = "1") @Positive @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser actor) {
        var download = applicationService.download(id, actor);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.filename(), StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(download.bytes());
    }
}
