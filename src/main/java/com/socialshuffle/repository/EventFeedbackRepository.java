package com.socialshuffle.repository;

import com.socialshuffle.model.EventFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventFeedbackRepository extends JpaRepository<EventFeedback, String> {

    List<EventFeedback> findByEventId(String eventId);

    List<EventFeedback> findAllByOrderByCreatedAtDesc();
}
