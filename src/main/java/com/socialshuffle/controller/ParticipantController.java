package com.socialshuffle.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialshuffle.model.ConqueredGameEntry;
import com.socialshuffle.model.Game;
import com.socialshuffle.model.Participant;
import com.socialshuffle.model.User;
import com.socialshuffle.repository.*;
import com.socialshuffle.security.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
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
    private final GameRepository gameRepository;
    private final CacheManager cacheManager;
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public ParticipantController(ParticipantRepository participantRepository,
                                 RegistrationRepository registrationRepository,
                                 UserRepository userRepository,
                                 UserCredentialRepository userCredentialRepository,
                                 ParticipantBadgeRepository participantBadgeRepository,
                                 GoogleUserRepository googleUserRepository,
                                 GameRepository gameRepository,
                                 CacheManager cacheManager) {
        this.participantRepository = participantRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.participantBadgeRepository = participantBadgeRepository;
        this.googleUserRepository = googleUserRepository;
        this.gameRepository = gameRepository;
        this.cacheManager = cacheManager;
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

        Set<String> previousPlayedIds = new LinkedHashSet<>(
                existing.getGamesPlayedIds() != null ? existing.getGamesPlayedIds() : Collections.emptyList()
        );
        List<Game> allGames = gameRepository.findAll();

        // Both the authenticated participant (self) and admins can update their gaming passport logs
        if (updates.get("gamesPlayedIds") instanceof List<?> list) {
            Set<String> gids = new LinkedHashSet<>();
            for (Object item : list) {
                if (item != null) {
                    String s = String.valueOf(item).trim();
                    if (!s.isEmpty()) gids.add(s);
                }
            }
            existing.setGamesPlayedIds(new ArrayList<>(gids));
        }
        if (updates.get("conqueredGames") instanceof List<?> rawConquered) {
            try {
                List<ConqueredGameEntry> parsedEntries = objectMapper.convertValue(
                        rawConquered,
                        new TypeReference<List<ConqueredGameEntry>>() {}
                );
                List<ConqueredGameEntry> normalizedEntries = new ArrayList<>();
                Set<String> currentPlayed = new LinkedHashSet<>(
                        existing.getGamesPlayedIds() != null ? existing.getGamesPlayedIds() : Collections.emptyList()
                );
                for (ConqueredGameEntry entry : parsedEntries) {
                    if (entry == null || entry.getName() == null || entry.getName().trim().isEmpty()) {
                        continue;
                    }
                    ConqueredGameEntry normalized = normalizeConqueredEntry(entry, allGames, now);
                    normalizedEntries.add(normalized);
                    if (normalized.getLibraryGameId() != null && !normalized.getLibraryGameId().isEmpty()) {
                        currentPlayed.add(normalized.getLibraryGameId());
                    }
                }
                existing.setConqueredGames(normalizedEntries);
                existing.setGamesPlayedIds(new ArrayList<>(currentPlayed));
            } catch (Exception ignored) {
            }
        }

        incrementNewlyPlayedLibraryGames(previousPlayedIds, existing.getGamesPlayedIds(), allGames, now);

        if (admin) {
            if (updates.get("totalEventsAttended") instanceof Number n) existing.setTotalEventsAttended(Math.max(0, n.intValue()));
            if (updates.get("totalRegistrations") instanceof Number n) existing.setTotalRegistrations(Math.max(0, n.intValue()));
            if (updates.get("totalPaxBrought") instanceof Number n) existing.setTotalPaxBrought(Math.max(0, n.intValue()));
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
                userRepository.save(u);
            });
            userCredentialRepository.findByEmailIgnoreCase(saved.getEmail()).ifPresent(c -> {
                c.setPhone(saved.getPhone());
                if (saved.getUsername() != null) c.setUsername(saved.getUsername());
                c.setUpdatedAt(now);
                userCredentialRepository.save(c);
            });
        }

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}/passport")
    public ResponseEntity<?> getPassport(@PathVariable String id, HttpServletRequest request) {
        if (!canAccessParticipant(id, request)) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Access denied: You can only view your own board gaming passport."
            ));
        }
        Optional<Participant> opt = participantRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<Game> allGames = gameRepository.findAll();
        return ResponseEntity.ok(buildPassportSummary(opt.get(), allGames));
    }

    @PostMapping("/{id}/passport/games")
    @Transactional
    public ResponseEntity<?> addPassportGame(@PathVariable String id,
                                             @RequestBody ConqueredGameEntry rawEntry,
                                             HttpServletRequest request) {
        if (!canAccessParticipant(id, request)) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Access denied: You can only update your own board gaming passport."
            ));
        }
        Optional<Participant> opt = participantRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (rawEntry == null || rawEntry.getName() == null || rawEntry.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Game name is required."
            ));
        }

        Participant participant = opt.get();
        String now = Instant.now().toString();
        List<Game> allGames = gameRepository.findAll();
        Set<String> previousPlayedIds = new LinkedHashSet<>(
                participant.getGamesPlayedIds() != null ? participant.getGamesPlayedIds() : Collections.emptyList()
        );

        ConqueredGameEntry normalized = normalizeConqueredEntry(rawEntry, allGames, now);
        List<ConqueredGameEntry> updatedList = new ArrayList<>();
        updatedList.add(normalized);
        if (participant.getConqueredGames() != null) {
            updatedList.addAll(participant.getConqueredGames());
        }
        participant.setConqueredGames(updatedList);

        Set<String> nextPlayedIds = new LinkedHashSet<>(previousPlayedIds);
        if (normalized.getLibraryGameId() != null && !normalized.getLibraryGameId().isEmpty()) {
            nextPlayedIds.add(normalized.getLibraryGameId());
        }
        participant.setGamesPlayedIds(new ArrayList<>(nextPlayedIds));
        participant.setUpdatedAt(now);

        incrementNewlyPlayedLibraryGames(previousPlayedIds, participant.getGamesPlayedIds(), allGames, now);
        Participant saved = participantRepository.save(participant);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("entry", normalized);
        resp.put("participant", saved);
        resp.put("passport", buildPassportSummary(saved, allGames));
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/{id}/passport/games/{entryId}")
    @Transactional
    public ResponseEntity<?> updatePassportGame(@PathVariable String id,
                                                @PathVariable String entryId,
                                                @RequestBody ConqueredGameEntry updates,
                                                HttpServletRequest request) {
        if (!canAccessParticipant(id, request)) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Access denied: You can only update your own board gaming passport."
            ));
        }
        Optional<Participant> opt = participantRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Participant participant = opt.get();
        List<ConqueredGameEntry> existingEntries = participant.getConqueredGames() != null
                ? new ArrayList<>(participant.getConqueredGames())
                : new ArrayList<>();
        List<Game> allGames = gameRepository.findAll();
        String now = Instant.now().toString();
        Set<String> previousPlayedIds = new LinkedHashSet<>(
                participant.getGamesPlayedIds() != null ? participant.getGamesPlayedIds() : Collections.emptyList()
        );

        ConqueredGameEntry updatedEntry = null;
        for (int i = 0; i < existingEntries.size(); i++) {
            ConqueredGameEntry current = existingEntries.get(i);
            if (current != null && entryId.equals(current.getId())) {
                ConqueredGameEntry merged = new ConqueredGameEntry();
                merged.setId(current.getId());
                merged.setName(updates.getName() != null && !updates.getName().trim().isEmpty()
                        ? updates.getName().trim() : current.getName());
                merged.setGenre(updates.getGenre() != null && !updates.getGenre().trim().isEmpty()
                        ? updates.getGenre().trim() : current.getGenre());
                merged.setPlayers(updates.getPlayers() != null && !updates.getPlayers().trim().isEmpty()
                        ? updates.getPlayers().trim() : current.getPlayers());
                merged.setDuration(updates.getDuration() != null && !updates.getDuration().trim().isEmpty()
                        ? updates.getDuration().trim() : current.getDuration());
                merged.setDatePlayed(updates.getDatePlayed() != null && !updates.getDatePlayed().trim().isEmpty()
                        ? updates.getDatePlayed().trim() : current.getDatePlayed());
                merged.setNotes(updates.getNotes() != null ? updates.getNotes().trim() : current.getNotes());
                merged.setLibraryGameId(updates.getLibraryGameId());
                merged.setCreatedAt(current.getCreatedAt() != null ? current.getCreatedAt() : now);

                updatedEntry = normalizeConqueredEntry(merged, allGames, now);
                existingEntries.set(i, updatedEntry);
                break;
            }
        }

        if (updatedEntry == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "Conquered game entry not found."
            ));
        }

        participant.setConqueredGames(existingEntries);
        Set<String> nextPlayedIds = new LinkedHashSet<>(previousPlayedIds);
        for (ConqueredGameEntry item : existingEntries) {
            if (item.getLibraryGameId() != null && !item.getLibraryGameId().isEmpty()) {
                nextPlayedIds.add(item.getLibraryGameId());
            }
        }
        participant.setGamesPlayedIds(new ArrayList<>(nextPlayedIds));
        participant.setUpdatedAt(now);

        incrementNewlyPlayedLibraryGames(previousPlayedIds, participant.getGamesPlayedIds(), allGames, now);
        Participant saved = participantRepository.save(participant);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("entry", updatedEntry);
        resp.put("participant", saved);
        resp.put("passport", buildPassportSummary(saved, allGames));
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{id}/passport/games/{entryId}")
    @Transactional
    public ResponseEntity<?> deletePassportGame(@PathVariable String id,
                                                @PathVariable String entryId,
                                                HttpServletRequest request) {
        if (!canAccessParticipant(id, request)) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Access denied: You can only update your own board gaming passport."
            ));
        }
        Optional<Participant> opt = participantRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Participant participant = opt.get();
        List<ConqueredGameEntry> existingEntries = participant.getConqueredGames() != null
                ? participant.getConqueredGames()
                : Collections.emptyList();

        ConqueredGameEntry removed = null;
        List<ConqueredGameEntry> remaining = new ArrayList<>();
        for (ConqueredGameEntry item : existingEntries) {
            if (item != null && entryId.equals(item.getId()) && removed == null) {
                removed = item;
            } else if (item != null) {
                remaining.add(item);
            }
        }

        if (removed == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "Conquered game entry not found."
            ));
        }

        participant.setConqueredGames(remaining);

        if (removed.getLibraryGameId() != null && !removed.getLibraryGameId().isEmpty()) {
            String removedGid = removed.getLibraryGameId();
            boolean stillInConquered = remaining.stream()
                    .anyMatch(e -> removedGid.equals(e.getLibraryGameId()));
            boolean playedInMeetup = registrationRepository.findByParticipantId(id).stream()
                    .anyMatch(r -> r.getGamesPlayed() != null && r.getGamesPlayed().contains(removedGid));
            if (!stillInConquered && !playedInMeetup) {
                List<String> nextPlayed = new ArrayList<>(
                        participant.getGamesPlayedIds() != null ? participant.getGamesPlayedIds() : Collections.emptyList()
                );
                nextPlayed.removeIf(removedGid::equals);
                participant.setGamesPlayedIds(nextPlayed);
            }
        }

        participant.setUpdatedAt(Instant.now().toString());
        Participant saved = participantRepository.save(participant);
        List<Game> allGames = gameRepository.findAll();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("entryId", entryId);
        resp.put("participant", saved);
        resp.put("passport", buildPassportSummary(saved, allGames));
        return ResponseEntity.ok(resp);
    }

    private boolean canAccessParticipant(String targetParticipantId, HttpServletRequest request) {
        if (isAdmin(request)) return true;
        Optional<Participant> self = resolveCallerParticipant(request);
        return self.isPresent() && targetParticipantId.equals(self.get().getId());
    }

    private String normalizeGameTitle(String name) {
        if (name == null) return "";
        return name.toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[:'’\".,!?()-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Game matchLibraryGame(String rawName, String libraryGameId, List<Game> allGames) {
        if (allGames == null || allGames.isEmpty()) return null;
        if (libraryGameId != null && !libraryGameId.trim().isEmpty()) {
            String targetId = libraryGameId.trim();
            for (Game g : allGames) {
                if (targetId.equals(g.getId())) return g;
            }
        }
        String clean = rawName != null ? rawName.trim().toLowerCase(Locale.ROOT) : "";
        if (clean.isEmpty()) return null;
        for (Game g : allGames) {
            if (g.getName() != null && g.getName().trim().toLowerCase(Locale.ROOT).equals(clean)) {
                return g;
            }
        }
        String norm = normalizeGameTitle(rawName);
        if (norm.isEmpty()) return null;
        for (Game g : allGames) {
            if (g.getName() != null && normalizeGameTitle(g.getName()).equals(norm)) {
                return g;
            }
        }
        return null;
    }

    private ConqueredGameEntry normalizeConqueredEntry(ConqueredGameEntry raw, List<Game> allGames, String fallbackTimestamp) {
        String cleanName = raw.getName() != null ? raw.getName().trim() : "";
        Game matchedLib = matchLibraryGame(cleanName, raw.getLibraryGameId(), allGames);

        ConqueredGameEntry entry = new ConqueredGameEntry();
        entry.setId(raw.getId() != null && !raw.getId().trim().isEmpty()
                ? raw.getId().trim()
                : "cg-" + UUID.randomUUID().toString().substring(0, 8));
        entry.setName(matchedLib != null && matchedLib.getName() != null ? matchedLib.getName() : cleanName);

        String genre = raw.getGenre() != null && !raw.getGenre().trim().isEmpty()
                ? raw.getGenre().trim()
                : (matchedLib != null && matchedLib.getCategory() != null ? matchedLib.getCategory() : "Strategy");
        String players = raw.getPlayers() != null && !raw.getPlayers().trim().isEmpty()
                ? raw.getPlayers().trim()
                : (matchedLib != null && matchedLib.getPlayers() != null ? matchedLib.getPlayers() : "2-4 Players");
        String duration = raw.getDuration() != null && !raw.getDuration().trim().isEmpty()
                ? raw.getDuration().trim()
                : (matchedLib != null && matchedLib.getDuration() != null ? matchedLib.getDuration() : "45-60 mins");

        entry.setGenre(genre);
        entry.setPlayers(players);
        entry.setDuration(duration);
        entry.setDatePlayed(raw.getDatePlayed() != null && !raw.getDatePlayed().trim().isEmpty()
                ? raw.getDatePlayed().trim()
                : LocalDate.now().toString());
        entry.setNotes(raw.getNotes() != null ? raw.getNotes().trim() : "");
        entry.setLibraryGameId(matchedLib != null ? matchedLib.getId() : null);
        entry.setSource(matchedLib != null ? "social_shuffle" : "outside");
        entry.setCreatedAt(raw.getCreatedAt() != null && !raw.getCreatedAt().trim().isEmpty()
                ? raw.getCreatedAt().trim()
                : fallbackTimestamp);
        return entry;
    }

    private void incrementNewlyPlayedLibraryGames(Set<String> previousPlayedIds,
                                                  List<String> updatedPlayedIds,
                                                  List<Game> allGames,
                                                  String now) {
        if (updatedPlayedIds == null || updatedPlayedIds.isEmpty() || allGames == null || allGames.isEmpty()) {
            return;
        }
        Map<String, Game> gameById = new HashMap<>();
        for (Game g : allGames) {
            if (g.getId() != null) gameById.put(g.getId(), g);
        }
        boolean anyChanged = false;
        for (String gid : updatedPlayedIds) {
            if (gid != null && !previousPlayedIds.contains(gid)) {
                Game g = gameById.get(gid);
                if (g != null) {
                    g.setPlaysCount(Math.max(0, g.getPlaysCount()) + 1);
                    g.setUpdatedAt(now);
                    gameRepository.save(g);
                    anyChanged = true;
                }
            }
        }
        if (anyChanged && cacheManager != null) {
            Cache gamesCache = cacheManager.getCache("games");
            if (gamesCache != null) gamesCache.clear();
            Cache statsCache = cacheManager.getCache("communityStats");
            if (statsCache != null) statsCache.clear();
        }
    }

    private Map<String, Object> buildPassportSummary(Participant participant, List<Game> allGames) {
        Set<String> playedLibraryIdSet = new LinkedHashSet<>(
                participant.getGamesPlayedIds() != null ? participant.getGamesPlayedIds() : Collections.emptyList()
        );
        Set<String> outsideNormalizedNames = new LinkedHashSet<>();

        List<ConqueredGameEntry> conqueredGames = participant.getConqueredGames() != null
                ? participant.getConqueredGames()
                : Collections.emptyList();

        for (ConqueredGameEntry entry : conqueredGames) {
            if (entry == null || entry.getName() == null || entry.getName().trim().isEmpty()) continue;
            Game matchedLib = matchLibraryGame(entry.getName(), entry.getLibraryGameId(), allGames);
            if (matchedLib != null) {
                playedLibraryIdSet.add(matchedLib.getId());
            } else {
                String norm = normalizeGameTitle(entry.getName());
                if (!norm.isEmpty()) outsideNormalizedNames.add(norm);
            }
        }

        int socialShufflePlayedCount = 0;
        for (Game g : allGames) {
            if (playedLibraryIdSet.contains(g.getId())) {
                socialShufflePlayedCount++;
            }
        }
        int overallConqueredCount = socialShufflePlayedCount + outsideNormalizedNames.size();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("participantId", participant.getId());
        summary.put("name", participant.getName());
        summary.put("overallConqueredCount", overallConqueredCount);
        summary.put("socialShufflePlayedCount", socialShufflePlayedCount);
        summary.put("socialShuffleLibraryTotal", allGames.size());
        summary.put("conqueredGames", conqueredGames);
        summary.put("gamesPlayedIds", new ArrayList<>(playedLibraryIdSet));
        return summary;
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

        List<ConqueredGameEntry> mergedConquered = new ArrayList<>(target.getConqueredGames());
        Set<String> existingEntryIds = new HashSet<>();
        for (ConqueredGameEntry entry : mergedConquered) {
            if (entry.getId() != null) existingEntryIds.add(entry.getId());
        }
        for (ConqueredGameEntry entry : source.getConqueredGames()) {
            if (entry.getId() == null || !existingEntryIds.contains(entry.getId())) {
                mergedConquered.add(entry);
            }
        }
        target.setConqueredGames(mergedConquered);

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
