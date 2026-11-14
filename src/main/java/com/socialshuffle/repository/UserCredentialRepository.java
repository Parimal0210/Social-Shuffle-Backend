package com.socialshuffle.repository;

import com.socialshuffle.model.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserCredentialRepository extends JpaRepository<UserCredential, String> {

    Optional<UserCredential> findByEmailIgnoreCase(String email);

    Optional<UserCredential> findFirstByPhone(String phone);

    Optional<UserCredential> findFirstBySubjectId(String subjectId);

    Optional<UserCredential> findFirstBySubjectTypeAndSubjectId(String subjectType, String subjectId);

    @Query("SELECT c FROM UserCredential c WHERE LOWER(c.email) = LOWER(:identifier) OR c.phone = :identifier OR LOWER(c.username) = LOWER(:identifier)")
    Optional<UserCredential> findByIdentifier(@Param("identifier") String identifier);

    boolean existsByEmailIgnoreCase(String email);

    @Transactional
    @Modifying
    @Query("DELETE FROM UserCredential c WHERE c.subjectId = :subjectId OR LOWER(c.email) = LOWER(:email)")
    void deleteBySubjectIdOrEmail(@Param("subjectId") String subjectId, @Param("email") String email);
}
