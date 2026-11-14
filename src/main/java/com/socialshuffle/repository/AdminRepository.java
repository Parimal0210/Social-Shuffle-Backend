package com.socialshuffle.repository;

import com.socialshuffle.model.AdminAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<AdminAccount, String> {

    List<AdminAccount> findByActiveTrue();

    Optional<AdminAccount> findByEmailIgnoreCase(String email);

    Optional<AdminAccount> findFirstByPhone(String phone);

    default Optional<AdminAccount> findByPhone(String phone) {
        return findFirstByPhone(phone);
    }

    @Query("SELECT a FROM AdminAccount a WHERE (LOWER(a.email) = LOWER(:identifier) OR a.phone = :identifier) AND a.active = true ORDER BY a.id ASC")
    List<AdminAccount> findAllActiveByIdentifier(@Param("identifier") String identifier);

    default Optional<AdminAccount> findActiveByIdentifier(String identifier) {
        return findAllActiveByIdentifier(identifier).stream().findFirst();
    }

    boolean existsByEmailIgnoreCase(String email);
}
