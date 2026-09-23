package com.socialshuffle.controller;

import com.socialshuffle.model.GuestInfo;
import com.socialshuffle.model.Participant;
import com.socialshuffle.model.Registration;
import com.socialshuffle.repository.GameRepository;
import com.socialshuffle.repository.ParticipantRepository;
import com.socialshuffle.repository.RegistrationRepository;
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

    public RegistrationController(RegistrationRepository registrationRepository,
                                  ParticipantRepository participantRepository,
                                  GameRepository gameRepository) {
        this.registrationRepository = registrationRepository;
        this.participantRepository = participantRepository;
        this.gameRepository = gameRepository;
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
