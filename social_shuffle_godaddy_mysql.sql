-- ==============================================================================
-- Social Shuffle Pune — Complete MySQL Database Schema & Seed Data
-- Specially formatted for GoDaddy cPanel MySQL & phpMyAdmin 1-Click Import
-- Compatible with MySQL 5.7 / 8.0+ and Spring Boot / Node.js Backends
-- ==============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `social_shuffle` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `social_shuffle`;

-- ------------------------------------------------------------------------------
-- 1. Table structure for table `admins`
-- Dedicated admin table for administrator role validation, permissions, and co-founders
-- ------------------------------------------------------------------------------
DROP TABLE IF EXISTS `admins`;
CREATE TABLE `admins` (
  `id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `email` VARCHAR(150) NOT NULL,
  `phone` VARCHAR(50) DEFAULT NULL,
  `role` VARCHAR(50) DEFAULT 'admin',
  `designation` VARCHAR(100) DEFAULT 'Community Lead & Founder',
  `avatar` TEXT DEFAULT NULL,
  `bio` TEXT DEFAULT NULL,
  `area` VARCHAR(255) DEFAULT 'Koregaon Park, Pune',
  `active` TINYINT(1) DEFAULT 1,
  `created_at` VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admins_email` (`email`),
  KEY `idx_admins_active` (`active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 2. Table structure for table `user_credentials`
-- Dedicated credentials vault storing salted PBKDF2-HMAC-SHA512 password hashes
-- ------------------------------------------------------------------------------
DROP TABLE IF EXISTS `user_credentials`;
CREATE TABLE `user_credentials` (
  `user_id` VARCHAR(64) NOT NULL,
  `subject_type` ENUM('admin', 'participant') NOT NULL,
  `subject_id` VARCHAR(64) NOT NULL,
  `email` VARCHAR(150) NOT NULL,
  `phone` VARCHAR(50) DEFAULT '',
  `username` VARCHAR(150) DEFAULT '',
  `password_hash` TEXT DEFAULT NULL,
  `auth_provider` VARCHAR(30) DEFAULT 'local',
  `failed_attempts` INT DEFAULT 0,
  `locked_until` BIGINT DEFAULT NULL,
  `last_login_at` VARCHAR(50) DEFAULT NULL,
  `password_updated_at` VARCHAR(50) DEFAULT NULL,
  `created_at` VARCHAR(50) NOT NULL,
  `updated_at` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_creds_email` (`email`),
  KEY `idx_creds_subject` (`subject_type`, `subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 3. Table structure for table `participants`
-- ------------------------------------------------------------------------------
DROP TABLE IF EXISTS `participants`;
CREATE TABLE `participants` (
  `id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(255) DEFAULT NULL,
  `email` VARCHAR(150) NOT NULL,
  `phone` VARCHAR(50) DEFAULT NULL,
  `username` VARCHAR(150) DEFAULT NULL,
  `area` VARCHAR(255) DEFAULT NULL,
  `avatar` TEXT DEFAULT NULL,
  `bio` TEXT DEFAULT NULL,
  `joined_date` VARCHAR(50) DEFAULT NULL,
  `total_events_attended` INT DEFAULT 0,
  `total_registrations` INT DEFAULT 0,
  `games_played_ids` TEXT DEFAULT NULL,
  `conquered_games` TEXT DEFAULT NULL,
  `venues_visited` TEXT DEFAULT NULL,
  `total_pax_brought` INT DEFAULT 0,
  `last_attended_event_id` VARCHAR(64) DEFAULT NULL,
  `last_attended_event_title` VARCHAR(255) DEFAULT NULL,
  `auth_provider` VARCHAR(30) DEFAULT 'local',
  `registered_at` VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_participants_email` (`email`),
  KEY `idx_participants_phone` (`phone`),
  KEY `idx_participants_area` (`area`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 4. Table structure for table `venues`
-- ------------------------------------------------------------------------------
DROP TABLE IF EXISTS `venues`;
CREATE TABLE `venues` (
  `id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `address` TEXT DEFAULT NULL,
  `area` VARCHAR(100) DEFAULT NULL,
  `capacity` INT DEFAULT 40,
  `contact_phone` VARCHAR(50) DEFAULT NULL,
  `maps_url` TEXT DEFAULT NULL,
  `image` TEXT DEFAULT NULL,
  `active` TINYINT(1) DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_venues_area` (`area`),
  KEY `idx_venues_active` (`active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 5. Table structure for table `events`
-- ------------------------------------------------------------------------------
DROP TABLE IF EXISTS `events`;
CREATE TABLE `events` (
  `id` VARCHAR(64) NOT NULL,
  `number` INT DEFAULT 0,
  `title` VARCHAR(255) DEFAULT NULL,
  `date` VARCHAR(50) DEFAULT NULL,
  `time` VARCHAR(100) DEFAULT NULL,
  `venue` VARCHAR(255) DEFAULT NULL,
  `address` TEXT DEFAULT NULL,
  `area` VARCHAR(255) DEFAULT NULL,
  `category` VARCHAR(100) DEFAULT NULL,
  `capacity` INT DEFAULT 30,
  `ticket_price` INT DEFAULT 350,
  `description` TEXT DEFAULT NULL,
  `notes` TEXT DEFAULT NULL,
  `planned_games` TEXT DEFAULT NULL,
  `status` VARCHAR(50) DEFAULT 'upcoming',
  `cover_image` TEXT DEFAULT NULL,
  `map_location` TEXT DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_events_date` (`date`),
  KEY `idx_events_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 6. Table structure for table `games`
-- ------------------------------------------------------------------------------
DROP TABLE IF EXISTS `games`;
CREATE TABLE `games` (
  `id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(255) DEFAULT NULL,
  `category` VARCHAR(100) DEFAULT NULL,
  `difficulty` VARCHAR(50) DEFAULT NULL,
  `players` VARCHAR(50) DEFAULT NULL,
  `duration` VARCHAR(50) DEFAULT NULL,
  `description` TEXT DEFAULT NULL,
  `active` TINYINT(1) DEFAULT 1,
  `plays_count` INT DEFAULT 0,
  `tags` TEXT DEFAULT NULL,
  `rating` DOUBLE DEFAULT 4.5,
  `image` TEXT DEFAULT NULL,
  `owner` VARCHAR(100) DEFAULT NULL,
  `available` TINYINT(1) DEFAULT 1,
  `current_location` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_games_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 7. Table structure for table `registrations`
-- ------------------------------------------------------------------------------
DROP TABLE IF EXISTS `registrations`;
CREATE TABLE `registrations` (
  `id` VARCHAR(64) NOT NULL,
  `event_id` VARCHAR(64) DEFAULT NULL,
  `participant_id` VARCHAR(64) DEFAULT NULL,
  `participant_name` VARCHAR(255) DEFAULT NULL,
  `participant_email` VARCHAR(150) DEFAULT NULL,
  `participant_phone` VARCHAR(50) DEFAULT NULL,
  `participant_area` VARCHAR(255) DEFAULT NULL,
  `pax_count` INT DEFAULT 1,
  `guests` TEXT DEFAULT NULL,
  `payment_status` VARCHAR(50) DEFAULT 'Pending',
  `payment_method` VARCHAR(50) DEFAULT 'UPI',
  `amount_paid` INT DEFAULT 350,
  `attendance_status` VARCHAR(50) DEFAULT 'Registered',
  `check_in_time` VARCHAR(50) DEFAULT NULL,
  `checked_in_at` VARCHAR(50) DEFAULT NULL,
  `games_played` TEXT DEFAULT NULL,
  `ticket_code` VARCHAR(100) DEFAULT NULL,
  `qr_code_token` VARCHAR(100) DEFAULT NULL,
  `qr_code_url` TEXT DEFAULT NULL,
  `razorpay_payment_id` VARCHAR(100) DEFAULT NULL,
  `razorpay_order_id` VARCHAR(100) DEFAULT NULL,
  `registered_at` VARCHAR(50) DEFAULT NULL,
  `notes` TEXT DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_regs_event` (`event_id`),
  KEY `idx_regs_participant` (`participant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 10. Table structure for table `feedback`
-- ------------------------------------------------------------------------------
DROP TABLE IF EXISTS `feedback`;
CREATE TABLE `feedback` (
  `id` VARCHAR(64) NOT NULL,
  `event_id` VARCHAR(64) DEFAULT NULL,
  `participant_name` VARCHAR(255) DEFAULT NULL,
  `rating` INT DEFAULT 5,
  `comments` TEXT DEFAULT NULL,
  `favorite_game` VARCHAR(255) DEFAULT NULL,
  `venue_rating` INT DEFAULT 5,
  `submitted_at` VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 11. Table structure for table `reports`
-- ------------------------------------------------------------------------------
DROP TABLE IF EXISTS `reports`;
CREATE TABLE `reports` (
  `id` VARCHAR(64) NOT NULL,
  `reporter_name` VARCHAR(255) DEFAULT NULL,
  `reported_participant` VARCHAR(255) DEFAULT NULL,
  `category` VARCHAR(100) DEFAULT NULL,
  `description` TEXT DEFAULT NULL,
  `event_id` VARCHAR(64) DEFAULT NULL,
  `status` VARCHAR(50) DEFAULT 'New',
  `resolution_notes` TEXT DEFAULT NULL,
  `created_at` VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 12. Table structure for table `volunteers`
-- ------------------------------------------------------------------------------
DROP TABLE IF EXISTS `volunteers`;
CREATE TABLE `volunteers` (
  `id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(255) DEFAULT NULL,
  `phone` VARCHAR(50) DEFAULT NULL,
  `email` VARCHAR(150) DEFAULT NULL,
  `experience` TEXT DEFAULT NULL,
  `preferred_games` TEXT DEFAULT NULL,
  `availability` VARCHAR(255) DEFAULT NULL,
  `status` VARCHAR(50) DEFAULT 'New',
  `submitted_at` VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 13. Table structure for table `audit_logs`
-- ------------------------------------------------------------------------------
DROP TABLE IF EXISTS `audit_logs`;
CREATE TABLE `audit_logs` (
  `id` VARCHAR(64) NOT NULL,
  `admin_name` VARCHAR(255) DEFAULT NULL,
  `action` VARCHAR(255) DEFAULT NULL,
  `target` VARCHAR(255) DEFAULT NULL,
  `details` TEXT DEFAULT NULL,
  `description` TEXT DEFAULT NULL,
  `timestamp` VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_audit_timestamp` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 14. Table structure for table `notifications`
-- Supports dual delivery (Email + WhatsApp) with automatic Email fallback
-- ------------------------------------------------------------------------------
DROP TABLE IF EXISTS `notifications`;
CREATE TABLE `notifications` (
  `id` VARCHAR(64) NOT NULL,
  `title` VARCHAR(255) DEFAULT NULL,
  `message` TEXT DEFAULT NULL,
  `type` VARCHAR(50) DEFAULT NULL,
  `channel` VARCHAR(50) DEFAULT 'dual',
  `recipient_email` VARCHAR(150) DEFAULT NULL,
  `recipient_phone` VARCHAR(50) DEFAULT NULL,
  `event_id` VARCHAR(64) DEFAULT NULL,
  `delivery_status` VARCHAR(50) DEFAULT 'Sent',
  `whatsapp_status` VARCHAR(100) DEFAULT NULL,
  `timestamp` VARCHAR(50) DEFAULT NULL,
  `is_read` TINYINT(1) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_notif_time` (`timestamp`),
  KEY `idx_notif_event` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 15. Table structure for table `participant_conquered_games`
-- Normalized Personal Board Gaming Passport game log entries
-- ------------------------------------------------------------------------------
DROP TABLE IF EXISTS `participant_conquered_games`;
CREATE TABLE `participant_conquered_games` (
  `id` VARCHAR(64) NOT NULL,
  `participant_id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `normalized_name` VARCHAR(255) NOT NULL,
  `genre` VARCHAR(100) DEFAULT 'Strategy',
  `date_played` VARCHAR(50) DEFAULT NULL,
  `notes` TEXT DEFAULT NULL,
  `library_game_id` VARCHAR(64) DEFAULT NULL,
  `source` VARCHAR(40) DEFAULT 'outside',
  `created_at` VARCHAR(50) DEFAULT NULL,
  `updated_at` VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_pcg_participant` (`participant_id`),
  KEY `idx_pcg_library_game` (`library_game_id`),
  KEY `idx_pcg_genre` (`genre`),
  KEY `idx_pcg_source` (`source`),
  KEY `idx_pcg_norm_name` (`normalized_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 16. Table structure for table `board_game_catalog`
-- Daily-synced ranked board game auto-suggestion catalog & verified specs
-- ------------------------------------------------------------------------------
DROP TABLE IF EXISTS `board_game_catalog`;
CREATE TABLE `board_game_catalog` (
  `id` VARCHAR(64) NOT NULL,
  `external_id` VARCHAR(64) DEFAULT NULL,
  `name` VARCHAR(255) NOT NULL,
  `normalized_name` VARCHAR(255) NOT NULL,
  `genre` VARCHAR(100) DEFAULT 'Strategy',
  `players` VARCHAR(64) DEFAULT '2-4 Players',
  `duration` VARCHAR(64) DEFAULT '45-60 mins',
  `bgg_rank` INT DEFAULT 99999,
  `has_verified_specs` TINYINT(1) DEFAULT 0,
  `snapshot_date` VARCHAR(32) DEFAULT NULL,
  `updated_at` VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_bgc_norm_name` (`normalized_name`),
  KEY `idx_bgc_external_id` (`external_id`),
  KEY `idx_bgc_rank` (`bgg_rank`),
  KEY `idx_bgc_snapshot_date` (`snapshot_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- INITIAL SEED DATA (FETCHED DIRECTLY FROM DATABASE TABLES ONLY)
-- ==============================================================================

-- 1. Seed `admins` (credentials managed separately in `user_credentials` with salted PBKDF2 hashes)
INSERT INTO `admins` (`id`, `name`, `email`, `phone`, `role`, `designation`, `avatar`, `bio`, `area`, `active`, `created_at`) VALUES
('u-admin-parimal', 'Parimal Shete', 'parimalmshete@gmail.com', '+91 92849 83072', 'admin', 'Co-Founder & Community Architect', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=200', 'Social Shuffle Co-Founder & Community Architect', 'Koregaon Park, Pune', 1, '2024-01-01T00:00:00Z'),
('u-admin-diksha', 'Diksha', 'birladiksha01@gmail.com', '+91 92849 83072', 'admin', 'Co-Founder & Experience Director', 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=200', 'Social Shuffle Co-Founder & Experience Director', 'Kalyani Nagar, Pune', 1, '2024-01-01T00:00:00Z')
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `email`=VALUES(`email`), `phone`=VALUES(`phone`), `role`=VALUES(`role`), `designation`=VALUES(`designation`), `area`=VALUES(`area`), `active`=VALUES(`active`);

-- 2. `participants` & `user_credentials` (hashed credentials initialized by backend auth engine)

-- 4. Seed `venues`
INSERT INTO `venues` (`id`, `name`, `address`, `area`, `capacity`, `contact_phone`, `maps_url`, `image`, `active`) VALUES
('ven-1', 'The Rustle Nest Cafe', 'Lane 6, Koregaon Park, Pune, Maharashtra 411001', 'Koregaon Park', 45, '+91 92849 83072', 'https://maps.google.com/?q=The+Rustle+Nest+Cafe+Pune', 'https://images.unsplash.com/photo-1554118811-1e0d58224f24?auto=format&fit=crop&q=80&w=800', 1),
('ven-2', 'Pagdandi Bookstore Cafe', 'Shop 6, Regent Plaza, Baner - Pashan Link Rd, Pune 411045', 'Baner', 35, '+91 98220 44556', 'https://maps.google.com/?q=Pagdandi+Bookstore+Cafe+Baner', 'https://images.unsplash.com/photo-1521017432531-fbd92d768814?auto=format&fit=crop&q=80&w=800', 1),
('ven-3', 'Waari Book Cafe', 'Ground Floor, Choice Institute, Kothrud, Pune 411038', 'Kothrud', 50, '+91 98220 77889', 'https://maps.google.com/?q=Waari+Book+Cafe+Kothrud', 'https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&q=80&w=800', 1);

-- 5. Seed `events`
INSERT INTO `events` (`id`, `number`, `title`, `date`, `time`, `venue`, `address`, `area`, `category`, `capacity`, `ticket_price`, `description`, `notes`, `planned_games`, `status`, `cover_image`) VALUES
('evt-35', 35, 'Social Shuffle #35: Weekend Dice & Strategy', '2026-09-27', '4:00 PM - 8:00 PM', 'The Rustle Nest Cafe', 'Lane 6, Koregaon Park, Pune, Maharashtra 411001', 'Koregaon Park', 'Casual & Social', 30, 350, 'Join us for our signature weekend board gaming mixer! Over 40+ curated board games, warm coffee, and friendly table hosts to teach you the rules in under 5 minutes.', 'Wear comfortable clothing. Welcome tea/coffee included with your registration ticket.', '[\"g-catan\",\"g-avalon\",\"g-codenames\",\"g-splendor\"]', 'upcoming', 'https://images.unsplash.com/photo-1610890716171-6b1bb98ffd09?auto=format&fit=crop&q=80&w=800'),
('evt-36', 36, 'Social Shuffle #36: Tactical Tournament', '2026-10-04', '3:30 PM - 8:30 PM', 'Pagdandi Bookstore Cafe', 'Shop 6, Regent Plaza, Baner - Pashan Link Rd, Pune 411045', 'Baner', 'Tournament', 32, 400, 'A tactical showdown featuring Catan, 7 Wonders, and Wingspan with special prizes and handcrafted coffee.', 'Pre-registration mandatory. Tables limited to 32 players.', '[\"g-catan\",\"g-splendor\",\"g-7wonders\"]', 'upcoming', 'https://images.unsplash.com/photo-1563941402622-4e7a488bcc57?auto=format&fit=crop&q=80&w=800');

-- 6. Seed `games`
INSERT INTO `games` (`id`, `name`, `category`, `difficulty`, `players`, `duration`, `description`, `active`, `plays_count`, `tags`, `rating`, `image`, `owner`, `available`, `current_location`) VALUES
('g-catan', 'Catan (Settlers of Catan)', 'Strategy', 'Medium', '3-4 Players', '60-90 mins', 'Collect resources, trade with fellow shufflers, build settlements and roads on the island of Catan.', 1, 48, '[\"trading\",\"negotiation\",\"dice\",\"classic\"]', 4.8, 'https://images.unsplash.com/photo-1610890716171-6b1bb98ffd09?auto=format&fit=crop&q=80&w=400', 'Community Library', 1, 'The Rustle Nest Cafe'),
('g-avalon', 'The Resistance: Avalon', 'Social Deduction', 'Beginner', '5-10 Players', '30-45 mins', 'Loyal servants of Arthur vs Minions of Mordred. Pure deception, table talk, and dramatic accusations.', 1, 62, '[\"bluffing\",\"party\",\"hidden-role\"]', 4.9, 'https://images.unsplash.com/photo-1543083477-4f785aeafaa9?auto=format&fit=crop&q=80&w=400', 'Community Library', 1, 'The Rustle Nest Cafe'),
('g-codenames', 'Codenames', 'Party', 'Beginner', '4-8 Players', '15-20 mins', 'Two rival spymasters know the secret identities of 25 agents. Teammates try to guess words based on one-word clues.', 1, 89, '[\"word\",\"deduction\",\"icebreaker\"]', 4.8, 'https://images.unsplash.com/photo-1563941402622-4e7a488bcc57?auto=format&fit=crop&q=80&w=400', 'Community Library', 1, 'Pagdandi Bookstore Cafe'),
('g-splendor', 'Splendor', 'Strategy', 'Beginner', '2-4 Players', '30 mins', 'Renaissance merchants acquiring gemstone mines, transportation, and shops to attract aristocratic patrons.', 1, 35, '[\"engine-building\",\"chips\",\"cards\"]', 4.7, 'https://images.unsplash.com/photo-1585504198199-20277593b94f?auto=format&fit=crop&q=80&w=400', 'Community Library', 1, 'Pagdandi Bookstore Cafe');

-- 7. `registrations` (Clean initial state: recorded via event checkouts)

-- 8. `feedback` (Clean initial state: submitted by event attendees)

-- 9. Seed `audit_logs`
INSERT INTO `audit_logs` (`id`, `admin_name`, `action`, `target`, `details`, `description`, `timestamp`) VALUES
('log-1', 'Parimal Shete', 'SECURITY_INIT', 'Database', 'All domain entities (admins, venues, events, games, passport, catalog) persisted directly in database', 'Anti-phishing and direct database query baseline active', '2026-09-29T12:00:00Z');

-- 12. Seed `notifications`
INSERT INTO `notifications` (`id`, `title`, `message`, `type`, `channel`, `recipient_email`, `recipient_phone`, `event_id`, `delivery_status`, `whatsapp_status`, `timestamp`, `is_read`) VALUES
('notif-1', 'Social Shuffle #35 Live', 'Meetup announced for Koregaon Park. Registrations are open!', 'event', 'dual', 'birladiksha01@gmail.com', '+91 92849 83072', 'evt-35', 'Sent', 'Delivered', '2026-09-29T12:00:00Z', 0);

SET FOREIGN_KEY_CHECKS = 1;
-- ==============================================================================
-- End of Social Shuffle MySQL Script
-- ==============================================================================


-- ------------------------------------------------------------------------------
-- Seed Participants Table with Real Pune Shuffler Profiles (credentials in user_credentials)
-- ------------------------------------------------------------------------------
INSERT INTO `participants` (`id`, `name`, `email`, `phone`, `username`, `area`, `avatar`, `bio`, `joined_date`, `total_events_attended`, `total_registrations`, `games_played_ids`, `venues_visited`, `total_pax_brought`, `last_attended_event_id`, `last_attended_event_title`) VALUES
  ('p-varad', 'Varad', 'varad@socialshuffle.in', '+91 98221 12345', 'varad', 'Koregaon Park', 'https://api.dicebear.com/7.x/bottts/svg?seed=Varad', 'Pune board game enthusiast • Catan & Social Deduction lover!', '2026-11-11', 0, 0, '[]', '[]', 0, NULL, NULL),
  ('p-mw97kkb5', 'Aman Sharma', 'aman.sharma@example.com', '9822099112', 'aman.sharma', 'Baner', 'https://api.dicebear.com/7.x/bottts/svg?seed=Aman%20Sharma', 'Board game lover eager to play!', '2026-11-09', 0, 0, '[]', '[]', 0, NULL, NULL),
  ('p-priya', 'Priya Nair', 'priya.nair@example.com', '+91 97632 99011', 'priya.nair', 'Wakad', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=160&q=80', 'Weekly regular shuffler. Always looking for teammates for Just One & Wingspan!', '2024-01-15', 6, 7, '["g-codenames","g-justone","g-azul","g-wingspan","g-dixit","g-cascadia"]', '["Toit Brewery","FC Road Social","Waari Book Cafe"]', 3, 'ev-ss3', 'Secret Social & Mystery Games'),
  ('p-rohan', 'Rohan Deshmukh', 'rohan.deshmukh@example.com', '+91 98223 44551', 'rohan.deshmukh', 'Koregaon Park', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=160&q=80', 'Heavy strategy enthusiast. Terraforming Mars, Dune Imperium & Scythe fan.', '2024-01-05', 11, 12, '["g-dune","g-terraformingmars","g-scythe","g-brass","g-root"]', '["Toit Brewery","FC Road Social","Effingut KP","Waari Book Cafe"]', 8, 'ev-ss3', 'Secret Social & Mystery Games'),
  ('p-ananya', 'Ananya Kulkarni', 'ananya.k@example.com', '+91 98901 22334', 'ananya.k', 'Baner', 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?auto=format&fit=crop&w=160&q=80', 'Designer by day, social deduction master by night. Secret Hitler champ.', '2024-01-20', 8, 8, '["g-secrethitler","g-avalon","g-bloodontheclocktower","g-decrypto","g-wavelength"]', '["Toit Brewery","Waari Book Cafe"]', 5, 'ev-ss3', 'Secret Social & Mystery Games'),
  ('p-tanmay', 'Tanmay Joshi', 'tanmay.j@example.com', '+91 98221 88990', 'tanmay.j', 'Viman Nagar', 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&w=160&q=80', 'Love learning new modern European games and meeting new people over coffee.', '2024-02-01', 5, 6, '["g-catan","g-ticketoride","g-carcassonne","g-splendor","g-cascadia"]', '["FC Road Social","Toit Brewery"]', 2, 'ev-ss2', 'Strategy & Heavyweight Battles'),
  ('p-sneha', 'Sneha Patil', 'sneha.patil@example.com', '+91 97665 11223', 'sneha.patil', 'Aundh', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=160&q=80', 'Board game collector and casual teacher. Let us play Cascadia and Sushi Go!', '2024-02-14', 4, 4, '["g-cascadia","g-sushigo","g-azul","g-flip7","g-dixit"]', '["Waari Book Cafe","FC Road Social"]', 1, 'ev-ss3', 'Secret Social & Mystery Games'),
  ('p-aditya', 'Aditya Shinde', 'aditya.shinde@example.com', '+91 98220 33445', 'aditya.shinde', 'Kothrud', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=160&q=80', 'Tournament competitor. Competitive Spirit Island and Blood on the Clocktower storyteller.', '2024-01-10', 9, 10, '["g-spiritisland","g-bloodontheclocktower","g-dune","g-root","g-clank"]', '["Toit Brewery","FC Road Social","Effingut KP"]', 6, 'ev-ss3', 'Secret Social & Mystery Games');
