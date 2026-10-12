package com.socialshuffle.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles root endpoint ('/' and '/health') for cloud platform health checks (Render, AWS, Heroku)
 * and verifies that the Spring Boot REST API is responsive and UP.
 */
@RestController
@CrossOrigin(origins = "*")
public class RootController {

    @RequestMapping(value = {"/", "/health", "/api"}, method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<Map<String, Object>> rootHealthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Social Shuffle Pune Spring Boot REST API");
        response.put("version", "1.0.0");
        response.put("database", "PostgreSQL (Supabase Cloud)");
        response.put("healthCheck", "/api/system/health");
        response.put("docs", "https://social-shuffle-backend.onrender.com/api/system/health");
        return ResponseEntity.ok(response);
    }
}
