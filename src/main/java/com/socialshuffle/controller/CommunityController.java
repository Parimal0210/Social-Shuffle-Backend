package com.socialshuffle.controller;

import com.socialshuffle.model.EventFeedback;
import com.socialshuffle.model.SafetyReport;
import com.socialshuffle.model.VolunteerApplication;
import com.socialshuffle.repository.EventFeedbackRepository;
import com.socialshuffle.repository.SafetyReportRepository;
import com.socialshuffle.repository.VolunteerApplicationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/community")
@CrossOrigin(origins = "*")
public class CommunityController {

    private final EventFeedbackRepository feedbackRepository;
    private final SafetyReportRepository safetyReportRepository;
    private final VolunteerApplicationRepository volunteerRepository;

    public CommunityController(EventFeedbackRepository feedbackRepository,
                               SafetyReportRepository safetyReportRepository,
                               VolunteerApplicationRepository volunteerRepository) {
        this.feedbackRepository = feedbackRepository;
        this.safetyReportRepository = safetyReportRepository;
        this.volunteerRepository = volunteerRepository;
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
        if (feedback.getId() == null || feedback.getId().trim().isEmpty()) {
            feedback.setId("fb-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (feedback.getCreatedAt() == null) {
            feedback.setCreatedAt(Instant.now().toString());
        }
        return feedbackRepository.save(feedback);
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
        if (report.getId() == null || report.getId().trim().isEmpty()) {
            report.setId("rep-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (report.getStatus() == null) {
            report.setStatus("New");
        }
        if (report.getCreatedAt() == null) {
            report.setCreatedAt(Instant.now().toString());
        }
        return safetyReportRepository.save(report);
    }

    @PatchMapping("/reports/{id}/status")
    public ResponseEntity<SafetyReport> updateReportStatus(@PathVariable String id,
                                                          @RequestParam String status,
                                                          @RequestParam(required = false) String notes) {
        return safetyReportRepository.findById(id).map(rep -> {
            rep.setStatus(status);
            if (notes != null) {
                rep.setResolutionNotes(notes);
            }
            SafetyReport saved = safetyReportRepository.save(rep);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    // --- Volunteer Applications ---
    @GetMapping("/volunteers")
    public List<VolunteerApplication> getVolunteers(@RequestParam(required = false) String status) {
        if (status != null && !status.trim().isEmpty()) {
            return volunteerRepository.findByStatusIgnoreCase(status.trim());
        }
        return volunteerRepository.findAllByOrderBySubmittedAtDesc();
    }

    @PostMapping("/volunteers")
    public VolunteerApplication submitVolunteer(@RequestBody VolunteerApplication app) {
        if (app.getId() == null || app.getId().trim().isEmpty()) {
            app.setId("vol-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (app.getStatus() == null) {
            app.setStatus("New");
        }
        if (app.getSubmittedAt() == null) {
            app.setSubmittedAt(Instant.now().toString());
        }
        return volunteerRepository.save(app);
    }

    @PatchMapping("/volunteers/{id}/status")
    public ResponseEntity<VolunteerApplication> updateVolunteerStatus(@PathVariable String id,
                                                                      @RequestParam String status) {
        return volunteerRepository.findById(id).map(app -> {
            app.setStatus(status);
            VolunteerApplication saved = volunteerRepository.save(app);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }
}
