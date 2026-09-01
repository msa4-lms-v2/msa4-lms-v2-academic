package com.msa4lmsv2academic.domain.counseling.controller;

import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

import com.msa4lmsv2academic.domain.counseling.request.CounselingAppointmentCreateRequestDTO;
import com.msa4lmsv2academic.domain.counseling.request.CounselingAppointmentSearchRequestDTO;
import com.msa4lmsv2academic.domain.counseling.request.CounselingAppointmentStatusRequestDTO;
import com.msa4lmsv2academic.domain.counseling.request.CounselingNotificationSearchRequestDTO;
import com.msa4lmsv2academic.domain.counseling.request.CounselorAvailabilityReplaceRequestDTO;
import com.msa4lmsv2academic.domain.counseling.response.CounselingAppointmentResponseDTO;
import com.msa4lmsv2academic.domain.counseling.response.CounselingNotificationResponseDTO;
import com.msa4lmsv2academic.domain.counseling.response.CounselorAvailabilityResponseDTO;
import com.msa4lmsv2academic.domain.counseling.service.CounselingAppointmentService;
import com.msa4lmsv2academic.domain.counseling.service.CounselingNotificationService;
import com.msa4lmsv2academic.domain.counseling.service.CounselorAvailabilityService;
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
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Counseling", description = "교수 상담 가능 시간과 학생 상담 예약 API")
@Validated
@RestController
@RequestMapping("/api/academic/counseling")
@RequiredArgsConstructor
public class CounselingController {

    private final CounselorAvailabilityService availabilityService;
    private final CounselingAppointmentService appointmentService;
    private final CounselingNotificationService notificationService;

    @Operation(
            summary = "상담 가능 시간 조회",
            description = "학생은 가용 시간을 공개한 전체 교수를, 교수는 기본적으로 본인의 시간을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "200", description = "조회 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping("/availability")
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalResponseDTO<List<CounselorAvailabilityResponseDTO>>> getAvailabilities(
            @RequestParam(required = false) @Positive Long professorId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                availabilityService.getAvailabilities(professorId, currentUser)
        ));
    }

    @Operation(
            summary = "상담 가능 시간 전체 교체",
            description = "교수가 자신의 상담 가능 시간 목록을 전체 교체합니다. 빈 목록은 공개 중단을 뜻합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "200", description = "교체 성공")
    @CustomApiResponse({
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PutMapping("/availability")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<List<CounselorAvailabilityResponseDTO>>> replaceAvailabilities(
            @Valid @RequestBody CounselorAvailabilityReplaceRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                availabilityService.replaceAvailabilities(request, currentUser)
        ));
    }

    @Operation(
            summary = "상담 예약 목록 조회",
            description = "학생과 교수는 자신이 참여한 예약을 조회하고, 관리자는 전체 예약을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/appointments")
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<CounselingAppointmentResponseDTO>>> searchAppointments(
            @ParameterObject @Valid @ModelAttribute CounselingAppointmentSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(appointmentService.search(request, currentUser)));
    }

    @Operation(
            summary = "상담 예약 상세 조회",
            description = "예약 참여자와 관리자가 예약 상태와 교수 메모를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/appointments/{appointmentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GlobalResponseDTO<CounselingAppointmentResponseDTO>> getAppointment(
            @PathVariable @Positive Long appointmentId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(appointmentService.get(appointmentId, currentUser)));
    }

    @Operation(
            summary = "상담 예약",
            description = "학생이 가용 시간을 공개한 교수의 30분 상담 슬롯을 예약합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "201", description = "예약 성공")
    @CustomApiResponse({
            CustomResponseCode.DUPLICATE_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PostMapping("/appointments")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponseDTO<CounselingAppointmentResponseDTO>> createAppointment(
            @Valid @RequestBody CounselingAppointmentCreateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponseDTO.success(appointmentService.create(request, currentUser)));
    }

    @Operation(
            summary = "상담 예약 상태 변경",
            description = "학생은 본인 예약을 취소하고, 교수는 예약 승인·반려·완료와 교수 메모를 관리합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "200", description = "상태 변경 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.DUPLICATE_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PatchMapping("/appointments/{appointmentId}/status")
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<CounselingAppointmentResponseDTO>> changeAppointmentStatus(
            @PathVariable @Positive Long appointmentId,
            @Valid @RequestBody CounselingAppointmentStatusRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(
                appointmentService.changeStatus(appointmentId, request, currentUser)
        ));
    }

    @Operation(
            summary = "내 상담 알림 목록 조회",
            description = "학생과 교수가 본인에게 전달된 상담 변경·취소 알림을 최신순으로 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "200", description = "조회 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping("/notifications")
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<CounselingNotificationResponseDTO>>> searchNotifications(
            @ParameterObject @Valid @ModelAttribute CounselingNotificationSearchRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(notificationService.search(request, currentUser)));
    }

    @Operation(
            summary = "상담 알림 읽음 처리",
            description = "학생과 교수가 본인에게 전달된 상담 알림을 읽음으로 변경합니다. 반복 호출해도 같은 결과를 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
            @ApiResponse(responseCode = "200", description = "읽음 처리 성공")
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PatchMapping("/notifications/{notificationId}/read")
    @PreAuthorize("hasAnyRole('STUDENT', 'PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<CounselingNotificationResponseDTO>> markNotificationRead(
            @PathVariable @Positive Long notificationId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(notificationService.markRead(notificationId, currentUser)));
    }
}
