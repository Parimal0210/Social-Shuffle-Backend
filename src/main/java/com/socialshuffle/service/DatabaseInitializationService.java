package com.socialshuffle.service;

import com.socialshuffle.model.*;
import com.socialshuffle.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Ensures all required domain tables and initial seed data exist in the database.
 * Strictly guarantees that all application queries fetch persisted data from the database.
 */
@Component
public class DatabaseInitializationService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializationService.class);

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final ShuffleEventRepository eventRepository;
    private final GameRepository gameRepository;
    private final BadgeLevelRepository badgeLevelRepository;
    private final VenueRepository venueRepository;
    private final RegistrationRepository registrationRepository;
    private final EventFeedbackRepository feedbackRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationItemRepository notificationRepository;

    public DatabaseInitializationService(AdminRepository adminRepository,
                                         UserRepository userRepository,
                                         ParticipantRepository participantRepository,
                                         ShuffleEventRepository eventRepository,
                                         GameRepository gameRepository,
                                         BadgeLevelRepository badgeLevelRepository,
                                         VenueRepository venueRepository,
                                         RegistrationRepository registrationRepository,
                                         EventFeedbackRepository feedbackRepository,
                                         AuditLogRepository auditLogRepository,
                                         NotificationItemRepository notificationRepository) {
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.participantRepository = participantRepository;
        this.eventRepository = eventRepository;
        this.gameRepository = gameRepository;
        this.badgeLevelRepository = badgeLevelRepository;
        this.venueRepository = venueRepository;
        this.registrationRepository = registrationRepository;
        this.feedbackRepository = feedbackRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Starting Social Shuffle Database verification and seeding check...");

        seedAdminsIfEmpty();
        seedUsersIfEmpty();
        seedParticipantsIfEmpty();
        seedVenuesIfEmpty();
        seedEventsIfEmpty();
        seedGamesIfEmpty();
        seedBadgeLevelsIfEmpty();
        seedRegistrationsIfEmpty();
        seedFeedbackIfEmpty();
        seedAuditAndNotificationsIfEmpty();

        log.info("Database verification completed successfully. All data is persisted in database tables.");
    }

    private void seedAdminsIfEmpty() {
        if (adminRepository.count() == 0) {
            log.info("Seeding admins table in database...");
            AdminAccount parimal = new AdminAccount("u-admin-parimal", "Parimal Shete", "parimalmshete@gmail.com", "+91 98220 99887", "admin123");
            parimal.setRole("admin");
            parimal.setDesignation("Co-Founder & Community Architect");
            parimal.setAvatar("https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=200");
            parimal.setBio("Social Shuffle Co-Founder & Community Architect");
            parimal.setActive(true);
            parimal.setCreatedAt("2024-01-01T00:00:00Z");

            AdminAccount parimalWork = new AdminAccount("u-admin-parimal-work", "Parimal Shete (Work)", "parimal.work@gmail.com", "+91 98220 55443", "admin123");
            parimalWork.setRole("admin");
            parimalWork.setDesignation("Co-Founder");
            parimalWork.setAvatar("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=200");
            parimalWork.setBio("Social Shuffle Co-Founder");
            parimalWork.setActive(true);
            parimalWork.setCreatedAt("2024-01-01T00:00:00Z");

            AdminAccount diksha = new AdminAccount("u-admin-diksha", "Diksha", "diksha@socialshuffle.in", "+91 98220 88776", "admin123");
            diksha.setRole("admin");
            diksha.setDesignation("Co-Founder & Experience Director");
            diksha.setAvatar("https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=200");
            diksha.setBio("Social Shuffle Co-Founder & Experience Director");
            diksha.setActive(true);
            diksha.setCreatedAt("2024-01-01T00:00:00Z");

            AdminAccount aman = new AdminAccount("u-admin-aman", "Aman Joshi (Host)", "admin@socialshuffle.com", "+91 98220 11223", "admin123");
            aman.setRole("admin");
            aman.setDesignation("Community Host & Super Admin");
            aman.setAvatar("https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&q=80&w=200");
            aman.setBio("Social Shuffle Lead Host & Admin");
            aman.setActive(true);
            aman.setCreatedAt("2024-01-01T00:00:00Z");

            adminRepository.saveAll(Arrays.asList(parimal, parimalWork, diksha, aman));
        }
    }

    private void seedUsersIfEmpty() {
        if (userRepository.count() == 0) {
            log.info("Seeding users table in database with admin user...");
            User u1 = new User("u-admin-parimal", "Parimal Shete", "parimalmshete@gmail.com", "+91 98220 99887", "admin123", "admin", "Koregaon Park");
            u1.setAvatar("https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=200");
            u1.setBio("Co-Founder & Community Architect at Social Shuffle");
            u1.setAuthProvider("google");

            userRepository.save(u1);
        }
    }

    private void seedParticipantsIfEmpty() {
        // Clean initial state: Active participant records are generated through live user registration
        log.info("Participants table ready for real member registrations.");
    }

    private void seedVenuesIfEmpty() {
        if (venueRepository.count() == 0) {
            log.info("Seeding venues table in database...");
            Venue v1 = new Venue("ven-1", "The Rustle Nest Cafe", "Lane 6, Koregaon Park, Pune, Maharashtra 411001", "Koregaon Park", 45, "+91 98220 11223", "https://maps.google.com/?q=The+Rustle+Nest+Cafe+Pune", "https://images.unsplash.com/photo-1554118811-1e0d58224f24?auto=format&fit=crop&q=80&w=800", true);
            Venue v2 = new Venue("ven-2", "Pagdandi Bookstore Cafe", "Shop 6, Regent Plaza, Baner - Pashan Link Rd, Pune 411045", "Baner", 35, "+91 98220 44556", "https://maps.google.com/?q=Pagdandi+Bookstore+Cafe+Baner", "https://images.unsplash.com/photo-1521017432531-fbd92d768814?auto=format&fit=crop&q=80&w=800", true);
            Venue v3 = new Venue("ven-3", "Waari Book Cafe", "Ground Floor, Choice Institute, Kothrud, Pune 411038", "Kothrud", 50, "+91 98220 77889", "https://maps.google.com/?q=Waari+Book+Cafe+Kothrud", "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&q=80&w=800", true);
            venueRepository.saveAll(Arrays.asList(v1, v2, v3));
        }
    }

    private void seedEventsIfEmpty() {
        if (eventRepository.count() == 0) {
            log.info("Seeding events table in database...");
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
            e1.setDescription("Join us for our signature weekend board gaming mixer! Over 40+ curated board games, warm coffee, and friendly table hosts to teach you the rules in under 5 minutes.");
            e1.setNotes("Wear comfortable clothing. Welcome tea/coffee included with your registration ticket.");
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
            log.info("Seeding games table in database...");
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

    private void seedBadgeLevelsIfEmpty() {
        if (badgeLevelRepository.count() == 0) {
            log.info("Seeding badge_levels table in database...");
            List<BadgeLevel> seeds = Arrays.asList(
                    new BadgeLevel(1, "Novice Roller", "Initiate of the Ivory Dice", "The First Move",
                            "Broke the ice, rolled the initial dice, and attended their first Pune tabletop gathering.",
                            "🎲", 4, 1, "Common", "from-slate-800 via-purple-950/40 to-slate-900",
                            "border-slate-600/50", "text-slate-300", "bg-slate-700/50 text-slate-300 border-slate-600",
                            "shadow-slate-500/10", Arrays.asList("Unlock Digital Shuffler Passport", "Table Rookie Profile Badge")),

                    new BadgeLevel(2, "Apprentice Shuffler", "Acolyte of the Meeple Order", "Table Apprentice",
                            "Expanded beyond the basics with multiple games tested and a verified event check-in.",
                            "♟️", 10, 1, "Common", "from-amber-950/30 via-slate-900 to-purple-950/30",
                            "border-amber-700/50", "text-amber-300", "bg-amber-900/30 text-amber-300 border-amber-700/50",
                            "shadow-amber-700/15", Arrays.asList("Game Table Tagging Rights", "Access to Sunday Game Discussions")),

                    new BadgeLevel(3, "Casual Tactician", "Bronze Vanguard of Koregaon", "Cafe Contender",
                            "Frequents Pune board game cafes and demonstrates reliable tabletop instincts.",
                            "⚔️", 20, 2, "Uncommon", "from-amber-800/30 via-slate-900 to-amber-950/40",
                            "border-amber-600/60", "text-amber-200", "bg-amber-600/20 text-amber-200 border-amber-500/40",
                            "shadow-amber-500/20", Arrays.asList("Early Seat Reservations", "Bronze Shuffler Tier Status")),

                    new BadgeLevel(4, "Board Game Enthusiast", "Master of the Card & Tile", "Strategy Adept",
                            "Explores nearly a third of the catalog with a strong grasp of tile placement and card play.",
                            "🏰", 30, 2, "Uncommon", "from-blue-950/40 via-slate-900 to-indigo-950/40",
                            "border-blue-500/60", "text-blue-300", "bg-blue-500/20 text-blue-300 border-blue-400/40",
                            "shadow-blue-500/25", Arrays.asList("Custom Table Name Tag", "Host Co-Player Recommendation")),

                    new BadgeLevel(5, "Tabletop Strategist", "Silver Viceroy of Baner Tables", "Tabletop Champion",
                            "Comfortably maneuvers euro-style resource management, engine building, and deduction games.",
                            "🛡️", 40, 3, "Rare", "from-slate-700/40 via-indigo-950/50 to-slate-900",
                            "border-slate-300/60", "text-slate-100", "bg-slate-200/20 text-slate-100 border-slate-300/50",
                            "shadow-slate-300/25", Arrays.asList("Silver Shuffler Tier Status", "Tournament Seeding Priority")),

                    new BadgeLevel(6, "Seasoned Shuffler", "Commander of a Thousand Battles", "Halfway to Catalog Mastery (50%+)",
                            "Conquered half the official game library and a loyal regular at weekend gatherings.",
                            "🌟", 50, 4, "Rare", "from-cyan-950/50 via-teal-950/40 to-slate-900",
                            "border-cyan-400/70", "text-cyan-200", "bg-cyan-500/20 text-cyan-200 border-cyan-400/50",
                            "shadow-cyan-400/30", Arrays.asList("New Game Library Voting Voice", "Exclusive Shuffler Community Role")),

                    new BadgeLevel(7, "Master of Meeples", "Gold Sovereign of Strategy", "High Tactician (60%+)",
                            "Commanding table presence with extensive knowledge across strategic and party classics.",
                            "👑", 60, 5, "Epic", "from-amber-600/30 via-yellow-600/20 to-purple-950/50",
                            "border-amber-400/80", "text-amber-200", "bg-amber-400/20 text-amber-200 border-amber-400/50",
                            "shadow-amber-400/35", Arrays.asList("Gold Strategist Tier Status", "Complimentary Table Host Drink")),

                    new BadgeLevel(8, "Grandmaster Tactician", "Platinum Grand Marshal", "Tabletop Virtuoso (70%+)",
                            "Demonstrates surgical precision in deep euro-games, complex negotiations, and heavy card systems.",
                            "💎", 70, 6, "Epic", "from-indigo-600/30 via-purple-600/20 to-sky-950/50",
                            "border-indigo-400/80", "text-indigo-200", "bg-indigo-400/20 text-indigo-200 border-indigo-400/50",
                            "shadow-indigo-400/40", Arrays.asList("VIP Game Day Lounge Pass", "Rule Explainer Recognition")),

                    new BadgeLevel(9, "Tabletop Sovereign", "Diamond Regent of Tabletop Realms", "Pune Board Game Legend (80%+)",
                            "A legendary pillar of Pune gaming who has conquered 80%+ of all curated board games.",
                            "🌌", 80, 7, "Legendary", "from-purple-600/40 via-pink-600/30 to-amber-900/40",
                            "border-purple-300", "text-purple-100", "bg-purple-500/30 text-purple-100 border-purple-300/60",
                            "shadow-purple-400/50", Arrays.asList("Diamond Grandmaster Tier Status", "Permanent Hall of Fame Listing", "Private Table Reservation")),

                    new BadgeLevel(10, "Apex Shuffler", "Mythic Tabletop Archon", "90%+ Catalog Mastery & Elite Veteran",
                            "Supreme mastery of 90%+ games in Pune meetups. An immortal legend of Social Shuffle.",
                            "⚡", 90, 8, "Mythic", "from-amber-400/40 via-purple-600/40 to-cyan-400/40",
                            "border-amber-300", "text-amber-100", "bg-gradient-to-r from-amber-400/30 to-purple-400/30 text-amber-200 border-amber-300/70",
                            "shadow-amber-400/60", Arrays.asList(
                                    "Mythic Golden Avatar Crown",
                                    "Free VIP Entry to Annual Championship",
                                    "Name Etched onto Social Shuffle Trophy",
                                    "Lifetime Pune Shuffler Pioneer Status"
                            ))
            );
            badgeLevelRepository.saveAll(seeds);
        }
    }

    private void seedRegistrationsIfEmpty() {
        // Clean initial state: Registrations are created via live checkout and booking
        log.info("Registrations table ready for live bookings.");
    }

    private void seedFeedbackIfEmpty() {
        // Clean initial state: Feedback is collected after live meetups
        log.info("Feedback table ready for participant submissions.");
    }

    private void seedAuditAndNotificationsIfEmpty() {
        if (auditLogRepository.count() == 0) {
            log.info("Seeding audit_logs table in database...");
            AuditLog log1 = new AuditLog("log-1", "Aman Joshi (Host)", "SECURITY_INIT", "Database", "Direct DB query architecture activated across all domain entities", "2026-09-29T12:00:00Z");
            auditLogRepository.save(log1);
        }

        if (notificationRepository.count() == 0) {
            log.info("Seeding notifications table in database...");
            NotificationItem notif = new NotificationItem("notif-1", "Social Shuffle #35 Live", "Meetup announced for Koregaon Park. Registrations are open!", "event", "2026-09-29T12:00:00Z", false);
            notif.setRecipientEmail("admin@socialshuffle.com");
            notif.setEventId("evt-35");
            notif.setDeliveryStatus("Sent");
            notificationRepository.save(notif);
        }
    }
}
