package com.socialshuffle.repository;

import com.socialshuffle.model.ShuffleEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShuffleEventRepository extends JpaRepository<ShuffleEvent, String> {

    List<ShuffleEvent> findByStatusIgnoreCase(String status);

    List<ShuffleEvent> findByStatusIn(List<String> statuses);

    List<ShuffleEvent> findAllByOrderByNumberDesc();

    List<ShuffleEvent> findAllByOrderByDateDesc();

    Optional<ShuffleEvent> findFirstByStatusInOrderByDateAsc(List<String> statuses);
}
