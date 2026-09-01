package com.msa4lmsv2academic.domain.attendance.controller;

import com.msa4lmsv2academic.domain.attendance.request.AttendanceCheckInRequestDTO;
import com.msa4lmsv2academic.domain.attendance.request.AttendanceSessionCreateRequestDTO;
import com.msa4lmsv2academic.domain.attendance.response.*;
import com.msa4lmsv2academic.domain.attendance.service.AttendanceCheckInService;
import com.msa4lmsv2academic.domain.attendance.service.AttendanceSessionService;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import okhttp3.Response;
import org.checkerframework.checker.units.qual.Current;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/academic/attendance")
public class AttendanceController {


    private final AttendanceSessionService attendanceSessionService;
    private final AttendanceCheckInService attendanceCheckInService;

    @PostMapping("/sessions")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<AttendanceSessionOpenResponseDTO>> openSession(
            @Valid @RequestBody AttendanceSessionCreateRequestDTO request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {

            AttendanceSessionOpenResponseDTO response = attendanceSessionService.open(
                    request, currentUser
            );


            return ResponseEntity.ok(
                GlobalResponseDTO.success(response)
            );

    }


    // QR 갱신 api
    @PostMapping("/sessions/{sessionId}/qr-tokens")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<AttendanceQrResponseDTO>> renewQr(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        AttendanceQrResponseDTO response = attendanceSessionService.renewQr(sessionId, currentUser);

        return ResponseEntity.ok(
                GlobalResponseDTO.success(response)
        );
    }


    // 출석 세션 종료
    @PostMapping("/sessions/{sessionId}/close")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<AttendanceSessionCloseResponseDTO>> close(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
            AttendanceSessionCloseResponseDTO response = attendanceSessionService.close(
                    sessionId,
                    currentUser
            );

            return ResponseEntity.ok(
                    GlobalResponseDTO.success(response)
            );
    }

    // 학생 출석 체크인
    @PostMapping("/check-ins")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponseDTO<AttendanceCheckInResponseDTO>> checkIn(
            @Valid @RequestBody AttendanceCheckInRequestDTO request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
            return ResponseEntity.ok(
                    GlobalResponseDTO.success(attendanceCheckInService.checkIn(request, currentUser))
            );
    }

    // 현재 세션 조회 해서 새로고침이나 페이지 이동 시 복구
    @GetMapping("/sessions/current")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<AttendanceSessionResponseDTO>> getCurrentSession(
            @RequestParam Long classId,
            @AuthenticationPrincipal CurrentUser currentUser
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

    // 출석 세션의 현재 참여 인원
    @GetMapping("/sessions/{sessionId}/summary")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<AttendanceSessionSummaryResponseDTO>> getSessionSummary(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        AttendanceSessionSummaryResponseDTO response = attendanceSessionService.getSummary(
                sessionId,
                currentUser
        );

        return ResponseEntity.ok(
                GlobalResponseDTO.success(response)
        );
    }

    // 현재 학기 출석 세션 내역 조회
    @GetMapping("/sessions")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<GlobalResponseDTO<PageResponseDTO<AttendanceSessionListResponseDTO>>> getSessions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        PageResponseDTO<AttendanceSessionListResponseDTO> response = attendanceSessionService.getSessions(
                page,
                size,
                currentUser
        );

        return ResponseEntity.ok(GlobalResponseDTO.success(response));
    }
}
