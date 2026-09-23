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

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Social Shuffle Spring Boot REST API");
        health.put("version", "1.0.0");
        health.put("database", "MySQL");

        try (Connection conn = dataSource.getConnection()) {
            health.put("databaseProduct", conn.getMetaData().getDatabaseProductName());
            health.put("databaseVersion", conn.getMetaData().getDatabaseProductVersion());
            health.put("databaseCatalog", conn.getCatalog());
            health.put("databaseUrl", conn.getMetaData().getURL());
        } catch (Exception e) {
            health.put("databaseStatus", "Checking connection: " + e.getMessage());
        }

        health.put("timestamp", Instant.now().toString());

        Map<String, Object> counts = new HashMap<>();
        counts.put("events", eventRepository.count());
        counts.put("games", gameRepository.count());
        counts.put("participants", participantRepository.count());
        counts.put("registrations", registrationRepository.count());
        counts.put("feedback", feedbackRepository.count());
        counts.put("safetyReports", safetyReportRepository.count());
        counts.put("volunteers", volunteerRepository.count());

        health.put("counts", counts);
        return ResponseEntity.ok(health);
    }
}
