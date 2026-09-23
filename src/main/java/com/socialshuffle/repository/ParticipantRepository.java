package com.socialshuffle.repository;

import com.socialshuffle.model.Participant;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantRepository extends MongoRepository<Participant, String> {

    Optional<Participant> findByEmailIgnoreCase(String email);

    Optional<Participant> findByPhone(String phone);

    @Query("{ '$or': [ { 'email': { '$regex': ?0, '$options': 'i' } }, { 'phone': ?1 } ] }")
    Optional<Participant> findByEmailOrPhone(String email, String phone);

    List<Participant> findByAreaIgnoreCase(String area);

    @Query("{ '$or': [ { 'name': { '$regex': ?0, '$options': 'i' } }, { 'email': { '$regex': ?0, '$options': 'i' } }, { 'phone': { '$regex': ?0, '$options': 'i' } }, { 'area': { '$regex': ?0, '$options': 'i' } } ] }")
    List<Participant> searchParticipants(String keyword);
}
