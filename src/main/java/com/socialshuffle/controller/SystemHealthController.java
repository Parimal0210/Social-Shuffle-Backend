package com.socialshuffle.controller;

import com.socialshuffle.repository.*;
import org.springframework.http.ResponseEntity;
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
    private final ShuffleEventRepository eventRepository;
    private final GameRepository gameRepository;
    private final ParticipantRepository participantRepository;
    private final RegistrationRepository registrationRepository;
    private final EventFeedbackRepository feedbackRepository;
    private final SafetyReportRepository safetyReportRepository;
    private final VolunteerApplicationRepository volunteerRepository;

    public SystemHealthController(DataSource dataSource,
                                  ShuffleEventRepository eventRepository,
                                  GameRepository gameRepository,
                                  ParticipantRepository participantRepository,
                                  RegistrationRepository registrationRepository,
                                  EventFeedbackRepository feedbackRepository,
                                  SafetyReportRepository safetyReportRepository,
                                  VolunteerApplicationRepository volunteerRepository) {
        this.dataSource = dataSource;
        this.eventRepository = eventRepository;
        this.gameRepository = gameRepository;
        this.participantRepository = participantRepository;
        this.registrationRepository = registrationRepository;
        this.feedbackRepository = feedbackRepository;
        this.safetyReportRepository = safetyReportRepository;
        this.volunteerRepository = volunteerRepository;
    }

    private volatile Map<String, Object> cachedMetadata = null;
    private volatile Map<String, Object> cachedCounts = null;
    private volatile long lastCountsCheck = 0;
    private static final long COUNTS_CACHE_TTL_MS = 15000; // 15 seconds cache

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Social Shuffle Spring Boot REST API");
        health.put("version", "1.0.0");
        health.put("database", "MySQL");

        // Lazy initialize and cache static database metadata to avoid repeated information_schema queries
        if (cachedMetadata == null) {
            Map<String, Object> meta = new HashMap<>();
            try (Connection conn = dataSource.getConnection()) {
                meta.put("databaseProduct", conn.getMetaData().getDatabaseProductName());
                meta.put("databaseVersion", conn.getMetaData().getDatabaseProductVersion());
                meta.put("databaseCatalog", conn.getCatalog());
                meta.put("databaseUrl", conn.getMetaData().getURL());
                cachedMetadata = meta;
            } catch (Exception e) {
                meta.put("databaseStatus", "Checking connection: " + e.getMessage());
            }
        }
        if (cachedMetadata != null) {
            health.putAll(cachedMetadata);
        }

        health.put("timestamp", Instant.now().toString());

        // Fast TTL-cached table counts to avoid executing 7 full table count queries on every ping
        long now = System.currentTimeMillis();
        if (cachedCounts == null || (now - lastCountsCheck) > COUNTS_CACHE_TTL_MS) {
            Map<String, Object> counts = new HashMap<>();
            counts.put("events", eventRepository.count());
            counts.put("games", gameRepository.count());
            counts.put("participants", participantRepository.count());
            counts.put("registrations", registrationRepository.count());
            counts.put("feedback", feedbackRepository.count());
            counts.put("safetyReports", safetyReportRepository.count());
            counts.put("volunteers", volunteerRepository.count());
            cachedCounts = counts;
            lastCountsCheck = now;
        }

        health.put("counts", cachedCounts);
        return ResponseEntity.ok(health);
    }
}
