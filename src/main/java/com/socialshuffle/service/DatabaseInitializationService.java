package com.socialshuffle.service;

import com.socialshuffle.model.*;
import com.socialshuffle.repository.*;
import com.socialshuffle.security.PasswordSecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Single fast startup verifier ensuring admins, user_credentials,
 * community_stats, and starter catalog rows exist in the unified PostgreSQL schema.
 */
@Component
public class DatabaseInitializationService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializationService.class);

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final VenueRepository venueRepository;
    private final ShuffleEventRepository eventRepository;
    private final GameRepository gameRepository;
    private final CommunityStatsRepository communityStatsRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationItemRepository notificationRepository;
    private final AppConfigRepository appConfigRepository;
    private final AppConfigService appConfigService;
    private final PasswordSecurityUtil passwordSecurityUtil;
    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializationService(AdminRepository adminRepository,
                                         UserRepository userRepository,
                                         UserCredentialRepository credentialRepository,
                                         VenueRepository venueRepository,
                                         ShuffleEventRepository eventRepository,
                                         GameRepository gameRepository,
                                         CommunityStatsRepository communityStatsRepository,
                                         AuditLogRepository auditLogRepository,
                                         NotificationItemRepository notificationRepository,
                                         AppConfigRepository appConfigRepository,
                                         AppConfigService appConfigService,
                                         PasswordSecurityUtil passwordSecurityUtil,
                                         JdbcTemplate jdbcTemplate) {
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.venueRepository = venueRepository;
        this.eventRepository = eventRepository;
        this.gameRepository = gameRepository;
        this.communityStatsRepository = communityStatsRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationRepository = notificationRepository;
        this.appConfigRepository = appConfigRepository;
        this.appConfigService = appConfigService;
        this.passwordSecurityUtil = passwordSecurityUtil;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Verifying unified Social Shuffle PostgreSQL schema & core data...");
        ensureSchemaMaintenance();
        ensureAdminsSynced();
        seedVenuesIfEmpty();
        seedEventsIfEmpty();
        seedGamesIfEmpty();
        seedCommunityStatsIfEmpty();
        seedAuditAndNotificationsIfEmpty();
        seedAppConfigsIfMissing();
        log.info("Unified PostgreSQL verification completed cleanly.");
    }

    private void ensureSchemaMaintenance() {
        try {
            jdbcTemplate.execute("DROP TABLE IF EXISTS participant_badges CASCADE");
            jdbcTemplate.execute("DROP TABLE IF EXISTS badge_levels CASCADE");
            jdbcTemplate.execute("ALTER TABLE participant_conquered_games DROP COLUMN IF EXISTS players, DROP COLUMN IF EXISTS duration");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_pcg_part_lib_norm ON participant_conquered_games (participant_id, library_game_id, normalized_name)");
            jdbcTemplate.execute("DELETE FROM otp_verifications WHERE expires_at < (EXTRACT(EPOCH FROM NOW()) * 1000)::bigint");

            jdbcTemplate.execute("""
                CREATE OR REPLACE VIEW v_top_board_game_conquerors AS
                WITH part_lib_json AS (
                  SELECT p.id AS participant_id, elem.val AS game_id
                  FROM participants p
                  CROSS JOIN LATERAL jsonb_array_elements_text(
                    CASE WHEN p.games_played_ids IS NOT NULL AND p.games_played_ids ~ '^\\s*\\[' THEN p.games_played_ids::jsonb ELSE '[]'::jsonb END
                  ) AS elem(val)
                  JOIN games g ON g.id = elem.val
                ),
                reg_lib AS (
                  SELECT DISTINCT r.participant_id, elem.val AS game_id
                  FROM registrations r
                  CROSS JOIN LATERAL jsonb_array_elements_text(
                    CASE WHEN r.games_played IS NOT NULL AND r.games_played ~ '^\\s*\\[' THEN r.games_played::jsonb ELSE '[]'::jsonb END
                  ) AS elem(val)
                  JOIN games g ON g.id = elem.val
                  WHERE LOWER(COALESCE(r.attendance_status, '')) <> 'cancelled'
                ),
                pcg_lib AS (
                  SELECT DISTINCT pcg.participant_id, g.id AS game_id
                  FROM participant_conquered_games pcg
                  JOIN games g ON g.id = pcg.library_game_id
                ),
                all_lib AS (
                  SELECT participant_id, game_id FROM part_lib_json
                  UNION
                  SELECT participant_id, game_id FROM reg_lib
                  UNION
                  SELECT participant_id, game_id FROM pcg_lib
                ),
                lib_counts AS (
                  SELECT participant_id, COUNT(*)::int AS library_conquered_count
                  FROM all_lib
                  GROUP BY participant_id
                ),
                outside_counts AS (
                  SELECT pcg.participant_id, COUNT(DISTINCT pcg.normalized_name)::int AS outside_conquered_count
                  FROM participant_conquered_games pcg
                  LEFT JOIN games g ON g.id = pcg.library_game_id
                  WHERE g.id IS NULL AND COALESCE(TRIM(pcg.normalized_name), '') <> ''
                  GROUP BY pcg.participant_id
                )
                SELECT
                  p.id AS participant_id,
                  p.name,
                  COALESCE(NULLIF(TRIM(p.area), ''), 'Pune') AS area,
                  p.avatar,
                  COALESCE(p.total_events_attended, 0) AS total_events_attended,
                  COALESCE(lc.library_conquered_count, 0) AS library_conquered_count,
                  COALESCE(oc.outside_conquered_count, 0) AS outside_conquered_count,
                  (COALESCE(lc.library_conquered_count, 0) + COALESCE(oc.outside_conquered_count, 0)) AS total_conquered_count
                FROM participants p
                LEFT JOIN lib_counts lc ON lc.participant_id = p.id
                LEFT JOIN outside_counts oc ON oc.participant_id = p.id
                WHERE (COALESCE(lc.library_conquered_count, 0) + COALESCE(oc.outside_conquered_count, 0)) > 0
                ORDER BY total_conquered_count DESC, library_conquered_count DESC, total_events_attended DESC, p.name ASC
                """);

            jdbcTemplate.execute("""
                CREATE OR REPLACE VIEW v_community_analytics_summary AS
                SELECT
                  (SELECT COUNT(*)::int FROM participants) AS total_participants,
                  (SELECT COUNT(*)::int FROM events) AS total_events,
                  (SELECT COUNT(*)::int FROM events WHERE LOWER(COALESCE(status, '')) = 'upcoming') AS upcoming_events,
                  (SELECT COUNT(*)::int FROM events WHERE LOWER(COALESCE(status, '')) = 'completed') AS completed_events,
                  (SELECT COUNT(*)::int FROM registrations) AS total_registrations,
                  (SELECT COUNT(*)::int FROM registrations WHERE LOWER(COALESCE(attendance_status, '')) <> 'cancelled') AS active_registrations,
                  (SELECT COUNT(*)::int FROM registrations WHERE LOWER(COALESCE(attendance_status, '')) = 'checked in') AS checked_in_registrations,
                  (SELECT COUNT(*)::int FROM registrations WHERE LOWER(COALESCE(attendance_status, '')) = 'cancelled') AS cancelled_registrations,
                  (SELECT COALESCE(SUM(COALESCE(amount_paid, 0)), 0)::numeric(12,2) FROM registrations WHERE LOWER(COALESCE(attendance_status, '')) <> 'cancelled' AND LOWER(COALESCE(payment_status, '')) IN ('confirmed', 'paid')) AS confirmed_ticket_revenue,
                  (SELECT COUNT(*)::int FROM games WHERE active = true) AS active_library_games,
                  (SELECT COUNT(*)::int FROM participant_conquered_games) AS total_conquered_entries,
                  (SELECT COUNT(*)::int FROM v_top_board_game_conquerors) AS passport_adopters,
                  (SELECT COUNT(*)::int FROM event_feedback) AS feedback_count,
                  (SELECT COALESCE(ROUND(AVG(overall_rating)::numeric, 1), 0) FROM event_feedback) AS avg_overall_rating,
                  (SELECT COUNT(*)::int FROM safety_reports) AS total_safety_reports,
                  (SELECT COUNT(*)::int FROM safety_reports WHERE LOWER(COALESCE(status, '')) <> 'resolved') AS open_safety_reports,
                  (SELECT COUNT(*)::int FROM volunteer_applications WHERE LOWER(COALESCE(status, '')) = 'accepted') AS accepted_volunteers,
                  (SELECT COUNT(*)::int FROM volunteer_applications WHERE LOWER(COALESCE(status, '')) IN ('new', 'under review')) AS pending_volunteers
                """);
        } catch (Exception ex) {
            log.warn("Non-fatal schema maintenance check warning: {}", ex.getMessage());
        }
    }

    private void ensureAdminsSynced() {
        String nowIso = Instant.now().toString();
        ensureAdminAccount(
                "u-admin-parimal",
                "Parimal Shete",
                "parimalmshete@gmail.com",
                "+91 92849 83072",
                "Co-Founder & Community Architect",
                "Koregaon Park, Pune",
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=200",
                "Social Shuffle Co-Founder & Community Architect",
                nowIso
        );
        ensureAdminAccount(
                "u-admin-diksha",
                "Diksha",
                "birladiksha01@gmail.com",
                "+91 92849 83072",
                "Co-Founder & Experience Director",
                "Kalyani Nagar, Pune",
                "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=200",
                "Social Shuffle Co-Founder & Experience Director",
                nowIso
        );
    }

    private void ensureAdminAccount(String canonicalId, String name, String email, String phone,
                                    String designation, String area, String avatar, String bio, String nowIso) {
        Optional<AdminAccount> existingOpt = adminRepository.findById(canonicalId)
                .or(() -> adminRepository.findByEmailIgnoreCase(email));

        AdminAccount admin = existingOpt.orElseGet(() -> new AdminAccount(canonicalId, name, email, phone, ""));
        if (admin.getId() == null || admin.getId().isEmpty()) {
            admin.setId(canonicalId);
        }
        String pwdHash = admin.getPassword();
        if (!passwordSecurityUtil.isHashed(pwdHash)) {
            Optional<UserCredential> credOpt = credentialRepository.findByEmailIgnoreCase(email);
            if (credOpt.isPresent() && passwordSecurityUtil.isHashed(credOpt.get().getPasswordHash())) {
                pwdHash = credOpt.get().getPasswordHash();
            } else {
                pwdHash = passwordSecurityUtil.hashPassword("admin123");
            }
        }
        admin.setName(name);
        admin.setEmail(email);
        admin.setPhone(phone);
        admin.setPassword(pwdHash);
        admin.setRole("admin");
        admin.setDesignation(designation);
        admin.setArea(area);
        admin.setAvatar(avatar);
        admin.setBio(bio);
        admin.setActive(true);
        if (admin.getCreatedAt() == null) admin.setCreatedAt("2024-01-01T00:00:00Z");
        adminRepository.save(admin);

        final String resolvedPwdHash = pwdHash;
        // Keep users table synced
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> new User(admin.getId(), name, email, phone, resolvedPwdHash, "admin", area));
        user.setName(name);
        user.setPhone(phone);
        user.setPassword(resolvedPwdHash);
        user.setRole("admin");
        user.setArea(area);
        user.setAvatar(avatar);
        user.setBio(bio);
        user.setAuthProvider("google");
        userRepository.save(user);

        // Keep user_credentials table synced
        if (!credentialRepository.existsByEmailIgnoreCase(email)) {
            UserCredential cred = new UserCredential();
            cred.setUserId("cred-" + admin.getId());
            cred.setSubjectType("admin");
            cred.setSubjectId(admin.getId());
            cred.setEmail(email);
            cred.setPhone(phone);
            cred.setUsername(email.split("@")[0]);
            cred.setPasswordHash(pwdHash);
            cred.setAuthProvider("local");
            cred.setFailedAttempts(0);
            cred.setCreatedAt(nowIso);
            cred.setUpdatedAt(nowIso);
            credentialRepository.save(cred);
        }
    }

    private void seedVenuesIfEmpty() {
        if (venueRepository.count() == 0) {
            Venue v1 = new Venue("ven-1", "The Rustle Nest Cafe", "Lane 6, Koregaon Park, Pune, Maharashtra 411001", "Koregaon Park", 45, "+91 92849 83072", "https://maps.google.com/?q=The+Rustle+Nest+Cafe+Pune", "https://images.unsplash.com/photo-1554118811-1e0d58224f24?auto=format&fit=crop&q=80&w=800", true);
            Venue v2 = new Venue("ven-2", "Pagdandi Bookstore Cafe", "Shop 6, Regent Plaza, Baner - Pashan Link Rd, Pune 411045", "Baner", 35, "+91 98220 44556", "https://maps.google.com/?q=Pagdandi+Bookstore+Cafe+Baner", "https://images.unsplash.com/photo-1521017432531-fbd92d768814?auto=format&fit=crop&q=80&w=800", true);
            Venue v3 = new Venue("ven-3", "Waari Book Cafe", "Ground Floor, Choice Institute, Kothrud, Pune 411038", "Kothrud", 50, "+91 98220 77889", "https://maps.google.com/?q=Waari+Book+Cafe+Kothrud", "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&q=80&w=800", true);
            venueRepository.saveAll(Arrays.asList(v1, v2, v3));
        }
    }

    private void seedEventsIfEmpty() {
        if (eventRepository.count() == 0) {
            ShuffleEvent e1 = new ShuffleEvent();
            e1.setId("evt-35");
            e1.setNumber(35);
            e1.setTitle("Social Shuffle #35: Weekend Dice & Strategy");
            e1.setDate("2026-09-27");
            e1.setTime("4:00 PM - 8:00 PM");
            e1.setVenue("The Rustle Nest Cafe");
            e1.setAddress("Lane 6, Koregaon Park, Pune, Maharashtra 411001");
            e1.setArea("Koregaon Park");
            e1.setCategory("Casual & Social");
            e1.setCapacity(30);
            e1.setTicketPrice(350.0);
            e1.setDescription("Join us for our signature weekend board gaming mixer! Over 40+ curated board games, warm coffee, and friendly table hosts.");
            e1.setNotes("Wear comfortable clothing. Welcome tea/coffee included.");
            e1.setPlannedGames(Arrays.asList("g-catan", "g-avalon", "g-codenames", "g-splendor"));
            e1.setStatus("upcoming");
            e1.setCoverImage("https://images.unsplash.com/photo-1610890716171-6b1bb98ffd09?auto=format&fit=crop&q=80&w=800");

            ShuffleEvent e2 = new ShuffleEvent();
            e2.setId("evt-36");
            e2.setNumber(36);
            e2.setTitle("Social Shuffle #36: Tactical Tournament");
            e2.setDate("2026-10-04");
            e2.setTime("3:30 PM - 8:30 PM");
            e2.setVenue("Pagdandi Bookstore Cafe");
            e2.setAddress("Shop 6, Regent Plaza, Baner - Pashan Link Rd, Pune 411045");
            e2.setArea("Baner");
            e2.setCategory("Tournament");
            e2.setCapacity(32);
            e2.setTicketPrice(400.0);
            e2.setDescription("A tactical showdown featuring Catan, 7 Wonders, and Wingspan with special prizes and handcrafted coffee.");
            e2.setNotes("Pre-registration mandatory. Tables limited to 32 players.");
            e2.setPlannedGames(Arrays.asList("g-catan", "g-splendor", "g-7wonders"));
            e2.setStatus("upcoming");
            e2.setCoverImage("https://images.unsplash.com/photo-1563941402622-4e7a488bcc57?auto=format&fit=crop&q=80&w=800");

            eventRepository.saveAll(Arrays.asList(e1, e2));
        }
    }

    private void seedGamesIfEmpty() {
        if (gameRepository.count() == 0) {
            Game g1 = new Game("g-catan", "Catan (Settlers of Catan)", "Strategy", "Medium", "3-4 Players", "60-90 mins",
                    "Collect resources, trade with fellow shufflers, build settlements and roads on the island of Catan.",
                    true, 48, Arrays.asList("trading", "negotiation", "dice", "classic"), 4.8,
                    "https://images.unsplash.com/photo-1610890716171-6b1bb98ffd09?auto=format&fit=crop&q=80&w=400",
                    "Community Library", "The Rustle Nest Cafe");
            Game g2 = new Game("g-avalon", "The Resistance: Avalon", "Social Deduction", "Beginner", "5-10 Players", "30-45 mins",
                    "Loyal servants of Arthur vs Minions of Mordred. Pure deception, table talk, and dramatic accusations.",
                    true, 62, Arrays.asList("bluffing", "party", "hidden-role"), 4.9,
                    "https://images.unsplash.com/photo-1543083477-4f785aeafaa9?auto=format&fit=crop&q=80&w=400",
                    "Community Library", "The Rustle Nest Cafe");
            Game g3 = new Game("g-codenames", "Codenames", "Party", "Beginner", "4-8 Players", "15-20 mins",
                    "Two rival spymasters know the secret identities of 25 agents. Teammates try to guess words based on one-word clues.",
                    true, 89, Arrays.asList("word", "deduction", "icebreaker"), 4.8,
                    "https://images.unsplash.com/photo-1563941402622-4e7a488bcc57?auto=format&fit=crop&q=80&w=400",
                    "Community Library", "Pagdandi Bookstore Cafe");
            Game g4 = new Game("g-splendor", "Splendor", "Strategy", "Beginner", "2-4 Players", "30 mins",
                    "Renaissance merchants acquiring gemstone mines, transportation, and shops to attract aristocratic patrons.",
                    true, 35, Arrays.asList("engine-building", "chips", "cards"), 4.7,
                    "https://images.unsplash.com/photo-1585504198199-20277593b94f?auto=format&fit=crop&q=80&w=400",
                    "Community Library", "Pagdandi Bookstore Cafe");
            gameRepository.saveAll(Arrays.asList(g1, g2, g3, g4));
        }
    }

    private void seedCommunityStatsIfEmpty() {
        if (communityStatsRepository.count() == 0) {
            CommunityStats cs = new CommunityStats();
            cs.setId("community-stats-main");
            cs.setTotalMembers(1200);
            cs.setTotalEvents(34);
            cs.setTotalGames(85);
            cs.setTotalCheckins(0);
            cs.setActiveAreas("[\"Koregaon Park\",\"Baner\",\"Viman Nagar\",\"FC Road / Deccan\",\"Aundh\",\"Kothrud\",\"Kalyani Nagar\",\"Wakad\"]");
            cs.setUpdatedBy("System Init");
            cs.setUpdatedAt(Instant.now().toString());
            communityStatsRepository.save(cs);
        }
    }

    private void seedAuditAndNotificationsIfEmpty() {
        if (auditLogRepository.count() == 0) {
            auditLogRepository.save(new AuditLog("log-1", "Parimal Shete", "SECURITY_INIT", "PostgreSQL Database", "Unified PostgreSQL persistence verified across all modules", Instant.now().toString()));
        }
        if (notificationRepository.count() == 0) {
            NotificationItem notif = new NotificationItem("notif-1", "Social Shuffle #35 Live", "Meetup announced for Koregaon Park. Registrations are open!", "event", Instant.now().toString(), false);
            notif.setEventId("evt-35");
            notif.setDeliveryStatus("Sent");
            notificationRepository.save(notif);
        }
    }

    private void seedAppConfigsIfMissing() {
        if (!appConfigRepository.existsById(AppConfigService.KEY_COMMUNICATIONS)) {
            java.util.Map<String, Object> defaults = new java.util.LinkedHashMap<>();
            defaults.put("smtpHost", "smtp.gmail.com");
            defaults.put("smtpPort", 587);
            defaults.put("smtpUser", "socialshuffle.pune@gmail.com");
            defaults.put("smtpFrom", "socialshuffle.pune@gmail.com");
            defaults.put("adminNotificationEmail", "socialshuffle.pune@gmail.com");
            appConfigService.saveCommunicationsConfig(defaults, "System Init");
        } else {
            java.util.Map<String, Object> existing = appConfigService.getCommunicationsConfig();
            String currentUser = existing.get("smtpUser") != null ? String.valueOf(existing.get("smtpUser")).trim() : "";
            String currentFrom = existing.get("smtpFrom") != null ? String.valueOf(existing.get("smtpFrom")).trim() : "";
            String currentAdmin = existing.get("adminNotificationEmail") != null ? String.valueOf(existing.get("adminNotificationEmail")).trim() : "";
            String resolvedPass = existing.get("smtpPass") != null ? String.valueOf(existing.get("smtpPass")).trim() : "";
            String rawStoredJson = appConfigRepository.findById(AppConfigService.KEY_COMMUNICATIONS)
                    .map(com.socialshuffle.model.AppConfig::getConfigJson).orElse("");
            boolean dbNeedsPassSync = !resolvedPass.isEmpty() && (!rawStoredJson.contains("\"smtpPass\"") || rawStoredJson.contains("\"smtpPass\":\"\""));
            boolean adminNeedsUpdate = currentAdmin.isEmpty() || "parimalmshete@gmail.com".equalsIgnoreCase(currentAdmin);
            if (currentUser.isEmpty() || "parimalmshete@gmail.com".equalsIgnoreCase(currentUser)
                    || currentFrom.isEmpty() || "parimalmshete@gmail.com".equalsIgnoreCase(currentFrom)
                    || "noreply@socialshufflepune.com".equalsIgnoreCase(currentFrom)
                    || adminNeedsUpdate
                    || dbNeedsPassSync) {
                java.util.Map<String, Object> updates = new java.util.LinkedHashMap<>();
                updates.put("smtpUser", "socialshuffle.pune@gmail.com");
                updates.put("smtpFrom", "socialshuffle.pune@gmail.com");
                updates.put("adminNotificationEmail", "socialshuffle.pune@gmail.com");
                if (!resolvedPass.isEmpty()) {
                    updates.put("smtpPass", resolvedPass);
                }
                appConfigService.saveCommunicationsConfig(updates, "SMTP & Admin Email Sync");
            }
        }
        if (!appConfigRepository.existsById(AppConfigService.KEY_RAZORPAY)) {
            appConfigService.saveRazorpayConfig(new java.util.LinkedHashMap<>(), "System Init");
        }
    }
}
