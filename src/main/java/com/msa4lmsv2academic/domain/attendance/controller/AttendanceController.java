package com.msa4lmsv2academic.domain.attendance.controller;

import com.msa4lmsv2academic.domain.attendance.request.AttendanceCheckInRequestDTO;
import com.msa4lmsv2academic.domain.attendance.request.AttendanceSessionCreateRequestDTO;
import com.msa4lmsv2academic.domain.attendance.response.*;
import com.msa4lmsv2academic.domain.attendance.service.AttendanceCheckInService;
import com.msa4lmsv2academic.domain.attendance.service.AttendanceSessionService;
import com.msa4lmsv2academic.global.config.openapi.CustomApiResponse;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.response.CustomResponseCode;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/academic/attendance")
@Tag(name = "Attendance", description = "QR 출석 관리 API")
public class AttendanceController {


    private final AttendanceSessionService attendanceSessionService;
    private final AttendanceCheckInService attendanceCheckInService;

    @Operation(
            summary = "출석 세션 생성 또는 재개",
            description = "현재 학기 담당 강의의 출석 세션을 생성합니다. 같은 날짜와 교시의 종료된 세션이 있으면 재개하고 첫 QR을 발급합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.DUPLICATE_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PostMapping("/sessions")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<AttendanceSessionOpenResponseDTO>> openSession(
            @Valid @RequestBody AttendanceSessionCreateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {

            AttendanceSessionOpenResponseDTO response = attendanceSessionService.open(
                    request, currentUser
            );


            return ResponseEntity.ok(
                GlobalResponseDTO.success(response)
            );

    }


    @Operation(
            summary = "출석 QR 갱신",
            description = "진행 중인 출석 세션의 기존 QR을 대체할 새 QR 토큰을 발급합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PostMapping("/sessions/{sessionId}/qr-tokens")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<AttendanceQrResponseDTO>> renewQr(
            @Parameter(description = "출석 세션 ID", example = "3", required = true)
            @PathVariable Long sessionId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        AttendanceQrResponseDTO response = attendanceSessionService.renewQr(sessionId, currentUser);

        return ResponseEntity.ok(
                GlobalResponseDTO.success(response)
        );
    }


    @Operation(
            summary = "출석 세션 종료",
            description = "담당 교수의 진행 중인 출석 세션을 종료하고 해당 세션의 QR 사용을 중단합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.DUPLICATE_DATA,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PostMapping("/sessions/{sessionId}/close")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<AttendanceSessionCloseResponseDTO>> close(
            @Parameter(description = "종료할 출석 세션 ID", example = "3", required = true)
            @PathVariable Long sessionId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
            AttendanceSessionCloseResponseDTO response = attendanceSessionService.close(
                    sessionId,
                    currentUser
            );

            return ResponseEntity.ok(
                    GlobalResponseDTO.success(response)
            );
    }

    @Operation(
            summary = "학생 QR 출석 체크인",
            description = "로그인한 학생이 QR의 세션 ID와 토큰으로 출석합니다. 학생 ID는 JWT 인증 정보에서 확인합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.DUPLICATE_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @PostMapping("/check-ins")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponseDTO<AttendanceCheckInResponseDTO>> checkIn(
            @Valid @RequestBody AttendanceCheckInRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
            return ResponseEntity.ok(
                    GlobalResponseDTO.success(attendanceCheckInService.checkIn(request, currentUser))
            );
    }

    @Operation(
            summary = "현재 출석 세션 조회",
            description = "페이지 재접속 또는 이동 후 선택한 강의의 진행 중인 출석 세션을 복구하기 위해 조회합니다. 진행 중인 세션이 없으면 data는 null입니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping("/sessions/current")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<AttendanceSessionResponseDTO>> getCurrentSession(
            @Parameter(description = "강의 ID", example = "8", required = true)
            @RequestParam Long classId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(
                GlobalResponseDTO.success(
                        attendanceSessionService.getCurrent(
                                classId,
                                currentUser
                        )
                )
        );
    }

    @Operation(
            summary = "출석 세션 참여 현황 조회",
            description = "출석 세션의 출석 완료 인원과 전체 활성 수강생 수를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.NOT_FOUND_DATA,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping("/sessions/{sessionId}/summary")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<AttendanceSessionSummaryResponseDTO>> getSessionSummary(
            @Parameter(description = "출석 세션 ID", example = "3", required = true)
            @PathVariable Long sessionId,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        AttendanceSessionSummaryResponseDTO response = attendanceSessionService.getSummary(
                sessionId,
                currentUser
        );

        return ResponseEntity.ok(
                GlobalResponseDTO.success(response)
        );
    }

    @Operation(
            summary = "현재 학기 출석 세션 목록 조회",
            description = "로그인한 교수의 현재 학기 출석 세션을 최근 시작 순으로 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @CustomApiResponse({
            CustomResponseCode.UNAUTHENTICATED,
            CustomResponseCode.ACCESS_DENIED,
            CustomResponseCode.INVALID_PARAMETER,
            CustomResponseCode.DATABASE_ERROR,
            CustomResponseCode.SYSTEM_ERROR
    })
    @GetMapping("/sessions")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<AttendanceSessionListResponseDTO>>> getSessions(
            @Parameter(description = "페이지 번호(1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기(최대 100)", example = "5")
            @RequestParam(defaultValue = "5") int size,
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser
    ) {
        PageResponseDTO<AttendanceSessionListResponseDTO> response = attendanceSessionService.getSessions(
                page,
                size,
                currentUser
        );

        return ResponseEntity.ok(GlobalResponseDTO.success(response));
    }
}
