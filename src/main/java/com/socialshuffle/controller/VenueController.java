package com.socialshuffle.controller;

import com.socialshuffle.model.Venue;
import com.socialshuffle.repository.VenueRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/venues")
@CrossOrigin(origins = "*")
public class VenueController {

    private final VenueRepository venueRepository;

    public VenueController(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    @GetMapping
    public List<Venue> getAllVenues(@RequestParam(required = false) Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return venueRepository.findByActiveTrue();
        }
        return venueRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Venue> getVenueById(@PathVariable String id) {
        return venueRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Venue createVenue(@RequestBody Venue venue) {
        if (venue.getId() == null || venue.getId().trim().isEmpty()) {
            venue.setId("ven-" + UUID.randomUUID().toString().substring(0, 8));
        }
        return venueRepository.save(venue);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Venue> updateVenue(@PathVariable String id, @RequestBody Venue updates) {
        return venueRepository.findById(id).map(existing -> {
            if (updates.getName() != null) existing.setName(updates.getName());
            if (updates.getAddress() != null) existing.setAddress(updates.getAddress());
            if (updates.getArea() != null) existing.setArea(updates.getArea());
            if (updates.getCapacity() > 0) existing.setCapacity(updates.getCapacity());
            if (updates.getContactPhone() != null) existing.setContactPhone(updates.getContactPhone());
            if (updates.getMapsUrl() != null) existing.setMapsUrl(updates.getMapsUrl());
            if (updates.getImage() != null) existing.setImage(updates.getImage());
            existing.setActive(updates.isActive());
            Venue saved = venueRepository.save(existing);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }
}
