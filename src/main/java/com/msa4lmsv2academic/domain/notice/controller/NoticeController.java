package com.msa4lmsv2academic.domain.notice.controller;

import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

import com.msa4lmsv2academic.domain.notice.request.NoticeCreateRequestDTO;
import com.msa4lmsv2academic.domain.notice.request.NoticeSearchRequestDTO;
import com.msa4lmsv2academic.domain.notice.request.NoticeUpdateRequestDTO;
import com.msa4lmsv2academic.domain.notice.response.NoticeDetailResponseDTO;
import com.msa4lmsv2academic.domain.notice.response.NoticeSummaryResponseDTO;
import com.msa4lmsv2academic.domain.notice.service.NoticeService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notices", description = "공지사항 조회·등록·수정·논리 삭제 API")
@Validated
@RestController
@RequestMapping("/api/academic/catalog/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(
            summary = "공지사항 목록 조회",
            description = "STUDENT와 PROFESSOR는 활성 상태인 ALL 또는 본인 역할 대상 공지만 조회합니다. "
                    + "ADMIN은 대상 역할과 활성 상태 전체를 검색하며 active를 생략하면 활성·비활성을 모두 조회합니다. "
                    + "목록 응답에는 본문과 작성자 정보가 포함되지 않습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "200", description = "조회 성공. 결과가 없으면 빈 items를 반환합니다.")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<NoticeSummaryResponseDTO>>> searchNotices(
            @ParameterObject @Valid @ModelAttribute NoticeSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(noticeService.searchNotices(request, currentUser)));
    }

    @Operation(
            summary = "공지사항 상세 조회",
            description = "STUDENT와 PROFESSOR는 활성 상태인 ALL 또는 본인 역할 대상 공지만 조회합니다. "
                    + "다른 역할 대상 공지는 403, 비활성 공지는 404로 처리하며 ADMIN은 모든 공지를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "200", description = "조회 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping("/{noticeId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalResponseDTO<NoticeDetailResponseDTO>> getNotice(
            @Parameter(description = "공지사항 ID", example = "1", required = true)
            @PathVariable @Positive(message = "noticeId는 양수여야 합니다.") Long noticeId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(noticeService.getNotice(noticeId, currentUser)));
    }

    @Operation(
            summary = "공지사항 등록",
            description = "ADMIN만 공지사항을 등록할 수 있습니다. 공지는 활성 상태로 생성되고 작성자 ID는 "
                    + "Gateway 인증 사용자로 기록합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "201", description = "등록 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<NoticeDetailResponseDTO>> createNotice(
            @Valid @RequestBody NoticeCreateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @Parameter(description = "분산 추적 및 감사 로그 요청 ID", example = "01JABCDEF1234567890")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        NoticeDetailResponseDTO response = noticeService.createNotice(
                request,
                currentUser,
                requestId,
                httpServletRequest.getRemoteAddr()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponseDTO.success(response));
    }

    @Operation(
            summary = "공지사항 부분 수정 및 복구",
            description = "ADMIN만 제목, 본문, 대상 역할, 활성 상태를 부분 수정합니다. 비활성 공지도 수정·복구할 수 "
                    + "있으며 같은 활성 상태 요청은 409로 처리합니다. 본문에 공백 문자열을 전달하면 null로 제거합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "200", description = "수정 성공 또는 상태 외 동일 값 요청")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.DUPLICATE_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PatchMapping("/{noticeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<NoticeDetailResponseDTO>> updateNotice(
            @Parameter(description = "공지사항 ID", example = "1", required = true)
            @PathVariable @Positive(message = "noticeId는 양수여야 합니다.") Long noticeId,
            @Valid @RequestBody NoticeUpdateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @Parameter(description = "분산 추적 및 감사 로그 요청 ID", example = "01JABCDEF1234567890")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(noticeService.updateNotice(
                noticeId,
                request,
                currentUser,
                requestId,
                httpServletRequest.getRemoteAddr()
        )));
    }

    @Operation(
            summary = "공지사항 비활성화",
            description = "ADMIN만 공지사항 상태를 비활성으로 변경할 수 있습니다. 실제 행은 제거하지 않으며 "
                    + "이미 비활성인 공지를 다시 요청하면 409로 처리합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "200", description = "비활성화 성공. data는 null입니다.")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.DUPLICATE_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PatchMapping("/{noticeId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponseDTO<Void>> deactivateNotice(
            @Parameter(description = "공지사항 ID", example = "1", required = true)
            @PathVariable @Positive(message = "noticeId는 양수여야 합니다.") Long noticeId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @Parameter(description = "분산 추적 및 감사 로그 요청 ID", example = "01JABCDEF1234567890")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        noticeService.deleteNotice(
                noticeId,
                currentUser,
                requestId,
                httpServletRequest.getRemoteAddr()
        );
        return ResponseEntity.ok(GlobalResponseDTO.success());
    }
}
