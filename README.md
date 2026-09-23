# 🎲 Social Shuffle — Spring Boot & MySQL Backend (STS & GoDaddy Ready)

Production-grade Spring Boot 3.x REST API with **Spring Data JPA & MySQL** for **Social Shuffle** (Pune's Board Game Community).
Fully compatible with **GoDaddy cPanel MySQL hosting**, Local MySQL 8.x, and cloud MySQL (AWS RDS / GCP Cloud SQL).

---

## 🚀 Setting up with GoDaddy MySQL (cPanel Hosting)

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

## 🛠️ Running Locally in Spring Tool Suite (STS)

### 1. Prerequisites
- **Java Development Kit (JDK 17 or 21)**.
- **MySQL 8.x** running locally (`localhost:3306`) OR GoDaddy Remote MySQL.
- **Spring Tool Suite 4 (STS)** or Eclipse with Spring Tools.

### 2. Import into STS
1. Open **Spring Tool Suite (STS)**.
2. Click **File** ➔ **Import...**
3. Select **Maven** ➔ **Existing Maven Projects** and click **Next**.
4. Browse to this backend folder, ensure `pom.xml` is checked, and click **Finish**.
5. Maven will download `spring-boot-starter-data-jpa` and `mysql-connector-j`.

### 3. Run the Application
1. In the **Package Explorer**, right-click `SocialShuffleApplication.java`.
2. Select **Run As** ➔ **Spring Boot App**.
3. The console will display:
   ```text
   =================================================
   🎲 Social Shuffle Pune Spring Boot API Started!
   📡 Server running at: http://localhost:8080/api
   🐬 MySQL Connected (GoDaddy cPanel / Local DB)
   =================================================
   ```
4. `DataSeeder.java` will automatically verify your MySQL tables and seed Pune board games, upcoming meetups, and participant data if empty.

---

## 📡 REST API Reference

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/system/health` | Health check, MySQL connection status & table row counts |
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

---

## 💻 Connecting to Frontend
In the React frontend, go to **Admin Console ➔ Backend & Database Hub**:
- Set API URL to `http://localhost:8080/api` (or your GoDaddy server URL, e.g. `https://api.yourdomain.com/api`).
- Click **Test Connection** to verify live communication with MySQL.
