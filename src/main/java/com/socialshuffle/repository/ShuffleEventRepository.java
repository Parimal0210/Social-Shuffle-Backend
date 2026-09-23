package com.socialshuffle.repository;

import com.socialshuffle.model.ShuffleEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShuffleEventRepository extends MongoRepository<ShuffleEvent, String> {

    List<ShuffleEvent> findByStatusIgnoreCase(String status);

    List<ShuffleEvent> findByStatusIn(List<String> statuses);

    List<ShuffleEvent> findAllByOrderByNumberDesc();

    List<ShuffleEvent> findAllByOrderByDateDesc();

    Optional<ShuffleEvent> findFirstByStatusInOrderByDateAsc(List<String> statuses);
}
