package com.socialshuffle.controller;

import com.socialshuffle.dto.BadgeCalculationRequest;
import com.socialshuffle.dto.BadgeCalculationResult;
import com.socialshuffle.dto.BadgeLevelDefinition;
import com.socialshuffle.service.BadgeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/badges")
@CrossOrigin(origins = "*")
public class BadgeController {

    private final BadgeService badgeService;

    public BadgeController(BadgeService badgeService) {
        this.badgeService = badgeService;
    }

    /**
     * GET /api/badges/levels
     * Retrieves all 10 badge level definitions with requirements and perks.
     */
    @GetMapping("/levels")
    public ResponseEntity<Map<String, Object>> getBadgeLevels() {
        List<BadgeLevelDefinition> levels = badgeService.getAllLevels();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("totalLevels", levels.size());
        response.put("levels", levels);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/badges/calculate
     * Calculates badge level and progression for provided parameters.
     */
    @PostMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculateBadge(@RequestBody BadgeCalculationRequest request) {
        BadgeCalculationResult result = badgeService.calculateBadgeLevel(request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", result);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/badges/participant/{id}
     * Calculates badge level for a registered participant by ID.
     */
    @GetMapping("/participant/{id}")
    public ResponseEntity<Map<String, Object>> getParticipantBadge(@PathVariable String id) {
        BadgeCalculationResult result = badgeService.calculateForParticipant(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", result);
        return ResponseEntity.ok(response);
    }
}
