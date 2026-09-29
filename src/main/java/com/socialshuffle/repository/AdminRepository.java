package com.socialshuffle.repository;

import com.socialshuffle.model.AdminAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<AdminAccount, String> {

    Optional<AdminAccount> findByEmailIgnoreCase(String email);

    Optional<AdminAccount> findByPhone(String phone);

    @Query("SELECT a FROM AdminAccount a WHERE (LOWER(a.email) = LOWER(:identifier) OR a.phone = :identifier) AND a.active = true")
    Optional<AdminAccount> findActiveByIdentifier(@Param("identifier") String identifier);

    boolean existsByEmailIgnoreCase(String email);
}
