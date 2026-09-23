package com.socialshuffle.controller;

import com.socialshuffle.model.Game;
import com.socialshuffle.repository.GameRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/games")
@CrossOrigin(origins = "*")
public class GameController {

    private final GameRepository gameRepository;

    public GameController(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @GetMapping
    public List<Game> getAllGames(@RequestParam(required = false) Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return gameRepository.findByActiveTrue();
        }
        return gameRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Game> getGameById(@PathVariable String id) {
        return gameRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Game createGame(@RequestBody Game game) {
        if (game.getId() == null || game.getId().trim().isEmpty()) {
            game.setId("g-" + UUID.randomUUID().toString().substring(0, 8));
        }
        return gameRepository.save(game);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Game> updateGame(@PathVariable String id, @RequestBody Game updates) {
        return gameRepository.findById(id).map(existing -> {
            if (updates.getName() != null) existing.setName(updates.getName());
            if (updates.getCategory() != null) existing.setCategory(updates.getCategory());
            if (updates.getDifficulty() != null) existing.setDifficulty(updates.getDifficulty());
            if (updates.getPlayers() != null) existing.setPlayers(updates.getPlayers());
            if (updates.getDuration() != null) existing.setDuration(updates.getDuration());
            if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
            existing.setActive(updates.isActive());
            if (updates.getPlaysCount() >= 0) existing.setPlaysCount(updates.getPlaysCount());
            if (updates.getTags() != null) existing.setTags(updates.getTags());
            if (updates.getRating() != null) existing.setRating(updates.getRating());
            if (updates.getImage() != null) existing.setImage(updates.getImage());
            Game saved = gameRepository.save(existing);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<Game> toggleActive(@PathVariable String id) {
        return gameRepository.findById(id).map(existing -> {
            existing.setActive(!existing.isActive());
            Game saved = gameRepository.save(existing);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGame(@PathVariable String id) {
        if (gameRepository.existsById(id)) {
            gameRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
