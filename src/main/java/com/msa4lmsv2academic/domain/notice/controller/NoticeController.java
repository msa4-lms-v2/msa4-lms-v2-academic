package com.msa4lmsv2academic.domain.notice.controller;

import com.msa4lmsv2academic.domain.notice.request.NoticeCreateRequestDTO;
import com.msa4lmsv2academic.domain.notice.request.NoticeSearchRequestDTO;
import com.msa4lmsv2academic.domain.notice.request.NoticeUpdateRequestDTO;
import com.msa4lmsv2academic.domain.notice.response.NoticeDetailResponseDTO;
import com.msa4lmsv2academic.domain.notice.response.NoticeSummaryResponseDTO;
import com.msa4lmsv2academic.domain.notice.service.NoticeService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공. 결과가 없으면 빈 items를 반환합니다."),
            @ApiResponse(responseCode = "400", description = "잘못된 페이지·검색 조건",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "403", description = "일반 사용자가 다른 역할 필터를 요청함",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalRes<PageRes<NoticeSummaryResponseDTO>>> searchNotices(
            @ParameterObject @Valid @ModelAttribute NoticeSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalRes.success(noticeService.searchNotices(request, currentUser)));
    }

    @Operation(
            summary = "공지사항 상세 조회",
            description = "STUDENT와 PROFESSOR는 활성 상태인 ALL 또는 본인 역할 대상 공지만 조회합니다. "
                    + "다른 역할 대상 공지는 403, 비활성 공지는 404로 처리하며 ADMIN은 모든 공지를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 공지사항 ID",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "403", description = "공지 대상 역할 불일치",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "404", description = "공지사항 없음 또는 일반 사용자에게 숨겨진 비활성 공지",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class)))
    })
    @GetMapping("/{noticeId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalRes<NoticeDetailResponseDTO>> getNotice(
            @Parameter(description = "공지사항 ID", example = "1", required = true)
            @PathVariable @Positive(message = "noticeId는 양수여야 합니다.") Long noticeId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalRes.success(noticeService.getNotice(noticeId, currentUser)));
    }

    @Operation(
            summary = "공지사항 등록",
            description = "ADMIN만 공지사항을 등록할 수 있습니다. 공지는 활성 상태로 생성되고 작성자 ID는 "
                    + "Gateway 인증 사용자로 기록합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "필수값 누락 또는 입력값 오류",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "404", description = "Academic에 동기화된 관리자 정보 없음",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalRes<NoticeDetailResponseDTO>> createNotice(
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
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalRes.success(response));
    }

    @Operation(
            summary = "공지사항 부분 수정 및 복구",
            description = "ADMIN만 제목, 본문, 대상 역할, 활성 상태를 부분 수정합니다. 비활성 공지도 수정·복구할 수 "
                    + "있으며 같은 활성 상태 요청은 409로 처리합니다. 본문에 공백 문자열을 전달하면 null로 제거합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공 또는 상태 외 동일 값 요청"),
            @ApiResponse(responseCode = "400", description = "빈 PATCH 또는 입력값 오류",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "404", description = "공지사항 없음",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "409", description = "같은 활성 상태 요청",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class)))
    })
    @PatchMapping("/{noticeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalRes<NoticeDetailResponseDTO>> updateNotice(
            @Parameter(description = "공지사항 ID", example = "1", required = true)
            @PathVariable @Positive(message = "noticeId는 양수여야 합니다.") Long noticeId,
            @Valid @RequestBody NoticeUpdateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @Parameter(description = "분산 추적 및 감사 로그 요청 ID", example = "01JABCDEF1234567890")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(GlobalRes.success(noticeService.updateNotice(
                noticeId,
                request,
                currentUser,
                requestId,
                httpServletRequest.getRemoteAddr()
        )));
    }

    @Operation(
            summary = "공지사항 논리 삭제",
            description = "ADMIN만 공지사항을 삭제할 수 있습니다. 실제 행은 제거하지 않고 비활성 상태로 변경하며 "
                    + "이미 비활성인 공지를 다시 삭제하면 409로 처리합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "논리 삭제 성공. data는 null입니다."),
            @ApiResponse(responseCode = "400", description = "잘못된 공지사항 ID",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "404", description = "공지사항 없음",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class))),
            @ApiResponse(responseCode = "409", description = "이미 비활성 상태인 공지",
                    content = @Content(schema = @Schema(implementation = GlobalRes.class)))
    })
    @DeleteMapping("/{noticeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalRes<Void>> deleteNotice(
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
        return ResponseEntity.ok(GlobalRes.success());
    }
}
