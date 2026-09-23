package com.socialshuffle.repository;

import com.socialshuffle.model.NotificationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationItemRepository extends JpaRepository<NotificationItem, String> {

    List<NotificationItem> findAllByOrderByTimestampDesc();

    List<NotificationItem> findByReadFalse();
}
