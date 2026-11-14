package com.socialshuffle.controller;

import com.socialshuffle.repository.*;
import org.springframework.beans.factory.annotation.Value;
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
    private final AdminRepository adminRepository;
    private final VenueRepository venueRepository;
    private final UserCredentialRepository userCredentialRepository;

    @Value("${app.api.endpoint:http://localhost:8080/api}")
    private String apiEndpoint;

    private volatile Map<String, Object> cachedMetadata = null;
    private volatile Map<String, Object> cachedCounts = null;
    private volatile long lastCountsCheck = 0;
    private static final long COUNTS_CACHE_TTL_MS = 15000;

    public SystemHealthController(DataSource dataSource,
                                  ShuffleEventRepository eventRepository,
                                  GameRepository gameRepository,
                                  ParticipantRepository participantRepository,
                                  RegistrationRepository registrationRepository,
                                  EventFeedbackRepository feedbackRepository,
                                  SafetyReportRepository safetyReportRepository,
                                  VolunteerApplicationRepository volunteerRepository,
                                  AdminRepository adminRepository,
                                  VenueRepository venueRepository,
                                  UserCredentialRepository userCredentialRepository) {
        this.dataSource = dataSource;
        this.eventRepository = eventRepository;
        this.gameRepository = gameRepository;
        this.participantRepository = participantRepository;
        this.registrationRepository = registrationRepository;
        this.feedbackRepository = feedbackRepository;
        this.safetyReportRepository = safetyReportRepository;
        this.volunteerRepository = volunteerRepository;
        this.adminRepository = adminRepository;
        this.venueRepository = venueRepository;
        this.userCredentialRepository = userCredentialRepository;
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
                meta.put("databaseUrl", conn.getMetaData().getURL());
                meta.put("databaseUser", conn.getMetaData().getUserName());
                cachedMetadata = meta;
            }
        } catch (Exception e) {
            health.put("databaseError", e.getMessage());
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
                Map<String, Object> counts = new HashMap<>();
                counts.put("events", eventRepository.count());
                counts.put("games", gameRepository.count());
                counts.put("participants", participantRepository.count());
                counts.put("registrations", registrationRepository.count());
                counts.put("feedback", feedbackRepository.count());
                counts.put("safetyReports", safetyReportRepository.count());
                counts.put("volunteers", volunteerRepository.count());
                counts.put("admins", adminRepository.count());
                counts.put("venues", venueRepository.count());
                counts.put("userCredentials", userCredentialRepository.count());
                cachedCounts = counts;
                lastCountsCheck = now;
            }
            health.put("counts", cachedCounts);
        }
        return ResponseEntity.ok(health);
    }
}
