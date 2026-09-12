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
import com.msa4lmsv2academic.global.scheduling.CronScheduling;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * spring.threads.virtual.enabled=true 환경에서 Spring {@code @Scheduled}가 배포 pod에서 실행되지
 * 않는 현상이 확인돼(OutboxWorker 참고), 별도 ScheduledExecutorService로 직접 폴링한다.
 * warnStudentsWithoutNextTermAction(LocalDate)의 @Transactional은 AOP 프록시를 거쳐야 적용되므로,
 * this로 직접 호출하지 않고 지연 주입한 자기 자신(self)의 프록시를 통해 호출한다(self-invocation 우회).
 */
@Slf4j
@Service
public class LeaveExpiryWarningService {
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final List<LeaveRequestType> ACTION_TYPES = List.of(
            LeaveRequestType.GENERAL_LEAVE, LeaveRequestType.GENERAL_RETURN, LeaveRequestType.MILITARY_RETURN);
    private static final List<LeaveRequestStatus> ACTION_STATUSES = List.of(
            LeaveRequestStatus.PENDING, LeaveRequestStatus.ADVISOR_APPROVED, LeaveRequestStatus.APPROVED);

    private final LeaveRequestQueryRepository queries;
    private final LeaveRequestRepository repository;
    private final NotificationService notifications;
    private final LeaveExpiryWarningService self;

    @Value("${academic.leave.expiry-warning.cron:0 0 9 * * *}")
    private String cron;

    private ScheduledExecutorService scheduler;

    public LeaveExpiryWarningService(LeaveRequestQueryRepository queries, LeaveRequestRepository repository,
                                      NotificationService notifications, @Lazy LeaveExpiryWarningService self) {
        this.queries = queries;
        this.repository = repository;
        this.notifications = notifications;
        this.self = self;
    }

    @PostConstruct
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "leave-expiry-warning-scheduler");
            thread.setDaemon(true);
            return thread;
        });
        CronScheduling.scheduleCron(scheduler, cron, KOREA_ZONE, this::runWarnStudentsWithoutNextTermAction);
    }

    @PreDestroy
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private void runWarnStudentsWithoutNextTermAction() {
        try {
            self.warnStudentsWithoutNextTermAction(LocalDate.now(KOREA_ZONE));
        } catch (Exception exception) {
            log.error("휴학 복학 미조치 학생 알림 발송 중 예상치 못한 예외 발생", exception);
        }
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
