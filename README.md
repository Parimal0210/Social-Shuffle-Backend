# 🎲 Social Shuffle — Spring Boot Backend (Supabase PostgreSQL & MySQL Ready)

Production-grade Spring Boot 3.x REST API with **Spring Data JPA & PostgreSQL / MySQL** for **Social Shuffle** (Pune's Board Game Community).
Pre-configured for **Supabase PostgreSQL Cloud Database**, Docker, Local development, and **GoDaddy cPanel MySQL hosting**.

> **Database Query Guarantee**: All data—including initial seed data, administrators, badge progression tiers, events, board games, meetup venues, registrations, and reviews—is fetched strictly from the database tables only.

---

## 🏛️ Comprehensive Database Architecture & Tables

The backend utilizes 14 relational database tables, fully mapped to Spring Data JPA entities:

| Table | JPA Entity | Description |
|---|---|---|
| `admins` | `AdminAccount` | Dedicated admin table for identifying and authenticating admins, founders & co-founders |
| `users` | `User` | Application accounts for participants and administrative users |
| `participants` | `Participant` | Shuffler gamer profiles, attended events, games played, venues visited |
| `venues` | `Venue` | Official board game meetup cafes in Pune (Rustle Nest, Pagdandi, Waari) |
| `events` | `ShuffleEvent` | Meetup sessions, ticket capacity, prices, schedules, and planned games |
| `games` | `Game` | Curated board game catalog (Catan, Avalon, Codenames, Splendor, etc.) |
| `badge_levels` | `BadgeLevel` | The 10 official badge progression tiers, requirements, gradients, and perks |
| `participant_badges` | `ParticipantBadge` | Unlocked badge levels and achievements per participant |
| `registrations` | `Registration` | Attendee event bookings, guest lists, payment status, QR pass tokens |
| `feedback` | `EventFeedback` | Attendee ratings, favorite games, and meetup reviews |
| `reports` | `SafetyReport` | Confidential community safety and conduct reports |
| `volunteers` | `VolunteerApplication` | Table host and volunteer co-organizer applications |
| `audit_logs` | `AuditLog` | Administrative action audit trails and security logs |
| `notifications` | `NotificationItem` | Dual-delivery push notifications (Email + WhatsApp with Email fallback) |

---

## ⚡ Supabase PostgreSQL Database Setup (Active Configuration)

The backend is connected out-of-the-box using the **Supabase IPv4 Connection Pooler** (Session Mode, Port 5432). This resolves over IPv4 and prevents `java.net.SocketException: Network is unreachable` on IPv4-only platforms like **Render**, AWS, Heroku, and Docker:

- **Host**: `aws-0-ap-southeast-2.pooler.supabase.com`
- **Port**: `5432` (Session mode for HikariCP & Hibernate)
- **Database**: `postgres`
- **User**: `postgres.pngqqlrrqovdkzvljgjc`
- **Password**: `u7ihNL9yTUg8MnTw`
- **SSL**: `require`
- **Connection String**: `postgresql://postgres.pngqqlrrqovdkzvljgjc:u7ihNL9yTUg8MnTw@aws-0-ap-southeast-2.pooler.supabase.com:5432/postgres`

### Supabase SQL Editor 1-Click Import
1. Open your **Supabase Project** ➔ **SQL Editor**.
2. Open `social_shuffle_supabase_postgres.sql` from this repository.
3. Paste the contents and click **Run**. All 14 PostgreSQL tables, indexes, and initial Pune community data will be created with `ON CONFLICT DO NOTHING` safety!

---

## 🚀 Setting up with GoDaddy MySQL (cPanel Hosting Alternate)

GoDaddy Linux / cPanel hosting comes with MySQL database management built-in:

### 1. Create MySQL Database in GoDaddy cPanel
1. Log in to your **GoDaddy Account** and navigate to your **cPanel Admin**.
2. Under the **Databases** section, click **MySQL® Databases**.
3. Under **Create New Database**, enter `social_shuffle`. Click **Create Database**.

### 2. Create MySQL User & Grant Permissions
1. On the same page, scroll down to **MySQL Users ➔ Add New User**.
2. Enter username (e.g., `dbuser`) and password. Click **Create User**.
3. Under **Add User To Database**, select your user and database, click **Add**, check **ALL PRIVILEGES**, and click **Make Changes**.

### 3. Import Schema & Initial Seed Data into GoDaddy via phpMyAdmin
1. In cPanel, click **phpMyAdmin**.
2. In the left sidebar, click your new database name.
3. Click the **Import** tab at the top.
4. Click **Choose File** and select `social_shuffle_godaddy_mysql.sql`.
5. Click **Go** at the bottom. All 14 tables and initial records will be created instantly!

### 4. Configure Connection (`src/main/resources/application.properties`)
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/cpuser_socialshuffle?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true
spring.datasource.username=cpuser_dbuser
spring.datasource.password=YourSecurePassword123!
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

---

## 📡 REST API Reference

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/system/health` | Health check, DB connection status & table row counts |
| `GET` | `/api/admins` | List active admins from database `admins` table |
| `GET` | `/api/admins/check` | Identify if logged-in individual is an admin (`?identifier=...`) |
| `POST`| `/api/auth/login` | Email/Phone + password participant and admin login |
| `POST`| `/api/auth/register` | Self-register as a participant |
| `POST`| `/api/auth/google` | Google OAuth Sign-In & admin role identification |
| `GET` | `/api/auth/me` | Fetch active logged-in user and role |
| `GET` | `/api/venues` | List official meetup cafes and venues from database |
| `POST`| `/api/venues` | Add a new meetup venue to the database |
| `GET` | `/api/events` | List all events from database (`?status=upcoming`) |
| `POST`| `/api/events` | Create new meetup event |
| `PUT` | `/api/events/{id}` | Update event details in database |
| `POST`| `/api/events/{id}/duplicate` | Clone event for next edition |
| `GET` | `/api/games` | List board game library (`?activeOnly=true`) |
| `POST`| `/api/games` | Add new game to library |
| `PATCH`| `/api/games/{id}/toggle-active` | Toggle game active/inactive in database |
| `GET` | `/api/participants` | Search participants by name/phone/area |
| `POST`| `/api/participants` | Register new participant profile |
| `POST`| `/api/participants/merge` | Merge duplicate participant profiles in database |
| `GET` | `/api/registrations` | List registrations from database (`?eventId=...`) |
| `POST`| `/api/registrations` | Book spot / RSVP (generates QR pass & triggers email) |
| `POST`| `/api/registrations/verify-qr` | Verify & check-in attendee pass by QR token |
| `PATCH`| `/api/registrations/{id}/attendance` | Check-in participant (`?status=Checked In`) |
| `POST`| `/api/notifications/event-broadcast` | Dual Push notification (Email + WhatsApp with fallback) |
| `GET` | `/api/badges/levels` | Fetch all 10 badge tiers directly from database `badge_levels` table |
| `POST`| `/api/badges/calculate` | Calculate 10-level badge from games played & events attended |
| `GET` | `/api/badges/participant/{id}` | Compute badge level directly for a participant by ID |
| `GET/POST`| `/api/community/feedback` | Shuffler event feedback & reviews |
| `GET/POST`| `/api/community/reports` | Confidential safety & issue reports |
| `GET/POST`| `/api/community/volunteers` | Volunteer application workflow |

---

## 🔄 Syncing to GitHub
To push changes to `https://github.com/Parimal0210/Social-Shuffle-Backend.git`:
```bash
./push_to_github.sh <YOUR_GITHUB_PERSONAL_ACCESS_TOKEN>
```
Or from the root directory:
```bash
./push_backend.sh <YOUR_GITHUB_PERSONAL_ACCESS_TOKEN>
```
