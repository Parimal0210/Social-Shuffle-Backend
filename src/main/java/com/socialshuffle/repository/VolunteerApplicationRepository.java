package com.socialshuffle.repository;

import com.socialshuffle.model.VolunteerApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VolunteerApplicationRepository extends JpaRepository<VolunteerApplication, String> {

    List<VolunteerApplication> findByStatusIgnoreCase(String status);

    List<VolunteerApplication> findAllByOrderBySubmittedAtDesc();
}
