package com.socialshuffle.repository;

import com.socialshuffle.model.ParticipantConqueredGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ParticipantConqueredGameRepository extends JpaRepository<ParticipantConqueredGame, String> {

    List<ParticipantConqueredGame> findByParticipantIdOrderByDatePlayedDescNameAsc(String participantId);

    List<ParticipantConqueredGame> findByParticipantIdAndSourceOrderByDatePlayedDesc(String participantId, String source);

    @Transactional
    @Modifying
    @Query("DELETE FROM ParticipantConqueredGame p WHERE p.participantId = :participantId")
    void deleteByParticipantId(@Param("participantId") String participantId);

    @Transactional
    @Modifying
    @Query("DELETE FROM ParticipantConqueredGame p WHERE p.participantId = :participantId AND p.id = :id")
    void deleteByParticipantIdAndId(@Param("participantId") String participantId, @Param("id") String id);
}
