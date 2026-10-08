package com.socialshuffle.config;

import com.socialshuffle.model.*;
import com.socialshuffle.repository.*;
import com.socialshuffle.security.PasswordSecurityUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Automatically seeds initial Pune board game data into MySQL if tables are empty.
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

            String userHash = passwordSecurityUtil.hashPassword("shuffler123");

            User p1 = new User("u-rohan", "Rohan Kulkarni", "rohan.kulkarni@example.com", "+91 98220 12345", userHash, "participant", "Koregaon Park");
            p1.setParticipantId("p-rahul");
            p1.setBio("Board game lover, strategy nerd, Catan enthusiast.");
            userRepository.save(p1);

            User p2 = new User("u-ananya", "Ananya Deshmukh", "ananya.d@example.com", "+91 97654 32100", userHash, "participant", "Baner");
            p2.setParticipantId("p-ananya");
            p2.setBio("Casual gamer & Azul fan. Love meeting new people in Pune!");
            userRepository.save(p2);
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
            Participant p1 = new Participant("p-rahul", "Rohan Kulkarni", "rohan.kulkarni@example.com", "+91 98220 12345", "Koregaon Park");
            p1.setBio("Board game lover, strategy nerd, Catan enthusiast.");
            p1.setTotalEventsAttended(8);
            p1.setTotalRegistrations(9);
            p1.setJoinedDate("2026-03-12");

            Participant p2 = new Participant("p-ananya", "Ananya Deshmukh", "ananya.d@example.com", "+91 97654 32100", "Baner");
            p2.setBio("Casual gamer & Azul fan. Love meeting new people in Pune!");
            p2.setTotalEventsAttended(5);
            p2.setTotalRegistrations(6);
            p2.setJoinedDate("2026-04-05");

            Participant p3 = new Participant("p-sid", "Siddharth Mehta", "siddharth.m@example.com", "+91 98901 23456", "Viman Nagar");
            p3.setBio("Heavy eurogame enthusiast. Ark Nova and Terraforming Mars!");
            p3.setTotalEventsAttended(12);
            p3.setTotalRegistrations(13);
            p3.setJoinedDate("2026-01-20");

            participantRepository.saveAll(Arrays.asList(p1, p2, p3));
        }
    }

    private void seedRegistrations() {
        if (registrationRepository.count() == 0) {
            Registration r1 = new Registration("r-101", "evt-35", "p-rahul", "Rohan Kulkarni", "rohan.kulkarni@example.com", "+91 98220 12345", 1, "Paid", "UPI", 350.0, "Registered", "2026-09-22 14:30");
            r1.setTicketCode("SHUFFLE-35-RK01");

            Registration r2 = new Registration("r-102", "evt-35", "p-ananya", "Ananya Deshmukh", "ananya.d@example.com", "+91 97654 32100", 2, "Paid", "Razorpay", 700.0, "Registered", "2026-09-23 11:15");
            r2.setTicketCode("SHUFFLE-35-AD02");

            Registration r3 = new Registration("r-103", "evt-34", "p-sid", "Siddharth Mehta", "siddharth.m@example.com", "+91 98901 23456", 1, "Paid", "UPI", 350.0, "Checked In", "2026-09-18 19:40");
            r3.setTicketCode("SHUFFLE-34-SM01");

            registrationRepository.saveAll(Arrays.asList(r1, r2, r3));
        }
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
