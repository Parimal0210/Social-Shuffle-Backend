package com.socialshuffle.repository;

import com.socialshuffle.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByPhone(String phone);

    @Query("{ '$or': [ { 'email': { '$regex': ?0, '$options': 'i' } }, { 'phone': ?0 } ] }")
    Optional<User> findByEmailOrPhone(String emailOrPhone);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);
}
