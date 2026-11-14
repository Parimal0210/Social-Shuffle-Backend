package com.socialshuffle.repository;

import com.socialshuffle.model.CommunityStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommunityStatsRepository extends JpaRepository<CommunityStats, String> {

    Optional<CommunityStats> findTopByOrderByUpdatedAtDesc();
}
