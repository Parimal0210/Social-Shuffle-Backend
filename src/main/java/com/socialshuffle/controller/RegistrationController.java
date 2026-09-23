package com.socialshuffle.controller;

import com.socialshuffle.model.GuestInfo;
import com.socialshuffle.model.Participant;
import com.socialshuffle.model.Registration;
import com.socialshuffle.model.ShuffleEvent;
import com.socialshuffle.repository.GameRepository;
import com.socialshuffle.repository.ParticipantRepository;
import com.socialshuffle.repository.RegistrationRepository;
import com.socialshuffle.repository.ShuffleEventRepository;
import com.socialshuffle.service.EmailNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/registrations")
@CrossOrigin(origins = "*")
public class RegistrationController {

    private final RegistrationRepository registrationRepository;
    private final ParticipantRepository participantRepository;
    private final GameRepository gameRepository;
    private final ShuffleEventRepository shuffleEventRepository;
    private final EmailNotificationService emailNotificationService;

    public RegistrationController(RegistrationRepository registrationRepository,
                                  ParticipantRepository participantRepository,
                                  GameRepository gameRepository,
                                  ShuffleEventRepository shuffleEventRepository,
                                  EmailNotificationService emailNotificationService) {
        this.registrationRepository = registrationRepository;
        this.participantRepository = participantRepository;
        this.gameRepository = gameRepository;
        this.shuffleEventRepository = shuffleEventRepository;
        this.emailNotificationService = emailNotificationService;
    }

    @GetMapping
    public List<Registration> getRegistrations(@RequestParam(required = false) String eventId,
                                              @RequestParam(required = false) String participantId) {
        if (eventId != null && !eventId.trim().isEmpty()) {
            return registrationRepository.findByEventId(eventId.trim());
        }
        if (participantId != null && !participantId.trim().isEmpty()) {
            return registrationRepository.findByParticipantId(participantId.trim());
        }
        return registrationRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Registration> getRegistrationById(@PathVariable String id) {
        return registrationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createRegistration(@RequestBody Registration registration) {
        if (registration.getId() == null || registration.getId().trim().isEmpty()) {
            registration.setId("reg-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (registration.getRegisteredAt() == null) {
            registration.setRegisteredAt(Instant.now().toString());
        }
        if (registration.getAttendanceStatus() == null) {
            registration.setAttendanceStatus("Pending");
        }
        if (registration.getPaymentStatus() == null) {
            registration.setPaymentStatus("Pending");
        }

        // Generate QR code pass
        String qrToken = "SS-REG-" + registration.getId();
        registration.setQrCodeToken(qrToken);
        registration.setQrCodeUrl("https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + qrToken);

        // Fetch event details for the quirky email
        ShuffleEvent event = null;
        if (registration.getEventId() != null) {
            event = shuffleEventRepository.findById(registration.getEventId()).orElse(null);
        }

        // Send quirky confirmation email with QR code
        boolean sent = emailNotificationService.sendRegistrationConfirmationEmail(registration, event);
        registration.setEmailSent(sent);
        if (sent) {
            registration.setEmailSentAt(Instant.now().toString());
        }

        Registration saved = registrationRepository.save(registration);

        // Update participant registration count
        if (registration.getParticipantId() != null) {
            participantRepository.findById(registration.getParticipantId()).ifPresent(p -> {
                p.setTotalRegistrations(p.getTotalRegistrations() + 1);
                if (registration.getPaxCount() > 1) {
                    p.setTotalPaxBrought(p.getTotalPaxBrought() + (registration.getPaxCount() - 1));
                }
                participantRepository.save(p);
            });
        }

        return ResponseEntity.ok(saved);
    }

    /**
     * Verifies an attendee's QR code or Registration ID at the event entrance.
     */
    @PostMapping("/verify-qr")
    public ResponseEntity<?> verifyQrCode(@RequestBody Map<String, String> payload) {
        String code = payload.get("code");
        String eventId = payload.get("eventId");

        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "QR Code or Ticket Token is required."));
        }

        String cleanedCode = code.trim();
        // Extract registration ID if prefixed with SS-REG-
        String regId = cleanedCode.startsWith("SS-REG-") ? cleanedCode.replace("SS-REG-", "") : cleanedCode;

        // Try lookup by ID first, then by QR token
        Optional<Registration> regOpt = registrationRepository.findById(regId);
        if (regOpt.isEmpty()) {
            List<Registration> all = registrationRepository.findAll();
            regOpt = all.stream()
                    .filter(r -> cleanedCode.equalsIgnoreCase(r.getQrCodeToken()) || 
                                 (r.getId() != null && cleanedCode.equalsIgnoreCase(r.getId())))
                    .findFirst();
        }

        if (regOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "Invalid Ticket: No registration found for code '" + cleanedCode + "'"
            ));
        }

        Registration reg = regOpt.get();

        // Check if ticket matches current event
        if (eventId != null && !eventId.trim().isEmpty() && !eventId.equals(reg.getEventId())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Ticket Mismatch: This pass belongs to another event (Event ID: " + reg.getEventId() + ")",
                    "registration", reg
            ));
        }

        // Check if already checked in
        if ("Checked In".equalsIgnoreCase(reg.getAttendanceStatus())) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "alreadyCheckedIn", true,
                    "message", "Already Checked-In earlier at " + (reg.getCheckInTime() != null ? reg.getCheckInTime() : "entrance"),
                    "registration", reg
            ));
        }

        // Mark Checked In
        reg.setAttendanceStatus("Checked In");
        reg.setCheckInTime(Instant.now().toString());
        Registration saved = registrationRepository.save(reg);

        // Update participant statistics
        if (reg.getParticipantId() != null) {
            participantRepository.findById(reg.getParticipantId()).ifPresent(p -> {
                p.setTotalEventsAttended(p.getTotalEventsAttended() + 1);
                p.setLastAttendedEventId(reg.getEventId());
                participantRepository.save(p);
            });
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "alreadyCheckedIn", false,
                "message", "Check-In Verified! Welcome " + reg.getParticipantName() + " (" + reg.getPaxCount() + " PAX)",
                "registration", saved
        ));
    }

    /**
     * Preview the quirky gratitude email rendered for this registration.
     */
    @GetMapping("/{id}/email-preview")
    public ResponseEntity<String> previewRegistrationEmail(@PathVariable String id) {
        return registrationRepository.findById(id).map(reg -> {
            ShuffleEvent event = null;
            if (reg.getEventId() != null) {
                event = shuffleEventRepository.findById(reg.getEventId()).orElse(null);
            }
            String html = emailNotificationService.generateQuirkyRegistrationEmailHtml(reg, event);
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/attendance")
    public ResponseEntity<Registration> updateAttendance(@PathVariable String id,
                                                         @RequestParam String status) {
        return registrationRepository.findById(id).map(reg -> {
            reg.setAttendanceStatus(status);
            if ("Checked In".equalsIgnoreCase(status)) {
                reg.setCheckInTime(Instant.now().toString());

                // Update participant stats
                if (reg.getParticipantId() != null) {
                    participantRepository.findById(reg.getParticipantId()).ifPresent(p -> {
                        p.setTotalEventsAttended(p.getTotalEventsAttended() + 1);
                        p.setLastAttendedEventId(reg.getEventId());
                        participantRepository.save(p);
                    });
                }
            }
            Registration saved = registrationRepository.save(reg);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/payment")
    public ResponseEntity<Registration> updatePayment(@PathVariable String id,
                                                      @RequestParam String status) {
        return registrationRepository.findById(id).map(reg -> {
            reg.setPaymentStatus(status);
            Registration saved = registrationRepository.save(reg);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/toggle-game")
    public ResponseEntity<Registration> toggleGamePlayed(@PathVariable String id,
                                                         @RequestParam String gameId) {
        return registrationRepository.findById(id).map(reg -> {
            List<String> games = new ArrayList<>(reg.getGamesPlayed());
            if (games.contains(gameId)) {
                games.remove(gameId);
            } else {
                games.add(gameId);
                // Increment plays count in game
                gameRepository.findById(gameId).ifPresent(g -> {
                    g.setPlaysCount(g.getPlaysCount() + 1);
                    gameRepository.save(g);
                });
                // Add to participant played games
                if (reg.getParticipantId() != null) {
                    participantRepository.findById(reg.getParticipantId()).ifPresent(p -> {
                        if (!p.getGamesPlayedIds().contains(gameId)) {
                            p.getGamesPlayedIds().add(gameId);
                            participantRepository.save(p);
                        }
                    });
                }
            }
            reg.setGamesPlayed(games);
            Registration saved = registrationRepository.save(reg);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/walk-in")
    public ResponseEntity<?> addWalkIn(@RequestParam String eventId, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String phone = (String) body.get("phone");
        String email = (String) body.get("email");
        String area = (String) body.getOrDefault("area", "Pune");
        int paxCount = body.get("paxCount") != null ? ((Number) body.get("paxCount")).intValue() : 1;
        String paymentStatus = (String) body.getOrDefault("paymentStatus", "Confirmed");

        // Find or create participant
        Participant participant = participantRepository.findByPhone(phone)
                .orElseGet(() -> {
                    Participant np = new Participant("p-" + UUID.randomUUID().toString().substring(0, 8), name, email, phone, area);
                    return participantRepository.save(np);
                });

        Registration reg = new Registration();
        reg.setId("reg-walkin-" + UUID.randomUUID().toString().substring(0, 6));
        reg.setEventId(eventId);
        reg.setParticipantId(participant.getId());
        reg.setParticipantName(name);
        reg.setParticipantEmail(email);
        reg.setParticipantPhone(phone);
        reg.setParticipantArea(area);
        reg.setPaxCount(paxCount);
        reg.setPaymentStatus(paymentStatus);
        reg.setAttendanceStatus("Checked In");
        reg.setCheckInTime(Instant.now().toString());
        reg.setRegisteredAt(Instant.now().toString());

        Registration saved = registrationRepository.save(reg);

        participant.setTotalEventsAttended(participant.getTotalEventsAttended() + 1);
        participant.setTotalRegistrations(participant.getTotalRegistrations() + 1);
        participantRepository.save(participant);

        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelRegistration(@PathVariable String id) {
        return registrationRepository.findById(id).map(reg -> {
            reg.setAttendanceStatus("Cancelled");
            registrationRepository.save(reg);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
