package com.socialshuffle.config;

import com.socialshuffle.model.*;
import com.socialshuffle.repository.*;
import com.socialshuffle.security.PasswordSecurityUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Automatically seeds initial Pune board game data into PostgreSQL / Supabase if tables are empty.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final ShuffleEventRepository eventRepository;
    private final GameRepository gameRepository;
    private final ParticipantRepository participantRepository;
    private final RegistrationRepository registrationRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationItemRepository notificationRepository;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final PasswordSecurityUtil passwordSecurityUtil;

    public DataSeeder(ShuffleEventRepository eventRepository,
                      GameRepository gameRepository,
                      ParticipantRepository participantRepository,
                      RegistrationRepository registrationRepository,
                      AuditLogRepository auditLogRepository,
                      NotificationItemRepository notificationRepository,
                      UserRepository userRepository,
                      AdminRepository adminRepository,
                      PasswordSecurityUtil passwordSecurityUtil) {
        this.eventRepository = eventRepository;
        this.gameRepository = gameRepository;
        this.participantRepository = participantRepository;
        this.registrationRepository = registrationRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.passwordSecurityUtil = passwordSecurityUtil;
    }

    @Override
    public void run(String... args) {
        seedAdmins();
        seedUsers();
        seedEvents();
        seedGames();
        seedParticipants();
        seedRegistrations();
        seedAuditAndNotifications();
    }

    private void seedAdmins() {
        if (adminRepository.count() == 0) {
            AdminAccount admin = new AdminAccount(
                    "u-admin",
                    "Aman Joshi (Host)",
                    "admin@socialshuffle.com",
                    "+91 98220 11223",
                    passwordSecurityUtil.hashPassword("admin123"),
                    "Community Lead & Founder",
                    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80"
            );
            admin.setBio("Social Shuffle Founder & Super Admin. Refers to database table for name and admin details.");
            adminRepository.save(admin);
        }
    }

    private void seedUsers() {
        if (userRepository.count() == 0) {
            String adminHash = passwordSecurityUtil.hashPassword("admin123");
            User admin = new User("u-admin", "Aman Joshi (Host)", "admin@socialshuffle.com", "+91 98220 11223", adminHash, "admin", "Pune");
            admin.setBio("Community Lead & Founder");
            userRepository.save(admin);
        }
    }

    private void seedEvents() {
        if (eventRepository.count() == 0) {
            ShuffleEvent e1 = new ShuffleEvent();
            e1.setId("evt-35");
            e1.setNumber(35);
            e1.setTitle("Social Shuffle #35: Weekend Board Game Bonanza");
            e1.setDate("2026-09-27");
            e1.setTime("Sunday, 4:00 PM – 8:00 PM");
            e1.setVenue("The Rustle Nest Cafe");
            e1.setAddress("Lane 6, Koregaon Park, Pune");
            e1.setArea("Koregaon Park");
            e1.setCategory("Regular Meetup");
            e1.setCapacity(40);
            e1.setTicketPrice(350);
            e1.setDescription("Join 40+ friendly board gamers across Pune for high-energy strategy, bluffing, and laughter. Hosts teach every game!");
            e1.setStatus("upcoming");
            e1.setPlannedGames(Arrays.asList("g-catan", "g-avalon", "g-ticket", "g-codenames", "g-splendor"));

            ShuffleEvent e2 = new ShuffleEvent();
            e2.setId("evt-36");
            e2.setNumber(36);
            e2.setTitle("Social Shuffle #36: Baner Strategic Showdown");
            e2.setDate("2026-10-04");
            e2.setTime("Sunday, 3:30 PM – 7:30 PM");
            e2.setVenue("One Restaurant & Bar");
            e2.setAddress("Balewadi High St, Baner, Pune");
            e2.setArea("Baner");
            e2.setCategory("Strategy Special");
            e2.setCapacity(32);
            e2.setTicketPrice(400);
            e2.setDescription("Dedicated tables for mid-weight to heavy eurogames. Terraforming Mars, Dune Imperium, and Wingspan tables with game masters.");
            e2.setStatus("upcoming");
            e2.setPlannedGames(Arrays.asList("g-terraforming", "g-wingspan", "g-dune", "g-root"));

            ShuffleEvent e3 = new ShuffleEvent();
            e3.setId("evt-34");
            e3.setNumber(34);
            e3.setTitle("Social Shuffle #34: Social Deduction Night");
            e3.setDate("2026-09-20");
            e3.setTime("Sunday, 4:00 PM – 8:00 PM");
            e3.setVenue("FC Road Social");
            e3.setAddress("FC Road, Deccan Gymkhana, Pune");
            e3.setArea("FC Road / Deccan");
            e3.setCategory("Party & Deduction");
            e3.setCapacity(45);
            e3.setTicketPrice(350);
            e3.setDescription("Blood on the Clocktower, Secret Hitler, and Avalon games with 42 attendees.");
            e3.setStatus("completed");
            e3.setPlannedGames(Arrays.asList("g-botc", "g-secrethitler", "g-avalon", "g-deception"));

            eventRepository.saveAll(Arrays.asList(e1, e2, e3));
        }
    }

    private void seedGames() {
        if (gameRepository.count() == 0) {
            Game g1 = new Game("g-catan", "Catan", "Strategy", "3-4", "60-90 min", "Medium", "Aman Joshi", true, "Lane 6 Cafe", "The iconic resource trading game that started modern board gaming.");
            Game g2 = new Game("g-avalon", "The Resistance: Avalon", "Social Deduction", "5-10", "30-45 min", "Easy", "Community Box", true, "Lane 6 Cafe", "Test loyalties, discover minions of Mordred, and embark on quests.");
            Game g3 = new Game("g-ticket", "Ticket to Ride: Europe", "Family Strategy", "2-5", "45-60 min", "Easy", "Parimal Shete", true, "FC Road Cafe", "Build train routes connecting iconic European cities.");
            Game g4 = new Game("g-codenames", "Codenames", "Party / Word", "4-8+", "15-20 min", "Easy", "Community Box", true, "Lane 6 Cafe", "Two rival spymasters know the secret identities of 25 agents.");
            Game g5 = new Game("g-splendor", "Splendor", "Engine Building", "2-4", "30 min", "Easy", "Rohan Kulkarni", true, "Baner Venue", "Renaissance merchants collecting gem tokens to acquire cards and nobles.");
            Game g6 = new Game("g-wingspan", "Wingspan", "Engine Building", "1-5", "40-70 min", "Medium", "Aman Joshi", true, "Baner Venue", "Attract beautiful birds to your wildlife preserve.");

            gameRepository.saveAll(Arrays.asList(g1, g2, g3, g4, g5, g6));
        }
    }

    private void seedParticipants() {
        if (participantRepository.count() == 0) {
            Participant pVarad = new Participant();
            pVarad.setId("p-varad");
            pVarad.setName("Varad");
            pVarad.setEmail("varad@socialshuffle.in");
            pVarad.setPhone("+91 98221 12345");
            pVarad.setUsername("varad");
            pVarad.setArea("Koregaon Park");
            pVarad.setAvatar("https://api.dicebear.com/7.x/bottts/svg?seed=Varad");
            pVarad.setBio("Pune board game enthusiast • Catan & Social Deduction lover!");
            pVarad.setJoinedDate("2026-11-11");
            pVarad.setTotalEventsAttended(0);
            pVarad.setTotalRegistrations(0);
            participantRepository.save(pVarad);
        }
    }

    private void seedRegistrations() {
        // Clean initial state: Registrations are created via live checkout and booking
    }

    private void seedAuditAndNotifications() {
        if (auditLogRepository.count() == 0) {
            AuditLog log1 = new AuditLog("audit-1", "Aman Joshi (Host)", "Publish Meetup", "Event #35 created and published", "2026-09-20 10:00");
            AuditLog log2 = new AuditLog("audit-2", "Aman Joshi (Host)", "Security Audit", "Enabled PBKDF2 password hashing & rate limiting defense", "2026-09-25 12:00");
            auditLogRepository.saveAll(Arrays.asList(log1, log2));
        }

        if (notificationRepository.count() == 0) {
            NotificationItem n1 = new NotificationItem("notif-1", "Registration Confirmed", "You are confirmed for Social Shuffle #35 this Sunday!", "2026-09-23 11:16", false, "r-102");
            notificationRepository.save(n1);
        }
    }
}
