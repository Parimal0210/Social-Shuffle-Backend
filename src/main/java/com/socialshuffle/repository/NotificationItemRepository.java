package com.socialshuffle.repository;

import com.socialshuffle.model.NotificationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface NotificationItemRepository extends JpaRepository<NotificationItem, String> {

    List<NotificationItem> findAllByOrderByTimestampDesc();

    @Query("SELECT n FROM NotificationItem n WHERE n.id NOT LIKE 'notif-otp-%' ORDER BY n.timestamp DESC")
    List<NotificationItem> findVisibleOrderByTimestampDesc();

    @Query("SELECT n FROM NotificationItem n WHERE n.id NOT LIKE 'notif-otp-%' AND (LOWER(COALESCE(n.recipientEmail, '')) = LOWER(:email) OR n.userId = :userId OR (COALESCE(n.recipientEmail, '') = '' AND n.type IN ('event', 'community'))) ORDER BY n.timestamp DESC")
    List<NotificationItem> findVisibleForParticipant(@Param("email") String email, @Param("userId") String userId);

    List<NotificationItem> findByReadFalse();

    List<NotificationItem> findByRecipientEmailOrderByTimestampDesc(String recipientEmail);

    List<NotificationItem> findByRecipientEmailIgnoreCaseOrderByTimestampDesc(String recipientEmail);

    List<NotificationItem> findByEventIdOrderByTimestampDesc(String eventId);

    @Transactional
    @Modifying
    @Query("UPDATE NotificationItem n SET n.read = true WHERE n.read = false")
    int markAllReadFast();

    @Transactional
    @Modifying
    @Query("UPDATE NotificationItem n SET n.read = true WHERE n.read = false AND (LOWER(COALESCE(n.recipientEmail, '')) = LOWER(:email) OR n.userId = :userId)")
    int markParticipantReadFast(@Param("email") String email, @Param("userId") String userId);
}
