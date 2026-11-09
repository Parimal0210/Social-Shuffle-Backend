package com.socialshuffle.repository;

import com.socialshuffle.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VenueRepository extends JpaRepository<Venue, String> {
    List<Venue> findByActiveTrue();
    List<Venue> findByAreaIgnoreCase(String area);
}
