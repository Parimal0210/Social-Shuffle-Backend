package com.socialshuffle.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
@CrossOrigin(origins = "*")
public class SystemHealthController {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.api.endpoint:http://localhost:8080/api}")
    private String apiEndpoint;

    private volatile Map<String, Object> cachedMetadata = null;
    private volatile Map<String, Object> cachedCounts = null;
    private volatile long lastCountsCheck = 0;
    private static final long COUNTS_CACHE_TTL_MS = 60000;

    public SystemHealthController(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Social Shuffle Spring Boot STS REST API",
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("service", "Social Shuffle Spring Boot STS REST API");
        health.put("version", "2.0.0");
        health.put("database", "PostgreSQL (Supabase)");
        health.put("apiEndpoint", apiEndpoint);

        boolean dbConnected = false;
        try (Connection conn = dataSource.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT 1")) {
            dbConnected = rs.next();
            if (cachedMetadata == null) {
                Map<String, Object> meta = new HashMap<>();
                meta.put("databaseProduct", conn.getMetaData().getDatabaseProductName());
                meta.put("databaseVersion", conn.getMetaData().getDatabaseProductVersion());
                meta.put("databaseCatalog", conn.getCatalog());
                String rawUrl = conn.getMetaData().getURL();
                if (rawUrl != null) {
                    int qIdx = rawUrl.indexOf('?');
                    meta.put("databaseHost", qIdx > 0 ? rawUrl.substring(0, qIdx) : rawUrl);
                }
                cachedMetadata = meta;
            }
        } catch (Exception e) {
            health.put("databaseError", "Database connectivity check failed");
        }

        health.put("status", dbConnected ? "UP" : "DEGRADED");
        health.put("databaseConnected", dbConnected);
        if (cachedMetadata != null) {
            health.putAll(cachedMetadata);
        }

        health.put("timestamp", Instant.now().toString());

        if (dbConnected) {
            long now = System.currentTimeMillis();
            if (cachedCounts == null || (now - lastCountsCheck) > COUNTS_CACHE_TTL_MS) {
                try {
                    Map<String, Object> row = jdbcTemplate.queryForMap("""
                        SELECT
                          (SELECT COUNT(*) FROM events) AS events,
                          (SELECT COUNT(*) FROM games) AS games,
                          (SELECT COUNT(*) FROM participants) AS participants,
                          (SELECT COUNT(*) FROM registrations) AS registrations,
                          (SELECT COUNT(*) FROM event_feedback) AS feedback,
                          (SELECT COUNT(*) FROM safety_reports) AS safety_reports,
                          (SELECT COUNT(*) FROM volunteer_applications) AS volunteers,
                          (SELECT COUNT(*) FROM admins) AS admins,
                          (SELECT COUNT(*) FROM venues) AS venues,
                          (SELECT COUNT(*) FROM user_credentials) AS user_credentials
                        """);
                    Map<String, Object> counts = new HashMap<>();
                    counts.put("events", row.get("events"));
                    counts.put("games", row.get("games"));
                    counts.put("participants", row.get("participants"));
                    counts.put("registrations", row.get("registrations"));
                    counts.put("feedback", row.get("feedback"));
                    counts.put("safetyReports", row.get("safety_reports"));
                    counts.put("volunteers", row.get("volunteers"));
                    counts.put("admins", row.get("admins"));
                    counts.put("venues", row.get("venues"));
                    counts.put("userCredentials", row.get("user_credentials"));
                    cachedCounts = counts;
                    lastCountsCheck = now;
                } catch (Exception ignored) {
                }
            }
            if (cachedCounts != null) {
                health.put("counts", cachedCounts);
            }
        }
        return ResponseEntity.ok(health);
    }
}
