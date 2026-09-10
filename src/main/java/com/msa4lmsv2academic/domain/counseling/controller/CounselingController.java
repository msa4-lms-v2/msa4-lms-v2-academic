package com.msa4lmsv2academic.domain.counseling.controller;

import com.msa4lmsv2academic.domain.counseling.request.CounselingAnswerRequestDTO;
import com.msa4lmsv2academic.domain.counseling.request.CounselingCreateRequestDTO;
import com.msa4lmsv2academic.domain.counseling.request.CounselingNotificationSearchRequestDTO;
import com.msa4lmsv2academic.domain.counseling.request.CounselingSearchRequestDTO;
import com.msa4lmsv2academic.domain.counseling.response.CounselingResponseDTO;
import com.msa4lmsv2academic.domain.counseling.response.CounselingProfessorResponseDTO;
import com.msa4lmsv2academic.domain.counseling.service.CounselingService;
import com.msa4lmsv2academic.domain.notification.response.NotificationResponseDTO;
import com.msa4lmsv2academic.domain.notification.service.NotificationService;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Counseling", description = "학생 신청·교수 답변 방식의 온라인 상담 API")
@Validated
@RestController
@RequestMapping("/api/academic/counseling")
@RequiredArgsConstructor
public class CounselingController {
    private final CounselingService counselingService;
    private final NotificationService notificationService;

    @Operation(summary = "내 담당 교수 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/professor")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponseDTO<CounselingProfessorResponseDTO>> professor(
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser user) {
        return ResponseEntity.ok(GlobalResponseDTO.success(counselingService.professor(user)));
    }

    @Operation(summary = "내 상담 목록 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<CounselingResponseDTO>>> search(
            @ParameterObject @Valid @ModelAttribute CounselingSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser user) {
        return ResponseEntity.ok(GlobalResponseDTO.success(counselingService.search(request, user)));
    }

    @Operation(summary = "상담 상세 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{counselingId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalResponseDTO<CounselingResponseDTO>> get(
            @PathVariable @Positive Long counselingId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser user) {
        return ResponseEntity.ok(GlobalResponseDTO.success(counselingService.get(counselingId, user)));
    }

    @Operation(summary = "온라인 상담 신청", description = "학생이 교수, 제목, 상담 내용을 선택해 신청합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponseDTO<CounselingResponseDTO>> create(
            @Valid @RequestBody CounselingCreateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponseDTO.success(counselingService.create(request, user)));
    }

    @Operation(summary = "온라인 상담 답변 등록·수정", description = "상담 대상 교수만 답변을 등록하거나 수정합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/{counselingId}/answer")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<CounselingResponseDTO>> answer(
            @PathVariable @Positive Long counselingId,
            @Valid @RequestBody CounselingAnswerRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser user) {
        return ResponseEntity.ok(GlobalResponseDTO.success(counselingService.answer(counselingId, request, user)));
    }

    @Operation(summary = "내 상담 알림 목록 조회", description = "공통 알림 API의 상담 범주 호환 경로입니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/notifications")
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<NotificationResponseDTO>>> notifications(
            @ParameterObject @Valid @ModelAttribute CounselingNotificationSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser user) {
        return ResponseEntity.ok(GlobalResponseDTO.success(notificationService.searchCounseling(
                request.resolvedPage(), request.resolvedSize(), request.resolvedUnreadOnly(), user)));
    }

    @Operation(summary = "상담 알림 읽음 처리", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/notifications/{notificationId}/read")
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<NotificationResponseDTO>> markRead(
            @PathVariable @Positive Long notificationId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser user) {
        return ResponseEntity.ok(GlobalResponseDTO.success(notificationService.markRead(notificationId, user)));
    }
}
