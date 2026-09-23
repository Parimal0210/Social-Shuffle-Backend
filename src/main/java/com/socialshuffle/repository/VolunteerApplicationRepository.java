package com.socialshuffle.repository;

import com.socialshuffle.model.VolunteerApplication;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VolunteerApplicationRepository extends MongoRepository<VolunteerApplication, String> {

    List<VolunteerApplication> findByStatusIgnoreCase(String status);

    List<VolunteerApplication> findAllByOrderBySubmittedAtDesc();
}
