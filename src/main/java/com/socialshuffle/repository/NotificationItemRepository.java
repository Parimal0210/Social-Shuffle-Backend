package com.socialshuffle.repository;

import com.socialshuffle.model.NotificationItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationItemRepository extends MongoRepository<NotificationItem, String> {

    List<NotificationItem> findAllByOrderByTimestampDesc();

    List<NotificationItem> findByReadFalse();
}
