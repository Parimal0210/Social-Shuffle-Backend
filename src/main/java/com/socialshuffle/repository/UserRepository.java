package com.socialshuffle.repository;

import com.socialshuffle.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByPhone(String phone);

    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:identifier) OR u.phone = :identifier")
    Optional<User> findByEmailOrPhone(@Param("identifier") String identifier);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);
}
