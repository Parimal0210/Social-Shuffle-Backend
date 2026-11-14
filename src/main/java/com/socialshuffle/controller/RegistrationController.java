package com.socialshuffle.controller;

import com.socialshuffle.model.GuestInfo;
import com.socialshuffle.model.Participant;
import com.socialshuffle.model.Registration;
import com.socialshuffle.model.ShuffleEvent;
import com.socialshuffle.model.User;
import com.socialshuffle.model.UserCredential;
import com.socialshuffle.repository.*;
import com.socialshuffle.security.AuthInterceptor;
import com.socialshuffle.security.PasswordSecurityUtil;
import com.socialshuffle.service.EmailNotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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
    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final EmailNotificationService emailNotificationService;
    private final CacheManager cacheManager;

    public RegistrationController(RegistrationRepository registrationRepository,
                                  ParticipantRepository participantRepository,
                                  GameRepository gameRepository,
                                  ShuffleEventRepository shuffleEventRepository,
                                  UserRepository userRepository,
                                  UserCredentialRepository userCredentialRepository,
                                  EmailNotificationService emailNotificationService,
                                  CacheManager cacheManager) {
        this.registrationRepository = registrationRepository;
        this.participantRepository = participantRepository;
        this.gameRepository = gameRepository;
        this.shuffleEventRepository = shuffleEventRepository;
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.emailNotificationService = emailNotificationService;
        this.cacheManager = cacheManager;
    }

    private boolean isAdmin(HttpServletRequest request) {
        return "admin".equalsIgnoreCase(String.valueOf(request.getAttribute(AuthInterceptor.ATTR_ROLE)));
    }

    private Optional<User> resolveCallerUser(HttpServletRequest request) {
        Object userIdObj = request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        if (userIdObj == null) return Optional.empty();
        return userRepository.findById(String.valueOf(userIdObj));
    }

    private boolean isOwner(Registration reg, User caller) {
        if (caller == null || reg == null) return false;
        if (caller.getParticipantId() != null && caller.getParticipantId().equals(reg.getParticipantId())) {
            return true;
        }
        if (caller.getId() != null && caller.getId().equals(reg.getParticipantId())) {
            return true;
        }
        return caller.getEmail() != null
                && reg.getParticipantEmail() != null
                && caller.getEmail().equalsIgnoreCase(reg.getParticipantEmail().trim());
    }

    private void evictCache(String... names) {
        for (String name : names) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        }
    }

    private void syncEventCounts(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) return;
        shuffleEventRepository.findById(eventId.trim()).ifPresent(evt -> {
            int registered = registrationRepository.sumActivePaxByEventId(eventId.trim());
            int checkedIn = registrationRepository.sumCheckedInPaxByEventId(eventId.trim());
            evt.setRegisteredCount(registered);
            evt.setCheckedInCount(checkedIn);
            evt.setUpdatedAt(Instant.now().toString());
            shuffleEventRepository.save(evt);
        });
        evictCache("events", "communityStats");
    }

    @GetMapping
    public List<Registration> getRegistrations(@RequestParam(required = false) String eventId,
                                               @RequestParam(required = false) String participantId,
                                               HttpServletRequest request) {
        if (!isAdmin(request)) {
            Optional<User> callerOpt = resolveCallerUser(request);
            if (callerOpt.isEmpty()) {
                return Collections.emptyList();
            }
            User caller = callerOpt.get();
            String pid = caller.getParticipantId() != null ? caller.getParticipantId() : caller.getId();
            String email = caller.getEmail() != null ? caller.getEmail() : "";
            List<Registration> mine = registrationRepository.findByParticipantIdOrParticipantEmailIgnoreCase(pid, email);
            if (eventId != null && !eventId.trim().isEmpty()) {
                String cleanEventId = eventId.trim();
                mine = mine.stream().filter(r -> cleanEventId.equals(r.getEventId())).toList();
            }
            return mine;
        }

        if (eventId != null && !eventId.trim().isEmpty()) {
            return registrationRepository.findByEventId(eventId.trim());
        }
        if (participantId != null && !participantId.trim().isEmpty()) {
            return registrationRepository.findByParticipantId(participantId.trim());
        }
        return registrationRepository.findAll();
    }

    @DeleteMapping("/revert-priya-35")
    public ResponseEntity<?> revertPriyaNairEvent35() {
        int deleted = registrationRepository.deletePriyaEvent35();
        syncEventCounts("evt-35");
        syncEventCounts("ev-35");
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Reverted Priya Nair registration for Event #35.",
                "revertedCount", deleted
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRegistrationById(@PathVariable String id, HttpServletRequest request) {
        Optional<Registration> opt = registrationRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Registration reg = opt.get();
        if (!isAdmin(request)) {
            User caller = resolveCallerUser(request).orElse(null);
            if (!isOwner(reg, caller)) {
                return ResponseEntity.status(403).body(Map.of("success", false, "message", "Access denied"));
            }
        }
        return ResponseEntity.ok(reg);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> createRegistration(@RequestBody Registration registration, HttpServletRequest request) {
        String now = Instant.now().toString();
        if (registration.getId() == null || registration.getId().trim().isEmpty()) {
            registration.setId("reg-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (!isAdmin(request)) {
            User caller = resolveCallerUser(request).orElse(null);
            if (caller != null) {
                if (registration.getParticipantId() == null || registration.getParticipantId().isBlank()) {
                    registration.setParticipantId(caller.getParticipantId() != null ? caller.getParticipantId() : caller.getId());
                }
                if (registration.getParticipantEmail() == null || registration.getParticipantEmail().isBlank()) {
                    registration.setParticipantEmail(caller.getEmail());
                }
            }
        }
        if (registration.getParticipantId() == null || registration.getParticipantId().isBlank()) {
            String email = registration.getParticipantEmail();
            if (email != null && !email.isBlank()) {
                participantRepository.findByEmailIgnoreCase(email.trim()).ifPresent(p -> registration.setParticipantId(p.getId()));
            }
            if (registration.getParticipantId() == null || registration.getParticipantId().isBlank()) {
                registration.setParticipantId("p-" + UUID.randomUUID().toString().substring(0, 8));
            }
        }

        if (registration.getRegisteredAt() == null) registration.setRegisteredAt(now);
        if (registration.getCreatedAt() == null) registration.setCreatedAt(now);
        registration.setUpdatedAt(now);
        if (registration.getAttendanceStatus() == null) registration.setAttendanceStatus("Pending");
        if (registration.getPaymentStatus() == null) registration.setPaymentStatus("Pending");
        if (registration.getPaymentMethod() == null) registration.setPaymentMethod("UPI");
        if (registration.getPaxCount() < 1) registration.setPaxCount(1);

        if (registration.getGuests() != null) {
            for (GuestInfo g : registration.getGuests()) {
                if (g.getId() == null || g.getId().trim().isEmpty()) {
                    g.setId("guest-" + UUID.randomUUID().toString().substring(0, 8));
                }
                if (g.getName() != null) {
                    g.setName(g.getName().trim());
                }
            }
        }

        String qrToken = registration.getQrCodeToken() != null && !registration.getQrCodeToken().isBlank()
                ? registration.getQrCodeToken()
                : ("SS-REG-" + registration.getId());
        registration.setQrCodeToken(qrToken);
        if (registration.getTicketCode() == null || registration.getTicketCode().isBlank()) {
            registration.setTicketCode(qrToken);
        }
        registration.setQrCodeUrl("https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + qrToken);

        ShuffleEvent event = null;
        if (registration.getEventId() != null) {
            event = shuffleEventRepository.findById(registration.getEventId()).orElse(null);
            if (event != null && (registration.getAmountPaid() == null || registration.getAmountPaid() <= 0)) {
                registration.setAmountPaid(event.getTicketPrice() * Math.max(1, registration.getPaxCount()));
            }
        }

        Registration saved = registrationRepository.save(registration);

        if (registration.getParticipantId() != null) {
            participantRepository.findById(registration.getParticipantId()).ifPresent(p -> {
                p.setTotalRegistrations(p.getTotalRegistrations() + 1);
                if (registration.getPaxCount() > 1) {
                    p.setTotalPaxBrought(p.getTotalPaxBrought() + (registration.getPaxCount() - 1));
                }
                p.setUpdatedAt(now);
                participantRepository.save(p);
            });
        }

        syncEventCounts(saved.getEventId());
        emailNotificationService.sendRegistrationConfirmationAsync(saved, event);

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateRegistration(@PathVariable String id,
                                                @RequestBody Registration updates,
                                                HttpServletRequest request) {
        Optional<Registration> opt = registrationRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Registration existing = opt.get();
        boolean admin = isAdmin(request);
        if (!admin) {
            User caller = resolveCallerUser(request).orElse(null);
            if (!isOwner(existing, caller)) {
                return ResponseEntity.status(403).body(Map.of("success", false, "message", "Access denied"));
            }
        }

        if (updates.getParticipantName() != null) existing.setParticipantName(updates.getParticipantName());
        if (updates.getParticipantPhone() != null) existing.setParticipantPhone(updates.getParticipantPhone());
        if (updates.getParticipantArea() != null) existing.setParticipantArea(updates.getParticipantArea());
        if (updates.getPaxCount() >= 1) existing.setPaxCount(updates.getPaxCount());
        if (updates.getGuests() != null) existing.setGuests(updates.getGuests());
        if (updates.getNotes() != null) existing.setNotes(updates.getNotes());
        if (admin) {
            if (updates.getPaymentStatus() != null) existing.setPaymentStatus(updates.getPaymentStatus());
            if (updates.getAttendanceStatus() != null) existing.setAttendanceStatus(updates.getAttendanceStatus());
            if (updates.getAmountPaid() != null) existing.setAmountPaid(updates.getAmountPaid());
            if (updates.getPaymentMethod() != null) existing.setPaymentMethod(updates.getPaymentMethod());
        }
        if (updates.getGamesPlayed() != null) existing.setGamesPlayed(updates.getGamesPlayed());

        existing.setUpdatedAt(Instant.now().toString());
        Registration saved = registrationRepository.save(existing);
        syncEventCounts(saved.getEventId());
        return ResponseEntity.ok(saved);
    }

    @PatchMapping("/{id}/cancel")
    @Transactional
    public ResponseEntity<?> cancelRegistrationStatus(@PathVariable String id, HttpServletRequest request) {
        Optional<Registration> opt = registrationRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Registration reg = opt.get();
        if (!isAdmin(request)) {
            User caller = resolveCallerUser(request).orElse(null);
            if (!isOwner(reg, caller)) {
                return ResponseEntity.status(403).body(Map.of("success", false, "message", "Access denied"));
            }
        }
        reg.setAttendanceStatus("Cancelled");
        reg.setUpdatedAt(Instant.now().toString());
        Registration saved = registrationRepository.save(reg);
        syncEventCounts(saved.getEventId());
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/verify-qr")
    @Transactional
    public ResponseEntity<?> verifyQrCode(@RequestBody Map<String, String> payload) {
        String code = payload.get("code");
        String eventId = payload.get("eventId");

        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "QR Code or Ticket Token is required."));
        }

        String cleanedCode = code.trim();
        String regId = cleanedCode.startsWith("SS-REG-") ? cleanedCode.replace("SS-REG-", "") : cleanedCode;

        Optional<Registration> regOpt = registrationRepository.findByIdOrQrCodeTokenIgnoreCase(regId, cleanedCode);
        if (regOpt.isEmpty()) {
            regOpt = registrationRepository.findByQrCodeTokenIgnoreCase(cleanedCode);
        }
        if (regOpt.isEmpty()) {
            regOpt = registrationRepository.findByIdOrTicketCodeIgnoreCase(regId, cleanedCode);
        }

        if (regOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "Invalid Ticket: No registration found for code '" + cleanedCode + "'"
            ));
        }

        Registration reg = regOpt.get();

        if (eventId != null && !eventId.trim().isEmpty() && !eventId.equals(reg.getEventId())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Ticket Mismatch: This pass belongs to another event (Event ID: " + reg.getEventId() + ")",
                    "registration", reg
            ));
        }

        if ("Checked In".equalsIgnoreCase(reg.getAttendanceStatus())) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "alreadyCheckedIn", true,
                    "message", "Already Checked-In earlier at " + (reg.getCheckInTime() != null ? reg.getCheckInTime() : "entrance"),
                    "registration", reg
            ));
        }

        String now = Instant.now().toString();
        reg.setAttendanceStatus("Checked In");
        reg.setCheckInTime(now);
        reg.setCheckedInAt(now);
        reg.setUpdatedAt(now);
        Registration saved = registrationRepository.save(reg);

        if (reg.getParticipantId() != null) {
            participantRepository.findById(reg.getParticipantId()).ifPresent(p -> {
                p.setTotalEventsAttended(p.getTotalEventsAttended() + 1);
                p.setLastAttendedEventId(reg.getEventId());
                if (reg.getEventId() != null) {
                    shuffleEventRepository.findById(reg.getEventId()).ifPresent(ev -> {
                        p.setLastAttendedEventTitle(ev.getTitle());
                        if (ev.getVenue() != null && !p.getVenuesVisited().contains(ev.getVenue())) {
                            List<String> venues = new ArrayList<>(p.getVenuesVisited());
                            venues.add(ev.getVenue());
                            p.setVenuesVisited(venues);
                        }
                    });
                }
                p.setUpdatedAt(now);
                participantRepository.save(p);
            });
        }

        syncEventCounts(saved.getEventId());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "alreadyCheckedIn", false,
                "message", "Check-In Verified! Welcome " + reg.getParticipantName() + " (" + reg.getPaxCount() + " PAX)",
                "registration", saved
        ));
    }

    @GetMapping("/{id}/email-preview")
    public ResponseEntity<String> previewRegistrationEmail(@PathVariable String id) {
        return registrationRepository.findById(id).map(reg -> {
            ShuffleEvent event = reg.getEventId() != null
                    ? shuffleEventRepository.findById(reg.getEventId()).orElse(null)
                    : null;
            String html = emailNotificationService.generateQuirkyRegistrationEmailHtml(reg, event);
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/attendance")
    @Transactional
    public ResponseEntity<?> updateAttendance(@PathVariable String id,
                                              @RequestParam String status,
                                              HttpServletRequest request) {
        Optional<Registration> opt = registrationRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Registration reg = opt.get();
        if (!isAdmin(request)) {
            User caller = resolveCallerUser(request).orElse(null);
            if (!isOwner(reg, caller) || !"Cancelled".equalsIgnoreCase(status)) {
                return ResponseEntity.status(403).body(Map.of("success", false, "message", "Access denied"));
            }
        }
        String previous = reg.getAttendanceStatus();
        String now = Instant.now().toString();
        reg.setAttendanceStatus(status);
        reg.setUpdatedAt(now);
        if ("Checked In".equalsIgnoreCase(status) && !"Checked In".equalsIgnoreCase(previous)) {
            reg.setCheckInTime(now);
            reg.setCheckedInAt(now);
            if (reg.getParticipantId() != null) {
                participantRepository.findById(reg.getParticipantId()).ifPresent(p -> {
                    p.setTotalEventsAttended(p.getTotalEventsAttended() + 1);
                    p.setLastAttendedEventId(reg.getEventId());
                    p.setUpdatedAt(now);
                    participantRepository.save(p);
                });
            }
        }
        Registration saved = registrationRepository.save(reg);
        syncEventCounts(saved.getEventId());
        return ResponseEntity.ok(saved);
    }

    @PatchMapping("/{id}/payment")
    @Transactional
    public ResponseEntity<Registration> updatePayment(@PathVariable String id, @RequestParam String status) {
        return registrationRepository.findById(id).map(reg -> {
            reg.setPaymentStatus(status);
            reg.setUpdatedAt(Instant.now().toString());
            Registration saved = registrationRepository.save(reg);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/games")
    @Transactional
    public ResponseEntity<?> updateRegistrationGames(@PathVariable String id,
                                                     @RequestBody Map<String, List<String>> body,
                                                     HttpServletRequest request) {
        Optional<Registration> opt = registrationRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Registration reg = opt.get();
        if (!isAdmin(request)) {
            User caller = resolveCallerUser(request).orElse(null);
            if (!isOwner(reg, caller)) {
                return ResponseEntity.status(403).body(Map.of("success", false, "message", "Access denied"));
            }
        }

        List<String> incoming = body != null && body.get("gameIds") != null ? body.get("gameIds") : Collections.emptyList();
        Set<String> prevGames = new HashSet<>(reg.getGamesPlayed());
        List<String> sanitized = new ArrayList<>(new LinkedHashSet<>(incoming));

        for (String gid : sanitized) {
            if (!prevGames.contains(gid)) {
                gameRepository.findById(gid).ifPresent(g -> {
                    g.setPlaysCount(g.getPlaysCount() + 1);
                    g.setUpdatedAt(Instant.now().toString());
                    gameRepository.save(g);
                });
            }
        }

        reg.setGamesPlayed(sanitized);
        reg.setUpdatedAt(Instant.now().toString());
        Registration saved = registrationRepository.save(reg);

        if (reg.getParticipantId() != null) {
            participantRepository.findById(reg.getParticipantId()).ifPresent(p -> {
                Set<String> union = new LinkedHashSet<>(p.getGamesPlayedIds());
                union.addAll(sanitized);
                p.setGamesPlayedIds(new ArrayList<>(union));
                p.setUpdatedAt(Instant.now().toString());
                participantRepository.save(p);
            });
        }

        evictCache("games", "communityStats");
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/{id}/toggle-game")
    @Transactional
    public ResponseEntity<?> toggleGamePlayed(@PathVariable String id,
                                              @RequestParam String gameId,
                                              HttpServletRequest request) {
        Optional<Registration> opt = registrationRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Registration reg = opt.get();
        if (!isAdmin(request)) {
            User caller = resolveCallerUser(request).orElse(null);
            if (!isOwner(reg, caller)) {
                return ResponseEntity.status(403).body(Map.of("success", false, "message", "Access denied"));
            }
        }

        List<String> games = new ArrayList<>(reg.getGamesPlayed());
        if (games.contains(gameId)) {
            games.remove(gameId);
        } else {
            games.add(gameId);
            gameRepository.findById(gameId).ifPresent(g -> {
                g.setPlaysCount(g.getPlaysCount() + 1);
                g.setUpdatedAt(Instant.now().toString());
                gameRepository.save(g);
            });
            if (reg.getParticipantId() != null) {
                participantRepository.findById(reg.getParticipantId()).ifPresent(p -> {
                    if (!p.getGamesPlayedIds().contains(gameId)) {
                        List<String> pGames = new ArrayList<>(p.getGamesPlayedIds());
                        pGames.add(gameId);
                        p.setGamesPlayedIds(pGames);
                        p.setUpdatedAt(Instant.now().toString());
                        participantRepository.save(p);
                    }
                });
            }
        }
        reg.setGamesPlayed(games);
        reg.setUpdatedAt(Instant.now().toString());
        Registration saved = registrationRepository.save(reg);
        evictCache("games", "communityStats");
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/walk-in")
    @Transactional
    public ResponseEntity<?> addWalkIn(@RequestParam String eventId, @RequestBody Map<String, Object> body) {
        String name = body.get("name") != null ? String.valueOf(body.get("name")).trim() : "Walk-In Shuffler";
        String phone = body.get("phone") != null ? String.valueOf(body.get("phone")).trim() : "";
        String email = body.get("email") != null ? String.valueOf(body.get("email")).trim().toLowerCase() : "";
        String area = body.get("area") != null ? String.valueOf(body.get("area")).trim() : "Pune";
        int paxCount = body.get("paxCount") instanceof Number n ? Math.max(1, n.intValue()) : 1;
        String paymentStatus = body.get("paymentStatus") != null ? String.valueOf(body.get("paymentStatus")) : "Confirmed";
        Double amountPaid = body.get("amountPaid") instanceof Number n ? n.doubleValue() : null;
        String notes = body.get("notes") != null ? String.valueOf(body.get("notes")) : null;
        String now = Instant.now().toString();

        Participant participant = null;
        if (!email.isEmpty()) {
            participant = participantRepository.findByEmailIgnoreCase(email).orElse(null);
        }
        if (participant == null && !phone.isEmpty()) {
            participant = participantRepository.findByPhone(phone).orElse(null);
        }
        if (participant == null) {
            String newId = "p-" + UUID.randomUUID().toString().substring(0, 8);
            String resolvedEmail = !email.isEmpty() ? email : (newId + "@walkin.socialshuffle.in");
            Participant np = new Participant(newId, name, resolvedEmail, phone, area);
            np.setJoinedDate(java.time.LocalDate.now().toString());
            np.setRegisteredAt(now);
            np.setCreatedAt(now);
            np.setUpdatedAt(now);
            participant = participantRepository.save(np);

            if (!email.isEmpty() && userCredentialRepository.findByEmailIgnoreCase(email).isEmpty()) {
                String randomHash = PasswordSecurityUtil.hashPassword(UUID.randomUUID().toString());
                User u = new User();
                u.setId("usr-" + UUID.randomUUID().toString().substring(0, 8));
                u.setName(name);
                u.setEmail(email);
                u.setPhone(phone);
                u.setArea(area);
                u.setRole("participant");
                u.setParticipantId(participant.getId());
                u.setPassword(randomHash);
                u.setCreatedAt(now);
                userRepository.save(u);

                UserCredential cred = new UserCredential();
                cred.setId("cred-" + UUID.randomUUID().toString().substring(0, 8));
                cred.setSubjectType("participant");
                cred.setSubjectId(participant.getId());
                cred.setName(name);
                cred.setEmail(email);
                cred.setPhone(phone);
                cred.setPasswordHash(randomHash);
                cred.setRole("participant");
                cred.setArea(area);
                cred.setAuthProvider("walk-in");
                cred.setCreatedAt(now);
                cred.setUpdatedAt(now);
                userCredentialRepository.save(cred);
            }
        }

        String regId = "reg-walkin-" + UUID.randomUUID().toString().substring(0, 6);
        String qrToken = "SS-REG-" + regId;

        Registration reg = new Registration();
        reg.setId(regId);
        reg.setEventId(eventId);
        reg.setParticipantId(participant.getId());
        reg.setParticipantName(name);
        reg.setParticipantEmail(email.isEmpty() ? participant.getEmail() : email);
        reg.setParticipantPhone(phone);
        reg.setParticipantArea(area);
        reg.setPaxCount(paxCount);
        reg.setPaymentStatus(paymentStatus);
        reg.setNotes(notes);
        if (amountPaid != null) {
            reg.setAmountPaid(amountPaid);
        }
        reg.setQrCodeToken(qrToken);
        reg.setTicketCode(qrToken);
        reg.setQrCodeUrl("https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + qrToken);
        reg.setAttendanceStatus("Checked In");
        reg.setCheckInTime(now);
        reg.setCheckedInAt(now);
        reg.setRegisteredAt(now);
        reg.setCreatedAt(now);
        reg.setUpdatedAt(now);

        if (body.get("guests") instanceof List<?> rawGuests) {
            List<GuestInfo> guestList = new ArrayList<>();
            int idx = 1;
            for (Object gObj : rawGuests) {
                if (gObj instanceof Map<?, ?> gMap) {
                    Object gName = gMap.get("name");
                    if (gName != null && !String.valueOf(gName).trim().isEmpty()) {
                        GuestInfo gi = new GuestInfo();
                        gi.setId(gMap.get("id") != null ? String.valueOf(gMap.get("id")) : "g-" + idx++);
                        gi.setName(String.valueOf(gName).trim());
                        guestList.add(gi);
                    }
                }
            }
            reg.setGuests(guestList);
        }

        Registration saved = registrationRepository.save(reg);

        participant.setTotalEventsAttended(participant.getTotalEventsAttended() + 1);
        participant.setTotalRegistrations(participant.getTotalRegistrations() + 1);
        participant.setTotalPaxBrought(participant.getTotalPaxBrought() + Math.max(0, paxCount - 1));
        participant.setLastAttendedEventId(eventId);
        participant.setUpdatedAt(now);
        participantRepository.save(participant);

        syncEventCounts(eventId);
        ShuffleEvent event = shuffleEventRepository.findById(eventId).orElse(null);
        if (saved.getParticipantEmail() != null && saved.getParticipantEmail().contains("@") && !saved.getParticipantEmail().endsWith("@walkin.socialshuffle.in")) {
            emailNotificationService.sendRegistrationConfirmationAsync(saved, event);
        }

        return ResponseEntity.ok(saved);
    }

    @PostMapping("/bulk-import")
    @Transactional
    public ResponseEntity<?> bulkImportRegistrations(@RequestBody List<Registration> registrations) {
        if (registrations == null || registrations.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "No registrations provided."));
        }
        String now = Instant.now().toString();
        Set<String> affectedEvents = new HashSet<>();
        for (Registration reg : registrations) {
            if (reg.getId() == null || reg.getId().trim().isEmpty()) {
                reg.setId("reg-imp-" + UUID.randomUUID().toString().substring(0, 8));
            }
            if (reg.getRegisteredAt() == null) reg.setRegisteredAt(now);
            if (reg.getCreatedAt() == null) reg.setCreatedAt(now);
            reg.setUpdatedAt(now);
            if (reg.getAttendanceStatus() == null) reg.setAttendanceStatus("Pending");
            if (reg.getPaymentStatus() == null) reg.setPaymentStatus("Confirmed");
            if (reg.getQrCodeToken() == null) {
                String token = "SS-REG-" + reg.getId();
                reg.setQrCodeToken(token);
                reg.setTicketCode(token);
                reg.setQrCodeUrl("https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + token);
            }
            if (reg.getEventId() != null) affectedEvents.add(reg.getEventId());
        }
        List<Registration> saved = registrationRepository.saveAll(registrations);
        affectedEvents.forEach(this::syncEventCounts);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "importedCount", saved.size(),
                "registrations", saved
        ));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteRegistration(@PathVariable String id, HttpServletRequest request) {
        Optional<Registration> opt = registrationRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Registration reg = opt.get();
        if (!isAdmin(request)) {
            User caller = resolveCallerUser(request).orElse(null);
            if (!isOwner(reg, caller)) {
                return ResponseEntity.status(403).body(Map.of("success", false, "message", "Access denied"));
            }
        }
        String eventId = reg.getEventId();
        registrationRepository.deleteById(id);
        syncEventCounts(eventId);
        return ResponseEntity.ok(Map.of("success", true, "id", id));
    }
}
