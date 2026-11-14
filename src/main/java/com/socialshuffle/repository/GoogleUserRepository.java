package com.socialshuffle.repository;

import com.socialshuffle.model.GoogleUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface GoogleUserRepository extends JpaRepository<GoogleUser, String> {

    Optional<GoogleUser> findByEmailIgnoreCase(String email);

    @Transactional
    @Modifying
    @Query("DELETE FROM GoogleUser g WHERE g.participantId = :participantId OR LOWER(g.email) = LOWER(:email)")
    void deleteByParticipantIdOrEmail(@Param("participantId") String participantId, @Param("email") String email);
}
