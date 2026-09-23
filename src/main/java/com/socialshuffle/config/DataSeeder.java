package com.socialshuffle.config;

import com.socialshuffle.model.*;
import com.socialshuffle.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Automatically seeds initial Pune board game data into MongoDB if collections are empty.
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

    public DataSeeder(ShuffleEventRepository eventRepository,
                      GameRepository gameRepository,
                      ParticipantRepository participantRepository,
                      RegistrationRepository registrationRepository,
                      AuditLogRepository auditLogRepository,
                      NotificationItemRepository notificationRepository,
                      UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.gameRepository = gameRepository;
        this.participantRepository = participantRepository;
        this.registrationRepository = registrationRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedEvents();
        seedGames();
        seedParticipants();
        seedRegistrations();
        seedAuditAndNotifications();
    }

    private void seedUsers() {
        if (userRepository.count() == 0) {
            User admin = new User("u-admin", "Aman Joshi", "admin@socialshuffle.com", "+91 98220 11223", "admin123", "admin", "Pune");
            userRepository.save(admin);

            User p1 = new User("u-rohan", "Rohan Kulkarni", "rohan.kulkarni@example.com", "+91 98220 12345", "shuffler123", "participant", "Koregaon Park");
            p1.setParticipantId("p-rahul");
            p1.setBio("Board game lover, strategy nerd, Catan enthusiast.");
            userRepository.save(p1);

            User p2 = new User("u-ananya", "Ananya Deshmukh", "ananya.d@example.com", "+91 97654 32100", "shuffler123", "participant", "Baner");
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
            e2.setTime("Sunday, 4:30 PM – 8:30 PM");
            e2.setVenue("Pagdandi Bookstore Cafe");
            e2.setAddress("Regent Plaza, Baner Pashan Link Rd, Pune");
            e2.setArea("Baner");
            e2.setCategory("Tournament");
            e2.setCapacity(32);
            e2.setTicketPrice(400);
            e2.setDescription("A tactical showdown featuring Catan, 7 Wonders, and Wingspan with special prizes and handcrafted coffee.");
            e2.setStatus("upcoming");
            e2.setPlannedGames(Arrays.asList("g-catan", "g-splendor", "g-7wonders"));

            eventRepository.saveAll(Arrays.asList(e1, e2));
            System.out.println("✅ Seeded initial events into MongoDB");
        }
    }

    private void seedGames() {
        if (gameRepository.count() == 0) {
            Game g1 = new Game();
            g1.setId("g-catan");
            g1.setName("Catan (Settlers of Catan)");
            g1.setCategory("Strategy");
            g1.setDifficulty("Medium");
            g1.setPlayers("3-4 Players");
            g1.setDuration("60-90 mins");
            g1.setDescription("Collect resources, trade with fellow shufflers, build settlements and roads on the island of Catan.");
            g1.setActive(true);
            g1.setPlaysCount(48);

            Game g2 = new Game();
            g2.setId("g-avalon");
            g2.setName("The Resistance: Avalon");
            g2.setCategory("Social Deduction");
            g2.setDifficulty("Beginner");
            g2.setPlayers("5-10 Players");
            g2.setDuration("30-45 mins");
            g2.setDescription("Loyal servants of Arthur vs Minions of Mordred. Pure deception, table talk, and dramatic accusations.");
            g2.setActive(true);
            g2.setPlaysCount(62);

            Game g3 = new Game();
            g3.setId("g-codenames");
            g3.setName("Codenames");
            g3.setCategory("Party");
            g3.setDifficulty("Beginner");
            g3.setPlayers("4-8 Players");
            g3.setDuration("15-20 mins");
            g3.setDescription("Two rival spymasters know the secret identities of 25 agents. Teammates try to guess words based on one-word clues.");
            g3.setActive(true);
            g3.setPlaysCount(89);

            Game g4 = new Game();
            g4.setId("g-splendor");
            g4.setName("Splendor");
            g4.setCategory("Strategy");
            g4.setDifficulty("Beginner");
            g4.setPlayers("2-4 Players");
            g4.setDuration("30 mins");
            g4.setDescription("Renaissance merchants acquiring gemstone mines, transportation, and shops to attract aristocratic patrons.");
            g4.setActive(true);
            g4.setPlaysCount(35);

            gameRepository.saveAll(Arrays.asList(g1, g2, g3, g4));
            System.out.println("✅ Seeded initial games into MongoDB");
        }
    }

    private void seedParticipants() {
        if (participantRepository.count() == 0) {
            Participant p1 = new Participant("p-rohan", "Rohan Kulkarni", "rohan.kulkarni@example.com", "9822012345", "Koregaon Park");
            p1.setTotalEventsAttended(8);
            p1.setTotalRegistrations(9);
            p1.setGamesPlayedIds(Arrays.asList("g-catan", "g-avalon", "g-codenames"));
            p1.setVenuesVisited(Arrays.asList("The Rustle Nest Cafe", "Pagdandi Bookstore Cafe"));
            p1.setTotalPaxBrought(4);

            Participant p2 = new Participant("p-ananya", "Ananya Deshmukh", "ananya.d@example.com", "9823056789", "Baner");
            p2.setTotalEventsAttended(5);
            p2.setTotalRegistrations(5);
            p2.setGamesPlayedIds(Arrays.asList("g-codenames", "g-splendor"));
            p2.setVenuesVisited(Arrays.asList("Pagdandi Bookstore Cafe"));
            p2.setTotalPaxBrought(2);

            participantRepository.saveAll(Arrays.asList(p1, p2));
            System.out.println("✅ Seeded initial participants into MongoDB");
        }
    }

    private void seedRegistrations() {
        if (registrationRepository.count() == 0) {
            Registration r1 = new Registration();
            r1.setId("reg-1");
            r1.setEventId("evt-35");
            r1.setParticipantId("p-rohan");
            r1.setParticipantName("Rohan Kulkarni");
            r1.setParticipantEmail("rohan.kulkarni@example.com");
            r1.setParticipantPhone("9822012345");
            r1.setParticipantArea("Koregaon Park");
            r1.setPaxCount(2);
            r1.setGuests(Arrays.asList(new GuestInfo("g-1", "Tanvi Joshi")));
            r1.setPaymentStatus("Confirmed");
            r1.setAttendanceStatus("Pending");
            r1.setRegisteredAt("2026-09-20T10:30:00Z");

            registrationRepository.save(r1);
            System.out.println("✅ Seeded initial registrations into MongoDB");
        }
    }

    private void seedAuditAndNotifications() {
        if (auditLogRepository.count() == 0) {
            AuditLog log = new AuditLog("log-1", "Aman Joshi (Host)", "SYSTEM_INIT", "Database", "Initialized MongoDB schemas and seeded Pune community records", "2026-09-22T10:00:00Z");
            auditLogRepository.save(log);
        }

        if (notificationRepository.count() == 0) {
            NotificationItem notif = new NotificationItem("notif-1", "Social Shuffle #35 Live", "Meetup announced for Koregaon Park. Registrations are open!", "event", "2026-09-22T10:00:00Z", false);
            notificationRepository.save(notif);
        }
    }
}
