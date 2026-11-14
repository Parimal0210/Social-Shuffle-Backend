package com.socialshuffle.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialshuffle.model.*;
import com.socialshuffle.repository.*;
import com.socialshuffle.security.AuthInterceptor;
import com.socialshuffle.service.EmailNotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/community")
@CrossOrigin(origins = "*")
public class CommunityController {

    private final EventFeedbackRepository feedbackRepository;
    private final SafetyReportRepository safetyReportRepository;
    private final VolunteerApplicationRepository volunteerRepository;
    private final CommunityStatsRepository communityStatsRepository;
    private final ParticipantRepository participantRepository;
    private final GameRepository gameRepository;
    private final ShuffleEventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final EmailNotificationService emailNotificationService;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;

    private volatile Map<String, Long> cachedLiveCounts;
    private volatile long lastLiveCountsAt = 0L;

    public CommunityController(EventFeedbackRepository feedbackRepository,
                               SafetyReportRepository safetyReportRepository,
                               VolunteerApplicationRepository volunteerRepository,
                               CommunityStatsRepository communityStatsRepository,
                               ParticipantRepository participantRepository,
                               GameRepository gameRepository,
                               ShuffleEventRepository eventRepository,
                               RegistrationRepository registrationRepository,
                               UserRepository userRepository,
                               EmailNotificationService emailNotificationService,
                               ObjectMapper objectMapper,
                               CacheManager cacheManager) {
        this.feedbackRepository = feedbackRepository;
        this.safetyReportRepository = safetyReportRepository;
        this.volunteerRepository = volunteerRepository;
        this.communityStatsRepository = communityStatsRepository;
        this.participantRepository = participantRepository;
        this.gameRepository = gameRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.emailNotificationService = emailNotificationService;
        this.objectMapper = objectMapper;
        this.cacheManager = cacheManager;
    }

    private boolean isAdmin(HttpServletRequest request) {
        return "admin".equalsIgnoreCase(String.valueOf(request.getAttribute(AuthInterceptor.ATTR_ROLE)));
    }

    // --- Community Stats ---
    @GetMapping("/stats")
    public ResponseEntity<?> getCommunityStats() {
        Map<String, Object> stats = buildDefaultStatsPayload();
        Optional<CommunityStats> storedOpt = communityStatsRepository.findById("community-stats-main");
        if (storedOpt.isPresent()) {
            CommunityStats stored = storedOpt.get();
            if (stored.getPayloadJson() != null && !stored.getPayloadJson().isBlank()) {
                try {
                    Map<String, Object> parsed = objectMapper.readValue(stored.getPayloadJson(), new TypeReference<>() {});
                    stats.putAll(parsed);
                } catch (Exception ignored) {
                }
            }
            if (stored.getUpdatedAt() != null) stats.put("updatedAt", stored.getUpdatedAt());
            if (stored.getUpdatedBy() != null) stats.put("updatedBy", stored.getUpdatedBy());
        }

        long now = System.currentTimeMillis();
        if (cachedLiveCounts == null || (now - lastLiveCountsAt) > 5000L) {
            Map<String, Long> counts = new LinkedHashMap<>();
            counts.put("participants", participantRepository.count());
            counts.put("games", gameRepository.count());
            counts.put("events", eventRepository.count());
            counts.put("registrations", registrationRepository.count());
            cachedLiveCounts = counts;
            lastLiveCountsAt = now;
        }
        stats.put("liveCounts", cachedLiveCounts);
        return ResponseEntity.ok(stats);
    }

    @RequestMapping(value = "/stats", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<?> updateCommunityStats(@RequestBody Map<String, Object> updates, HttpServletRequest request) {
        Map<String, Object> current = buildDefaultStatsPayload();
        CommunityStats entity = communityStatsRepository.findById("community-stats-main").orElseGet(CommunityStats::new);
        entity.setId("community-stats-main");

        if (entity.getPayloadJson() != null && !entity.getPayloadJson().isBlank()) {
            try {
                Map<String, Object> parsed = objectMapper.readValue(entity.getPayloadJson(), new TypeReference<>() {});
                current.putAll(parsed);
            } catch (Exception ignored) {
            }
        }
        if (updates != null) {
            for (Map.Entry<String, Object> entry : updates.entrySet()) {
                if (!"liveCounts".equals(entry.getKey()) && entry.getValue() != null) {
                    current.put(entry.getKey(), entry.getValue());
                }
            }
        }

        String updatedBy = current.get("updatedBy") != null ? String.valueOf(current.get("updatedBy")) : "Admin";
        Object authUserId = request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        if (authUserId != null) {
            updatedBy = userRepository.findById(String.valueOf(authUserId)).map(User::getName).orElse(updatedBy);
        }
        String now = Instant.now().toString();
        current.put("updatedAt", now);
        current.put("updatedBy", updatedBy);

        if (current.get("membersCountNumeric") instanceof Number n) entity.setTotalMembers(n.intValue());
        if (current.get("eventsHostedNumeric") instanceof Number n) entity.setTotalEvents(n.intValue());
        if (current.get("gamesTaughtNumeric") instanceof Number n) entity.setTotalGames(n.intValue());
        if (current.get("neighborhoods") instanceof List<?> neighborhoods) {
            try {
                entity.setActiveAreas(objectMapper.writeValueAsString(neighborhoods));
            } catch (Exception ignored) {
            }
        }
        entity.setUpdatedBy(updatedBy);
        entity.setUpdatedAt(now);
        try {
            entity.setPayloadJson(objectMapper.writeValueAsString(current));
        } catch (Exception ignored) {
        }
        communityStatsRepository.save(entity);

        Cache cache = cacheManager.getCache("communityStats");
        if (cache != null) cache.clear();
        cachedLiveCounts = null;

        return getCommunityStats();
    }

    private Map<String, Object> buildDefaultStatsPayload() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", "community-stats-main");
        map.put("membersCount", "1,400+");
        map.put("membersCountNumeric", 1400);
        map.put("gamesTaught", "50+");
        map.put("gamesTaughtNumeric", 50);
        map.put("soloPercentage", "80%");
        map.put("soloPercentageNumeric", 80);
        map.put("eventsHosted", "36+");
        map.put("eventsHostedLabel", "Weekly");
        map.put("eventsHostedNumeric", 36);
        map.put("neighborhoods", Arrays.asList("Koregaon Park", "Baner", "Viman Nagar", "Kalyani Nagar", "FC Road", "Kothrud", "Wakad", "Bavdhan"));
        map.put("heroHeadline", "Pune's Friendliest Board Game Meetups");
        map.put("heroSchedule", "Every Saturday & Sunday across Pune's top cafes");
        map.put("heroDescription", "Show up solo or with friends. Learn any game in 5 minutes and share unforgettable tabletop evenings across Pune.");
        map.put("communityTagline", "Roll Dice. Meet Pune.");
        map.put("whyJoinText", "Over 80% of first-timers arrive solo and leave with a new weekend tabletop crew.");
        map.put("curatedMatchmakingText", "Our table hosts match you by vibe, strategy depth, and group size so nobody sits on the sidelines.");
        return map;
    }

    // --- Feedback ---
    @GetMapping("/feedback")
    public List<EventFeedback> getFeedback(@RequestParam(required = false) String eventId) {
        if (eventId != null && !eventId.trim().isEmpty()) {
            return feedbackRepository.findByEventId(eventId.trim());
        }
        return feedbackRepository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping("/feedback")
    public EventFeedback submitFeedback(@RequestBody EventFeedback feedback) {
        String now = Instant.now().toString();
        if (feedback.getId() == null || feedback.getId().trim().isEmpty()) {
            feedback.setId("fb-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (feedback.getCreatedAt() == null) feedback.setCreatedAt(now);
        if (feedback.getSubmittedAt() == null) feedback.setSubmittedAt(now);

        EventFeedback saved = feedbackRepository.save(feedback);
        emailNotificationService.sendFeedbackAdminAlertAsync(saved);
        return saved;
    }

    // --- Safety Reports ---
    @GetMapping("/reports")
    public List<SafetyReport> getSafetyReports(@RequestParam(required = false) String status) {
        if (status != null && !status.trim().isEmpty()) {
            return safetyReportRepository.findByStatusIgnoreCase(status.trim());
        }
        return safetyReportRepository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping("/reports")
    public SafetyReport submitSafetyReport(@RequestBody SafetyReport report) {
        String now = Instant.now().toString();
        if (report.getId() == null || report.getId().trim().isEmpty()) {
            report.setId("rep-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (report.getStatus() == null || report.getStatus().trim().isEmpty()) report.setStatus("New");
        if (report.getCategory() == null && report.getType() != null) report.setCategory(report.getType());
        if (report.getCreatedAt() == null) report.setCreatedAt(now);
        if (report.getSubmittedAt() == null) report.setSubmittedAt(now);

        SafetyReport saved = safetyReportRepository.save(report);
        emailNotificationService.sendSafetyReportAdminAlertAsync(saved);
        return saved;
    }

    @PatchMapping("/reports/{id}/status")
    public ResponseEntity<SafetyReport> updateReportStatus(@PathVariable String id,
                                                           @RequestParam(required = false) String status,
                                                           @RequestParam(required = false) String notes,
                                                           @RequestBody(required = false) Map<String, String> body) {
        String finalStatus = status != null ? status : (body != null ? body.get("status") : null);
        String finalNotes = notes != null ? notes : (body != null ? (body.get("notes") != null ? body.get("notes") : body.get("resolutionNotes")) : null);
        if (finalStatus == null || finalStatus.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return safetyReportRepository.findById(id).map(rep -> {
            rep.setStatus(finalStatus.trim());
            if (finalNotes != null) {
                rep.setResolutionNotes(finalNotes);
                rep.setAdminNotes(finalNotes);
            }
            SafetyReport saved = safetyReportRepository.save(rep);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    // --- Volunteer Applications ---
    @GetMapping("/volunteers")
    public List<VolunteerApplication> getVolunteers(@RequestParam(required = false) String status, HttpServletRequest request) {
        if (!isAdmin(request)) {
            Object authUserId = request.getAttribute(AuthInterceptor.ATTR_USER_ID);
            if (authUserId == null) return Collections.emptyList();
            String email = userRepository.findById(String.valueOf(authUserId)).map(User::getEmail).orElse("");
            if (email.isBlank()) return Collections.emptyList();
            return volunteerRepository.findByEmailIgnoreCaseOrderBySubmittedAtDesc(email);
        }
        if (status != null && !status.trim().isEmpty()) {
            return volunteerRepository.findByStatusIgnoreCase(status.trim());
        }
        return volunteerRepository.findAllByOrderBySubmittedAtDesc();
    }

    @PostMapping("/volunteers")
    public VolunteerApplication submitVolunteer(@RequestBody VolunteerApplication app) {
        String now = Instant.now().toString();
        if (app.getId() == null || app.getId().trim().isEmpty()) {
            app.setId("vol-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (app.getStatus() == null || app.getStatus().trim().isEmpty()) app.setStatus("New");
        if (app.getSubmittedAt() == null) app.setSubmittedAt(now);

        VolunteerApplication saved = volunteerRepository.save(app);
        emailNotificationService.sendVolunteerConfirmationAndAdminAlertAsync(saved);
        return saved;
    }

    @PatchMapping("/volunteers/{id}/status")
    public ResponseEntity<VolunteerApplication> updateVolunteerStatus(@PathVariable String id,
                                                                      @RequestParam(required = false) String status,
                                                                      @RequestBody(required = false) Map<String, String> body) {
        String finalStatus = status != null ? status : (body != null ? body.get("status") : null);
        if (finalStatus == null || finalStatus.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return volunteerRepository.findById(id).map(app -> {
            app.setStatus(finalStatus.trim());
            VolunteerApplication saved = volunteerRepository.save(app);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }
}
