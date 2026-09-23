package com.socialshuffle.repository;

import com.socialshuffle.model.EventFeedback;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventFeedbackRepository extends MongoRepository<EventFeedback, String> {

    List<EventFeedback> findByEventId(String eventId);

    List<EventFeedback> findAllByOrderByCreatedAtDesc();
}
