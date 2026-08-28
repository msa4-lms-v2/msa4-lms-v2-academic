package com.msa4lmsv2academic.domain.counseling.repository;

import com.msa4lmsv2academic.domain.counseling.entity.CounselingNotification;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CounselingNotificationRepository extends JpaRepository<CounselingNotification, Long> {

    boolean existsByDeduplicationKey(String deduplicationKey);

    @EntityGraph(attributePaths = {
            "appointment",
            "appointment.student",
            "appointment.student.user",
            "appointment.professor",
            "appointment.professor.user",
            "recipient"
    })
    Page<CounselingNotification> findByRecipientId(Long recipientUserId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "appointment",
            "appointment.student",
            "appointment.student.user",
            "appointment.professor",
            "appointment.professor.user",
            "recipient"
    })
    Page<CounselingNotification> findByRecipientIdAndReadAtIsNull(Long recipientUserId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "appointment",
            "appointment.student",
            "appointment.student.user",
            "appointment.professor",
            "appointment.professor.user",
            "recipient"
    })
    @Query("select notification from CounselingNotification notification "
            + "where notification.id = :notificationId and notification.recipient.id = :recipientUserId")
    Optional<CounselingNotification> findOwnedByIdForUpdate(
            @Param("notificationId") Long notificationId,
            @Param("recipientUserId") Long recipientUserId
    );
}
