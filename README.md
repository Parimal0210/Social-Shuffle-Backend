# 🎲 Social Shuffle — Spring Boot & MongoDB Backend (STS Ready)

Complete production-grade Spring Boot 3.x REST API with Spring Data MongoDB for **Social Shuffle** (Pune's Board Game Community).

---

## 🚀 How to Run in Spring Tool Suite (STS)

### 1. Prerequisites
- **Java Development Kit (JDK 17 or 21)** installed.
- **Spring Tool Suite 4 (STS)** or Eclipse with Spring Tools plugin.
- **MongoDB** running locally (`localhost:27017`) OR a **MongoDB Atlas Cloud URI**.

### 2. Import into STS
1. Open **Spring Tool Suite (STS)**.
2. Click **File** ➔ **Import...**
3. Select **Maven** ➔ **Existing Maven Projects** and click **Next**.
4. Click **Browse...** and select the `backend-spring-boot-mongodb` folder.
5. Ensure `pom.xml` is checked and click **Finish**.
6. Wait for Maven to download dependencies (Spring Boot, Spring Data MongoDB, etc.).

### 3. Configure MongoDB (`src/main/resources/application.properties`)
- Default is configured for local MongoDB:
  ```properties
  spring.data.mongodb.uri=mongodb://localhost:27017/social_shuffle
  ```
- If using MongoDB Atlas Cloud:
  ```properties
  spring.data.mongodb.uri=mongodb+srv://<username>:<password>@cluster0.mongodb.net/social_shuffle?retryWrites=true&w=majority
  ```

### 4. Run the Application
1. In the **Package Explorer** or **Boot Dashboard**, right-click `SocialShuffleApplication.java`.
2. Select **Run As** ➔ **Spring Boot App**.
3. Console will output:
   ```text
   🎲 Social Shuffle Pune Spring Boot API Started!
   📡 Server running at: http://localhost:8080/api
   🍃 MongoDB Connected: social_shuffle database
   ```
4. Automatic Data Seeder will populate default Pune board games, upcoming meetups, and participants on first launch!

---

## 📡 REST API Reference

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/system/health` | Health check & MongoDB document counts |
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
| `POST` | `/api/registrations` | Book spot / RSVP for meetup |
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
In the React frontend, go to **Admin Console ➔ Backend & MongoDB Hub**:
- Set API URL to `http://localhost:8080/api`.
- Click **Test Connection** to verify live communication with MongoDB.
