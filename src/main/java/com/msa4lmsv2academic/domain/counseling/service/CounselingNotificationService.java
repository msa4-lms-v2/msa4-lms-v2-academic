package com.msa4lmsv2academic.domain.counseling.service;

import com.msa4lmsv2academic.domain.counseling.entity.Counseling;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingStatus;
import com.msa4lmsv2academic.domain.notification.entity.*;
import com.msa4lmsv2academic.domain.notification.service.NotificationService;
import com.msa4lmsv2academic.domain.user.entity.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CounselingNotificationService {
    private final NotificationService notifications;

    @Transactional(propagation = Propagation.MANDATORY)
    public void createRequested(Counseling counseling) {
        create(counseling, counseling.getProfessor().getUser(), NotificationType.COUNSELING_REQUESTED,
                null, CounselingStatus.WAITING, "새로운 온라인 상담이 신청되었습니다.", counseling.getQuestion());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createAnswered(Counseling counseling, CounselingStatus previous, boolean updated) {
        create(counseling, counseling.getStudent().getUser(), updated ? NotificationType.COUNSELING_ANSWER_UPDATED
                : NotificationType.COUNSELING_ANSWERED, previous, CounselingStatus.ANSWERED,
                updated ? "교수 상담 답변이 수정되었습니다." : "교수 상담 답변이 등록되었습니다.", counseling.getAnswer());
    }

    private void create(Counseling counseling, User recipient, NotificationType type, CounselingStatus previous,
                        CounselingStatus next, String message, String content) {
        notifications.create(recipient, NotificationCategory.COUNSELING, type, NotificationResourceType.COUNSELING,
                counseling.getId(), counseling.getTitle(), message, digest(counseling.getId(), recipient.getId(), type,
                        previous, next, content));
    }

    private String digest(Long counselingId, Long recipientId, NotificationType type, CounselingStatus previous,
                          CounselingStatus next, String content) {
        String source = String.join("|", Objects.toString(counselingId, ""), Objects.toString(recipientId, ""),
                type.name(), Objects.toString(previous, ""), next.name(), Objects.toString(content, ""));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
