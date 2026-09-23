package com.socialshuffle.repository;

import com.socialshuffle.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, String> {

    List<Game> findByActiveTrue();

    List<Game> findByCategoryIgnoreCase(String category);

    List<Game> findByDifficultyIgnoreCase(String difficulty);

    List<Game> findByNameContainingIgnoreCase(String name);
}
