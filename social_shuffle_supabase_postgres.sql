-- ==============================================================================
-- Social Shuffle Pune — Complete PostgreSQL Database Schema & Seed Data
-- Specially formatted for Supabase PostgreSQL & PostgreSQL 15/16/17
-- Compatible with Spring Boot 3.x / Spring Data JPA Hibernate PostgreSQL Dialect
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. Table structure for table: admins
-- (Used for Administrator authentication, role validation, and founder details)
-- Public registration NEVER writes to this table.
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
-- (Participant and admin application accounts)
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

-- ------------------------------------------------------------------------------
-- 3. Table structure for table: participants
-- (Community member profiles, gamer stats, attendance records)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS participants (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(50),
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
-- 4. Table structure for table: events
-- (Social Shuffle meetup sessions across Pune cafes)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS events (
    id VARCHAR(64) PRIMARY KEY,
    number INTEGER DEFAULT 0,
    title VARCHAR(255),
    date VARCHAR(50),
    time VARCHAR(100),
    venue VARCHAR(255),
    address TEXT,
    area VARCHAR(255),
    category VARCHAR(100),
    capacity INTEGER DEFAULT 30,
    ticket_price INTEGER DEFAULT 350,
    description TEXT,
    notes TEXT,
    planned_games TEXT,
    status VARCHAR(50) DEFAULT 'upcoming',
    cover_image TEXT
);

CREATE INDEX IF NOT EXISTS idx_events_date ON events (date);
CREATE INDEX IF NOT EXISTS idx_events_status ON events (status);
CREATE INDEX IF NOT EXISTS idx_events_area ON events (area);

-- ------------------------------------------------------------------------------
-- 5. Table structure for table: games
-- (Curated board game library catalogue)
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
    available BOOLEAN DEFAULT TRUE,
    current_location VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_games_category ON games (category);
CREATE INDEX IF NOT EXISTS idx_games_active ON games (active);

-- ------------------------------------------------------------------------------
-- 6. Table structure for table: registrations
-- (Meetup RSVP, ticket codes, check-ins, payments)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS registrations (
    id VARCHAR(64) PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    participant_id VARCHAR(64) NOT NULL,
    participant_name VARCHAR(255),
    participant_email VARCHAR(150),
    participant_phone VARCHAR(50),
    participant_area VARCHAR(255),
    pax_count INTEGER DEFAULT 1,
    guests TEXT,
    payment_status VARCHAR(50) DEFAULT 'Pending',
    payment_method VARCHAR(50) DEFAULT 'UPI',
    amount_paid INTEGER DEFAULT 350,
    attendance_status VARCHAR(50) DEFAULT 'Registered',
    check_in_time VARCHAR(50),
    ticket_code VARCHAR(100),
    registered_at VARCHAR(50),
    games_played_at_event TEXT,
    feedback_rating INTEGER,
    feedback_comments TEXT,
    checked_in_by VARCHAR(64),
    payment_transaction_id VARCHAR(64),
    receipt_number VARCHAR(64),
    cancellation_reason TEXT,
    cancelled_at VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_regs_event ON registrations (event_id);
CREATE INDEX IF NOT EXISTS idx_regs_participant ON registrations (participant_id);
CREATE INDEX IF NOT EXISTS idx_regs_ticket_code ON registrations (ticket_code);
CREATE INDEX IF NOT EXISTS idx_regs_payment_status ON registrations (payment_status);

-- ------------------------------------------------------------------------------
-- 7. Table structure for table: event_feedback
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS event_feedback (
    id VARCHAR(64) PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    registration_id VARCHAR(64),
    participant_name VARCHAR(255),
    rating INTEGER DEFAULT 5,
    comments TEXT,
    favorite_game VARCHAR(255),
    venue_rating INTEGER DEFAULT 5,
    submitted_at VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_feedback_event ON event_feedback (event_id);

-- ------------------------------------------------------------------------------
-- 8. Table structure for table: safety_reports
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS safety_reports (
    id VARCHAR(64) PRIMARY KEY,
    event_id VARCHAR(64),
    reporter_name VARCHAR(255),
    reporter_contact VARCHAR(64),
    reported_participant VARCHAR(255),
    incident_category VARCHAR(100),
    description TEXT,
    severity VARCHAR(50),
    status VARCHAR(50) DEFAULT 'Investigating',
    resolution_notes TEXT,
    created_at VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_reports_event ON safety_reports (event_id);
CREATE INDEX IF NOT EXISTS idx_reports_status ON safety_reports (status);

-- ------------------------------------------------------------------------------
-- 9. Table structure for table: volunteer_applications
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS volunteer_applications (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255),
    phone VARCHAR(50),
    email VARCHAR(150),
    area VARCHAR(100),
    experience TEXT,
    preferred_games TEXT,
    availability TEXT,
    motivation TEXT,
    skills TEXT,
    status VARCHAR(50) DEFAULT 'Pending',
    submitted_at VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_volunteers_email ON volunteer_applications (email);
CREATE INDEX IF NOT EXISTS idx_volunteers_status ON volunteer_applications (status);

-- ------------------------------------------------------------------------------
-- 10. Table structure for table: audit_logs
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(64) PRIMARY KEY,
    admin_name VARCHAR(255),
    action VARCHAR(255),
    target VARCHAR(255),
    details TEXT,
    description TEXT,
    timestamp VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_logs (timestamp);

-- ------------------------------------------------------------------------------
-- 11. Table structure for table: notifications
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(255),
    message TEXT,
    type VARCHAR(50),
    timestamp VARCHAR(50),
    is_read BOOLEAN DEFAULT FALSE,
    recipient_email VARCHAR(120),
    event_id VARCHAR(64),
    delivery_status VARCHAR(32)
);

CREATE INDEX IF NOT EXISTS idx_notif_time ON notifications (timestamp);
CREATE INDEX IF NOT EXISTS idx_notif_recipient ON notifications (recipient_email);

-- ==============================================================================
-- INITIAL SEED DATA (PostgreSQL Safe with ON CONFLICT DO NOTHING)
-- ==============================================================================

-- 1. Seed Dedicated Admin Table (`admins`)
INSERT INTO admins (id, name, email, phone, password, role, designation, avatar, bio, active, created_at)
VALUES (
    'u-admin',
    'Aman Joshi (Host)',
    'admin@socialshuffle.com',
    '+91 98220 11223',
    'admin123',
    'admin',
    'Community Lead & Founder',
    'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=200',
    'Social Shuffle Founder & Super Admin. Refers to database table for name and admin details.',
    TRUE,
    '2024-01-01T00:00:00Z'
)
ON CONFLICT (id) DO NOTHING;

-- 2. Seed `users`
INSERT INTO users (id, name, email, phone, password, role, area, avatar, bio, participant_id, auth_provider, last_active_at, login_at, created_at)
VALUES 
(
    'u-admin',
    'Aman Joshi (Host)',
    'admin@socialshuffle.com',
    '+91 98220 11223',
    'admin123',
    'admin',
    'Pune',
    'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=200',
    'Host & Organizer at Social Shuffle Pune',
    NULL,
    'local',
    '2026-09-29T12:00:00Z',
    '2026-09-29T12:00:00Z',
    '2024-01-01T00:00:00Z'
),
(
    'u-parimal',
    'Parimal Shete',
    'parimalmshete@gmail.com',
    '+91 98220 00000',
    'oauth_google',
    'participant',
    'Koregaon Park',
    'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=200',
    'Pune board gamer • Joined via Google Account',
    'p-parimal',
    'google',
    '2026-09-29T12:00:00Z',
    '2026-09-29T12:00:00Z',
    '2026-09-23T12:00:00Z'
),
(
    'u-rohan',
    'Rohan Kulkarni',
    'rohan.kulkarni@example.com',
    '+91 98220 12345',
    'shuffler123',
    'participant',
    'Koregaon Park',
    'https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?auto=format&fit=crop&q=80&w=200',
    'Board game lover, strategy nerd, Catan enthusiast.',
    'p-rohan',
    'local',
    '2026-09-29T12:00:00Z',
    '2026-09-29T12:00:00Z',
    '2024-01-15T00:00:00Z'
),
(
    'u-ananya',
    'Ananya Deshmukh',
    'ananya.d@example.com',
    '+91 98230 56789',
    'shuffler123',
    'participant',
    'Baner',
    'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=200',
    'Codenames spymaster and social deduction aficionado.',
    'p-ananya',
    'local',
    '2026-09-29T12:00:00Z',
    '2026-09-29T12:00:00Z',
    '2024-02-01T00:00:00Z'
)
ON CONFLICT (id) DO NOTHING;

-- 3. Seed `participants`
INSERT INTO participants (id, name, email, phone, area, avatar, bio, joined_date, total_events_attended, total_registrations, games_played_ids, venues_visited, total_pax_brought, last_attended_event_id, last_attended_event_title)
VALUES
(
    'p-parimal',
    'Parimal Shete',
    'parimalmshete@gmail.com',
    '+91 98220 00000',
    'Koregaon Park',
    'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=200',
    'Pune board gamer • Joined via Google Account',
    '2026-09-23',
    0,
    0,
    '[]',
    '[]',
    0,
    NULL,
    NULL
),
(
    'p-rohan',
    'Rohan Kulkarni',
    'rohan.kulkarni@example.com',
    '9822012345',
    'Koregaon Park',
    'https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?auto=format&fit=crop&q=80&w=200',
    'Board game lover, strategy nerd, Catan enthusiast.',
    '2024-01-15',
    8,
    9,
    '["g-catan","g-avalon","g-codenames"]',
    '["The Rustle Nest Cafe","Pagdandi Bookstore Cafe"]',
    4,
    'evt-34',
    'Social Shuffle #34 - Strategy Showdown'
),
(
    'p-ananya',
    'Ananya Deshmukh',
    'ananya.d@example.com',
    '9823056789',
    'Baner',
    'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=200',
    'Codenames spymaster and social deduction aficionado.',
    '2024-02-01',
    5,
    5,
    '["g-codenames","g-splendor"]',
    '["Pagdandi Bookstore Cafe"]',
    2,
    'evt-34',
    'Social Shuffle #34 - Strategy Showdown'
)
ON CONFLICT (id) DO NOTHING;

-- 4. Seed `events`
INSERT INTO events (id, number, title, date, time, venue, address, area, category, capacity, ticket_price, description, notes, planned_games, status, cover_image)
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
    350,
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
    400,
    'A tactical showdown featuring Catan, 7 Wonders, and Wingspan with special prizes and handcrafted coffee.',
    'Pre-registration mandatory. Tables limited to 32 players.',
    '["g-catan","g-splendor","g-7wonders"]',
    'upcoming',
    'https://images.unsplash.com/photo-1563941402622-4e7a488bcc57?auto=format&fit=crop&q=80&w=800'
)
ON CONFLICT (id) DO NOTHING;

-- 5. Seed `games`
INSERT INTO games (id, name, category, difficulty, players, duration, description, active, plays_count, tags, rating, image, owner, available, current_location)
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
    TRUE,
    'Koregaon Park Box'
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
    TRUE,
    'Koregaon Park Box'
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
    TRUE,
    'Baner Box'
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
    TRUE,
    'Baner Box'
)
ON CONFLICT (id) DO NOTHING;

-- 6. Seed `registrations`
INSERT INTO registrations (id, event_id, participant_id, participant_name, participant_email, participant_phone, participant_area, pax_count, guests, payment_status, attendance_status, check_in_time, registered_at)
VALUES
(
    'reg-1',
    'evt-35',
    'p-rohan',
    'Rohan Kulkarni',
    'rohan.kulkarni@example.com',
    '9822012345',
    'Koregaon Park',
    2,
    '[{"id":"g-1","name":"Tanvi Joshi","email":"tanvi@example.com","phone":"9822099999","area":"Koregaon Park"}]',
    'Confirmed',
    'Registered',
    NULL,
    '2026-09-20T10:30:00Z'
)
ON CONFLICT (id) DO NOTHING;

-- 7. Seed `audit_logs`
INSERT INTO audit_logs (id, admin_name, action, target, details, description, timestamp)
VALUES
(
    'log-1',
    'Aman Joshi (Host)',
    'SECURITY_INIT',
    'PostgreSQL Supabase Database',
    'Configured Supabase PostgreSQL connection with TLS/SSL encryption and PBKDF2 password hashing',
    'Active production PostgreSQL datasource verified',
    '2026-09-29T12:00:00Z'
)
ON CONFLICT (id) DO NOTHING;

-- 8. Seed `notifications`
INSERT INTO notifications (id, title, message, type, timestamp, is_read, recipient_email)
VALUES
(
    'notif-1',
    'Social Shuffle #35 Live',
    'Meetup announced for Koregaon Park. Registrations are open!',
    'event',
    '2026-09-29T12:00:00Z',
    FALSE,
    'admin@socialshuffle.com'
)
ON CONFLICT (id) DO NOTHING;

-- ==============================================================================
-- End of Social Shuffle PostgreSQL Schema & Seed Script
-- ==============================================================================
