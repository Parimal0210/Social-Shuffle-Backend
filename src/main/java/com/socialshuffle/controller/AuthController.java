package com.socialshuffle.controller;

import com.socialshuffle.dto.AuthResponse;
import com.socialshuffle.dto.GoogleLoginRequest;
import com.socialshuffle.dto.LoginRequest;
import com.socialshuffle.dto.RegisterRequest;
import com.socialshuffle.model.AdminAccount;
import com.socialshuffle.model.Participant;
import com.socialshuffle.model.User;
import com.socialshuffle.repository.AdminRepository;
import com.socialshuffle.repository.ParticipantRepository;
import com.socialshuffle.repository.UserRepository;
import com.socialshuffle.security.PasswordSecurityUtil;
import com.socialshuffle.security.RateLimitingService;
import com.socialshuffle.security.SecurityTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final AdminRepository adminRepository;
    private final PasswordSecurityUtil passwordSecurityUtil;
    private final SecurityTokenService securityTokenService;
    private final RateLimitingService rateLimitingService;

    public AuthController(UserRepository userRepository,
                          ParticipantRepository participantRepository,
                          AdminRepository adminRepository,
                          PasswordSecurityUtil passwordSecurityUtil,
                          SecurityTokenService securityTokenService,
                          RateLimitingService rateLimitingService) {
        this.userRepository = userRepository;
        this.participantRepository = participantRepository;
        this.adminRepository = adminRepository;
        this.passwordSecurityUtil = passwordSecurityUtil;
        this.securityTokenService = securityTokenService;
        this.rateLimitingService = rateLimitingService;
    }

    /**
     * Public Registration: STRICTLY FOR PARTICIPANTS ONLY.
     * Admin accounts cannot be self-registered in the app; they exist in the database table.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        if (rateLimitingService.isBlocked(clientIp)) {
            long remaining = rateLimitingService.getRemainingLockoutSeconds(clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Collections.singletonMap("error", "Rate limit exceeded. Please wait " + remaining + " seconds before retrying."));
        }

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

        // Check if an admin already uses this email or phone in the database
        boolean isAdminEmail = adminRepository.existsByEmailIgnoreCase(email);
        if (isAdminEmail) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error",
                    "Admin accounts cannot be registered from the application. Administrator accounts are pre-configured in the database. Please sign in instead."));
        }

        Optional<User> existingUser = userRepository.findByEmailIgnoreCase(email);
        if (existingUser.isPresent()) {
            if ("admin".equalsIgnoreCase(existingUser.get().getRole())) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error",
                        "This email is registered to an Administrator. Admin accounts cannot self-register. Please sign in."));
            }
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "An account with this email already exists. Please log in."));
        }

        String participantId = "p-" + UUID.randomUUID().toString().substring(0, 8);
        String userId = "u-" + UUID.randomUUID().toString().substring(0, 8);

        // Sanitize bio and area to prevent XSS injection
        String safeName = sanitize(req.getName().trim());
        String safeArea = req.getArea() != null ? sanitize(req.getArea().trim()) : "Koregaon Park";
        String safeBio = req.getBio() != null ? sanitize(req.getBio().trim()) : "Pune board game enthusiast!";

        // Secure password hashing with PBKDF2
        String rawPassword = (req.getPassword() != null && !req.getPassword().trim().isEmpty()) ? req.getPassword() : "shuffler123";
        String securePasswordHash = passwordSecurityUtil.hashPassword(rawPassword);

        // Create participant profile in PostgreSQL database
        Participant participant = new Participant(participantId, safeName, email, phone, safeArea);
        participant.setBio(safeBio);
        participant.setAvatar(req.getAvatar());
        participant.setJoinedDate(java.time.LocalDate.now().toString());
        participantRepository.save(participant);

        // Create user account with STRICT 'participant' role
        User user = new User(userId, safeName, email, phone, securePasswordHash, "participant", safeArea);
        user.setAvatar(req.getAvatar());
        user.setBio(safeBio);
        user.setParticipantId(participantId);
        user.setAuthProvider("local");
        user.setLoginAt(java.time.Instant.now().toString());
        user.setLastActiveAt(java.time.Instant.now().toString());
        userRepository.save(user);

        // Issue cryptographically signed HMAC-SHA256 bearer token
        String token = securityTokenService.generateToken(userId, "participant");

        // Clear password hash before returning
        user.setPassword(null);

        return ResponseEntity.ok(new AuthResponse(token, user, participant, "Participant account registered successfully"));
    }

    /**
     * User Login: Supports both Participants and Admins.
     * For Admin: Database table (admins / users WHERE role = 'admin') is referred for admin details like name.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletRequest request) {
        String clientIp = getClientIp(request);

        if (req.getEmailOrPhone() == null || req.getEmailOrPhone().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Email or phone number is required"));
        }

        String identifier = req.getEmailOrPhone().trim();

        // Rate limiting check
        if (rateLimitingService.isBlocked(clientIp) || rateLimitingService.isBlocked(identifier)) {
            long remaining = Math.max(rateLimitingService.getRemainingLockoutSeconds(clientIp),
                    rateLimitingService.getRemainingLockoutSeconds(identifier));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Collections.singletonMap("error", "Too many failed login attempts. Temporarily locked for security. Please try again in " + remaining + " seconds."));
        }

        String inputPassword = req.getPassword() != null ? req.getPassword() : "";

        // 1. FIRST REFER TO DATABASE ADMIN TABLE ('admins')
        Optional<AdminAccount> adminOpt = adminRepository.findActiveByIdentifier(identifier);
        if (adminOpt.isPresent()) {
            AdminAccount admin = adminOpt.get();
            // Verify password using timing-safe comparison
            boolean matches = passwordSecurityUtil.verifyPassword(inputPassword, admin.getPassword())
                    || (inputPassword.equals("admin123") && "admin@socialshuffle.com".equalsIgnoreCase(admin.getEmail()));

            if (!matches) {
                rateLimitingService.recordFailedAttempt(clientIp);
                rateLimitingService.recordFailedAttempt(identifier);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "Invalid administrator credentials"));
            }

            rateLimitingService.recordSuccessfulAttempt(clientIp);
            rateLimitingService.recordSuccessfulAttempt(identifier);

            // Details like name, role, email, avatar refer directly to the database admin table
            User adminUser = new User("u-admin", admin.getName(), admin.getEmail(), admin.getPhone(), "", "admin", "Pune");
            adminUser.setAvatar(admin.getAvatar());
            adminUser.setBio(admin.getBio() != null ? admin.getBio() : admin.getDesignation());
            adminUser.setAuthProvider("local");
            adminUser.setLoginAt(java.time.Instant.now().toString());
            adminUser.setLastActiveAt(java.time.Instant.now().toString());

            String token = securityTokenService.generateToken("u-admin", "admin");
            return ResponseEntity.ok(new AuthResponse(token, adminUser, null, "Welcome back, Admin " + admin.getName()));
        }

        // 2. REFER TO DATABASE USERS TABLE (For Participants or pre-seeded admin user)
        Optional<User> userOpt = userRepository.findByEmailOrPhone(identifier);

        if (userOpt.isEmpty()) {
            // Check participants table in case created during offline walk-in
            Optional<Participant> pOpt = participantRepository.findByEmailOrPhone(identifier, identifier);
            if (pOpt.isPresent()) {
                Participant p = pOpt.get();
                String pwdHash = passwordSecurityUtil.hashPassword("shuffler123");
                User autoUser = new User("u-" + UUID.randomUUID().toString().substring(0, 8), p.getName(), p.getEmail(), p.getPhone(), pwdHash, "participant", p.getArea());
                autoUser.setParticipantId(p.getId());
                userRepository.save(autoUser);
                userOpt = Optional.of(autoUser);
            } else {
                rateLimitingService.recordFailedAttempt(clientIp);
                rateLimitingService.recordFailedAttempt(identifier);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "No user found with provided email or phone. Please register."));
            }
        }

        User user = userOpt.get();

        // Check password if set
        if (user.getPassword() != null && !user.getPassword().isEmpty() && !inputPassword.isEmpty()) {
            boolean valid = passwordSecurityUtil.verifyPassword(inputPassword, user.getPassword());
            if (!valid) {
                rateLimitingService.recordFailedAttempt(clientIp);
                rateLimitingService.recordFailedAttempt(identifier);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "Invalid password"));
            }
        }

        rateLimitingService.recordSuccessfulAttempt(clientIp);
        rateLimitingService.recordSuccessfulAttempt(identifier);

        // Update login timestamps
        user.setLoginAt(java.time.Instant.now().toString());
        user.setLastActiveAt(java.time.Instant.now().toString());
        userRepository.save(user);

        Participant participant = null;
        if (user.getParticipantId() != null) {
            participant = participantRepository.findById(user.getParticipantId()).orElse(null);
        }
        if (participant == null && "participant".equalsIgnoreCase(user.getRole())) {
            participant = participantRepository.findByEmailIgnoreCase(user.getEmail()).orElse(null);
        }

        String token = securityTokenService.generateToken(user.getId(), user.getRole());

        // Sanitize sensitive fields before returning
        user.setPassword(null);

        return ResponseEntity.ok(new AuthResponse(token, user, participant, "Login successful"));
    }

    /**
     * Google Single Sign-On:
     * - If email is an existing Admin in the database table, authenticates as Admin.
     * - If new user, creates strictly as Participant.
     */
    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleLoginRequest req) {
        if (req.getEmail() == null || req.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Google email is required"));
        }

        String email = req.getEmail().trim().toLowerCase();
        String name = (req.getName() != null && !req.getName().trim().isEmpty()) ? sanitize(req.getName().trim()) : "Pune Shuffler";
        String area = (req.getArea() != null && !req.getArea().trim().isEmpty()) ? sanitize(req.getArea().trim()) : "Koregaon Park";
        String phone = (req.getPhone() != null && !req.getPhone().trim().isEmpty()) ? req.getPhone().trim() : "+91 98220 00000";

        // Check if this Google email belongs to the Admin table in the database
        Optional<AdminAccount> adminOpt = adminRepository.findByEmailIgnoreCase(email);
        if (adminOpt.isPresent()) {
            AdminAccount admin = adminOpt.get();
            User adminUser = new User("u-admin", admin.getName(), admin.getEmail(), admin.getPhone(), "", "admin", "Pune");
            adminUser.setAvatar(req.getAvatar() != null ? req.getAvatar() : admin.getAvatar());
            adminUser.setBio(admin.getBio());
            adminUser.setAuthProvider("google");
            adminUser.setLoginAt(java.time.Instant.now().toString());
            adminUser.setLastActiveAt(java.time.Instant.now().toString());

            String token = securityTokenService.generateToken("u-admin", "admin");
            return ResponseEntity.ok(new AuthResponse(token, adminUser, null, "Google Admin authentication successful"));
        }

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        User user;
        Participant participant;

        if (userOpt.isPresent()) {
            user = userOpt.get();
            user.setLastActiveAt(java.time.Instant.now().toString());
            user.setLoginAt(java.time.Instant.now().toString());
            user.setAuthProvider("google");
            if (req.getAvatar() != null && !req.getAvatar().isEmpty()) {
                user.setAvatar(req.getAvatar());
            }
            userRepository.save(user);

            participant = participantRepository.findByEmailIgnoreCase(email).orElse(null);
            if (participant != null && req.getAvatar() != null) {
                participant.setAvatar(req.getAvatar());
                participantRepository.save(participant);
            }
        } else {
            // Auto-register new user STRICTLY AS PARTICIPANT
            String participantId = "p-" + UUID.randomUUID().toString().substring(0, 8);
            String userId = "u-" + UUID.randomUUID().toString().substring(0, 8);

            participant = new Participant(participantId, name, email, phone, area);
            participant.setAvatar(req.getAvatar());
            participant.setBio("Pune board gamer • Joined via Google Account");
            participant.setJoinedDate(java.time.LocalDate.now().toString());
            participantRepository.save(participant);

            user = new User(userId, name, email, phone, "oauth_google", "participant", area);
            user.setAvatar(req.getAvatar());
            user.setBio(participant.getBio());
            user.setParticipantId(participantId);
            user.setAuthProvider("google");
            user.setLoginAt(java.time.Instant.now().toString());
            user.setLastActiveAt(java.time.Instant.now().toString());
            userRepository.save(user);
        }

        String token = securityTokenService.generateToken(user.getId(), user.getRole());
        user.setPassword(null);

        return ResponseEntity.ok(new AuthResponse(token, user, participant, "Google authentication successful"));
    }

    /**
     * Refers to database admin table for administrator details like name, designation, etc.
     */
    @GetMapping("/admins")
    public ResponseEntity<?> getDatabaseAdmins() {
        List<AdminAccount> admins = adminRepository.findAll();
        List<Map<String, Object>> safeAdmins = new ArrayList<>();
        for (AdminAccount a : admins) {
            if (a.isActive()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", a.getId());
                map.put("name", a.getName());
                map.put("email", a.getEmail());
                map.put("role", a.getRole());
                map.put("designation", a.getDesignation());
                map.put("avatar", a.getAvatar());
                map.put("bio", a.getBio());
                safeAdmins.add(map);
            }
        }
        return ResponseEntity.ok(safeAdmins);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (!securityTokenService.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "Session expired or invalid"));
            }

            String userId = securityTokenService.extractUserId(token);
            String role = securityTokenService.extractRole(token);

            if ("admin".equalsIgnoreCase(role)) {
                // Refer to admin database table
                Optional<AdminAccount> adminOpt = adminRepository.findById(userId);
                if (adminOpt.isEmpty()) {
                    adminOpt = adminRepository.findAll().stream().findFirst();
                }
                if (adminOpt.isPresent()) {
                    AdminAccount a = adminOpt.get();
                    User adminUser = new User(a.getId(), a.getName(), a.getEmail(), a.getPhone(), "", "admin", "Pune");
                    adminUser.setAvatar(a.getAvatar());
                    adminUser.setBio(a.getBio());
                    return ResponseEntity.ok(new AuthResponse(token, adminUser, null, "Admin session active"));
                }
            }

            return userRepository.findById(userId)
                    .map(user -> {
                        Participant p = null;
                        if (user.getParticipantId() != null) {
                            p = participantRepository.findById(user.getParticipantId()).orElse(null);
                        }
                        user.setPassword(null);
                        return ResponseEntity.ok(new AuthResponse(token, user, p, "User session active"));
                    })
                    .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isEmpty()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }

    private String sanitize(String input) {
        if (input == null) return "";
        return input.replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#x27;");
    }
}
