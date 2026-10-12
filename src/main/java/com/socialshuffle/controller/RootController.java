package com.socialshuffle.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles root endpoint ('/' and '/health') for cloud platform health checks (Render, AWS, Heroku)
 * and exposes backend API endpoint configurations to calling clients.
 */
@RestController
@CrossOrigin(origins = "*")
public class RootController {

    @Value("${app.api.endpoint:https://social-shuffle-backend.onrender.com/api}")
    private String apiEndpoint;

    @Value("${app.api.public-url:https://social-shuffle-backend.onrender.com}")
    private String publicUrl;

    @RequestMapping(value = {"/", "/health", "/api"}, method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<Map<String, Object>> rootHealthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Social Shuffle Pune Spring Boot REST API");
        response.put("version", "1.0.0");
        response.put("database", "PostgreSQL (Supabase Cloud)");
        response.put("apiEndpoint", apiEndpoint);
        response.put("publicUrl", publicUrl);
        response.put("healthCheck", apiEndpoint + "/system/health");
        response.put("docs", apiEndpoint + "/system/health");
        return ResponseEntity.ok(response);
    }
}
