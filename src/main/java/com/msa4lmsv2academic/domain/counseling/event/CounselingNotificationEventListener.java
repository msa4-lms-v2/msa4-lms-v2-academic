package com.msa4lmsv2academic.domain.counseling.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class CounselingNotificationEventListener {
    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(CounselingNotificationCreatedEvent event) {
        try {
            messagingTemplate.convertAndSendToUser(
                    event.recipientUserId().toString(),
                    "/queue/notifications",
                    event.payload()
            );
        } catch (MessagingException exception) {
            log.warn(
                    "상담 알림 실시간 전송 실패: notificationId={}",
                    event.payload().notificationId(),
                    exception
            );
        }
    }
}
