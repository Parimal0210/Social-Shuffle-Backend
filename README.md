# Social Shuffle — Java Spring Boot STS Backend (Supabase PostgreSQL)

Production Spring Boot 3.x REST API (`SocialShuffleApplication`) serving **all** Social Shuffle Pune backend APIs, authentication, OTP/SMTP/Gmail/Resend mail delivery, Razorpay payments, and Supabase PostgreSQL persistence. The legacy Node.js backend has been completely retired; the root `server.ts` serves only the React Vite SPA on port 3000 and forwards `/api/*` requests directly to this Java STS service on port `8080`.

---

## Unified Supabase PostgreSQL Tables (19 Tables)

| Table | JPA Entity | Purpose |
|---|---|---|
| `admins` | `AdminAccount` | Administrator accounts & verification |
| `users` | `User` | Core login user records synced with admins/participants |
| `user_credentials` | `UserCredential` | Unified PBKDF2-SHA512 credential store & auth provider tracking |
| `participants` | `Participant` | Shuffler profiles, stats, games played, venues visited |
| `venues` | `Venue` | Pune board game cafes and meetup venues |
| `events` | `ShuffleEvent` | Meetup sessions, capacity, prices, live counts |
| `games` | `Game` | Curated board game catalog & availability |
| `badge_levels` | `BadgeLevel` | 10-tier Shuffler badge progression definitions |
| `participant_badges` | `ParticipantBadge` | Unlocked badges per participant |
| `registrations` | `Registration` | Meetup bookings, guests, QR pass tokens, payment state |
| `event_feedback` | `EventFeedback` | Post-event ratings and reviews |
| `safety_reports` | `SafetyReport` | Confidential community safety reports |
| `volunteer_applications` | `VolunteerApplication` | Table host & volunteer applications |
| `audit_logs` | `AuditLog` | Administrative audit log entries |
| `notifications` | `NotificationItem` | In-app and email dispatch notification records |
| `community_stats` | `CommunityStats` | Homepage CMS copy & live community counters |
| `google_users` | `GoogleUser` | Google OAuth linked account profiles |
| `otp_verifications` | `OtpVerification` | Email registration 6-digit OTP challenges |
| `app_configs` | `AppConfig` | Persisted SMTP/Gmail/Resend & Razorpay settings |

To initialize or sync a fresh Supabase database, run `social_shuffle_supabase_postgres.sql` in Supabase SQL Editor.

---

## Running Java STS Backend + Frontend Proxy

1. **Start Java STS Backend (Port 8080)**:
   - Import `/Social-Shuffle-Backend` as a **Maven Project** in Spring Tool Suite (STS) or run:
     ```bash
     cd Social-Shuffle-Backend
     mvn spring-boot:run
     ```
   - Set environment variables (`SMTP_USER`, `SMTP_PASS`, `RESEND_API_KEY`, `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `SECURITY_TOKEN_SECRET`) or configure SMTP/Razorpay from **Admin -> Communications / Settings** (saved to `app_configs`).
2. **Frontend SPA + Proxy (Port 3000)**:
   - Root `server.ts` serves the Vite React frontend on port `3000` and proxies `/api/*` to `JAVA_BACKEND_URL` (default `http://127.0.0.1:8080`).

---

## Performance & Reliability Highlights

- **Async Email Thread Pool (`mailTaskExecutor`)**: Welcome, registration confirmation passes, volunteer alerts, safety alerts, and batch event broadcasts dispatch asynchronously or in parallel without blocking API responses.
- **Multi-Provider Mail Transport**: Gmail OAuth2 REST API -> Primary SMTP (with automatic 587 <-> 465 SSL failover) -> Resend REST API fallback.
- **PostgreSQL Pool & Batching**: HikariCP connection pooling, Hibernate batch inserts/updates (`batch_size=30`), PostgreSQL `reWriteBatchedInserts=true`, and GZIP HTTP response compression.
- **Read Caching**: Spring `ConcurrentMapCacheManager` caching for `events`, `games`, `venues`, `admins`, `badgeLevels`, and `communityStats` with automatic cache eviction on mutations.
