package com.socialshuffle.repository;

import com.socialshuffle.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findFirstByPhone(String phone);

    default Optional<User> findByPhone(String phone) {
        return findFirstByPhone(phone);
    }

    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:identifier) OR u.phone = :identifier ORDER BY u.id ASC")
    List<User> findAllByEmailOrPhone(@Param("identifier") String identifier);

    default Optional<User> findByEmailOrPhone(String identifier) {
        return findAllByEmailOrPhone(identifier).stream().findFirst();
    }

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);
}
