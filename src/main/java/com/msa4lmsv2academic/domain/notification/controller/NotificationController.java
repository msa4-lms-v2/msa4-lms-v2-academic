package com.msa4lmsv2academic.domain.notification.controller;

import com.msa4lmsv2academic.domain.notification.request.NotificationSearchRequestDTO;
import com.msa4lmsv2academic.domain.notification.response.NotificationResponseDTO;
import com.msa4lmsv2academic.domain.notification.service.NotificationService;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/academic/notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {
    private final NotificationService service;

    @Operation(operationId = "searchNotifications", summary = "내 알림 목록 조회",
            description = "STUDENT·PROFESSOR·ADMIN이 자신의 상담·학사 등 공통 알림을 조회합니다. category로 범주를 필터링할 수 있습니다.")
    @ApiResponse(responseCode = "200", description = "00: 조회 성공")
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT','PROFESSOR','ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<NotificationResponseDTO>>> search(
            @ParameterObject @Valid @ModelAttribute NotificationSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser user) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.search(request, user)));
    }

    @Operation(operationId = "markNotificationRead", summary = "알림 읽음 처리",
            description = "본인 수신 알림만 읽음 처리합니다. 이미 읽은 알림은 현재 상태를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "00: 읽음 처리 성공")
    @PatchMapping("/{notificationId}/read")
    @PreAuthorize("hasAnyRole('STUDENT','PROFESSOR','ADMIN')")
    public ResponseEntity<GlobalResponseDTO<NotificationResponseDTO>> markRead(
            @Parameter(in = ParameterIn.PATH, description = "알림 ID", required = true, schema = @Schema(minimum = "1"))
            @Positive @PathVariable Long notificationId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser user) {
        return ResponseEntity.ok(GlobalResponseDTO.success(service.markRead(notificationId, user)));
    }
}
