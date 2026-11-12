-- ==============================================================================
-- Social Shuffle Pune — Complete PostgreSQL Database Schema & Seed Data
-- Specially formatted for Supabase PostgreSQL & PostgreSQL 15/16/17
-- Compatible with Spring Boot 3.x / Spring Data JPA Hibernate PostgreSQL Dialect
-- All domain entities are strictly persisted and queried directly from database tables
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. Table structure for table: admins
-- Dedicated admin table for administrator role validation, permissions, and co-founders
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS admins (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(50),
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) DEFAULT 'admin',
    designation VARCHAR(100) DEFAULT 'Community Lead & Founder',
    avatar TEXT,
    bio TEXT,
    active BOOLEAN DEFAULT TRUE,
    created_at VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_admins_active ON admins (active);
CREATE INDEX IF NOT EXISTS idx_admins_email ON admins (email);

-- ------------------------------------------------------------------------------
-- 2. Table structure for table: users
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(50),
    password VARCHAR(255),
    role VARCHAR(30) DEFAULT 'participant',
    area VARCHAR(255),
    avatar TEXT,
    bio TEXT,
    participant_id VARCHAR(64),
    auth_provider VARCHAR(30) DEFAULT 'local',
    last_active_at VARCHAR(50),
    login_at VARCHAR(50),
    created_at VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_users_role ON users (role);
CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_participant_id ON users (participant_id);

-- ------------------------------------------------------------------------------
-- 3. Table structure for table: participants
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS participants (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(50),
    password VARCHAR(255) DEFAULT 'shuffler123',
    username VARCHAR(150),
    area VARCHAR(255),
    avatar TEXT,
    bio TEXT,
    joined_date VARCHAR(50),
    total_events_attended INTEGER DEFAULT 0,
    total_registrations INTEGER DEFAULT 0,
    games_played_ids TEXT,
    venues_visited TEXT,
    total_pax_brought INTEGER DEFAULT 0,
    last_attended_event_id VARCHAR(64),
    last_attended_event_title VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_participants_phone ON participants (phone);
CREATE INDEX IF NOT EXISTS idx_participants_area ON participants (area);
CREATE INDEX IF NOT EXISTS idx_participants_email ON participants (email);

-- ------------------------------------------------------------------------------
-- 4. Table structure for table: venues
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS venues (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address TEXT,
    area VARCHAR(100),
    capacity INTEGER DEFAULT 40,
    contact_phone VARCHAR(50),
    maps_url TEXT,
    image TEXT,
    active BOOLEAN DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_venues_area ON venues (area);
CREATE INDEX IF NOT EXISTS idx_venues_active ON venues (active);

-- ------------------------------------------------------------------------------
-- 5. Table structure for table: events
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS events (
    id VARCHAR(64) PRIMARY KEY,
    number INTEGER DEFAULT 0,
    title VARCHAR(255),
    date VARCHAR(50),
    "time" VARCHAR(50),
    venue VARCHAR(255),
    address TEXT,
    area VARCHAR(255),
    category VARCHAR(100),
    capacity INTEGER DEFAULT 30,
    ticket_price DOUBLE PRECISION DEFAULT 350.0,
    description TEXT,
    notes TEXT,
    planned_games TEXT,
    status VARCHAR(50) DEFAULT 'upcoming',
    cover_image TEXT
);

CREATE INDEX IF NOT EXISTS idx_events_date ON events (date);
CREATE INDEX IF NOT EXISTS idx_events_status ON events (status);

-- ------------------------------------------------------------------------------
-- 6. Table structure for table: games
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS games (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255),
    category VARCHAR(100),
    difficulty VARCHAR(50),
    players VARCHAR(50),
    duration VARCHAR(50),
    description TEXT,
    active BOOLEAN DEFAULT TRUE,
    plays_count INTEGER DEFAULT 0,
    tags TEXT,
    rating DOUBLE PRECISION DEFAULT 4.5,
    image TEXT,
    owner VARCHAR(100),
    venue VARCHAR(255),
    available BOOLEAN DEFAULT TRUE,
    current_location VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_games_category ON games (category);
CREATE INDEX IF NOT EXISTS idx_games_active ON games (active);

-- ------------------------------------------------------------------------------
-- 7. Table structure for table: badge_levels
-- Stores the 10 badge progression tiers
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS badge_levels (
    level INTEGER PRIMARY KEY,
    short_title VARCHAR(100) NOT NULL,
    fancy_name VARCHAR(150),
    subtitle VARCHAR(200),
    description TEXT,
    icon VARCHAR(20),
    min_game_percentage INTEGER DEFAULT 0,
    min_events_attended INTEGER DEFAULT 0,
    rarity VARCHAR(50) DEFAULT 'Common',
    gradient VARCHAR(255),
    border_color VARCHAR(100),
    text_color VARCHAR(100),
    badge_tag_color VARCHAR(150),
    glow_effect VARCHAR(100),
    perks TEXT
);

-- ------------------------------------------------------------------------------
-- 8. Table structure for table: participant_badges
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS participant_badges (
    id VARCHAR(64) PRIMARY KEY,
    participant_id VARCHAR(64) NOT NULL,
    badge_level INTEGER NOT NULL,
    badge_name VARCHAR(100),
    badge_icon VARCHAR(20),
    earned_at VARCHAR(50),
    is_current BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_pbadges_part ON participant_badges (participant_id);
CREATE INDEX IF NOT EXISTS idx_pbadges_lvl ON participant_badges (badge_level);

-- ------------------------------------------------------------------------------
-- 9. Table structure for table: registrations
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS registrations (
    id VARCHAR(64) PRIMARY KEY,
    event_id VARCHAR(64),
    participant_id VARCHAR(64),
    participant_name VARCHAR(255),
    participant_email VARCHAR(150),
    participant_phone VARCHAR(50),
    participant_area VARCHAR(255),
    pax_count INTEGER DEFAULT 1,
    guests TEXT,
    payment_status VARCHAR(50) DEFAULT 'Pending',
    payment_method VARCHAR(50) DEFAULT 'UPI',
    amount_paid DOUBLE PRECISION DEFAULT 350.0,
    attendance_status VARCHAR(50) DEFAULT 'Registered',
    check_in_time VARCHAR(50),
    ticket_code VARCHAR(100),
    registered_at VARCHAR(50),
    qr_code_token VARCHAR(100),
    email_sent BOOLEAN DEFAULT FALSE,
    email_sent_at VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_registrations_event_id ON registrations (event_id);
CREATE INDEX IF NOT EXISTS idx_registrations_participant_id ON registrations (participant_id);

-- ------------------------------------------------------------------------------
-- 10. Table structure for table: feedback
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS feedback (
    id VARCHAR(64) PRIMARY KEY,
    event_id VARCHAR(64),
    participant_name VARCHAR(255),
    rating INTEGER DEFAULT 5,
    comments TEXT,
    favorite_game VARCHAR(255),
    venue_rating INTEGER DEFAULT 5,
    submitted_at VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_feedback_event_id ON feedback (event_id);

-- ------------------------------------------------------------------------------
-- 11. Table structure for table: reports
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reports (
    id VARCHAR(64) PRIMARY KEY,
    reporter_name VARCHAR(255),
    reported_participant VARCHAR(255),
    category VARCHAR(100),
    description TEXT,
    event_id VARCHAR(64),
    status VARCHAR(50) DEFAULT 'New',
    resolution_notes TEXT,
    created_at VARCHAR(50)
);

-- ------------------------------------------------------------------------------
-- 12. Table structure for table: volunteers
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS volunteers (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255),
    phone VARCHAR(50),
    email VARCHAR(150),
    experience TEXT,
    preferred_games TEXT,
    availability VARCHAR(255),
    status VARCHAR(50) DEFAULT 'New',
    submitted_at VARCHAR(50)
);

-- ------------------------------------------------------------------------------
-- 13. Table structure for table: audit_logs
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(64) PRIMARY KEY,
    admin_name VARCHAR(255),
    action VARCHAR(255),
    target VARCHAR(255),
    details TEXT,
    description TEXT,
    "timestamp" VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs ("timestamp");

-- ------------------------------------------------------------------------------
-- 14. Table structure for table: notifications
-- Dual delivery (Email + WhatsApp) with automatic Email fallback
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(255),
    message TEXT,
    type VARCHAR(50),
    channel VARCHAR(50) DEFAULT 'dual',
    recipient_email VARCHAR(150),
    recipient_phone VARCHAR(50),
    event_id VARCHAR(64),
    delivery_status VARCHAR(50) DEFAULT 'Sent',
    whatsapp_status VARCHAR(100),
    "timestamp" VARCHAR(50),
    is_read BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_notifications_timestamp ON notifications ("timestamp");
CREATE INDEX IF NOT EXISTS idx_notifications_event_id ON notifications (event_id);

-- ==============================================================================
-- INITIAL SEED DATA (FETCHED DIRECTLY FROM DATABASE TABLES ONLY)
-- ==============================================================================

-- 1. Seed `admins`
INSERT INTO admins (id, name, email, phone, password, role, designation, avatar, bio, active, created_at)
VALUES
(
    'u-admin-parimal',
    'Parimal Shete',
    'parimalmshete@gmail.com',
    '+91 98220 99887',
    'admin123',
    'admin',
    'Co-Founder & Community Architect',
    'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=200',
    'Social Shuffle Co-Founder & Community Architect',
    TRUE,
    '2024-01-01T00:00:00Z'
),
(
    'u-admin-parimal-work',
    'Parimal Shete (Work)',
    'parimal.work@gmail.com',
    '+91 98220 55443',
    'admin123',
    'admin',
    'Co-Founder',
    'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=200',
    'Social Shuffle Co-Founder',
    TRUE,
    '2024-01-01T00:00:00Z'
),
(
    'u-admin-diksha',
    'Diksha',
    'diksha@socialshuffle.in',
    '+91 98220 88776',
    'admin123',
    'admin',
    'Co-Founder & Experience Director',
    'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=200',
    'Social Shuffle Co-Founder & Experience Director',
    TRUE,
    '2024-01-01T00:00:00Z'
),
(
    'u-admin-aman',
    'Aman Joshi (Host)',
    'admin@socialshuffle.com',
    '+91 98220 11223',
    'admin123',
    'admin',
    'Community Host & Super Admin',
    'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&q=80&w=200',
    'Social Shuffle Lead Host & Admin',
    TRUE,
    '2024-01-01T00:00:00Z'
)
ON CONFLICT (id) DO NOTHING;

-- 2. Seed `users`
INSERT INTO users (id, name, email, phone, password, role, area, avatar, bio, participant_id, auth_provider, last_active_at, login_at, created_at)
VALUES
(
    'u-admin-parimal',
    'Parimal Shete',
    'parimalmshete@gmail.com',
    '+91 98220 99887',
    'admin123',
    'admin',
    'Koregaon Park',
    'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=200',
    'Co-Founder & Community Architect at Social Shuffle',
    NULL,
    'google',
    '2026-09-29T12:00:00Z',
    '2026-09-29T12:00:00Z',
    '2024-01-01T00:00:00Z'
)ON CONFLICT (id) DO NOTHING;

-- 3. `participants` (Clean initial state: populated as real users register)

-- 4. Seed `venues`
INSERT INTO venues (id, name, address, area, capacity, contact_phone, maps_url, image, active)
VALUES
(
    'ven-1',
    'The Rustle Nest Cafe',
    'Lane 6, Koregaon Park, Pune, Maharashtra 411001',
    'Koregaon Park',
    45,
    '+91 98220 11223',
    'https://maps.google.com/?q=The+Rustle+Nest+Cafe+Pune',
    'https://images.unsplash.com/photo-1554118811-1e0d58224f24?auto=format&fit=crop&q=80&w=800',
    TRUE
),
(
    'ven-2',
    'Pagdandi Bookstore Cafe',
    'Shop 6, Regent Plaza, Baner - Pashan Link Rd, Pune 411045',
    'Baner',
    35,
    '+91 98220 44556',
    'https://maps.google.com/?q=Pagdandi+Bookstore+Cafe+Baner',
    'https://images.unsplash.com/photo-1521017432531-fbd92d768814?auto=format&fit=crop&q=80&w=800',
    TRUE
),
(
    'ven-3',
    'Waari Book Cafe',
    'Ground Floor, Choice Institute, Kothrud, Pune 411038',
    'Kothrud',
    50,
    '+91 98220 77889',
    'https://maps.google.com/?q=Waari+Book+Cafe+Kothrud',
    'https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&q=80&w=800',
    TRUE
)
ON CONFLICT (id) DO NOTHING;

-- 5. Seed `events`
INSERT INTO events (id, number, title, date, "time", venue, address, area, category, capacity, ticket_price, description, notes, planned_games, status, cover_image)
VALUES
(
    'evt-35',
    35,
    'Social Shuffle #35: Weekend Dice & Strategy',
    '2026-09-27',
    '4:00 PM - 8:00 PM',
    'The Rustle Nest Cafe',
    'Lane 6, Koregaon Park, Pune, Maharashtra 411001',
    'Koregaon Park',
    'Casual & Social',
    30,
    350.0,
    'Join us for our signature weekend board gaming mixer! Over 40+ curated board games, warm coffee, and friendly table hosts to teach you the rules in under 5 minutes.',
    'Wear comfortable clothing. Welcome tea/coffee included with your registration ticket.',
    '["g-catan","g-avalon","g-codenames","g-splendor"]',
    'upcoming',
    'https://images.unsplash.com/photo-1610890716171-6b1bb98ffd09?auto=format&fit=crop&q=80&w=800'
),
(
    'evt-36',
    36,
    'Social Shuffle #36: Tactical Tournament',
    '2026-10-04',
    '3:30 PM - 8:30 PM',
    'Pagdandi Bookstore Cafe',
    'Shop 6, Regent Plaza, Baner - Pashan Link Rd, Pune 411045',
    'Baner',
    'Tournament',
    32,
    400.0,
    'A tactical showdown featuring Catan, 7 Wonders, and Wingspan with special prizes and handcrafted coffee.',
    'Pre-registration mandatory. Tables limited to 32 players.',
    '["g-catan","g-splendor","g-7wonders"]',
    'upcoming',
    'https://images.unsplash.com/photo-1563941402622-4e7a488bcc57?auto=format&fit=crop&q=80&w=800'
)
ON CONFLICT (id) DO NOTHING;

-- 6. Seed `games`
INSERT INTO games (id, name, category, difficulty, players, duration, description, active, plays_count, tags, rating, image, owner, venue, available, current_location)
VALUES
(
    'g-catan',
    'Catan (Settlers of Catan)',
    'Strategy',
    'Medium',
    '3-4 Players',
    '60-90 mins',
    'Collect resources, trade with fellow shufflers, build settlements and roads on the island of Catan.',
    TRUE,
    48,
    '["trading","negotiation","dice","classic"]',
    4.8,
    'https://images.unsplash.com/photo-1610890716171-6b1bb98ffd09?auto=format&fit=crop&q=80&w=400',
    'Community Library',
    'The Rustle Nest Cafe',
    TRUE,
    'The Rustle Nest Cafe'
),
(
    'g-avalon',
    'The Resistance: Avalon',
    'Social Deduction',
    'Beginner',
    '5-10 Players',
    '30-45 mins',
    'Loyal servants of Arthur vs Minions of Mordred. Pure deception, table talk, and dramatic accusations.',
    TRUE,
    62,
    '["bluffing","party","hidden-role"]',
    4.9,
    'https://images.unsplash.com/photo-1543083477-4f785aeafaa9?auto=format&fit=crop&q=80&w=400',
    'Community Library',
    'The Rustle Nest Cafe',
    TRUE,
    'The Rustle Nest Cafe'
),
(
    'g-codenames',
    'Codenames',
    'Party',
    'Beginner',
    '4-8 Players',
    '15-20 mins',
    'Two rival spymasters know the secret identities of 25 agents. Teammates try to guess words based on one-word clues.',
    TRUE,
    89,
    '["word","deduction","icebreaker"]',
    4.8,
    'https://images.unsplash.com/photo-1563941402622-4e7a488bcc57?auto=format&fit=crop&q=80&w=400',
    'Community Library',
    'Pagdandi Bookstore Cafe',
    TRUE,
    'Pagdandi Bookstore Cafe'
),
(
    'g-splendor',
    'Splendor',
    'Strategy',
    'Beginner',
    '2-4 Players',
    '30 mins',
    'Renaissance merchants acquiring gemstone mines, transportation, and shops to attract aristocratic patrons.',
    TRUE,
    35,
    '["engine-building","chips","cards"]',
    4.7,
    'https://images.unsplash.com/photo-1585504198199-20277593b94f?auto=format&fit=crop&q=80&w=400',
    'Community Library',
    'Pagdandi Bookstore Cafe',
    TRUE,
    'Pagdandi Bookstore Cafe'
)
ON CONFLICT (id) DO NOTHING;

-- 7. Seed `badge_levels` (The 10 Badge Progression Tiers)
INSERT INTO badge_levels (level, short_title, fancy_name, subtitle, description, icon, min_game_percentage, min_events_attended, rarity, gradient, border_color, text_color, badge_tag_color, glow_effect, perks)
VALUES
(
    1,
    'Novice Roller',
    'Initiate of the Ivory Dice',
    'The First Move',
    'Broke the ice, rolled the initial dice, and attended their first Pune tabletop gathering.',
    '🎲',
    4,
    1,
    'Common',
    'from-slate-800 via-purple-950/40 to-slate-900',
    'border-slate-600/50',
    'text-slate-300',
    'bg-slate-700/50 text-slate-300 border-slate-600',
    'shadow-slate-500/10',
    '["Unlock Digital Shuffler Passport","Table Rookie Profile Badge"]'
),
(
    2,
    'Apprentice Shuffler',
    'Acolyte of the Meeple Order',
    'Table Apprentice',
    'Expanded beyond the basics with multiple games tested and a verified event check-in.',
    '♟️',
    10,
    1,
    'Common',
    'from-amber-950/30 via-slate-900 to-purple-950/30',
    'border-amber-700/50',
    'text-amber-300',
    'bg-amber-900/30 text-amber-300 border-amber-700/50',
    'shadow-amber-700/15',
    '["Game Table Tagging Rights","Access to Sunday Game Discussions"]'
),
(
    3,
    'Casual Tactician',
    'Bronze Vanguard of Koregaon',
    'Cafe Contender',
    'Frequents Pune board game cafes and demonstrates reliable tabletop instincts.',
    '⚔️',
    20,
    2,
    'Uncommon',
    'from-amber-800/30 via-slate-900 to-amber-950/40',
    'border-amber-600/60',
    'text-amber-200',
    'bg-amber-600/20 text-amber-200 border-amber-500/40',
    'shadow-amber-500/20',
    '["Early Seat Reservations","Bronze Shuffler Tier Status"]'
),
(
    4,
    'Board Game Enthusiast',
    'Master of the Card & Tile',
    'Strategy Adept',
    'Explores nearly a third of the catalog with a strong grasp of tile placement and card play.',
    '🏰',
    30,
    2,
    'Uncommon',
    'from-blue-950/40 via-slate-900 to-indigo-950/40',
    'border-blue-500/60',
    'text-blue-300',
    'bg-blue-500/20 text-blue-300 border-blue-400/40',
    'shadow-blue-500/25',
    '["Custom Table Name Tag","Host Co-Player Recommendation"]'
),
(
    5,
    'Tabletop Strategist',
    'Silver Viceroy of Baner Tables',
    'Tabletop Champion',
    'Comfortably maneuvers euro-style resource management, engine building, and deduction games.',
    '🛡️',
    40,
    3,
    'Rare',
    'from-slate-700/40 via-indigo-950/50 to-slate-900',
    'border-slate-300/60',
    'text-slate-100',
    'bg-slate-200/20 text-slate-100 border-slate-300/50',
    'shadow-slate-300/25',
    '["Silver Shuffler Tier Status","Tournament Seeding Priority"]'
),
(
    6,
    'Seasoned Shuffler',
    'Commander of a Thousand Battles',
    'Halfway to Catalog Mastery (50%+)',
    'Conquered half the official game library and a loyal regular at weekend gatherings.',
    '🌟',
    50,
    4,
    'Rare',
    'from-cyan-950/50 via-teal-950/40 to-slate-900',
    'border-cyan-400/70',
    'text-cyan-200',
    'bg-cyan-500/20 text-cyan-200 border-cyan-400/50',
    'shadow-cyan-400/30',
    '["New Game Library Voting Voice","Exclusive Shuffler Community Role"]'
),
(
    7,
    'Master of Meeples',
    'Gold Sovereign of Strategy',
    'High Tactician (60%+)',
    'Commanding table presence with extensive knowledge across strategic and party classics.',
    '👑',
    60,
    5,
    'Epic',
    'from-amber-600/30 via-yellow-600/20 to-purple-950/50',
    'border-amber-400/80',
    'text-amber-200',
    'bg-amber-400/20 text-amber-200 border-amber-400/50',
    'shadow-amber-400/35',
    '["Gold Strategist Tier Status","Complimentary Table Host Drink"]'
),
(
    8,
    'Grandmaster Tactician',
    'Platinum Grand Marshal',
    'Tabletop Virtuoso (70%+)',
    'Demonstrates surgical precision in deep euro-games, complex negotiations, and heavy card systems.',
    '💎',
    70,
    6,
    'Epic',
    'from-indigo-600/30 via-purple-600/20 to-sky-950/50',
    'border-indigo-400/80',
    'text-indigo-200',
    'bg-indigo-400/20 text-indigo-200 border-indigo-400/50',
    'shadow-indigo-400/40',
    '["VIP Game Day Lounge Pass","Rule Explainer Recognition"]'
),
(
    9,
    'Tabletop Sovereign',
    'Diamond Regent of Tabletop Realms',
    'Pune Board Game Legend (80%+)',
    'A legendary pillar of Pune gaming who has conquered 80%+ of all curated board games.',
    '🌌',
    80,
    7,
    'Legendary',
    'from-purple-600/40 via-pink-600/30 to-amber-900/40',
    'border-purple-300',
    'text-purple-100',
    'bg-purple-500/30 text-purple-100 border-purple-300/60',
    'shadow-purple-400/50',
    '["Diamond Grandmaster Tier Status","Permanent Hall of Fame Listing","Private Table Reservation"]'
),
(
    10,
    'Apex Shuffler',
    'Mythic Tabletop Archon',
    '90%+ Catalog Mastery & Elite Veteran',
    'Supreme mastery of 90%+ games in Pune meetups. An immortal legend of Social Shuffle.',
    '⚡',
    90,
    8,
    'Mythic',
    'from-amber-400/40 via-purple-600/40 to-cyan-400/40',
    'border-amber-300',
    'text-amber-100',
    'bg-gradient-to-r from-amber-400/30 to-purple-400/30 text-amber-200 border-amber-300/70',
    'shadow-amber-400/60',
    '["Mythic Golden Avatar Crown","Free VIP Entry to Annual Championship","Name Etched onto Social Shuffle Trophy","Lifetime Pune Shuffler Pioneer Status"]'
)
ON CONFLICT (level) DO NOTHING;

-- 8. `participant_badges` (Clean initial state: awarded as participants play games)

-- 9. `registrations` (Clean initial state: recorded via event checkouts)

-- 10. `feedback` (Clean initial state: submitted by event attendees)

-- 11. Seed `audit_logs`
INSERT INTO audit_logs (id, admin_name, action, target, details, description, "timestamp")
VALUES
(
    'log-1',
    'Aman Joshi (Host)',
    'SECURITY_INIT',
    'PostgreSQL Supabase Database',
    'All domain tables (admins, badges, venues, events, games) persisted directly in database',
    'Active production PostgreSQL datasource verified',
    '2026-09-29T12:00:00Z'
)
ON CONFLICT (id) DO NOTHING;

-- 12. Seed `notifications`
INSERT INTO notifications (id, title, message, type, channel, recipient_email, recipient_phone, event_id, delivery_status, whatsapp_status, "timestamp", is_read)
VALUES
(
    'notif-1',
    'Social Shuffle #35 Live',
    'Meetup announced for Koregaon Park. Registrations are open!',
    'event',
    'dual',
    'admin@socialshuffle.com',
    '+91 98220 11223',
    'evt-35',
    'Sent',
    'Delivered',
    '2026-09-29T12:00:00Z',
    FALSE
)
ON CONFLICT (id) DO NOTHING;

-- ==============================================================================
-- End of Social Shuffle PostgreSQL Schema & Seed Script
-- ==============================================================================


-- ------------------------------------------------------------------------------
-- Seed Participants Table with Real Pune Shuffler Profiles & Credentials
-- ------------------------------------------------------------------------------
INSERT INTO participants (id, name, email, phone, password, username, area, avatar, bio, joined_date, total_events_attended, total_registrations, games_played_ids, venues_visited, total_pax_brought, last_attended_event_id, last_attended_event_title) VALUES
  ('p-varad', 'Varad', 'varad@socialshuffle.in', '+91 98221 12345', 'shuffler123', 'varad', 'Koregaon Park', 'https://api.dicebear.com/7.x/bottts/svg?seed=Varad', 'Pune board game enthusiast • Catan & Social Deduction lover!', '2026-11-11', 0, 0, '[]', '[]', 0, NULL, NULL),
  ('p-mw97kkb5', 'Aman Sharma', 'aman.sharma@example.com', '9822099112', 'mysecretpass', 'aman.sharma', 'Baner', 'https://api.dicebear.com/7.x/bottts/svg?seed=Aman%20Sharma', 'Board game lover eager to play!', '2026-11-09', 0, 0, '[]', '[]', 0, NULL, NULL),
  ('p-priya', 'Priya Nair', 'priya.nair@example.com', '+91 97632 99011', 'shuffler123', 'priya.nair', 'Wakad', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=160&q=80', 'Weekly regular shuffler. Always looking for teammates for Just One & Wingspan!', '2024-01-15', 6, 7, '["g-codenames","g-justone","g-azul","g-wingspan","g-dixit","g-cascadia"]', '["Toit Brewery","FC Road Social","Waari Book Cafe"]', 3, 'ev-ss3', 'Secret Social & Mystery Games'),
  ('p-rohan', 'Rohan Deshmukh', 'rohan.deshmukh@example.com', '+91 98223 44551', 'shuffler123', 'rohan.deshmukh', 'Koregaon Park', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=160&q=80', 'Heavy strategy enthusiast. Terraforming Mars, Dune Imperium & Scythe fan.', '2024-01-05', 11, 12, '["g-dune","g-terraformingmars","g-scythe","g-brass","g-root"]', '["Toit Brewery","FC Road Social","Effingut KP","Waari Book Cafe"]', 8, 'ev-ss3', 'Secret Social & Mystery Games'),
  ('p-ananya', 'Ananya Kulkarni', 'ananya.k@example.com', '+91 98901 22334', 'shuffler123', 'ananya.k', 'Baner', 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?auto=format&fit=crop&w=160&q=80', 'Designer by day, social deduction master by night. Secret Hitler champ.', '2024-01-20', 8, 8, '["g-secrethitler","g-avalon","g-bloodontheclocktower","g-decrypto","g-wavelength"]', '["Toit Brewery","Waari Book Cafe"]', 5, 'ev-ss3', 'Secret Social & Mystery Games'),
  ('p-tanmay', 'Tanmay Joshi', 'tanmay.j@example.com', '+91 98221 88990', 'shuffler123', 'tanmay.j', 'Viman Nagar', 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&w=160&q=80', 'Love learning new modern European games and meeting new people over coffee.', '2024-02-01', 5, 6, '["g-catan","g-ticketoride","g-carcassonne","g-splendor","g-cascadia"]', '["FC Road Social","Toit Brewery"]', 2, 'ev-ss2', 'Strategy & Heavyweight Battles'),
  ('p-sneha', 'Sneha Patil', 'sneha.patil@example.com', '+91 97665 11223', 'shuffler123', 'sneha.patil', 'Aundh', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=160&q=80', 'Board game collector and casual teacher. Let us play Cascadia and Sushi Go!', '2024-02-14', 4, 4, '["g-cascadia","g-sushigo","g-azul","g-flip7","g-dixit"]', '["Waari Book Cafe","FC Road Social"]', 1, 'ev-ss3', 'Secret Social & Mystery Games'),
  ('p-aditya', 'Aditya Shinde', 'aditya.shinde@example.com', '+91 98220 33445', 'shuffler123', 'aditya.shinde', 'Kothrud', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=160&q=80', 'Tournament competitor. Competitive Spirit Island and Blood on the Clocktower storyteller.', '2024-01-10', 9, 10, '["g-spiritisland","g-bloodontheclocktower","g-dune","g-root","g-clank"]', '["Toit Brewery","FC Road Social","Effingut KP"]', 6, 'ev-ss3', 'Secret Social & Mystery Games'),
  ('p-meera-nov2', 'Meera Rao', 'meera.rao@example.com', '+91 98231 44001', 'shuffler123', 'meera.rao', 'Kalyani Nagar', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=160&q=80', 'Casual strategist & coffee addict. Eager to master Cascadia & Ticket to Ride!', '2026-11-02', 1, 1, '["g-cascadia"]', '["FC Road Social"]', 1, 'ev-ss1', 'Strategy & Heavyweight Battles'),
  ('p-kunal-nov4', 'Kunal Bapat', 'kunal.bapat@example.com', '+91 98902 11229', 'shuffler123', 'kunal.bapat', 'Baner', 'https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?auto=format&fit=crop&w=160&q=80', 'Techie in Baner. Loved playing Codenames and Just One at the weekend meetup.', '2026-11-04', 1, 1, '["g-codenames","g-justone"]', '["Toit Brewery"]', 2, 'ev-ss2', 'Beginner Friendly Meetup'),
  ('p-neha-nov6', 'Neha Mahajan', 'neha.m@example.com', '+91 97631 88772', 'shuffler123', 'neha.mahajan', 'Viman Nagar', 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=160&q=80', 'Social deduction fanatic. First time shuffler and loved the community vibe!', '2026-11-06', 1, 1, '["g-secrethitler","g-avalon"]', '["Effingut KP"]', 0, 'ev-ss3', 'Secret Social & Mystery Games'),
  ('p-vikram-nov7', 'Vikram Sen', 'vikram.sen@example.com', '+91 98224 55667', 'shuffler123', 'vikram.sen', 'Koregaon Park', 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=160&q=80', 'Heavy Euro games & tabletop architect. Excited for Dune & Scythe sessions.', '2026-11-07', 1, 2, '["g-dune","g-scythe"]', '["Toit Brewery"]', 1, 'ev-ss2', 'Strategy & Heavyweight Battles'),
  ('p-pooja-nov8', 'Pooja Hegde', 'pooja.h@example.com', '+91 99220 33991', 'shuffler123', 'pooja.hegde', 'Wakad', 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=160&q=80', 'Solo explorer who joined the table and made 5 new friends! Sushi Go enthusiast.', '2026-11-08', 1, 1, '["g-sushigo","g-flip7"]', '["FC Road Social"]', 0, 'ev-ss1', 'Beginner Friendly Meetup'),
  ('p-siddharth-nov10', 'Siddharth Verma', 'siddharth.v@example.com', '+91 98905 77114', 'shuffler123', 'siddharth.v', 'Baner', 'https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?auto=format&fit=crop&w=160&q=80', 'Passionate gamer recently moved from Bengaluru. Ready for weekend shuffles!', '2026-11-10', 0, 1, '[]', '[]', 0, NULL, NULL)
ON CONFLICT (id) DO NOTHING;

