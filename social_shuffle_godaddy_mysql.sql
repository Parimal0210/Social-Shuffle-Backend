-- ==============================================================================
-- Social Shuffle Pune — Complete MySQL Database Schema & Security Hardening
-- Specially formatted for GoDaddy cPanel MySQL & phpMyAdmin 1-Click Import
-- ==============================================================================
--
-- ------------------------------------------------------------------------------
-- DATABASE ACCESS SECURITY (ZERO DIRECT PUBLIC ACCESS)
-- ------------------------------------------------------------------------------
-- 1. NEVER EXPOSE PORT 3306 TO THE PUBLIC INTERNET:
--    Ensure your MySQL server config has:
--      [mysqld]
--      bind-address = 127.0.0.1
--    In GoDaddy cPanel: Do NOT add any IPs under "Remote MySQL". This ensures
--    only local backend applications (Spring Boot / Express) can connect.
--
-- 2. CREATE A DEDICATED RESTRICTED APPLICATION USER (LEAST PRIVILEGE):
--    Never connect to MySQL using the 'root' account from application code.
--    Run the following in phpMyAdmin / MySQL shell:
--
--      CREATE USER IF NOT EXISTS 'social_app'@'localhost' IDENTIFIED BY 'StrongRandomPass2026!';
--      GRANT SELECT, INSERT, UPDATE, DELETE ON `social_shuffle`.* TO 'social_app'@'localhost';
--      FLUSH PRIVILEGES;
--
-- ------------------------------------------------------------------------------

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- Database creation
CREATE DATABASE IF NOT EXISTS `social_shuffle` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `social_shuffle`;

-- ------------------------------------------------------------------------------
-- 0. Table structure for table `admins`
-- (Referred for Administrator authentication, role validation, and host name)
-- Public registration NEVER writes to this table.
-- ------------------------------------------------------------------------------
DROP TABLE IF EXISTS `admins`;
CREATE TABLE `admins` (
  `id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `email` VARCHAR(150) NOT NULL,
  `phone` VARCHAR(50) DEFAULT NULL,
  `password` VARCHAR(255) NOT NULL,
  `role` VARCHAR(50) DEFAULT 'admin',
  `designation` VARCHAR(100) DEFAULT 'Community Lead & Founder',
  `avatar` TEXT DEFAULT NULL,
  `bio` TEXT DEFAULT NULL,
  `active` TINYINT(1) DEFAULT 1,
  `created_at` VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admins_email` (`email`),
  KEY `idx_admins_active` (`active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 1. Table structure for table `users`
-- ------------------------------------------------------------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(255) DEFAULT NULL,
  `email` VARCHAR(150) NOT NULL,
  `phone` VARCHAR(50) DEFAULT NULL,
  `password` VARCHAR(255) DEFAULT NULL,
  `role` VARCHAR(30) DEFAULT 'participant',
  `area` VARCHAR(255) DEFAULT NULL,
  `avatar` TEXT DEFAULT NULL,
  `bio` TEXT DEFAULT NULL,
  `participant_id` VARCHAR(64) DEFAULT NULL,
  `auth_provider` VARCHAR(30) DEFAULT 'local',
  `last_active_at` VARCHAR(50) DEFAULT NULL,
  `login_at` VARCHAR(50) DEFAULT NULL,
  `created_at` VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_email` (`email`),
  KEY `idx_users_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 2. Table structure for table `participants`
-- ------------------------------------------------------------------------------
DROP TABLE IF EXISTS `participants`;
CREATE TABLE `participants` (
  `id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(255) DEFAULT NULL,
  `email` VARCHAR(150) NOT NULL,
  `phone` VARCHAR(50) DEFAULT NULL,
  `area` VARCHAR(255) DEFAULT NULL,
  `avatar` TEXT DEFAULT NULL,
  `bio` TEXT DEFAULT NULL,
  `joined_date` VARCHAR(50) DEFAULT NULL,
  `total_events_attended` INT DEFAULT 0,
  `total_registrations` INT DEFAULT 0,
  `games_played_ids` TEXT DEFAULT NULL,
  `venues_visited` TEXT DEFAULT NULL,
  `total_pax_brought` INT DEFAULT 0,
  `last_attended_event_id` VARCHAR(64) DEFAULT NULL,
  `last_attended_event_title` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_participants_email` (`email`),
  KEY `idx_participants_phone` (`phone`),
  KEY `idx_participants_area` (`area`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 3. Table structure for table `events`
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
  PRIMARY KEY (`id`),
  KEY `idx_events_date` (`date`),
  KEY `idx_events_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 4. Table structure for table `games`
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
-- 5. Table structure for table `registrations`
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
  `ticket_code` VARCHAR(100) DEFAULT NULL,
  `registered_at` VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_regs_event` (`event_id`),
  KEY `idx_regs_participant` (`participant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 6. Table structure for table `feedback`
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
-- 7. Table structure for table `reports`
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
-- 8. Table structure for table `volunteers`
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
-- 9. Table structure for table `audit_logs`
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
-- 10. Table structure for table `notifications`
-- ------------------------------------------------------------------------------
DROP TABLE IF EXISTS `notifications`;
CREATE TABLE `notifications` (
  `id` VARCHAR(64) NOT NULL,
  `title` VARCHAR(255) DEFAULT NULL,
  `message` TEXT DEFAULT NULL,
  `type` VARCHAR(50) DEFAULT NULL,
  `timestamp` VARCHAR(50) DEFAULT NULL,
  `is_read` TINYINT(1) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_notif_time` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- INITIAL SEED DATA
-- ==============================================================================

-- 1. Seed Dedicated Admin Table (`admins`)
-- Refers to the database table with admin details like name:
INSERT INTO `admins` (`id`, `name`, `email`, `phone`, `password`, `role`, `designation`, `avatar`, `bio`, `active`, `created_at`) VALUES
('u-admin', 'Aman Joshi (Host)', 'admin@socialshuffle.com', '+91 98220 11223', 'admin123', 'admin', 'Community Lead & Founder', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=200', 'Social Shuffle Founder & Super Admin. Refers to database table for name and admin details.', 1, '2024-01-01T00:00:00Z');

-- 2. Seed `users`
INSERT INTO `users` (`id`, `name`, `email`, `phone`, `password`, `role`, `area`, `avatar`, `bio`, `participant_id`, `auth_provider`, `last_active_at`, `login_at`, `created_at`) VALUES
('u-admin', 'Aman Joshi (Host)', 'admin@socialshuffle.com', '+91 98220 11223', 'admin123', 'admin', 'Pune', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=200', 'Host & Organizer at Social Shuffle Pune', NULL, 'local', '2026-09-29T12:00:00Z', '2026-09-29T12:00:00Z', '2024-01-01T00:00:00Z'),
('u-parimal', 'Parimal Shete', 'parimalmshete@gmail.com', '+91 98220 00000', 'oauth_google', 'participant', 'Koregaon Park', 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=200', 'Pune board gamer • Joined via Google Account', 'p-parimal', 'google', '2026-09-29T12:00:00Z', '2026-09-29T12:00:00Z', '2026-09-23T12:00:00Z'),
('u-rohan', 'Rohan Kulkarni', 'rohan.kulkarni@example.com', '+91 98220 12345', 'shuffler123', 'participant', 'Koregaon Park', 'https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?auto=format&fit=crop&q=80&w=200', 'Board game lover, strategy nerd, Catan enthusiast.', 'p-rohan', 'local', '2026-09-29T12:00:00Z', '2026-09-29T12:00:00Z', '2024-01-15T00:00:00Z'),
('u-ananya', 'Ananya Deshmukh', 'ananya.d@example.com', '+91 98230 56789', 'shuffler123', 'participant', 'Baner', 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=200', 'Codenames spymaster and social deduction aficionado.', 'p-ananya', 'local', '2026-09-29T12:00:00Z', '2026-09-29T12:00:00Z', '2024-02-01T00:00:00Z');

-- 3. Seed `participants`
INSERT INTO `participants` (`id`, `name`, `email`, `phone`, `area`, `avatar`, `bio`, `joined_date`, `total_events_attended`, `total_registrations`, `games_played_ids`, `venues_visited`, `total_pax_brought`, `last_attended_event_id`, `last_attended_event_title`) VALUES
('p-parimal', 'Parimal Shete', 'parimalmshete@gmail.com', '+91 98220 00000', 'Koregaon Park', 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=200', 'Pune board gamer • Joined via Google Account', '2026-09-23', 0, 0, '[]', '[]', 0, NULL, NULL),
('p-rohan', 'Rohan Kulkarni', 'rohan.kulkarni@example.com', '9822012345', 'Koregaon Park', 'https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?auto=format&fit=crop&q=80&w=200', 'Board game lover, strategy nerd, Catan enthusiast.', '2024-01-15', 8, 9, '[\"g-catan\",\"g-avalon\",\"g-codenames\"]', '[\"The Rustle Nest Cafe\",\"Pagdandi Bookstore Cafe\"]', 4, 'evt-34', 'Social Shuffle #34 - Strategy Showdown'),
('p-ananya', 'Ananya Deshmukh', 'ananya.d@example.com', '9823056789', 'Baner', 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=200', 'Codenames spymaster and social deduction aficionado.', '2024-02-01', 5, 5, '[\"g-codenames\",\"g-splendor\"]', '[\"Pagdandi Bookstore Cafe\"]', 2, 'evt-34', 'Social Shuffle #34 - Strategy Showdown');

-- 4. Seed `events`
INSERT INTO `events` (`id`, `number`, `title`, `date`, `time`, `venue`, `address`, `area`, `category`, `capacity`, `ticket_price`, `description`, `notes`, `planned_games`, `status`, `cover_image`) VALUES
('evt-35', 35, 'Social Shuffle #35: Weekend Dice & Strategy', '2026-09-27', '4:00 PM - 8:00 PM', 'The Rustle Nest Cafe', 'Lane 6, Koregaon Park, Pune, Maharashtra 411001', 'Koregaon Park', 'Casual & Social', 30, 350, 'Join us for our signature weekend board gaming mixer! Over 40+ curated board games, warm coffee, and friendly table hosts to teach you the rules in under 5 minutes.', 'Wear comfortable clothing. Welcome tea/coffee included with your registration ticket.', '[\"g-catan\",\"g-avalon\",\"g-codenames\",\"g-splendor\"]', 'upcoming', 'https://images.unsplash.com/photo-1610890716171-6b1bb98ffd09?auto=format&fit=crop&q=80&w=800'),
('evt-36', 36, 'Social Shuffle #36: Tactical Tournament', '2026-10-04', '3:30 PM - 8:30 PM', 'Pagdandi Bookstore Cafe', 'Shop 6, Regent Plaza, Baner - Pashan Link Rd, Pune 411045', 'Baner', 'Tournament', 32, 400, 'A tactical showdown featuring Catan, 7 Wonders, and Wingspan with special prizes and handcrafted coffee.', 'Pre-registration mandatory. Tables limited to 32 players.', '[\"g-catan\",\"g-splendor\",\"g-7wonders\"]', 'upcoming', 'https://images.unsplash.com/photo-1563941402622-4e7a488bcc57?auto=format&fit=crop&q=80&w=800');

-- 5. Seed `games`
INSERT INTO `games` (`id`, `name`, `category`, `difficulty`, `players`, `duration`, `description`, `active`, `plays_count`, `tags`, `rating`, `image`) VALUES
('g-catan', 'Catan (Settlers of Catan)', 'Strategy', 'Medium', '3-4 Players', '60-90 mins', 'Collect resources, trade with fellow shufflers, build settlements and roads on the island of Catan.', 1, 48, '[\"trading\",\"negotiation\",\"dice\",\"classic\"]', 4.8, 'https://images.unsplash.com/photo-1610890716171-6b1bb98ffd09?auto=format&fit=crop&q=80&w=400'),
('g-avalon', 'The Resistance: Avalon', 'Social Deduction', 'Beginner', '5-10 Players', '30-45 mins', 'Loyal servants of Arthur vs Minions of Mordred. Pure deception, table talk, and dramatic accusations.', 1, 62, '[\"bluffing\",\"party\",\"hidden-role\"]', 4.9, 'https://images.unsplash.com/photo-1543083477-4f785aeafaa9?auto=format&fit=crop&q=80&w=400'),
('g-codenames', 'Codenames', 'Party', 'Beginner', '4-8 Players', '15-20 mins', 'Two rival spymasters know the secret identities of 25 agents. Teammates try to guess words based on one-word clues.', 1, 89, '[\"word\",\"deduction\",\"icebreaker\"]', 4.8, 'https://images.unsplash.com/photo-1563941402622-4e7a488bcc57?auto=format&fit=crop&q=80&w=400'),
('g-splendor', 'Splendor', 'Strategy', 'Beginner', '2-4 Players', '30 mins', 'Renaissance merchants acquiring gemstone mines, transportation, and shops to attract aristocratic patrons.', 1, 35, '[\"engine-building\",\"chips\",\"cards\"]', 4.7, 'https://images.unsplash.com/photo-1585504198199-20277593b94f?auto=format&fit=crop&q=80&w=400');

-- 6. Seed `registrations`
INSERT INTO `registrations` (`id`, `event_id`, `participant_id`, `participant_name`, `participant_email`, `participant_phone`, `participant_area`, `pax_count`, `guests`, `payment_status`, `attendance_status`, `check_in_time`, `registered_at`) VALUES
('reg-1', 'evt-35', 'p-rohan', 'Rohan Kulkarni', 'rohan.kulkarni@example.com', '9822012345', 'Koregaon Park', 2, '[{\"id\":\"g-1\",\"name\":\"Tanvi Joshi\",\"email\":\"tanvi@example.com\",\"phone\":\"9822099999\",\"area\":\"Koregaon Park\"}]', 'Confirmed', 'Pending', NULL, '2026-09-20T10:30:00Z');

-- 7. Seed `audit_logs`
INSERT INTO `audit_logs` (`id`, `admin_name`, `action`, `target`, `details`, `description`, `timestamp`) VALUES
('log-1', 'Aman Joshi (Host)', 'SECURITY_INIT', 'Database', 'Hardened direct access, enabled PBKDF2 hashing, and isolated Admin table', 'Anti-phishing and anti-hacking baseline active', '2026-09-29T12:00:00Z');

-- 8. Seed `notifications`
INSERT INTO `notifications` (`id`, `title`, `message`, `type`, `timestamp`, `is_read`) VALUES
('notif-1', 'Social Shuffle #35 Live', 'Meetup announced for Koregaon Park. Registrations are open!', 'event', '2026-09-29T12:00:00Z', 0);

SET FOREIGN_KEY_CHECKS = 1;
-- ==============================================================================
-- End of Social Shuffle Hardened MySQL Script
-- ==============================================================================
