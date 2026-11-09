package com.socialshuffle.repository;

import com.socialshuffle.model.BadgeLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BadgeLevelRepository extends JpaRepository<BadgeLevel, Integer> {
    List<BadgeLevel> findAllByOrderByLevelAsc();
}
