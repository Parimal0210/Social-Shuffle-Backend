package com.socialshuffle.controller;

import com.socialshuffle.model.Participant;
import com.socialshuffle.model.User;
import com.socialshuffle.repository.*;
import com.socialshuffle.security.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/participants")
@CrossOrigin(origins = "*")
public class ParticipantController {

    private final ParticipantRepository participantRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final ParticipantBadgeRepository participantBadgeRepository;
    private final GoogleUserRepository googleUserRepository;

    public ParticipantController(ParticipantRepository participantRepository,
                                 RegistrationRepository registrationRepository,
                                 UserRepository userRepository,
                                 UserCredentialRepository userCredentialRepository,
                                 ParticipantBadgeRepository participantBadgeRepository,
                                 GoogleUserRepository googleUserRepository) {
        this.participantRepository = participantRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.participantBadgeRepository = participantBadgeRepository;
        this.googleUserRepository = googleUserRepository;
    }

    private boolean isAdmin(HttpServletRequest request) {
        return "admin".equalsIgnoreCase(String.valueOf(request.getAttribute(AuthInterceptor.ATTR_ROLE)));
    }

    private Optional<Participant> resolveCallerParticipant(HttpServletRequest request) {
        Object userIdObj = request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        if (userIdObj == null) return Optional.empty();
        String userId = String.valueOf(userIdObj);
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User u = userOpt.get();
            if (u.getParticipantId() != null && !u.getParticipantId().isEmpty()) {
                Optional<Participant> p = participantRepository.findById(u.getParticipantId());
                if (p.isPresent()) return p;
            }
            if (u.getEmail() != null && !u.getEmail().isEmpty()) {
                return participantRepository.findByEmailIgnoreCase(u.getEmail());
            }
        }
        return participantRepository.findById(userId);
    }

    @GetMapping
    public ResponseEntity<?> getAllParticipants(@RequestParam(required = false) String search,
                                                @RequestParam(required = false) String area,
                                                HttpServletRequest request) {
        if (!isAdmin(request)) {
            return ResponseEntity.ok(resolveCallerParticipant(request)
                    .map(Collections::singletonList)
                    .orElseGet(Collections::emptyList));
        }

        if (search != null && !search.trim().isEmpty()) {
            return ResponseEntity.ok(participantRepository.searchParticipants(search.trim()));
        }
        if (area != null && !area.trim().isEmpty()) {
            return ResponseEntity.ok(participantRepository.findByAreaIgnoreCase(area.trim()));
        }
        return ResponseEntity.ok(participantRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getParticipantById(@PathVariable String id, HttpServletRequest request) {
        if (!isAdmin(request)) {
            Optional<Participant> self = resolveCallerParticipant(request);
            if (self.isEmpty() || !id.equals(self.get().getId())) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "Access denied: You can only view your own participant profile."
                ));
            }
        }
        return participantRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createParticipant(@RequestBody Participant participant) {
        if (participant.getEmail() != null && !participant.getEmail().trim().isEmpty()) {
            String cleanEmail = participant.getEmail().trim().toLowerCase();
            participant.setEmail(cleanEmail);
            Optional<Participant> existing = participantRepository.findByEmailIgnoreCase(cleanEmail);
            if (existing.isPresent()) {
                return ResponseEntity.status(409).body(Map.of(
                        "success", false,
                        "message", "A participant with this email already exists."
                ));
            }
        }
        if (participant.getId() == null || participant.getId().trim().isEmpty()) {
            participant.setId("p-" + UUID.randomUUID().toString().substring(0, 8));
        }
        String now = Instant.now().toString();
        if (participant.getJoinedDate() == null) {
            participant.setJoinedDate(java.time.LocalDate.now().toString());
        }
        if (participant.getRegisteredAt() == null) participant.setRegisteredAt(now);
        if (participant.getCreatedAt() == null) participant.setCreatedAt(now);
        participant.setUpdatedAt(now);

        return ResponseEntity.ok(participantRepository.save(participant));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateParticipant(@PathVariable String id,
                                               @RequestBody Map<String, Object> updates,
                                               HttpServletRequest request) {
        boolean admin = isAdmin(request);
        if (!admin) {
            Optional<Participant> self = resolveCallerParticipant(request);
            if (self.isEmpty() || !id.equals(self.get().getId())) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "Access denied: You can only update your own participant profile."
                ));
            }
        }

        Optional<Participant> opt = participantRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Participant existing = opt.get();
        String now = Instant.now().toString();

        if (updates.get("name") != null) existing.setName(String.valueOf(updates.get("name")).trim());
        if (updates.get("email") != null && admin) existing.setEmail(String.valueOf(updates.get("email")).trim().toLowerCase());
        if (updates.get("phone") != null) existing.setPhone(String.valueOf(updates.get("phone")).trim());
        if (updates.get("username") != null) existing.setUsername(String.valueOf(updates.get("username")).trim());
        if (updates.get("area") != null) existing.setArea(String.valueOf(updates.get("area")).trim());
        if (updates.get("bio") != null) existing.setBio(String.valueOf(updates.get("bio")).trim());
        if (updates.get("avatar") != null) existing.setAvatar(String.valueOf(updates.get("avatar")).trim());

        if (admin) {
            if (updates.get("totalEventsAttended") instanceof Number n) existing.setTotalEventsAttended(Math.max(0, n.intValue()));
            if (updates.get("totalRegistrations") instanceof Number n) existing.setTotalRegistrations(Math.max(0, n.intValue()));
            if (updates.get("totalPaxBrought") instanceof Number n) existing.setTotalPaxBrought(Math.max(0, n.intValue()));
            if (updates.get("gamesPlayedIds") instanceof List<?> list) {
                List<String> gids = new ArrayList<>();
                for (Object item : list) if (item != null) gids.add(String.valueOf(item));
                existing.setGamesPlayedIds(gids);
            }
            if (updates.get("venuesVisited") instanceof List<?> list) {
                List<String> venues = new ArrayList<>();
                for (Object item : list) if (item != null) venues.add(String.valueOf(item));
                existing.setVenuesVisited(venues);
            }
            if (updates.get("lastAttendedEventId") != null) existing.setLastAttendedEventId(String.valueOf(updates.get("lastAttendedEventId")));
            if (updates.get("lastAttendedEventTitle") != null) existing.setLastAttendedEventTitle(String.valueOf(updates.get("lastAttendedEventTitle")));
        }

        existing.setUpdatedAt(now);
        Participant saved = participantRepository.save(existing);

        // Keep linked user & user_credentials records synchronized
        if (saved.getEmail() != null && !saved.getEmail().isEmpty()) {
            userRepository.findByEmailIgnoreCase(saved.getEmail()).ifPresent(u -> {
                u.setName(saved.getName());
                u.setPhone(saved.getPhone());
                u.setArea(saved.getArea());
                u.setAvatar(saved.getAvatar());
                if (saved.getUsername() != null) u.setUsername(saved.getUsername());
                userRepository.save(u);
            });
            userCredentialRepository.findByEmailIgnoreCase(saved.getEmail()).ifPresent(c -> {
                c.setName(saved.getName());
                c.setPhone(saved.getPhone());
                c.setArea(saved.getArea());
                c.setAvatar(saved.getAvatar());
                if (saved.getUsername() != null) c.setUsername(saved.getUsername());
                c.setUpdatedAt(now);
                userCredentialRepository.save(c);
            });
        }

        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteParticipant(@PathVariable String id) {
        Optional<Participant> opt = participantRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Participant not found"));
        }
        Participant p = opt.get();
        String email = p.getEmail() != null ? p.getEmail().trim().toLowerCase() : "";

        participantBadgeRepository.deleteByParticipantId(id);
        registrationRepository.deleteByParticipantId(id);
        googleUserRepository.deleteByParticipantIdOrEmail(id, email);
        userCredentialRepository.deleteBySubjectIdOrEmail(id, email);
        if (!email.isEmpty()) {
            userRepository.findByEmailIgnoreCase(email).ifPresent(userRepository::delete);
        }
        participantRepository.deleteById(id);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "id", id,
                "message", "Participant and associated records deleted from database."
        ));
    }

    @GetMapping("/check-duplicate")
    public ResponseEntity<?> checkDuplicate(@RequestParam(required = false) String email,
                                            @RequestParam(required = false) String phone) {
        if (email != null && !email.trim().isEmpty()) {
            Optional<Participant> byEmail = participantRepository.findByEmailIgnoreCase(email.trim());
            if (byEmail.isPresent()) {
                return ResponseEntity.ok(Collections.singletonMap("duplicate", byEmail.get()));
            }
        }
        if (phone != null && !phone.trim().isEmpty()) {
            Optional<Participant> byPhone = participantRepository.findByPhone(phone.trim());
            if (byPhone.isPresent()) {
                return ResponseEntity.ok(Collections.singletonMap("duplicate", byPhone.get()));
            }
        }
        return ResponseEntity.ok(Collections.singletonMap("duplicate", null));
    }

    @PostMapping("/merge")
    @Transactional
    public ResponseEntity<?> mergeParticipants(@RequestParam String sourceId, @RequestParam String targetId) {
        Optional<Participant> sourceOpt = participantRepository.findById(sourceId);
        Optional<Participant> targetOpt = participantRepository.findById(targetId);

        if (sourceOpt.isEmpty() || targetOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Source or target participant not found"));
        }

        Participant source = sourceOpt.get();
        Participant target = targetOpt.get();

        target.setTotalEventsAttended(target.getTotalEventsAttended() + source.getTotalEventsAttended());
        target.setTotalRegistrations(target.getTotalRegistrations() + source.getTotalRegistrations());
        target.setTotalPaxBrought(target.getTotalPaxBrought() + source.getTotalPaxBrought());

        Set<String> games = new HashSet<>(target.getGamesPlayedIds());
        games.addAll(source.getGamesPlayedIds());
        target.setGamesPlayedIds(new ArrayList<>(games));

        Set<String> venues = new HashSet<>(target.getVenuesVisited());
        venues.addAll(source.getVenuesVisited());
        target.setVenuesVisited(new ArrayList<>(venues));
        target.setUpdatedAt(Instant.now().toString());

        participantRepository.save(target);

        registrationRepository.reassignParticipantRegistrations(sourceId, targetId);
        participantBadgeRepository.deleteByParticipantId(sourceId);
        String sourceEmail = source.getEmail() != null ? source.getEmail().trim().toLowerCase() : "";
        userCredentialRepository.deleteBySubjectIdOrEmail(sourceId, sourceEmail);
        googleUserRepository.deleteByParticipantIdOrEmail(sourceId, sourceEmail);
        if (!sourceEmail.isEmpty()) {
            userRepository.findByEmailIgnoreCase(sourceEmail).ifPresent(userRepository::delete);
        }
        participantRepository.deleteById(sourceId);

        return ResponseEntity.ok(target);
    }
}
