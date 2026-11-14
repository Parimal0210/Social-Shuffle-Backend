package com.socialshuffle.repository;

import com.socialshuffle.model.ParticipantBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantBadgeRepository extends JpaRepository<ParticipantBadge, String> {
    List<ParticipantBadge> findByParticipantIdOrderByBadgeLevelAsc(String participantId);
    Optional<ParticipantBadge> findByParticipantIdAndCurrentTrue(String participantId);

    @Transactional
    @Modifying
    @Query("DELETE FROM ParticipantBadge b WHERE b.participantId = :participantId")
    void deleteByParticipantId(@Param("participantId") String participantId);
}
