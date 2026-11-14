package com.socialshuffle.repository;

import com.socialshuffle.model.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, String> {

    Optional<OtpVerification> findByEmailIgnoreCase(String email);

    @Transactional
    @Modifying
    @Query("DELETE FROM OtpVerification o WHERE LOWER(o.email) = LOWER(:email)")
    void deleteByEmailIgnoreCase(@Param("email") String email);
}
