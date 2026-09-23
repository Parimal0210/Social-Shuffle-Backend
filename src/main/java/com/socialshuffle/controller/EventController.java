package com.socialshuffle.controller;

import com.socialshuffle.model.ShuffleEvent;
import com.socialshuffle.repository.ShuffleEventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {

    private final ShuffleEventRepository eventRepository;

    public EventController(ShuffleEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @GetMapping
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
    public ShuffleEvent createEvent(@RequestBody ShuffleEvent event) {
        if (event.getId() == null || event.getId().trim().isEmpty()) {
            event.setId("evt-" + (event.getNumber() > 0 ? event.getNumber() : UUID.randomUUID().toString().substring(0, 6)));
        }
        if (event.getStatus() == null) {
            event.setStatus("upcoming");
        }
        return eventRepository.save(event);
    }

    @PutMapping("/{id}")
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
            ShuffleEvent saved = eventRepository.save(existing);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<ShuffleEvent> duplicateEvent(@PathVariable String id) {
        return eventRepository.findById(id).map(source -> {
            ShuffleEvent clone = new ShuffleEvent();
            int newNum = source.getNumber() + 1;
            clone.setId("evt-" + newNum);
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
            clone.setStatus("upcoming");
            ShuffleEvent saved = eventRepository.save(clone);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> archiveOrDeleteEvent(@PathVariable String id) {
        return eventRepository.findById(id).map(existing -> {
            existing.setStatus("archived");
            eventRepository.save(existing);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
