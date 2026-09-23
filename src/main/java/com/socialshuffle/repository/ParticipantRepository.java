package com.socialshuffle.repository;

import com.socialshuffle.model.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, String> {

    Optional<Participant> findByEmailIgnoreCase(String email);

    Optional<Participant> findByPhone(String phone);

    Optional<Participant> findByEmailIgnoreCaseOrPhone(String email, String phone);

    List<Participant> findByAreaIgnoreCase(String area);

    @Query("SELECT p FROM Participant p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "p.phone LIKE CONCAT('%', :keyword, '%') OR " +
           "LOWER(p.area) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Participant> searchParticipants(@Param("keyword") String keyword);
}
