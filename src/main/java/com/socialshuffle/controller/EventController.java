package com.socialshuffle.controller;

import com.socialshuffle.model.ShuffleEvent;
import com.socialshuffle.repository.ShuffleEventRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {

    private final ShuffleEventRepository eventRepository;
    private final NotificationController notificationController;

    public EventController(ShuffleEventRepository eventRepository,
                           NotificationController notificationController) {
        this.eventRepository = eventRepository;
        this.notificationController = notificationController;
    }

    @GetMapping
    @Cacheable(value = "events", key = "#status != null && !#status.trim().isEmpty() ? #status.trim().toLowerCase() : 'all'")
    public List<ShuffleEvent> getAllEvents(@RequestParam(required = false) String status) {
        if (status != null && !status.trim().isEmpty()) {
            return eventRepository.findByStatusIgnoreCase(status.trim());
        }
        return eventRepository.findAllByOrderByDateDesc();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShuffleEvent> getEventById(@PathVariable String id) {
        return eventRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @CacheEvict(value = {"events", "communityStats"}, allEntries = true)
    public ShuffleEvent createEvent(@RequestBody ShuffleEvent event) {
        if (event.getId() == null || event.getId().trim().isEmpty()) {
            event.setId("evt-" + (event.getNumber() > 0 ? event.getNumber() : UUID.randomUUID().toString().substring(0, 6)));
        }
        if (event.getStatus() == null || event.getStatus().trim().isEmpty()) {
            event.setStatus("upcoming");
        }
        String now = Instant.now().toString();
        if (event.getCreatedAt() == null) event.setCreatedAt(now);
        event.setUpdatedAt(now);
        return eventRepository.save(event);
    }

    @PutMapping("/{id}")
    @CacheEvict(value = {"events", "communityStats"}, allEntries = true)
    public ResponseEntity<ShuffleEvent> updateEvent(@PathVariable String id, @RequestBody ShuffleEvent updates) {
        return eventRepository.findById(id).map(existing -> {
            if (updates.getTitle() != null) existing.setTitle(updates.getTitle());
            if (updates.getDate() != null) existing.setDate(updates.getDate());
            if (updates.getTime() != null) existing.setTime(updates.getTime());
            if (updates.getVenue() != null) existing.setVenue(updates.getVenue());
            if (updates.getAddress() != null) existing.setAddress(updates.getAddress());
            if (updates.getArea() != null) existing.setArea(updates.getArea());
            if (updates.getCategory() != null) existing.setCategory(updates.getCategory());
            if (updates.getCapacity() > 0) existing.setCapacity(updates.getCapacity());
            if (updates.getTicketPrice() >= 0) existing.setTicketPrice(updates.getTicketPrice());
            if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
            if (updates.getNotes() != null) existing.setNotes(updates.getNotes());
            if (updates.getPlannedGames() != null) existing.setPlannedGames(updates.getPlannedGames());
            if (updates.getStatus() != null) existing.setStatus(updates.getStatus());
            if (updates.getCoverImage() != null) existing.setCoverImage(updates.getCoverImage());
            if (updates.getMapLocation() != null) existing.setMapLocation(updates.getMapLocation());
            existing.setUpdatedAt(Instant.now().toString());
            ShuffleEvent saved = eventRepository.save(existing);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/duplicate")
    @CacheEvict(value = {"events", "communityStats"}, allEntries = true)
    public ResponseEntity<ShuffleEvent> duplicateEvent(@PathVariable String id) {
        return eventRepository.findById(id).map(source -> {
            ShuffleEvent clone = new ShuffleEvent();
            int newNum = source.getNumber() + 1;
            clone.setId("evt-" + newNum + "-" + UUID.randomUUID().toString().substring(0, 4));
            clone.setNumber(newNum);
            clone.setTitle(source.getTitle().replaceAll("#\\d+", "#" + newNum) + " (Copy)");
            clone.setDate(source.getDate());
            clone.setTime(source.getTime());
            clone.setVenue(source.getVenue());
            clone.setAddress(source.getAddress());
            clone.setArea(source.getArea());
            clone.setCategory(source.getCategory());
            clone.setCapacity(source.getCapacity());
            clone.setTicketPrice(source.getTicketPrice());
            clone.setDescription(source.getDescription());
            clone.setNotes(source.getNotes());
            clone.setPlannedGames(source.getPlannedGames());
            clone.setCoverImage(source.getCoverImage());
            clone.setMapLocation(source.getMapLocation());
            clone.setStatus("upcoming");
            String now = Instant.now().toString();
            clone.setCreatedAt(now);
            clone.setUpdatedAt(now);
            ShuffleEvent saved = eventRepository.save(clone);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{eventId}/push-notification")
    public ResponseEntity<?> pushNotificationAlias(@PathVariable String eventId,
                                                   @RequestBody NotificationController.EventBroadcastRequest req) {
        return notificationController.sendEventBroadcast(eventId, req);
    }

    @DeleteMapping("/{id}")
    @CacheEvict(value = {"events", "communityStats"}, allEntries = true)
    public ResponseEntity<?> deleteEvent(@PathVariable String id) {
        if (!eventRepository.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Event not found"));
        }
        eventRepository.deleteById(id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "id", id,
                "message", "Event deleted from database."
        ));
    }
}
