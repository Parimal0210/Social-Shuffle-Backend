package com.socialshuffle.controller;

import com.socialshuffle.model.Participant;
import com.socialshuffle.repository.ParticipantRepository;
import com.socialshuffle.repository.RegistrationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/participants")
@CrossOrigin(origins = "*")
public class ParticipantController {

    private final ParticipantRepository participantRepository;
    private final RegistrationRepository registrationRepository;

    public ParticipantController(ParticipantRepository participantRepository,
                                 RegistrationRepository registrationRepository) {
        this.participantRepository = participantRepository;
        this.registrationRepository = registrationRepository;
    }

    @GetMapping
    public List<Participant> getAllParticipants(@RequestParam(required = false) String search,
                                                @RequestParam(required = false) String area) {
        if (search != null && !search.trim().isEmpty()) {
            return participantRepository.searchParticipants(search.trim());
        }
        if (area != null && !area.trim().isEmpty()) {
            return participantRepository.findByAreaIgnoreCase(area.trim());
        }
        return participantRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Participant> getParticipantById(@PathVariable String id) {
        return participantRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Participant createParticipant(@RequestBody Participant participant) {
        if (participant.getId() == null || participant.getId().trim().isEmpty()) {
            participant.setId("p-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (participant.getJoinedDate() == null) {
            participant.setJoinedDate(java.time.LocalDate.now().toString());
        }
        return participantRepository.save(participant);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Participant> updateParticipant(@PathVariable String id,
                                                         @RequestBody Participant updates) {
        return participantRepository.findById(id).map(existing -> {
            if (updates.getName() != null) existing.setName(updates.getName());
            if (updates.getEmail() != null) existing.setEmail(updates.getEmail());
            if (updates.getPhone() != null) existing.setPhone(updates.getPhone());
            if (updates.getArea() != null) existing.setArea(updates.getArea());
            if (updates.getBio() != null) existing.setBio(updates.getBio());
            if (updates.getAvatar() != null) existing.setAvatar(updates.getAvatar());
            if (updates.getTotalEventsAttended() > 0) existing.setTotalEventsAttended(updates.getTotalEventsAttended());
            if (updates.getTotalRegistrations() > 0) existing.setTotalRegistrations(updates.getTotalRegistrations());
            if (updates.getGamesPlayedIds() != null) existing.setGamesPlayedIds(updates.getGamesPlayedIds());
            if (updates.getVenuesVisited() != null) existing.setVenuesVisited(updates.getVenuesVisited());
            if (updates.getTotalPaxBrought() > 0) existing.setTotalPaxBrought(updates.getTotalPaxBrought());
            Participant saved = participantRepository.save(existing);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
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
    public ResponseEntity<?> mergeParticipants(@RequestParam String sourceId, @RequestParam String targetId) {
        Optional<Participant> sourceOpt = participantRepository.findById(sourceId);
        Optional<Participant> targetOpt = participantRepository.findById(targetId);

        if (sourceOpt.isEmpty() || targetOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Source or target participant not found");
        }

        Participant source = sourceOpt.get();
        Participant target = targetOpt.get();

        // Merge stats
        target.setTotalEventsAttended(target.getTotalEventsAttended() + source.getTotalEventsAttended());
        target.setTotalRegistrations(target.getTotalRegistrations() + source.getTotalRegistrations());
        target.setTotalPaxBrought(target.getTotalPaxBrought() + source.getTotalPaxBrought());

        // Merge games and venues uniquely
        Set<String> games = new HashSet<>(target.getGamesPlayedIds());
        games.addAll(source.getGamesPlayedIds());
        target.setGamesPlayedIds(new ArrayList<>(games));

        Set<String> venues = new HashSet<>(target.getVenuesVisited());
        venues.addAll(source.getVenuesVisited());
        target.setVenuesVisited(new ArrayList<>(venues));

        participantRepository.save(target);

        // Reassign registrations from source to target
        registrationRepository.findByParticipantId(sourceId).forEach(reg -> {
            reg.setParticipantId(targetId);
            registrationRepository.save(reg);
        });

        // Delete source
        participantRepository.deleteById(sourceId);

        return ResponseEntity.ok(target);
    }
}
