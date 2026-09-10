package com.msa4lmsv2academic.domain.leaverequest.service;

import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequest;
import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequestStatus;
import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequestType;
import com.msa4lmsv2academic.domain.leaverequest.repository.LeaveRequestQueryRepository;
import com.msa4lmsv2academic.domain.leaverequest.repository.LeaveRequestRepository;
import com.msa4lmsv2academic.domain.notification.entity.NotificationCategory;
import com.msa4lmsv2academic.domain.notification.entity.NotificationResourceType;
import com.msa4lmsv2academic.domain.notification.entity.NotificationType;
import com.msa4lmsv2academic.domain.notification.service.NotificationService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeaveExpiryWarningService {
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final List<LeaveRequestType> ACTION_TYPES = List.of(
            LeaveRequestType.GENERAL_LEAVE, LeaveRequestType.GENERAL_RETURN, LeaveRequestType.MILITARY_RETURN);
    private static final List<LeaveRequestStatus> ACTION_STATUSES = List.of(
            LeaveRequestStatus.PENDING, LeaveRequestStatus.APPROVED);

    private final LeaveRequestQueryRepository queries;
    private final LeaveRequestRepository repository;
    private final NotificationService notifications;

    @Scheduled(cron = "${academic.leave.expiry-warning.cron:0 0 9 * * *}", zone = "Asia/Seoul")
    @Transactional
    public void warnStudentsWithoutNextTermAction() {
        warnStudentsWithoutNextTermAction(LocalDate.now(KOREA_ZONE));
    }

    @Transactional
    public void warnStudentsWithoutNextTermAction(LocalDate today) {
        for (LeaveRequest leave : queries.findApprovedLeavesEndedBefore(today)) {
            if (leave.getReturnYear() == null || leave.getReturnSemester() == null || hasActionForReturnTerm(leave)) continue;
            String returnTerm = leave.getReturnYear() + "학년도 " + leave.getReturnSemester() + "학기";
            notifications.create(leave.getStudent().getUser(), NotificationCategory.ACADEMIC,
                    NotificationType.LEAVE_ACTION_REQUIRED, NotificationResourceType.LEAVE_REQUEST, leave.getId(),
                    "휴·복학 신청이 필요합니다.", returnTerm + " 복학 또는 휴학 연장 신청을 확인해 주세요.",
                    digest(leave.getId(), leave.getReturnYear(), leave.getReturnSemester()));
        }
    }

    private boolean hasActionForReturnTerm(LeaveRequest leave) {
        return repository.existsByStudentIdAndTargetYearAndTargetSemesterAndRequestTypeInAndStatusIn(
                leave.getStudent().getId(), leave.getReturnYear(), leave.getReturnSemester(), ACTION_TYPES, ACTION_STATUSES);
    }

    private String digest(Long leaveRequestId, short returnYear, byte returnSemester) {
        String source = "LEAVE_ACTION_REQUIRED|" + leaveRequestId + "|" + returnYear + "|" + returnSemester;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
