package com.socialshuffle.repository;

import com.socialshuffle.model.ParticipantBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantBadgeRepository extends JpaRepository<ParticipantBadge, String> {
    List<ParticipantBadge> findByParticipantIdOrderByBadgeLevelAsc(String participantId);
    Optional<ParticipantBadge> findByParticipantIdAndCurrentTrue(String participantId);
}
