package com.socialshuffle.controller;

import com.socialshuffle.dto.AuthResponse;
import com.socialshuffle.dto.LoginRequest;
import com.socialshuffle.dto.RegisterRequest;
import com.socialshuffle.model.Participant;
import com.socialshuffle.model.User;
import com.socialshuffle.repository.ParticipantRepository;
import com.socialshuffle.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;

    public AuthController(UserRepository userRepository, ParticipantRepository participantRepository) {
        this.userRepository = userRepository;
        this.participantRepository = participantRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (req.getEmail() == null || req.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Email is required"));
        }
        if (req.getName() == null || req.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Name is required"));
        }
        if (req.getPhone() == null || req.getPhone().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Phone number is required"));
        }

        String email = req.getEmail().trim().toLowerCase();
        String phone = req.getPhone().trim();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "An account with this email already exists"));
        }

        String participantId = "p-" + UUID.randomUUID().toString().substring(0, 8);
        String userId = "u-" + UUID.randomUUID().toString().substring(0, 8);

        // Create participant profile in MongoDB
        Participant participant = new Participant(participantId, req.getName().trim(), email, phone, req.getArea() != null ? req.getArea().trim() : "Pune");
        participant.setBio(req.getBio());
        participant.setAvatar(req.getAvatar());
        participant.setJoinedDate(java.time.LocalDate.now().toString());
        participantRepository.save(participant);

        // Create user account with strict 'participant' role
        User user = new User(userId, req.getName().trim(), email, phone, req.getPassword() != null ? req.getPassword() : "shuffler123", "participant", req.getArea());
        user.setAvatar(req.getAvatar());
        user.setBio(req.getBio());
        user.setParticipantId(participantId);
        userRepository.save(user);

        String token = "jwt_" + Base64.getEncoder().encodeToString((userId + ":participant:" + System.currentTimeMillis()).getBytes());

        return ResponseEntity.ok(new AuthResponse(token, user, participant, "Participant account registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (req.getEmailOrPhone() == null || req.getEmailOrPhone().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Email or phone number is required"));
        }

        String identifier = req.getEmailOrPhone().trim();
        Optional<User> userOpt = userRepository.findByEmailOrPhone(identifier);

        if (userOpt.isEmpty()) {
            // Also check participants table if created during walk-in or offline
            Optional<Participant> pOpt = participantRepository.findByEmailOrPhone(identifier, identifier);
            if (pOpt.isPresent()) {
                Participant p = pOpt.get();
                User autoUser = new User("u-" + UUID.randomUUID().toString().substring(0, 8), p.getName(), p.getEmail(), p.getPhone(), "shuffler123", "participant", p.getArea());
                autoUser.setParticipantId(p.getId());
                userRepository.save(autoUser);
                userOpt = Optional.of(autoUser);
            } else {
                return ResponseEntity.status(401).body(Collections.singletonMap("error", "No user found with provided email or phone. Please register."));
            }
        }

        User user = userOpt.get();

        // Validate password if user has password set
        if (user.getPassword() != null && !user.getPassword().isEmpty() && req.getPassword() != null && !req.getPassword().isEmpty()) {
            if (!user.getPassword().equals(req.getPassword())) {
                return ResponseEntity.status(401).body(Collections.singletonMap("error", "Invalid password"));
            }
        }

        Participant participant = null;
        if (user.getParticipantId() != null) {
            participant = participantRepository.findById(user.getParticipantId()).orElse(null);
        }
        if (participant == null && "participant".equalsIgnoreCase(user.getRole())) {
            participant = participantRepository.findByEmailIgnoreCase(user.getEmail()).orElse(null);
        }

        String token = "jwt_" + Base64.getEncoder().encodeToString((user.getId() + ":" + user.getRole() + ":" + System.currentTimeMillis()).getBytes());

        return ResponseEntity.ok(new AuthResponse(token, user, participant, "Login successful"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String decoded = new String(Base64.getDecoder().decode(token.replace("jwt_", "")));
                String[] parts = decoded.split(":");
                String userId = parts[0];
                return userRepository.findById(userId)
                        .map(user -> {
                            Participant p = null;
                            if (user.getParticipantId() != null) {
                                p = participantRepository.findById(user.getParticipantId()).orElse(null);
                            }
                            return ResponseEntity.ok(new AuthResponse(token, user, p, "User session active"));
                        })
                        .orElse(ResponseEntity.status(401).build());
            } catch (Exception e) {
                return ResponseEntity.status(401).build();
            }
        }
        return ResponseEntity.status(401).build();
    }
}
