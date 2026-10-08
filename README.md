# 🎲 Social Shuffle — Spring Boot Backend (Supabase PostgreSQL & MySQL Ready)

Production-grade Spring Boot 3.x REST API with **Spring Data JPA & PostgreSQL / MySQL** for **Social Shuffle** (Pune's Board Game Community).
Pre-configured for **Supabase PostgreSQL Cloud Database**, Docker, Local development, and **GoDaddy cPanel MySQL hosting**.

---

## ⚡ Supabase PostgreSQL Database Setup (Active Configuration)

The backend is connected out-of-the-box to **Supabase PostgreSQL**:

- **Host**: `db.pngqqlrrqovdkzvljgjc.supabase.co`
- **Port**: `5432`
- **Database**: `postgres`
- **User**: `postgres`
- **SSL**: `require`
- **Connection String**: `postgresql://postgres:u7ihNL9yTUg8MnTw@db.pngqqlrrqovdkzvljgjc.supabase.co:5432/postgres`

### Automatic Table Creation & Data Seeding
Spring Boot and Hibernate (`spring.jpa.hibernate.ddl-auto=update`) automatically creates all PostgreSQL tables (`events`, `games`, `participants`, `registrations`, `event_feedback`, `safety_reports`, `volunteer_applications`, `audit_logs`, `notifications`) and `DataSeeder.java` seeds initial Pune board games and meetup schedules on first startup.

### Supabase SQL Editor 1-Click Import (Optional)
If you prefer to initialize or inspect the tables directly in the Supabase Dashboard:
1. Open your **Supabase Project** ➔ **SQL Editor**.
2. Open `social_shuffle_supabase_postgres.sql` from this repository.
3. Paste the contents and click **Run**. All 11 PostgreSQL tables, indexes, and initial Pune board game data will be created with `ON CONFLICT DO NOTHING` safety!

### Environment Variable Overrides (Optional)
If running on Docker / Cloud / Render:
```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://db.pngqqlrrqovdkzvljgjc.supabase.co:5432/postgres?sslmode=require"
export DB_USER="postgres"
export DB_PASSWORD="YourPassword"
```

---

## 🚀 Setting up with GoDaddy MySQL (cPanel Hosting Alternate)

GoDaddy Linux / cPanel hosting comes with MySQL database management built-in. Follow these simple steps:

### 1. Create MySQL Database in GoDaddy cPanel
1. Log in to your **GoDaddy Account** and navigate to your **cPanel Admin**.
2. Under the **Databases** section, click **MySQL® Databases**.
3. Under **Create New Database**, enter `social_shuffle` (note: cPanel will prepend your cPanel username, e.g., `cpuser_socialshuffle`). Click **Create Database**.

### 2. Create MySQL User & Grant Permissions
1. On the same page, scroll down to **MySQL Users ➔ Add New User**.
2. Enter a username (e.g., `dbuser`) and generate a secure password. Click **Create User**.
3. Under **Add User To Database**:
   - Select your user (e.g., `cpuser_dbuser`)
   - Select your database (e.g., `cpuser_socialshuffle`)
   - Click **Add**
4. Check **ALL PRIVILEGES** and click **Make Changes**.

### 3. Import Schema & Initial Seed Data into GoDaddy via phpMyAdmin
1. In cPanel, click **phpMyAdmin**.
2. In the left sidebar, click your new database name.
3. Click the **Import** tab at the top.
4. Click **Choose File** and select `social_shuffle_godaddy_mysql.sql` (found in this repository).
5. Click **Go** at the bottom. All tables (`users`, `participants`, `events`, `games`, `registrations`, etc.) and initial Pune community records will be created instantly!

### 4. Configure Connection (`src/main/resources/application.properties`)
You can configure your GoDaddy database credentials directly in `application.properties` or via environment variables:

```properties
# If Spring Boot is running on your server / VPS:
spring.datasource.url=jdbc:mysql://localhost:3306/cpuser_socialshuffle?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true
spring.datasource.username=cpuser_dbuser
spring.datasource.password=YourSecurePassword123!
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Hibernate auto-update
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

Or run with environment variables without touching the code:
```bash
export MYSQL_HOST="localhost" # Or your GoDaddy server IP / domain
export MYSQL_PORT="3306"
export MYSQL_DATABASE="cpuser_socialshuffle"
export MYSQL_USER="cpuser_dbuser"
export MYSQL_PASSWORD="YourSecurePassword123!"

mvn spring-boot:run
```

---

## 🛠️ Running Locally in Spring Tool Suite (STS) / IntelliJ / VS Code

### 1. Prerequisites
- **Java Development Kit (JDK 17 or 21)**.
- **Supabase Cloud PostgreSQL** (pre-configured) OR local PostgreSQL 15/16/17 (or MySQL).
- **Spring Tool Suite 4 (STS)**, IntelliJ IDEA, or VS Code with Spring Tools.

### 2. Import into STS
1. Open **Spring Tool Suite (STS)**.
2. Click **File** ➔ **Import...**
3. Select **Maven** ➔ **Existing Maven Projects** and click **Next**.
4. Browse to this backend folder, ensure `pom.xml` is checked, and click **Finish**.
5. Maven will download `spring-boot-starter-data-jpa` and `postgresql` JDBC driver.

### 3. Run the Application
1. In the **Package Explorer**, right-click `SocialShuffleApplication.java`.
2. Select **Run As** ➔ **Spring Boot App**.
3. The console will display:
   ```text
   =================================================
   🎲 Social Shuffle Pune Spring Boot API Started!
   📡 Server running at: http://localhost:8080/api
   🐘 Database Connected (PostgreSQL / Supabase Cloud DB)
   =================================================
   ```
4. `DataSeeder.java` will automatically verify your PostgreSQL tables and seed Pune board games, upcoming meetups, and participant data if empty.

---

## 📡 REST API Reference

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/system/health` | Health check, PostgreSQL (Supabase) connection status & table row counts |
| `POST`| `/api/auth/login` | Email/Phone + password participant and admin login |
| `POST`| `/api/auth/register` | Self-register as a participant |
| `POST`| `/api/auth/google` | Google OAuth Sign-In & auto-registration |
| `GET` | `/api/events` | List all events (supports `?status=upcoming`) |
| `POST` | `/api/events` | Create new meetup event |
| `PUT` | `/api/events/{id}` | Update event details |
| `POST` | `/api/events/{id}/duplicate` | Clone event for next edition |
| `GET` | `/api/games` | List board game library (`?activeOnly=true`) |
| `POST` | `/api/games` | Add new game to library |
| `PATCH` | `/api/games/{id}/toggle-active` | Toggle game active/inactive |
| `GET` | `/api/participants` | Search participants by name/phone/area |
| `POST` | `/api/participants` | Register new participant profile |
| `GET` | `/api/participants/check-duplicate` | Check duplicate by email/phone |
| `POST` | `/api/participants/merge` | Merge duplicate participant profiles |
| `GET` | `/api/registrations` | List registrations (`?eventId=...`) |
| `POST` | `/api/registrations` | Book spot / RSVP for meetup (generates QR pass & triggers email) |
| `POST` | `/api/registrations/verify-qr` | Verify & check-in attendee pass by QR token or reg ID |
| `GET`  | `/api/registrations/{id}/email-preview` | Preview rendered HTML confirmation email |
| `PATCH` | `/api/registrations/{id}/attendance` | Check-in participant (`?status=Checked In`) |
| `PATCH` | `/api/registrations/{id}/payment` | Update payment (`?status=Confirmed`) |
| `POST` | `/api/registrations/{id}/toggle-game`| Mark game played during meetup |
| `POST` | `/api/registrations/walk-in` | On-the-spot walk-in registration |
| `GET/POST`| `/api/community/feedback` | Shuffler event feedback |
| `GET/POST`| `/api/community/reports` | Confidential safety & issue reports |
| `GET/POST`| `/api/community/volunteers` | Volunteer application workflow |
| `GET` | `/api/audit-logs` | Admin action audit log |
| `GET` | `/api/notifications` | Notifications & alerts |
| `GET` | `/api/badges/levels` | List all 10 badge level definitions with requirements, fancy names & perks |
| `POST`| `/api/badges/calculate` | Calculate 10-level badge from games played & events attended (90%+ = Level 10 Mythic Tabletop Archon) |
| `GET` | `/api/badges/participant/{id}` | Compute badge level directly for a participant by ID |

---

## 💻 Connecting to Frontend
In the React frontend, go to **Admin Console ➔ Backend & Database Hub**:
- Set API URL to `http://localhost:8080/api` (or your deployed backend URL).
- Click **Test Connection** to verify live communication with PostgreSQL (Supabase).
