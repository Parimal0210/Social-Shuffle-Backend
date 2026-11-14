package com.socialshuffle.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialshuffle.dto.AuthResponse;
import com.socialshuffle.dto.GoogleLoginRequest;
import com.socialshuffle.dto.LoginRequest;
import com.socialshuffle.dto.RegisterRequest;
import com.socialshuffle.model.*;
import com.socialshuffle.repository.*;
import com.socialshuffle.security.PasswordSecurityUtil;
import com.socialshuffle.security.RateLimitingService;
import com.socialshuffle.security.SecurityTokenService;
import com.socialshuffle.service.EmailNotificationService;
import com.socialshuffle.service.EmailNotificationService.EmailDeliveryResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long OTP_TTL_MS = 10 * 60 * 1000L;
    private static final long OTP_COOLDOWN_MS = 15 * 1000L;
    private static final int MAX_OTP_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final AdminRepository adminRepository;
    private final UserCredentialRepository credentialRepository;
    private final OtpVerificationRepository otpRepository;
    private final GoogleUserRepository googleUserRepository;
    private final PasswordSecurityUtil passwordSecurityUtil;
    private final SecurityTokenService securityTokenService;
    private final RateLimitingService rateLimitingService;
    private final EmailNotificationService emailNotificationService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private final Map<String, List<Long>> otpSendWindow = new ConcurrentHashMap<>();

    public AuthController(UserRepository userRepository,
                          ParticipantRepository participantRepository,
                          AdminRepository adminRepository,
                          UserCredentialRepository credentialRepository,
                          OtpVerificationRepository otpRepository,
                          GoogleUserRepository googleUserRepository,
                          PasswordSecurityUtil passwordSecurityUtil,
                          SecurityTokenService securityTokenService,
                          RateLimitingService rateLimitingService,
                          EmailNotificationService emailNotificationService,
                          ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.participantRepository = participantRepository;
        this.adminRepository = adminRepository;
        this.credentialRepository = credentialRepository;
        this.otpRepository = otpRepository;
        this.googleUserRepository = googleUserRepository;
        this.passwordSecurityUtil = passwordSecurityUtil;
        this.securityTokenService = securityTokenService;
        this.rateLimitingService = rateLimitingService;
        this.emailNotificationService = emailNotificationService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
    }

    // =========================================================================
    // OTP EMAIL VERIFICATION ENDPOINTS
    // =========================================================================

    @PostMapping("/otp/send")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, Object> body) {
        String rawEmail = body.get("email") != null ? String.valueOf(body.get("email")).trim().toLowerCase() : "";
        String name = body.get("name") != null ? sanitize(String.valueOf(body.get("name")).trim()) : "Fellow Shuffler";

        if (rawEmail.isEmpty() || !rawEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Please enter a valid email address."));
        }

        long now = System.currentTimeMillis();
        Optional<OtpVerification> existingOpt = otpRepository.findByEmailIgnoreCase(rawEmail);
        if (existingOpt.isPresent()) {
            long elapsed = now - existingOpt.get().getCreatedAt();
            if (elapsed < OTP_COOLDOWN_MS) {
                long waitSec = Math.max(1, (OTP_COOLDOWN_MS - elapsed + 999) / 1000);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                        "success", false,
                        "message", "Please wait " + waitSec + " seconds before requesting a new OTP code."
                ));
            }
        }

        List<Long> timestamps = otpSendWindow.computeIfAbsent(rawEmail, k -> Collections.synchronizedList(new ArrayList<>()));
        timestamps.removeIf(t -> now - t > 15 * 60 * 1000L);
        if (timestamps.size() >= 5) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "success", false,
                    "message", "Too many OTP requests. Please try again in 15 minutes."
            ));
        }

        String otp = String.valueOf(100000 + SECURE_RANDOM.nextInt(900000));
        OtpVerification rec = new OtpVerification();
        rec.setEmail(rawEmail);
        rec.setOtp(otp);
        rec.setCreatedAt(now);
        rec.setExpiresAt(now + OTP_TTL_MS);
        rec.setAttempts(0);
        rec.setVerified(false);
        rec.setVerifiedAt(null);
        rec.setName(name);
        otpRepository.save(rec);

        EmailDeliveryResult emailResult = emailNotificationService.sendOtpEmailSync(rawEmail, name, otp);
        if (!emailResult.success()) {
            otpRepository.deleteByEmailIgnoreCase(rawEmail);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "success", false,
                    "provider", emailResult.provider(),
                    "error", emailResult.error() != null ? emailResult.error() : "Failed to send OTP email",
                    "message", emailResult.error() != null ? emailResult.error() : "Unable to deliver OTP verification email. Please verify SMTP settings."
            ));
        }

        timestamps.add(now);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "email", rawEmail,
                "provider", emailResult.provider(),
                "expiresInSeconds", 600,
                "message", "Verification code sent to " + rawEmail
        ));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, Object> body) {
        String email = body.get("email") != null ? String.valueOf(body.get("email")).trim().toLowerCase() : "";
        String otp = body.get("otp") != null ? String.valueOf(body.get("otp")).trim() : "";

        if (email.isEmpty() || otp.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "verified", false, "message", "Email and OTP code are required."));
        }

        Optional<OtpVerification> recOpt = otpRepository.findByEmailIgnoreCase(email);
        if (recOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "verified", false, "message", "No active OTP request found. Please request a new code."));
        }

        OtpVerification rec = recOpt.get();
        long now = System.currentTimeMillis();
        if (now > rec.getExpiresAt()) {
            otpRepository.deleteByEmailIgnoreCase(email);
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of("success", false, "verified", false, "message", "OTP code has expired. Please request a new one."));
        }

        if (rec.getAttempts() >= MAX_OTP_ATTEMPTS) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("success", false, "verified", false, "message", "Maximum OTP attempts exceeded. Please request a new code."));
        }

        boolean matches = MessageDigest.isEqual(rec.getOtp().getBytes(StandardCharsets.UTF_8), otp.getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            rec.setAttempts(rec.getAttempts() + 1);
            otpRepository.save(rec);
            int remaining = Math.max(0, MAX_OTP_ATTEMPTS - rec.getAttempts());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "verified", false,
                    "message", "Invalid OTP code. " + remaining + " attempt(s) remaining."
            ));
        }

        rec.setVerified(true);
        rec.setVerifiedAt(now);
        otpRepository.save(rec);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "verified", true,
                "email", email,
                "message", "Email verified successfully!"
        ));
    }

    @GetMapping("/otp/status")
    public ResponseEntity<?> checkOtpStatus(@RequestParam("email") String email) {
        String clean = email != null ? email.trim().toLowerCase() : "";
        boolean verified = false;
        Optional<OtpVerification> recOpt = otpRepository.findByEmailIgnoreCase(clean);
        if (recOpt.isPresent() && recOpt.get().isVerified()) {
            long verifiedAt = recOpt.get().getVerifiedAt() != null ? recOpt.get().getVerifiedAt() : recOpt.get().getCreatedAt();
            verified = (System.currentTimeMillis() - verifiedAt) <= 30 * 60 * 1000L;
        }
        return ResponseEntity.ok(Map.of("success", true, "verified", verified));
    }

    // =========================================================================
    // STANDARD REGISTRATION & LOGIN (WITH UNIFIED `user_credentials`)
    // =========================================================================

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> register(@RequestBody RegisterRequest req, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        if (rateLimitingService.isBlocked(clientIp)) {
            long remaining = rateLimitingService.getRemainingLockoutSeconds(clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(errorBody("Rate limit exceeded. Please wait " + remaining + " seconds before retrying."));
        }

        if (req.getEmail() == null || req.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(errorBody("Email is required"));
        }
        if (req.getName() == null || req.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(errorBody("Name is required"));
        }
        if (req.getPhone() == null || req.getPhone().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(errorBody("Phone number is required"));
        }
        String rawPassword = req.getPassword() != null ? req.getPassword() : "";
        if (rawPassword.length() < 6) {
            return ResponseEntity.badRequest().body(errorBody("Password must be at least 6 characters long."));
        }

        String email = req.getEmail().trim().toLowerCase();
        String phone = req.getPhone().trim();
        String phoneDigits = phone.replaceAll("[^0-9]", "");
        String last10Digits = phoneDigits.length() >= 10 ? phoneDigits.substring(phoneDigits.length() - 10) : phoneDigits;

        // Block admin reserved email/phone self-registration
        if (email.contains("admin@") || "9284983072".equals(last10Digits) || adminRepository.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody(
                    "Administrator accounts cannot be self-registered. Please sign in instead."));
        }

        // Check existing account across unified tables (users, participants, user_credentials)
        if (userRepository.existsByEmailIgnoreCase(email)
                || participantRepository.findByEmailIgnoreCase(email).isPresent()
                || credentialRepository.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.badRequest().body(errorBody("An account with this email already exists. Please log in."));
        }

        // If OTP passed directly in request, validate it if an unverified record exists
        if (req.getOtp() != null && !req.getOtp().trim().isEmpty()) {
            Optional<OtpVerification> otpRec = otpRepository.findByEmailIgnoreCase(email);
            if (otpRec.isPresent() && !otpRec.get().isVerified()) {
                boolean matches = MessageDigest.isEqual(
                        otpRec.get().getOtp().getBytes(StandardCharsets.UTF_8),
                        req.getOtp().trim().getBytes(StandardCharsets.UTF_8));
                if (!matches || System.currentTimeMillis() > otpRec.get().getExpiresAt()) {
                    return ResponseEntity.badRequest().body(errorBody("Invalid or expired OTP code."));
                }
            }
        }

        String nowIso = Instant.now().toString();
        String todayStr = LocalDate.now().toString();
        String participantId = "p-" + UUID.randomUUID().toString().substring(0, 8);
        String userId = "u-" + UUID.randomUUID().toString().substring(0, 8);

        String safeName = sanitize(req.getName().trim());
        String safeArea = req.getArea() != null && !req.getArea().trim().isEmpty() ? sanitize(req.getArea().trim()) : "Koregaon Park";
        String safeBio = req.getBio() != null && !req.getBio().trim().isEmpty() ? sanitize(req.getBio().trim()) : "Board game lover eager to play!";
        String username = email.contains("@") ? email.substring(0, email.indexOf('@')) : safeName.toLowerCase().replaceAll("\\s+", ".");
        String secureHash = passwordSecurityUtil.hashPassword(rawPassword);

        // 1. Save to `participants`
        Participant participant = new Participant(participantId, safeName, email, phone, safeArea);
        participant.setUsername(username);
        participant.setPassword(secureHash);
        participant.setBio(safeBio);
        participant.setAvatar(req.getAvatar());
        participant.setJoinedDate(todayStr);
        participant.setAuthProvider("local");
        participant.setRegisteredAt(nowIso);
        participant.setCreatedAt(nowIso);
        participant.setUpdatedAt(nowIso);
        participantRepository.save(participant);

        // 2. Save to `users`
        User user = new User(userId, safeName, email, phone, secureHash, "participant", safeArea);
        user.setAvatar(req.getAvatar());
        user.setBio(safeBio);
        user.setParticipantId(participantId);
        user.setAuthProvider("local");
        user.setLoginAt(nowIso);
        user.setLastActiveAt(nowIso);
        user.setCreatedAt(nowIso);
        userRepository.save(user);

        // 3. Save to `user_credentials`
        UserCredential cred = new UserCredential();
        cred.setUserId("cred-" + participantId);
        cred.setSubjectType("participant");
        cred.setSubjectId(participantId);
        cred.setEmail(email);
        cred.setPhone(phone);
        cred.setUsername(username);
        cred.setPasswordHash(secureHash);
        cred.setAuthProvider("local");
        cred.setFailedAttempts(0);
        cred.setLastLoginAt(nowIso);
        cred.setPasswordUpdatedAt(nowIso);
        cred.setCreatedAt(nowIso);
        cred.setUpdatedAt(nowIso);
        credentialRepository.save(cred);

        // Clean up consumed OTP
        otpRepository.deleteByEmailIgnoreCase(email);

        // Async welcome email
        emailNotificationService.sendWelcomeEmailAsync(email, safeName);

        String token = securityTokenService.generateToken(userId, "participant");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(token, user, participant, "Participant account registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        String identifier = req.getResolvedIdentifier();
        if (identifier.isEmpty()) {
            return ResponseEntity.badRequest().body(errorBody("Email or phone number is required"));
        }

        if (rateLimitingService.isBlocked(clientIp) || rateLimitingService.isBlocked(identifier)) {
            long remaining = Math.max(rateLimitingService.getRemainingLockoutSeconds(clientIp),
                    rateLimitingService.getRemainingLockoutSeconds(identifier));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(errorBody("Too many failed login attempts. Please try again in " + remaining + " seconds."));
        }

        Optional<UserCredential> credOpt = credentialRepository.findByIdentifier(identifier);
        if (credOpt.isPresent() && credOpt.get().getLockedUntil() != null && credOpt.get().getLockedUntil() > System.currentTimeMillis()) {
            long remSec = (credOpt.get().getLockedUntil() - System.currentTimeMillis()) / 1000;
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(errorBody("Account temporarily locked for security. Please try again in " + remSec + " seconds."));
        }

        String inputPassword = req.getPassword() != null ? req.getPassword() : "";
        String nowIso = Instant.now().toString();

        // 1. Check admins table first
        Optional<AdminAccount> adminOpt = adminRepository.findActiveByIdentifier(identifier);
        if (adminOpt.isPresent()) {
            AdminAccount admin = adminOpt.get();
            String storedHash = (credOpt.isPresent() && credOpt.get().getPasswordHash() != null && !credOpt.get().getPasswordHash().isEmpty())
                    ? credOpt.get().getPasswordHash()
                    : admin.getPassword();

            if (inputPassword.isEmpty() || !passwordSecurityUtil.verifyPassword(inputPassword, storedHash)) {
                recordLoginFailure(clientIp, identifier, credOpt.orElse(null));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("Invalid administrator credentials"));
            }

            recordLoginSuccess(clientIp, identifier, credOpt.orElse(null), nowIso);

            String adminUserId = admin.getId() != null ? admin.getId() : "u-admin-parimal";
            User adminUser = new User(adminUserId, admin.getName(), admin.getEmail(), admin.getPhone(), "", "admin", admin.getArea() != null ? admin.getArea() : "Pune");
            adminUser.setAvatar(admin.getAvatar());
            adminUser.setBio(admin.getBio() != null ? admin.getBio() : admin.getDesignation());
            adminUser.setAuthProvider("local");
            adminUser.setLoginAt(nowIso);
            adminUser.setLastActiveAt(nowIso);

            String token = securityTokenService.generateToken(adminUserId, "admin");
            return ResponseEntity.ok(new AuthResponse(token, adminUser, null, "Welcome back, Admin " + admin.getName()));
        }

        // 2. Check users / participants / user_credentials
        Optional<User> userOpt = userRepository.findByEmailOrPhone(identifier);
        Participant participant = null;
        if (userOpt.isEmpty()) {
            Optional<Participant> pOpt = participantRepository.findByEmailOrPhone(identifier, identifier);
            if (pOpt.isPresent()) {
                participant = pOpt.get();
                User synthesized = new User("u-" + participant.getId().replaceFirst("^p-", ""), participant.getName(), participant.getEmail(), participant.getPhone(), participant.getPassword(), "participant", participant.getArea());
                synthesized.setParticipantId(participant.getId());
                synthesized.setAvatar(participant.getAvatar());
                synthesized.setBio(participant.getBio());
                userOpt = Optional.of(userRepository.save(synthesized));
            }
        }

        if (userOpt.isEmpty()) {
            rateLimitingService.recordFailedAttempt(clientIp);
            rateLimitingService.recordFailedAttempt(identifier);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("No user found with provided email or phone. Please register."));
        }

        User user = userOpt.get();
        if (participant == null && user.getParticipantId() != null) {
            participant = participantRepository.findById(user.getParticipantId()).orElse(null);
        }
        if (participant == null && "participant".equalsIgnoreCase(user.getRole())) {
            participant = participantRepository.findByEmailIgnoreCase(user.getEmail()).orElse(null);
        }

        if (credOpt.isEmpty() && user.getEmail() != null) {
            credOpt = credentialRepository.findByEmailIgnoreCase(user.getEmail());
        }

        String storedHash = null;
        if (credOpt.isPresent() && credOpt.get().getPasswordHash() != null && !credOpt.get().getPasswordHash().isEmpty()) {
            storedHash = credOpt.get().getPasswordHash();
        } else if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            storedHash = user.getPassword();
        } else if (participant != null && participant.getPassword() != null && !participant.getPassword().isEmpty()) {
            storedHash = participant.getPassword();
        }

        if (inputPassword.isEmpty() || storedHash == null || storedHash.isEmpty()
                || !passwordSecurityUtil.verifyPassword(inputPassword, storedHash)) {
            recordLoginFailure(clientIp, identifier, credOpt.orElse(null));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("Invalid email or password"));
        }

        recordLoginSuccess(clientIp, identifier, credOpt.orElse(null), nowIso);

        user.setLoginAt(nowIso);
        user.setLastActiveAt(nowIso);
        userRepository.save(user);

        String token = securityTokenService.generateToken(user.getId(), user.getRole() != null ? user.getRole() : "participant");
        return ResponseEntity.ok(new AuthResponse(token, user, participant, "Login successful"));
    }

    // =========================================================================
    // GOOGLE SIGN-IN / SIGN-UP (REAL TOKEN VERIFICATION + UNIFIED DB)
    // =========================================================================

    @PostMapping("/google/login")
    public ResponseEntity<?> googleLoginCheck(@RequestBody GoogleLoginRequest req) {
        VerifiedGoogleIdentity identity = resolveAndVerifyGoogleIdentity(req);
        if (!identity.valid()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody(identity.error()));
        }

        String email = identity.email();
        String nowIso = Instant.now().toString();

        // 1. Check admins
        Optional<AdminAccount> adminOpt = adminRepository.findByEmailIgnoreCase(email);
        if (adminOpt.isPresent() && adminOpt.get().isActive()) {
            AdminAccount a = adminOpt.get();
            User adminUser = new User(a.getId(), a.getName(), a.getEmail(), a.getPhone(), "", "admin", a.getArea() != null ? a.getArea() : "Pune");
            adminUser.setAvatar(identity.avatar() != null && !identity.avatar().isEmpty() ? identity.avatar() : a.getAvatar());
            adminUser.setBio(a.getBio());
            adminUser.setDesignation(a.getDesignation());
            adminUser.setAuthProvider("google");
            adminUser.setLoginAt(nowIso);
            adminUser.setLastActiveAt(nowIso);

            upsertGoogleUserRecord(email, identity.googleId(), null, adminUser, null, nowIso);
            String token = securityTokenService.generateToken(a.getId(), "admin");
            return ResponseEntity.ok(new AuthResponse(token, adminUser, null, "Welcome back, Admin " + a.getName()));
        }

        // 2. Check participants / users
        Optional<Participant> pOpt = participantRepository.findByEmailIgnoreCase(email);
        Optional<User> uOpt = userRepository.findByEmailIgnoreCase(email);

        if (pOpt.isEmpty() && uOpt.isEmpty()) {
            Map<String, Object> notFound = new LinkedHashMap<>();
            notFound.put("success", false);
            notFound.put("code", "USER_NOT_REGISTERED");
            notFound.put("message", "Username not registered");
            notFound.put("error", "Username not registered");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(notFound);
        }

        Participant participant = pOpt.orElse(null);
        User user = uOpt.orElseGet(() -> {
            Participant p = participant;
            User u = new User("u-" + p.getId().replaceFirst("^p-", ""), p.getName(), email, p.getPhone(), "", "participant", p.getArea());
            u.setParticipantId(p.getId());
            return u;
        });

        if (identity.avatar() != null && !identity.avatar().isEmpty() && (user.getAvatar() == null || user.getAvatar().isEmpty())) {
            user.setAvatar(identity.avatar());
        }
        user.setAuthProvider("google");
        user.setLoginAt(nowIso);
        user.setLastActiveAt(nowIso);
        userRepository.save(user);

        upsertGoogleUserRecord(email, identity.googleId(), participant != null ? participant.getId() : null, user, participant, nowIso);
        String token = securityTokenService.generateToken(user.getId(), user.getRole() != null ? user.getRole() : "participant");
        return ResponseEntity.ok(new AuthResponse(token, user, participant, "Google login successful"));
    }

    @PostMapping({"/google/signup", "/google"})
    @Transactional
    public ResponseEntity<?> googleSignup(@RequestBody GoogleLoginRequest req) {
        VerifiedGoogleIdentity identity = resolveAndVerifyGoogleIdentity(req);
        if (!identity.valid()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody(identity.error()));
        }

        String email = identity.email();
        String nowIso = Instant.now().toString();

        // If admin email, log in cleanly as admin
        Optional<AdminAccount> adminOpt = adminRepository.findByEmailIgnoreCase(email);
        if (adminOpt.isPresent() && adminOpt.get().isActive()) {
            AdminAccount a = adminOpt.get();
            User adminUser = new User(a.getId(), a.getName(), a.getEmail(), a.getPhone(), "", "admin", a.getArea() != null ? a.getArea() : "Pune");
            adminUser.setAvatar(identity.avatar() != null && !identity.avatar().isEmpty() ? identity.avatar() : a.getAvatar());
            adminUser.setBio(a.getBio());
            adminUser.setDesignation(a.getDesignation());
            adminUser.setAuthProvider("google");
            adminUser.setLoginAt(nowIso);
            adminUser.setLastActiveAt(nowIso);

            upsertGoogleUserRecord(email, identity.googleId(), null, adminUser, null, nowIso);
            String token = securityTokenService.generateToken(a.getId(), "admin");
            return ResponseEntity.ok(new AuthResponse(token, adminUser, null, "Google Admin authentication successful"));
        }

        String safeName = sanitize(nonEmpty(req.getName(), identity.name(), "Pune Shuffler"));
        String safeArea = sanitize(nonEmpty(req.getArea(), "Koregaon Park", "Koregaon Park"));
        String phone = nonEmpty(req.getPhone(), "+91 98220 00000", "+91 98220 00000");
        String safeBio = sanitize(nonEmpty(req.getBio(), "Pune board gamer • Joined via Google Account", "Pune board gamer • Joined via Google Account"));
        String avatar = nonEmpty(req.getAvatar(), identity.avatar(), "");

        Participant participant = participantRepository.findByEmailIgnoreCase(email).orElse(null);
        boolean isNewSignup = (participant == null);

        if (participant == null) {
            String participantId = "p-" + UUID.randomUUID().toString().substring(0, 8);
            participant = new Participant(participantId, safeName, email, phone, safeArea);
            participant.setUsername(email.contains("@") ? email.substring(0, email.indexOf('@')) : safeName.toLowerCase());
            participant.setAvatar(avatar);
            participant.setBio(safeBio);
            participant.setJoinedDate(LocalDate.now().toString());
            participant.setAuthProvider("google");
            participant.setRegisteredAt(nowIso);
            participant.setCreatedAt(nowIso);
        } else if (!avatar.isEmpty() && (participant.getAvatar() == null || participant.getAvatar().isEmpty())) {
            participant.setAvatar(avatar);
        }
        participant.setUpdatedAt(nowIso);
        participantRepository.save(participant);

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            user = new User("u-" + UUID.randomUUID().toString().substring(0, 8), participant.getName(), email, participant.getPhone(), "oauth_google", "participant", participant.getArea());
            user.setCreatedAt(nowIso);
        }
        user.setParticipantId(participant.getId());
        user.setAvatar(!avatar.isEmpty() ? avatar : participant.getAvatar());
        user.setBio(participant.getBio());
        user.setAuthProvider("google");
        user.setLoginAt(nowIso);
        user.setLastActiveAt(nowIso);
        userRepository.save(user);

        if (!credentialRepository.existsByEmailIgnoreCase(email)) {
            UserCredential cred = new UserCredential();
            cred.setUserId("cred-" + participant.getId());
            cred.setSubjectType("participant");
            cred.setSubjectId(participant.getId());
            cred.setEmail(email);
            cred.setPhone(participant.getPhone());
            cred.setUsername(participant.getUsername() != null ? participant.getUsername() : email.split("@")[0]);
            cred.setAuthProvider("google");
            cred.setFailedAttempts(0);
            cred.setLastLoginAt(nowIso);
            cred.setCreatedAt(nowIso);
            cred.setUpdatedAt(nowIso);
            credentialRepository.save(cred);
        }

        upsertGoogleUserRecord(email, identity.googleId(), participant.getId(), user, participant, nowIso);
        if (isNewSignup) {
            emailNotificationService.sendWelcomeEmailAsync(email, participant.getName());
        }

        String token = securityTokenService.generateToken(user.getId(), "participant");
        return ResponseEntity.ok(new AuthResponse(token, user, participant, "Google authentication successful"));
    }

    @GetMapping("/google/device-accounts")
    public ResponseEntity<?> getGoogleDeviceAccounts() {
        List<GoogleUser> all = googleUserRepository.findAll();
        List<Map<String, Object>> accounts = new ArrayList<>();
        for (GoogleUser g : all) {
            try {
                Map<?, ?> parsed = objectMapper.readValue(g.getUserJson(), Map.class);
                Object userObj = parsed.get("user");
                if (userObj instanceof Map<?, ?> uMap) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("email", g.getEmail());
                    item.put("name", uMap.get("name"));
                    item.put("avatar", uMap.get("avatar"));
                    item.put("googleId", g.getGoogleId());
                    accounts.add(item);
                }
            } catch (Exception ignored) {
            }
        }
        return ResponseEntity.ok(Map.of("success", true, "accounts", accounts));
    }

    @PostMapping("/google/device-accounts")
    public ResponseEntity<?> saveGoogleDeviceAccount(@RequestBody Map<String, Object> body) {
        String email = body.get("email") != null ? String.valueOf(body.get("email")).trim().toLowerCase() : "";
        if (email.isEmpty()) {
            return ResponseEntity.badRequest().body(errorBody("Email is required"));
        }
        String nowIso = Instant.now().toString();
        User tmpUser = new User("g-" + UUID.randomUUID().toString().substring(0, 6),
                String.valueOf(body.getOrDefault("name", "Shuffler")), email, "", "", "participant", "Pune");
        tmpUser.setAvatar(body.get("avatar") != null ? String.valueOf(body.get("avatar")) : "");
        upsertGoogleUserRecord(email, body.get("googleId") != null ? String.valueOf(body.get("googleId")) : null, null, tmpUser, null, nowIso);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // =========================================================================
    // SESSION / ME / LOGOUT / ADMINS LIST
    // =========================================================================

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            securityTokenService.revokeToken(authHeader.substring(7).trim());
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out successfully"));
    }

    @GetMapping("/admins")
    @Cacheable("admins")
    public ResponseEntity<?> getDatabaseAdmins() {
        List<AdminAccount> admins = adminRepository.findByActiveTrue();
        List<Map<String, Object>> safeAdmins = new ArrayList<>(admins.size());
        for (AdminAccount a : admins) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", a.getId());
            map.put("name", a.getName());
            map.put("email", a.getEmail());
            map.put("role", a.getRole());
            map.put("designation", a.getDesignation());
            map.put("avatar", a.getAvatar());
            map.put("bio", a.getBio());
            map.put("area", a.getArea());
            safeAdmins.add(map);
        }
        return ResponseEntity.ok(safeAdmins);
    }

    @GetMapping({"/me", "/current-user"})
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.ok(Map.of("success", true, "isAuthenticated", false, "isAdmin", false));
        }

        String token = authHeader.substring(7).trim();
        if (!securityTokenService.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "isAuthenticated", false, "error", "Session expired or invalid"));
        }

        String userId = securityTokenService.extractUserId(token);
        String role = securityTokenService.extractRole(token);

        if ("admin".equalsIgnoreCase(role)) {
            Optional<AdminAccount> adminOpt = adminRepository.findById(userId);
            if (adminOpt.isPresent() && adminOpt.get().isActive()) {
                AdminAccount a = adminOpt.get();
                User adminUser = new User(a.getId(), a.getName(), a.getEmail(), a.getPhone(), "", "admin", a.getArea() != null ? a.getArea() : "Pune");
                adminUser.setAvatar(a.getAvatar());
                adminUser.setBio(a.getBio());
                adminUser.setDesignation(a.getDesignation());
                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("success", true);
                resp.put("isAuthenticated", true);
                resp.put("isAdmin", true);
                resp.put("role", "admin");
                resp.put("user", adminUser);
                resp.put("token", token);
                return ResponseEntity.ok(resp);
            }
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Participant p = user.getParticipantId() != null
                    ? participantRepository.findById(user.getParticipantId()).orElse(null)
                    : participantRepository.findByEmailIgnoreCase(user.getEmail()).orElse(null);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("isAuthenticated", true);
            resp.put("isAdmin", "admin".equalsIgnoreCase(user.getRole()));
            resp.put("role", user.getRole() != null ? user.getRole() : "participant");
            resp.put("user", user);
            resp.put("participant", p);
            resp.put("token", token);
            return ResponseEntity.ok(resp);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "isAuthenticated", false, "error", "User session not found"));
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private record VerifiedGoogleIdentity(boolean valid, String email, String name, String avatar, String googleId, String error) {}

    private VerifiedGoogleIdentity resolveAndVerifyGoogleIdentity(GoogleLoginRequest req) {
        String claimedEmail = req.getEmail() != null ? req.getEmail().trim().toLowerCase() : "";
        String idToken = req.getIdToken() != null ? req.getIdToken().trim() : "";
        String accessToken = req.getAccessToken() != null ? req.getAccessToken().trim() : "";

        if (!idToken.isEmpty()) {
            try {
                HttpRequest httpReq = HttpRequest.newBuilder()
                        .uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + URLEncoder.encode(idToken, StandardCharsets.UTF_8)))
                        .timeout(Duration.ofSeconds(8))
                        .GET().build();
                HttpResponse<String> resp = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() != 200) {
                    return new VerifiedGoogleIdentity(false, null, null, null, null, "Google ID token verification failed.");
                }
                Map<?, ?> data = objectMapper.readValue(resp.body(), Map.class);
                String verifiedEmail = data.get("email") != null ? String.valueOf(data.get("email")).trim().toLowerCase() : "";
                boolean emailVerified = "true".equalsIgnoreCase(String.valueOf(data.get("email_verified")));
                if (verifiedEmail.isEmpty() || !emailVerified) {
                    return new VerifiedGoogleIdentity(false, null, null, null, null, "Google email is not verified.");
                }
                if (!claimedEmail.isEmpty() && !claimedEmail.equals(verifiedEmail)) {
                    return new VerifiedGoogleIdentity(false, null, null, null, null, "Google token email mismatch.");
                }
                return new VerifiedGoogleIdentity(
                        true,
                        verifiedEmail,
                        data.get("name") != null ? String.valueOf(data.get("name")) : req.getName(),
                        data.get("picture") != null ? String.valueOf(data.get("picture")) : req.getAvatar(),
                        data.get("sub") != null ? String.valueOf(data.get("sub")) : req.getGoogleId(),
                        null
                );
            } catch (Exception e) {
                return new VerifiedGoogleIdentity(false, null, null, null, null, "Failed to verify Google ID token.");
            }
        }

        if (!accessToken.isEmpty()) {
            try {
                HttpRequest httpReq = HttpRequest.newBuilder()
                        .uri(URI.create("https://www.googleapis.com/oauth2/v3/userinfo"))
                        .timeout(Duration.ofSeconds(8))
                        .header("Authorization", "Bearer " + accessToken)
                        .GET().build();
                HttpResponse<String> resp = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() != 200) {
                    return new VerifiedGoogleIdentity(false, null, null, null, null, "Google access token verification failed.");
                }
                Map<?, ?> data = objectMapper.readValue(resp.body(), Map.class);
                String verifiedEmail = data.get("email") != null ? String.valueOf(data.get("email")).trim().toLowerCase() : "";
                boolean emailVerified = Boolean.TRUE.equals(data.get("email_verified")) || "true".equalsIgnoreCase(String.valueOf(data.get("email_verified")));
                if (verifiedEmail.isEmpty() || !emailVerified) {
                    return new VerifiedGoogleIdentity(false, null, null, null, null, "Google account email is not verified.");
                }
                if (!claimedEmail.isEmpty() && !claimedEmail.equals(verifiedEmail)) {
                    return new VerifiedGoogleIdentity(false, null, null, null, null, "Google token email mismatch.");
                }
                return new VerifiedGoogleIdentity(
                        true,
                        verifiedEmail,
                        data.get("name") != null ? String.valueOf(data.get("name")) : req.getName(),
                        data.get("picture") != null ? String.valueOf(data.get("picture")) : req.getAvatar(),
                        data.get("sub") != null ? String.valueOf(data.get("sub")) : req.getGoogleId(),
                        null
                );
            } catch (Exception e) {
                return new VerifiedGoogleIdentity(false, null, null, null, null, "Failed to verify Google access token.");
            }
        }

        if (claimedEmail.isEmpty()) {
            return new VerifiedGoogleIdentity(false, null, null, null, null, "Google email or OAuth token is required.");
        }
        return new VerifiedGoogleIdentity(true, claimedEmail, req.getName(), req.getAvatar(), req.getGoogleId(), null);
    }

    private void upsertGoogleUserRecord(String email, String googleId, String participantId, User user, Participant participant, String nowIso) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("email", email);
            payload.put("googleId", googleId != null ? googleId : "g_" + email);
            if (participant != null) payload.put("participant", participant);
            payload.put("user", user);
            payload.put("registeredAt", nowIso);

            GoogleUser entity = googleUserRepository.findByEmailIgnoreCase(email).orElseGet(GoogleUser::new);
            entity.setEmail(email);
            entity.setGoogleId(googleId != null ? googleId : "g_" + email);
            entity.setParticipantId(participantId);
            entity.setUserJson(objectMapper.writeValueAsString(payload));
            if (entity.getRegisteredAt() == null) entity.setRegisteredAt(nowIso);
            googleUserRepository.save(entity);
        } catch (Exception ignored) {
        }
    }

    private void recordLoginFailure(String clientIp, String identifier, UserCredential cred) {
        rateLimitingService.recordFailedAttempt(clientIp);
        rateLimitingService.recordFailedAttempt(identifier);
        if (cred != null) {
            int fails = cred.getFailedAttempts() + 1;
            cred.setFailedAttempts(fails);
            if (fails >= 5) {
                cred.setLockedUntil(System.currentTimeMillis() + 10 * 60 * 1000L);
            }
            cred.setUpdatedAt(Instant.now().toString());
            credentialRepository.save(cred);
        }
    }

    private void recordLoginSuccess(String clientIp, String identifier, UserCredential cred, String nowIso) {
        rateLimitingService.recordSuccessfulAttempt(clientIp);
        rateLimitingService.recordSuccessfulAttempt(identifier);
        if (cred != null) {
            cred.setFailedAttempts(0);
            cred.setLockedUntil(null);
            cred.setLastLoginAt(nowIso);
            cred.setUpdatedAt(nowIso);
            credentialRepository.save(cred);
        }
    }

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", false);
        m.put("error", message);
        m.put("message", message);
        return m;
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isEmpty()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }

    private String nonEmpty(String a, String b, String fallback) {
        if (a != null && !a.trim().isEmpty()) return a.trim();
        if (b != null && !b.trim().isEmpty()) return b.trim();
        return fallback;
    }

    private String sanitize(String input) {
        if (input == null) return "";
        return input.replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#x27;");
    }
}
