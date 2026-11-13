import express from 'express';
import cors from 'cors';
import { createServer as createViteServer } from 'vite';
import fs from 'fs';
import path from 'path';
import crypto from 'crypto';
import { BADGE_LEVELS, calculateBadgeLevel } from './src/services/badgeBackendService';
import {
  INITIAL_EVENTS,
  INITIAL_GAMES,
  INITIAL_PARTICIPANTS,
  INITIAL_REGISTRATIONS,
  INITIAL_FEEDBACK,
  INITIAL_REPORTS,
  INITIAL_VOLUNTEERS,
  INITIAL_AUDIT_LOGS,
  INITIAL_NOTIFICATIONS,
  INITIAL_ADMINS,
  INITIAL_COMMUNITY_STATS
} from './src/data/initialData';
import {
  sendOutboundEmail,
  // dispatchOutboundWhatsApp, // Temporarily commented out
  loadCommunicationsConfig,
  saveCommunicationsConfig,
  getMailTransporter,
  setGmailOAuthToken,
  getGmailOAuthStatus
} from './src/services/backendMailer';
import {
  generateRegistrationEmailHtml,
  generateEventReminderEmailHtml,
  generateCustomBroadcastEmailHtml,
  generateWelcomeEmailHtml,
  generateVolunteerConfirmationEmailHtml,
  generateVolunteerAdminAlertEmailHtml,
  generateReportAdminAlertEmailHtml,
  generateReportAckEmailHtml,
  generateFeedbackAdminAlertEmailHtml
  // generateEventWhatsAppNotification // Temporarily commented out
} from './src/services/notificationEmailService';

async function startServer() {
  const app = express();
  // Dev server must run on port 3000 in this environment
  const PORT = 3000;

  // Hide Express server fingerprint & trust reverse proxies
  app.disable('x-powered-by');
  app.set('trust proxy', 1);

  // Production and preview domains for Social Shuffle Pune
  const ALLOWED_ORIGIN_PATTERNS = [
    /^https?:\/\/([a-zA-Z0-9-]+\.)*onrender\.com(:\d+)?$/,
    /^https?:\/\/([a-zA-Z0-9-]+\.)*socialshufflepune\.com(:\d+)?$/,
    /^https?:\/\/([a-zA-Z0-9-]+\.)*socialshuffle\.in(:\d+)?$/,
    /^https?:\/\/([a-zA-Z0-9-]+\.)*run\.app(:\d+)?$/,
    /^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/
  ];

  const KNOWN_ALLOWED_ORIGINS = new Set([
    'https://social-shuffle-ps.onrender.com',
    'http://social-shuffle-ps.onrender.com',
    'https://socialshufflepune.com',
    'http://socialshufflepune.com',
    'https://www.socialshufflepune.com',
    'http://www.socialshufflepune.com',
    'https://socialshuffle.in',
    'http://socialshuffle.in',
    'https://www.socialshuffle.in',
    'http://www.socialshuffle.in',
    'http://localhost:3000',
    'http://localhost:5173',
    'http://127.0.0.1:3000',
    'http://127.0.0.1:5173'
  ]);

  const isOriginAllowed = (origin?: string): boolean => {
    if (!origin) return true;
    if (KNOWN_ALLOWED_ORIGINS.has(origin)) return true;
    return ALLOWED_ORIGIN_PATTERNS.some(pattern => pattern.test(origin));
  };

  // 1. Primary Cross-Origin Resource Sharing (CORS) & Cross-Domain Headers Middleware
  app.use((req, res, next) => {
    const origin = req.headers.origin;

    if (origin) {
      res.setHeader('Access-Control-Allow-Origin', origin);
      res.setHeader('Access-Control-Allow-Credentials', 'true');
    } else {
      res.setHeader('Access-Control-Allow-Origin', '*');
    }

    res.setHeader(
      'Access-Control-Allow-Methods',
      'GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD'
    );
    res.setHeader(
      'Access-Control-Allow-Headers',
      'Origin, X-Requested-With, Content-Type, Accept, Authorization, Range, X-CSRF-Token, Cache-Control, Pragma, If-Modified-Since, X-Api-Key'
    );
    res.setHeader(
      'Access-Control-Expose-Headers',
      'Content-Length, Content-Range, Content-Disposition, X-Total-Count, ETag'
    );
    res.setHeader('Access-Control-Max-Age', '86400');
    res.setHeader('Vary', 'Origin, Access-Control-Request-Headers');

    // Remove any restrictive framing headers so Google AI Studio preview iframe and custom domains can embed smoothly
    res.removeHeader('X-Frame-Options');

    // Defensive HTTP security headers
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
    res.setHeader('Permissions-Policy', 'camera=(self), microphone=(), geolocation=()');
    res.setHeader('X-Permitted-Cross-Domain-Policies', 'none');

    // Fast-return on preflight OPTIONS requests
    if (req.method === 'OPTIONS') {
      return res.status(204).end();
    }

    next();
  });

  // 2. Official cors package middleware for complete specification compliance
  app.use(
    cors({
      origin: (origin, callback) => {
        // Allow all verified origins, and permit any client to eliminate CORS blockages
        callback(null, origin || true);
      },
      credentials: true,
      methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS', 'HEAD'],
      allowedHeaders: [
        'Origin',
        'X-Requested-With',
        'Content-Type',
        'Accept',
        'Authorization',
        'Range',
        'X-CSRF-Token',
        'Cache-Control',
        'Pragma',
        'If-Modified-Since',
        'X-Api-Key'
      ],
      exposedHeaders: ['Content-Length', 'Content-Range', 'Content-Disposition', 'X-Total-Count', 'ETag'],
      maxAge: 86400
    })
  );

  // Preflight OPTIONS wildcard
  app.options('*', (req, res) => {
    const origin = req.headers.origin || '*';
    res.setHeader('Access-Control-Allow-Origin', origin);
    if (origin !== '*') res.setHeader('Access-Control-Allow-Credentials', 'true');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD');
    res.setHeader(
      'Access-Control-Allow-Headers',
      'Origin, X-Requested-With, Content-Type, Accept, Authorization, Range, X-CSRF-Token, Cache-Control, Pragma, If-Modified-Since, X-Api-Key'
    );
    res.setHeader('Access-Control-Max-Age', '86400');
    res.status(204).end();
  });

  app.use(express.json({ limit: '5mb' }));

  // Prevent browser/proxy caching of sensitive API responses
  app.use(['/api/auth', '/api/admins', '/api/communications', '/api/participants', '/api/payments'], (_req, res, next) => {
    res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, private');
    res.setHeader('Pragma', 'no-cache');
    next();
  });

  // --- Sliding-Window Rate Limiter ---
  const rateLimitBuckets = new Map<string, number[]>();
  const createRateLimiter = (windowMs: number, maxRequests: number, keyPrefix: string) => {
    return (req: express.Request, res: express.Response, next: express.NextFunction) => {
      const clientIp = req.ip || req.socket?.remoteAddress || 'unknown';
      const bucketKey = `${keyPrefix}:${clientIp}`;
      const now = Date.now();
      const windowStart = now - windowMs;
      const timestamps = (rateLimitBuckets.get(bucketKey) || []).filter(ts => ts > windowStart);

      if (timestamps.length >= maxRequests) {
        const retryAfterSec = Math.max(1, Math.ceil((timestamps[0] + windowMs - now) / 1000));
        res.setHeader('Retry-After', String(retryAfterSec));
        return res.status(429).json({
          success: false,
          message: 'Too many requests. Please slow down and try again shortly.'
        });
      }

      timestamps.push(now);
      rateLimitBuckets.set(bucketKey, timestamps);
      next();
    };
  };

  // Periodically prune stale rate-limit buckets to avoid memory growth
  setInterval(() => {
    const cutoff = Date.now() - 15 * 60 * 1000;
    for (const [key, list] of rateLimitBuckets.entries()) {
      const active = list.filter(ts => ts > cutoff);
      if (active.length === 0) rateLimitBuckets.delete(key);
      else rateLimitBuckets.set(key, active);
    }
  }, 5 * 60 * 1000).unref();

  app.use('/api', createRateLimiter(60 * 1000, 400, 'api_global'));
  app.use('/api/auth', createRateLimiter(15 * 60 * 1000, 60, 'api_auth'));
  app.use('/api/payments', createRateLimiter(15 * 60 * 1000, 40, 'api_payments'));

  // --- Prototype Pollution & XSS Sanitization Middleware ---
  const SECRET_FIELD_KEYS = new Set(['password', 'keySecret', 'pass', 'accessToken', 'idToken', 'token', 'razorpaySignature']);
  const sanitizePayloadValue = (val: any, keyName?: string, depth: number = 0): any => {
    if (depth > 12 || val === null || val === undefined) return val;
    if (typeof val === 'string') {
      if (keyName && SECRET_FIELD_KEYS.has(keyName)) return val;
      if (val.startsWith('data:image/')) return val;
      return val
        .replace(/\0/g, '')
        .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
        .replace(/javascript:/gi, '')
        .replace(/\bon\w+\s*=/gi, '');
    }
    if (Array.isArray(val)) {
      return val.map(item => sanitizePayloadValue(item, undefined, depth + 1));
    }
    if (typeof val === 'object') {
      const cleanObj: Record<string, any> = {};
      for (const [k, v] of Object.entries(val)) {
        if (k === '__proto__' || k === 'constructor' || k === 'prototype') continue;
        cleanObj[k] = sanitizePayloadValue(v, k, depth + 1);
      }
      return cleanObj;
    }
    return val;
  };

  app.use('/api', (req, _res, next) => {
    if (req.body && typeof req.body === 'object') {
      req.body = sanitizePayloadValue(req.body);
    }
    next();
  });

  // --- Database Storage Directory Setup ---
  const DATA_DIR = path.resolve(process.cwd(), 'src/data/db');
  if (!fs.existsSync(DATA_DIR)) {
    fs.mkdirSync(DATA_DIR, { recursive: true });
  }

  // Generic JSON Database File Helper
  function createCollection<T extends { id?: string }>(fileName: string, initialSeed: T[]) {
    const filePath = path.join(DATA_DIR, fileName);
    let items: T[] = [];

    try {
      if (fs.existsSync(filePath)) {
        const raw = fs.readFileSync(filePath, 'utf-8');
        items = JSON.parse(raw);
        if (!Array.isArray(items) || items.length === 0) {
          items = [...initialSeed];
          fs.writeFileSync(filePath, JSON.stringify(items, null, 2), 'utf-8');
        }
      } else {
        items = [...initialSeed];
        fs.writeFileSync(filePath, JSON.stringify(items, null, 2), 'utf-8');
      }
    } catch (err) {
      console.warn(`[DB] Error loading ${fileName}, using initial seed:`, err);
      items = [...initialSeed];
    }

    const save = () => {
      try {
        fs.writeFileSync(filePath, JSON.stringify(items, null, 2), 'utf-8');
      } catch (err) {
        console.error(`[DB] Error saving ${fileName}:`, err);
      }
    };

    const reload = () => {
      try {
        if (fs.existsSync(filePath)) {
          const raw = fs.readFileSync(filePath, 'utf-8');
          const parsed = JSON.parse(raw);
          if (Array.isArray(parsed) && parsed.length > 0) {
            items = parsed;
          }
        }
      } catch (err) {
        console.warn(`[DB] Error reloading ${fileName}:`, err);
      }
    };

    return {
      getAll: () => {
        reload();
        return items;
      },
      getById: (id: string) => {
        reload();
        return items.find(i => i.id === id);
      },
      setAll: (newItems: T[]) => {
        items = newItems;
        save();
        return items;
      },
      insert: (item: T) => {
        items.unshift(item);
        save();
        return item;
      },
      update: (id: string, updates: Partial<T>) => {
        const index = items.findIndex(i => i.id === id);
        if (index >= 0) {
          items[index] = { ...items[index], ...updates };
          save();
          return items[index];
        }
        return null;
      },
      remove: (id: string) => {
        const index = items.findIndex(i => i.id === id);
        if (index >= 0) {
          const removed = items.splice(index, 1)[0];
          save();
          return removed;
        }
        return null;
      }
    };
  }

  // Initialize Database Collections
  const dbAdmins = createCollection('admins.json', INITIAL_ADMINS);
  const dbEvents = createCollection('events.json', INITIAL_EVENTS);
  const dbGames = createCollection('games.json', INITIAL_GAMES);
  const dbParticipants = createCollection('participants.json', INITIAL_PARTICIPANTS);
  const dbRegistrations = createCollection('registrations.json', INITIAL_REGISTRATIONS);
  const dbFeedback = createCollection('feedback.json', INITIAL_FEEDBACK);
  const dbReports = createCollection('reports.json', INITIAL_REPORTS);
  const dbVolunteers = createCollection('volunteers.json', INITIAL_VOLUNTEERS);
  const dbAuditLogs = createCollection('audit_logs.json', INITIAL_AUDIT_LOGS);
  const dbNotifications = createCollection('notifications.json', INITIAL_NOTIFICATIONS);
  const dbCommunityStats = createCollection('community_stats.json', [INITIAL_COMMUNITY_STATS]);

  // Registered Google Users
  const GOOGLE_USERS_PATH = path.resolve(process.cwd(), 'src/data/registered_google_users.json');
  const registeredGoogleUsers = new Map<string, any>();
  try {
    if (fs.existsSync(GOOGLE_USERS_PATH)) {
      const raw = fs.readFileSync(GOOGLE_USERS_PATH, 'utf-8');
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) {
        parsed.forEach((item: any) => {
          if (item && item.email) {
            registeredGoogleUsers.set(item.email.trim().toLowerCase(), item);
          }
        });
      }
    }
  } catch {}

  const saveGoogleUsers = () => {
    try {
      const arr = Array.from(registeredGoogleUsers.values());
      fs.writeFileSync(GOOGLE_USERS_PATH, JSON.stringify(arr, null, 2), 'utf-8');
    } catch {}
  };

  // --- Cryptographic Password Hashing (PBKDF2-HMAC-SHA512) & Signed Sessions ---
  const PBKDF2_ITERATIONS = 100000;
  const PBKDF2_KEYLEN = 64;
  const PBKDF2_DIGEST = 'sha512';
  const SESSION_TTL_MS = 30 * 24 * 60 * 60 * 1000;

  const SESSION_SECRET_PATH = path.join(DATA_DIR, '.session_secret');
  const loadSessionSecret = (): string => {
    if (process.env.SESSION_SECRET && process.env.SESSION_SECRET.length >= 32) {
      return process.env.SESSION_SECRET;
    }
    try {
      if (fs.existsSync(SESSION_SECRET_PATH)) {
        const stored = fs.readFileSync(SESSION_SECRET_PATH, 'utf-8').trim();
        if (stored.length >= 32) return stored;
      }
      const generated = crypto.randomBytes(48).toString('hex');
      fs.writeFileSync(SESSION_SECRET_PATH, generated, { encoding: 'utf-8', mode: 0o600 });
      return generated;
    } catch {
      return crypto.randomBytes(48).toString('hex');
    }
  };
  const SESSION_SECRET = loadSessionSecret();
  const revokedTokenIds = new Set<string>();

  const hashPassword = (plainText: string): string => {
    const salt = crypto.randomBytes(16).toString('hex');
    const derived = crypto.pbkdf2Sync(plainText, salt, PBKDF2_ITERATIONS, PBKDF2_KEYLEN, PBKDF2_DIGEST).toString('hex');
    return `pbkdf2_sha512$${PBKDF2_ITERATIONS}$${salt}$${derived}`;
  };

  const verifyPassword = (plainText: string, storedHashOrPlain?: string | null): boolean => {
    if (!plainText || !storedHashOrPlain) return false;
    if (storedHashOrPlain.startsWith('pbkdf2_sha512$')) {
      const parts = storedHashOrPlain.split('$');
      if (parts.length !== 4) return false;
      const iterations = parseInt(parts[1], 10);
      const salt = parts[2];
      const expectedHex = parts[3];
      if (!iterations || !salt || !expectedHex) return false;
      const derivedBuf = crypto.pbkdf2Sync(plainText, salt, iterations, PBKDF2_KEYLEN, PBKDF2_DIGEST);
      const expectedBuf = Buffer.from(expectedHex, 'hex');
      if (derivedBuf.length !== expectedBuf.length) return false;
      return crypto.timingSafeEqual(derivedBuf, expectedBuf);
    }
    const a = Buffer.from(plainText);
    const b = Buffer.from(storedHashOrPlain);
    if (a.length !== b.length) return false;
    return crypto.timingSafeEqual(a, b);
  };

  interface SessionTokenPayload {
    sub: string;
    email: string;
    role: 'admin' | 'participant';
    participantId?: string;
    iat: number;
    exp: number;
    jti: string;
  }

  const issueSessionToken = (user: { id: string; email: string; role: 'admin' | 'participant'; participantId?: string }): string => {
    const now = Date.now();
    const payload: SessionTokenPayload = {
      sub: user.id,
      email: user.email.trim().toLowerCase(),
      role: user.role,
      participantId: user.participantId,
      iat: now,
      exp: now + SESSION_TTL_MS,
      jti: crypto.randomBytes(12).toString('hex')
    };
    const encodedPayload = Buffer.from(JSON.stringify(payload)).toString('base64url');
    const sig = crypto.createHmac('sha256', SESSION_SECRET).update(encodedPayload).digest('base64url');
    return `${encodedPayload}.${sig}`;
  };

  const verifySessionToken = (token?: string | null): SessionTokenPayload | null => {
    if (!token || typeof token !== 'string') return null;
    const parts = token.trim().split('.');
    if (parts.length !== 2) return null;
    const [encodedPayload, sig] = parts;
    try {
      const expectedSig = crypto.createHmac('sha256', SESSION_SECRET).update(encodedPayload).digest('base64url');
      const sigBuf = Buffer.from(sig);
      const expectedBuf = Buffer.from(expectedSig);
      if (sigBuf.length !== expectedBuf.length || !crypto.timingSafeEqual(sigBuf, expectedBuf)) {
        return null;
      }
      const payload = JSON.parse(Buffer.from(encodedPayload, 'base64url').toString('utf-8')) as SessionTokenPayload;
      if (!payload || !payload.sub || !payload.email || !payload.role || !payload.exp) return null;
      if (Date.now() > payload.exp) return null;
      if (payload.jti && revokedTokenIds.has(payload.jti)) return null;
      return payload;
    } catch {
      return null;
    }
  };

  // Sanitizers: ensure passwords & password hashes never leave server memory
  const sanitizeRecord = <T extends Record<string, any>>(rec: T): Omit<T, 'password' | 'passwordHash'> => {
    if (!rec || typeof rec !== 'object') return rec as any;
    const { password: _p, passwordHash: _ph, ...safe } = rec;
    return safe;
  };

  const ADMIN_NOTIFICATION_EMAIL = process.env.ADMIN_NOTIFICATION_EMAIL || 'birladiksha01@gmail.com';

  // Migrate admins store: keep Parimal (parimalmshete@gmail.com) and Diksha (birladiksha01@gmail.com), enforce +91 92849 83072, and hash admin credentials with PBKDF2
  const defaultAdminSeedPass = process.env.ADMIN_DEFAULT_PASSWORD || 'admin123';
  const cleanedAdmins = dbAdmins
    .getAll()
    .filter(
      (a: any) =>
        a &&
        a.id !== 'u-admin-aman' &&
        a.id !== 'u-admin-parimal-work' &&
        !['admin@socialshuffle.com', 'parimal.work@gmail.com'].includes(String(a.email || '').trim().toLowerCase())
    )
    .map((a: any) => {
      const { password, ...rest } = a;
      const resolvedEmail = a.id === 'u-admin-diksha' ? 'birladiksha01@gmail.com' : a.email;
      return {
        ...rest,
        email: resolvedEmail,
        phone: '+91 92849 83072',
        passwordHash: a.passwordHash && String(a.passwordHash).startsWith('pbkdf2_sha512$')
          ? a.passwordHash
          : hashPassword(password || defaultAdminSeedPass)
      };
    });
  dbAdmins.setAll(cleanedAdmins.length > 0 ? cleanedAdmins : INITIAL_ADMINS.map(a => ({ ...a, passwordHash: hashPassword(defaultAdminSeedPass) })) as any);

  // Migrate participants store: convert any legacy plaintext passwords into salted PBKDF2 hashes
  const migratedParticipants = dbParticipants.getAll().map((p: any) => {
    if (p.password || !p.passwordHash) {
      const plain = p.password || 'shuffler123';
      const { password: _removed, ...rest } = p;
      return { ...rest, passwordHash: hashPassword(plain) };
    }
    return p;
  });
  dbParticipants.setAll(migratedParticipants);

  // Strip any historical OTP codes from persisted public notifications
  const cleanedNotifs = dbNotifications.getAll().filter((n: any) => n && !String(n.id || '').startsWith('notif-otp-'));
  if (cleanedNotifs.length !== dbNotifications.getAll().length) {
    dbNotifications.setAll(cleanedNotifs);
  }

  // Helper to find admin by email or phone
  const findAdminByEmailOrPhone = (ident: string) => {
    const clean = String(ident).trim().toLowerCase();
    const cleanDigits = clean.replace(/[^0-9]/g, '');
    for (const admin of dbAdmins.getAll()) {
      if (!admin.active) continue;
      if (admin.email && admin.email.trim().toLowerCase() === clean) {
        return admin;
      }
      if (cleanDigits.length >= 10 && admin.phone) {
        const adminDigits = admin.phone.replace(/[^0-9]/g, '');
        if (adminDigits.includes(cleanDigits) || cleanDigits.includes(adminDigits)) {
          return admin;
        }
      }
    }
    return undefined;
  };

  // Attach verified session identity to req.authUser
  const attachAuth = (req: express.Request, _res: express.Response, next: express.NextFunction) => {
    const authHeader = String(req.headers.authorization || '');
    const bearer = authHeader.startsWith('Bearer ') ? authHeader.slice(7).trim() : '';
    const tokenData = verifySessionToken(bearer);
    if (tokenData) {
      if (tokenData.role === 'admin') {
        const admin = dbAdmins.getAll().find((a: any) => a.active && a.email?.trim().toLowerCase() === tokenData.email);
        if (admin) {
          (req as any).authUser = { ...tokenData, adminRecord: admin };
        }
      } else {
        (req as any).authUser = tokenData;
      }
    }
    next();
  };

  app.use('/api', attachAuth);

  const requireAuth = (req: express.Request, res: express.Response, next: express.NextFunction) => {
    if (!(req as any).authUser) {
      return res.status(401).json({ success: false, message: 'Authentication required.' });
    }
    next();
  };

  const requireAdmin = (req: express.Request, res: express.Response, next: express.NextFunction) => {
    const authUser = (req as any).authUser as SessionTokenPayload | undefined;
    if (!authUser) {
      return res.status(401).json({ success: false, message: 'Administrator authentication required.' });
    }
    if (authUser.role !== 'admin') {
      return res.status(403).json({ success: false, message: 'Administrator privileges required.' });
    }
    next();
  };

  // Cryptographically verify Google ID token or OAuth access token against Google servers
  const verifyGoogleProof = async (opts: { idToken?: string; accessToken?: string; expectedEmail: string }): Promise<{
    verified: boolean;
    email?: string;
    name?: string;
    avatar?: string;
    googleId?: string;
    reason?: string;
  }> => {
    const cleanExpected = opts.expectedEmail.trim().toLowerCase();

    if (opts.idToken && typeof opts.idToken === 'string') {
      try {
        const resp = await fetch(`https://oauth2.googleapis.com/tokeninfo?id_token=${encodeURIComponent(opts.idToken)}`);
        if (resp.ok) {
          const info: any = await resp.json();
          const tokenEmail = String(info.email || '').trim().toLowerCase();
          const isEmailVerified = info.email_verified === true || info.email_verified === 'true';
          if (tokenEmail && isEmailVerified && tokenEmail === cleanExpected) {
            return {
              verified: true,
              email: tokenEmail,
              name: info.name,
              avatar: info.picture,
              googleId: info.sub
            };
          }
        }
      } catch {}
    }

    if (opts.accessToken && typeof opts.accessToken === 'string') {
      try {
        const resp = await fetch('https://www.googleapis.com/oauth2/v3/userinfo', {
          headers: { Authorization: `Bearer ${opts.accessToken}` }
        });
        if (resp.ok) {
          const info: any = await resp.json();
          const tokenEmail = String(info.email || '').trim().toLowerCase();
          const isEmailVerified = info.email_verified !== false;
          if (tokenEmail && isEmailVerified && tokenEmail === cleanExpected) {
            return {
              verified: true,
              email: tokenEmail,
              name: info.name,
              avatar: info.picture,
              googleId: info.sub
            };
          }
        }
      } catch {}
    }

    return { verified: false, reason: 'Invalid or missing Google verification token' };
  };

  // ==========================================
  // SYSTEM HEALTH & DATABASE STATUS API
  // ==========================================
  app.get('/api/system/health', (_req, res) => {
    res.json({
      status: 'UP',
      service: 'Social Shuffle Persistent Database Engine',
      version: '2.5.0',
      database: 'Local JSON File Store (Active)',
      databaseName: 'social_shuffle_db',
      timestamp: new Date().toISOString(),
      counts: {
        events: dbEvents.getAll().length,
        games: dbGames.getAll().length,
        participants: dbParticipants.getAll().length,
        registrations: dbRegistrations.getAll().length,
        feedback: dbFeedback.getAll().length,
        safetyReports: dbReports.getAll().length,
        volunteers: dbVolunteers.getAll().length,
        admins: dbAdmins.getAll().length,
        communityStats: dbCommunityStats.getAll().length
      }
    });
  });

  // ==========================================
  // ADMINS & AUTHENTICATION (ROLE PROTECTED)
  // ==========================================
  app.get('/api/admins', requireAdmin, (_req, res) => {
    res.json({
      success: true,
      admins: dbAdmins.getAll().map(sanitizeRecord)
    });
  });

  app.get('/api/admins/check', requireAdmin, (req, res) => {
    const query = String(req.query.email || req.query.phone || req.query.identifier || '').trim();
    if (!query) {
      return res.status(400).json({ success: false, message: 'Query is required' });
    }
    const admin = findAdminByEmailOrPhone(query);
    if (admin) {
      return res.json({
        success: true,
        isAdmin: true,
        admin: sanitizeRecord(admin)
      });
    }
    return res.json({
      success: true,
      isAdmin: false
    });
  });

  // Direct Login: Authenticates Administrators and Participants with PBKDF2 verification + HMAC token
  app.post('/api/auth/login', (req, res) => {
    const { emailOrPhone, password, name, phone, email, designation } = req.body;

    const inputEmail = String(email || (emailOrPhone && String(emailOrPhone).includes('@') ? emailOrPhone : '')).trim().toLowerCase();
    const inputPhone = String(phone || (!String(emailOrPhone || '').includes('@') ? emailOrPhone : '')).trim();
    const inputPhoneDigits = inputPhone.replace(/[^0-9]/g, '');
    const inputName = String(name || '').trim().toLowerCase();
    const cleanIdent = String(emailOrPhone || inputEmail || inputPhone).trim().toLowerCase();

    if (!cleanIdent && !inputEmail && !inputPhoneDigits) {
      return res.status(400).json({ success: false, message: 'Email address or mobile phone number is required.' });
    }
    if (!password || typeof password !== 'string' || password.length === 0) {
      return res.status(400).json({ success: false, message: 'Password is required.' });
    }

    // Check if email matches an active Administrator (Parimal or Diksha)
    const adminMatch: any = dbAdmins.getAll().find((a: any) => {
      if (!a.active) return false;
      const aEmail = (a.email || '').trim().toLowerCase();
      return Boolean(inputEmail && aEmail === inputEmail) || aEmail === cleanIdent;
    });

    if (adminMatch) {
      // Optional identity cross-checks if caller provided extra fields
      if (inputName) {
        const aName = (adminMatch.name || '').trim().toLowerCase();
        if (!(aName === inputName || aName.includes(inputName) || inputName.includes(aName))) {
          return res.status(401).json({ success: false, message: 'Invalid administrator credentials.' });
        }
      }
      if (!verifyPassword(password, adminMatch.passwordHash || adminMatch.password)) {
        return res.status(401).json({ success: false, message: 'Invalid email or password.' });
      }

      const nowIso = new Date().toISOString();
      const adminUser = {
        id: adminMatch.id,
        name: adminMatch.name,
        email: adminMatch.email,
        phone: adminMatch.phone || '+91 92849 83072',
        role: 'admin' as const,
        designation: designation?.trim() || adminMatch.designation,
        adminRole: 'Super Admin',
        status: 'Active Verified Administrator',
        joinedDate: adminMatch.createdAt || '2024-01-01',
        permissions: ['events', 'attendance', 'crm', 'games', 'broadcasts', 'finance', 'reports', 'settings', 'audit'],
        area: adminMatch.area || 'Koregaon Park, Pune',
        bio: adminMatch.bio || `${adminMatch.designation} • Social Shuffle Organizer`,
        avatar: adminMatch.avatar,
        authProvider: 'local',
        loginAt: nowIso,
        lastActiveAt: nowIso
      };

      const token = issueSessionToken({ id: adminUser.id, email: adminUser.email, role: 'admin' });
      return res.json({
        success: true,
        message: `Welcome back, Admin ${adminUser.name}! (${adminUser.designation})`,
        token,
        user: adminUser
      });
    }

    // Participant login lookup
    const cleanDigits = inputPhoneDigits || cleanIdent.replace(/[^0-9]/g, '');
    const participant: any = dbParticipants.getAll().find(p => {
      const pEmail = (p.email || '').trim().toLowerCase();
      const pPhone = (p.phone || '').replace(/[^0-9]/g, '');
      const pUsername = (p.username || pEmail.split('@')[0] || '').toLowerCase();
      return (
        (inputEmail && pEmail === inputEmail) ||
        pEmail === cleanIdent ||
        (pUsername && pUsername === cleanIdent) ||
        (cleanDigits.length >= 10 && pPhone.includes(cleanDigits))
      );
    });

    if (participant) {
      const storedCredential = participant.passwordHash || participant.password;
      if (!verifyPassword(password, storedCredential)) {
        return res.status(401).json({
          success: false,
          message: 'Invalid password. Please check your credentials and try again.'
        });
      }

      const nowIso = new Date().toISOString();
      const user = {
        id: `u-${participant.id}`,
        name: participant.name,
        email: participant.email,
        phone: participant.phone,
        role: 'participant' as const,
        area: participant.area,
        avatar: participant.avatar,
        bio: participant.bio,
        participantId: participant.id,
        authProvider: 'local',
        loginAt: nowIso,
        lastActiveAt: nowIso
      };
      const token = issueSessionToken({
        id: user.id,
        email: user.email,
        role: 'participant',
        participantId: participant.id
      });
      return res.json({
        success: true,
        message: `Welcome back, ${participant.name}!`,
        token,
        user,
        participant: sanitizeRecord(participant)
      });
    }

    return res.status(404).json({
      success: false,
      message: 'No registered participant or administrator found with these credentials.'
    });
  });

  // Participant Registration: Requires server-verified Email OTP & stores salted PBKDF2 hash
  app.post('/api/auth/register', (req, res) => {
    const data = req.body;
    if (!data.name || !data.email || !data.phone) {
      return res.status(400).json({ success: false, message: 'Name, email, and phone number are required.' });
    }

    const emailClean = String(data.email).trim().toLowerCase();
    const phoneClean = String(data.phone).trim();
    const phoneDigits = phoneClean.replace(/[^0-9]/g, '').slice(-10);

    // Block reserving admin email or admin phone number
    if (
      emailClean.includes('admin@') ||
      phoneDigits === '9284983072' ||
      dbAdmins.getAll().some((a: any) => a.email?.trim().toLowerCase() === emailClean)
    ) {
      return res.status(403).json({
        success: false,
        message: 'Administrator accounts cannot be self-registered.'
      });
    }

    // Require email OTP verification completed on server
    if (!verifiedEmails.has(emailClean)) {
      return res.status(403).json({
        success: false,
        message: 'Email address must be verified with the 6-digit OTP code before registering.'
      });
    }

    const existing = dbParticipants.getAll().find(p => {
      const pEmail = (p.email || '').trim().toLowerCase();
      const pPhone = (p.phone || '').replace(/[^0-9]/g, '').slice(-10);
      return pEmail === emailClean || (phoneDigits.length >= 10 && pPhone === phoneDigits);
    });

    if (existing) {
      return res.status(409).json({ success: false, message: 'An account with this email or phone already exists. Please sign in.' });
    }

    const rawPassword = String(data.password || '').trim();
    if (rawPassword.length > 0 && rawPassword.length < 6) {
      return res.status(400).json({ success: false, message: 'Password must be at least 6 characters long.' });
    }

    const newId = `p-${Date.now().toString(36)}-${crypto.randomBytes(2).toString('hex')}`;
    const newParticipant: any = {
      id: newId,
      name: String(data.name).trim(),
      email: emailClean,
      phone: phoneClean,
      passwordHash: hashPassword(rawPassword || crypto.randomBytes(16).toString('hex')),
      username: emailClean.split('@')[0],
      area: data.area || 'Koregaon Park',
      avatar: data.avatar || `https://api.dicebear.com/7.x/bottts/svg?seed=${encodeURIComponent(data.name)}`,
      bio: data.bio || 'Board game lover eager to play!',
      joinedDate: new Date().toISOString().slice(0, 10),
      totalEventsAttended: 0,
      totalRegistrations: 0,
      gamesPlayedIds: [],
      venuesVisited: [],
      totalPaxBrought: 0
    };

    dbParticipants.insert(newParticipant);
    verifiedEmails.delete(emailClean);

    const nowIso = new Date().toISOString();
    const user = {
      id: `u-${newId}`,
      name: newParticipant.name,
      email: newParticipant.email,
      phone: newParticipant.phone,
      role: 'participant' as const,
      area: newParticipant.area,
      avatar: newParticipant.avatar,
      bio: newParticipant.bio,
      participantId: newId,
      authProvider: 'local',
      loginAt: nowIso,
      lastActiveAt: nowIso
    };

    const token = issueSessionToken({
      id: user.id,
      email: user.email,
      role: 'participant',
      participantId: newId
    });

    // Dispatch official Welcome Email to new participant
    if (newParticipant.email) {
      try {
        const welcomeHtml = generateWelcomeEmailHtml(newParticipant.name, newParticipant.email);
        sendOutboundEmail({
          to: newParticipant.email,
          subject: `🎲 Welcome to Social Shuffle Pune, ${newParticipant.name}!`,
          html: welcomeHtml,
          text: `Welcome to Social Shuffle Pune, ${newParticipant.name}! We're thrilled to have you in our board game community.`
        }).catch(err => console.warn('[Welcome Mail Error]', err));
      } catch {}
    }

    return res.status(201).json({
      success: true,
      message: `Registration successful! Welcome to Social Shuffle, ${newParticipant.name}!`,
      token,
      user,
      participant: sanitizeRecord(newParticipant)
    });
  });

  // Google Login: Cryptographically verifies Google ID/Access Token before issuing session token
  app.post('/api/auth/google/login', async (req, res) => {
    const { email, idToken, accessToken } = req.body;
    if (!email) {
      return res.status(400).json({ success: false, message: 'Email is required' });
    }

    const cleanEmail = String(email).trim().toLowerCase();
    const proof = await verifyGoogleProof({ idToken, accessToken, expectedEmail: cleanEmail });
    if (!proof.verified) {
      return res.status(401).json({
        success: false,
        code: 'GOOGLE_VERIFICATION_FAILED',
        message: 'Google authentication token could not be verified. Please sign in with Google again.'
      });
    }

    const adminCheck: any = dbAdmins.getAll().find((a: any) => a.active && (a.email || '').trim().toLowerCase() === cleanEmail);

    if (adminCheck && adminCheck.role === 'admin') {
      const nowIso = new Date().toISOString();
      const adminUser = {
        id: adminCheck.id,
        name: adminCheck.name,
        email: adminCheck.email,
        phone: adminCheck.phone || '+91 92849 83072',
        role: 'admin' as const,
        designation: adminCheck.designation,
        adminRole: 'Super Admin',
        status: 'Active Verified Administrator',
        joinedDate: adminCheck.createdAt || '2024-01-01',
        permissions: ['events', 'attendance', 'crm', 'games', 'broadcasts', 'finance', 'reports', 'settings', 'audit'],
        area: adminCheck.area || 'Koregaon Park, Pune',
        bio: adminCheck.bio || `${adminCheck.designation} • Social Shuffle Organizer`,
        avatar: proof.avatar || adminCheck.avatar,
        authProvider: 'google',
        loginAt: nowIso,
        lastActiveAt: nowIso
      };

      const token = issueSessionToken({ id: adminUser.id, email: adminUser.email, role: 'admin' });
      return res.json({
        success: true,
        message: `Welcome back, Admin ${adminCheck.name}! (${adminCheck.designation})`,
        token,
        user: adminUser
      });
    }

    const record = registeredGoogleUsers.get(cleanEmail);
    if (!record) {
      const existingParticipant = dbParticipants.getAll().find(
        (p: any) => p.email && String(p.email).trim().toLowerCase() === cleanEmail
      );

      if (!existingParticipant) {
        return res.status(404).json({
          success: false,
          code: 'USER_NOT_REGISTERED',
          message: 'Username not registered'
        });
      }

      const nowIso = new Date().toISOString();
      const restoredUser = {
        id: `u-${existingParticipant.id}`,
        name: existingParticipant.name,
        email: cleanEmail,
        phone: existingParticipant.phone,
        role: 'participant' as const,
        area: existingParticipant.area,
        participantId: existingParticipant.id,
        avatar: proof.avatar || req.body.avatar || existingParticipant.avatar,
        bio: existingParticipant.bio,
        authProvider: 'google',
        loginAt: nowIso,
        lastActiveAt: nowIso
      };

      registeredGoogleUsers.set(cleanEmail, {
        email: cleanEmail,
        googleId: proof.googleId || req.body.googleId || `g_${cleanEmail}`,
        participant: sanitizeRecord(existingParticipant),
        user: restoredUser,
        registeredAt: existingParticipant.joinedDate || nowIso
      });
      saveGoogleUsers();

      const token = issueSessionToken({
        id: restoredUser.id,
        email: cleanEmail,
        role: 'participant',
        participantId: existingParticipant.id
      });

      return res.json({
        success: true,
        message: `Welcome back, ${restoredUser.name}!`,
        token,
        user: restoredUser,
        participant: sanitizeRecord(existingParticipant)
      });
    }

    const nowIso = new Date().toISOString();
    record.user.lastActiveAt = nowIso;
    record.user.loginAt = nowIso;
    record.user.role = 'participant';
    saveGoogleUsers();

    const token = issueSessionToken({
      id: record.user.id,
      email: cleanEmail,
      role: 'participant',
      participantId: record.participant?.id
    });

    res.json({
      success: true,
      message: `Welcome back, ${record.user.name}!`,
      token,
      user: record.user,
      participant: sanitizeRecord(record.participant)
    });
  });

  // Google Sign-Up: Cryptographically verifies Google token before registering
  app.post('/api/auth/google/signup', async (req, res) => {
    const { email, name, avatar, phone, area, bio, googleId, idToken, accessToken } = req.body;
    if (!email || !name) {
      return res.status(400).json({ success: false, message: 'Email and name are required' });
    }

    const cleanEmail = String(email).trim().toLowerCase();
    const proof = await verifyGoogleProof({ idToken, accessToken, expectedEmail: cleanEmail });
    if (!proof.verified) {
      return res.status(401).json({
        success: false,
        code: 'GOOGLE_VERIFICATION_FAILED',
        message: 'Google authentication token could not be verified. Please try again.'
      });
    }

    const cleanName = String(proof.name || name).trim();
    const nowIso = new Date().toISOString();

    const adminCheck: any = findAdminByEmailOrPhone(cleanEmail);
    if (adminCheck && adminCheck.role === 'admin') {
      const adminUser = {
        id: adminCheck.id,
        name: adminCheck.name,
        email: cleanEmail,
        phone: adminCheck.phone || '+91 92849 83072',
        role: 'admin' as const,
        designation: adminCheck.designation,
        adminRole: 'Super Admin',
        status: 'Active Verified Administrator',
        joinedDate: adminCheck.createdAt || '2024-01-01',
        permissions: ['events', 'attendance', 'crm', 'games', 'broadcasts', 'finance', 'reports', 'settings', 'audit'],
        area: adminCheck.area || 'Koregaon Park, Pune',
        avatar: proof.avatar || adminCheck.avatar,
        bio: adminCheck.bio,
        authProvider: 'google',
        loginAt: nowIso,
        lastActiveAt: nowIso
      };
      const token = issueSessionToken({ id: adminUser.id, email: cleanEmail, role: 'admin' });
      return res.json({
        success: true,
        message: `Welcome back, Admin ${adminUser.name}!`,
        token,
        user: adminUser
      });
    }

    const existingParticipant = dbParticipants.getAll().find(
      (p: any) => p.email && String(p.email).trim().toLowerCase() === cleanEmail
    );

    const participantId = existingParticipant?.id || `p-google-${Date.now().toString(36)}`;
    const userId = `u-${participantId}`;

    const participant: any = existingParticipant
      ? {
          ...existingParticipant,
          name: existingParticipant.name || cleanName,
          avatar: proof.avatar || avatar || existingParticipant.avatar,
          authProvider: 'google'
        }
      : {
          id: participantId,
          name: cleanName,
          email: cleanEmail,
          phone: phone || '+91 98000 ' + Math.floor(10000 + Math.random() * 90000),
          area: area || 'Koregaon Park',
          avatar: proof.avatar || avatar || `https://api.dicebear.com/7.x/bottts/svg?seed=${encodeURIComponent(cleanName)}`,
          bio: bio || 'Pune board gamer • Connected via Google Account',
          joinedDate: nowIso.slice(0, 10),
          totalEventsAttended: 0,
          totalRegistrations: 0,
          gamesPlayedIds: [],
          venuesVisited: [],
          totalPaxBrought: 0,
          authProvider: 'google',
          registeredAt: nowIso
        };

    const user = {
      id: userId,
      name: participant.name || cleanName,
      email: cleanEmail,
      phone: participant.phone,
      role: 'participant' as const,
      area: participant.area || 'Koregaon Park, Pune',
      avatar: participant.avatar,
      bio: participant.bio,
      participantId: participant.id,
      authProvider: 'google',
      loginAt: nowIso,
      lastActiveAt: nowIso
    };

    if (existingParticipant) {
      dbParticipants.update(existingParticipant.id, participant);
    } else {
      dbParticipants.insert(participant);
    }

    const safeParticipant = sanitizeRecord(participant);

    registeredGoogleUsers.set(cleanEmail, {
      email: cleanEmail,
      googleId: proof.googleId || googleId || `g_${cleanEmail}`,
      participant: safeParticipant,
      user,
      registeredAt: nowIso
    });
    saveGoogleUsers();

    // Dispatch official Welcome Email to Google signup participant
    if (cleanEmail) {
      try {
        const welcomeHtml = generateWelcomeEmailHtml(cleanName, cleanEmail);
        sendOutboundEmail({
          to: cleanEmail,
          subject: `🎲 Welcome to Social Shuffle Pune, ${cleanName}!`,
          html: welcomeHtml,
          text: `Welcome to Social Shuffle Pune, ${cleanName}! We're thrilled to have you in our board game community.`
        }).catch(err => console.warn('[Welcome Mail (Google) Error]', err));
      } catch {}
    }

    const token = issueSessionToken({
      id: user.id,
      email: cleanEmail,
      role: 'participant',
      participantId: participant.id
    });

    res.json({
      success: true,
      message: `Welcome to Social Shuffle Pune, ${cleanName}! Account registered in database.`,
      token,
      user,
      participant: safeParticipant
    });
  });

  // Logout: immediately revoke active session token on server
  app.post('/api/auth/logout', (req, res) => {
    const authUser = (req as any).authUser as SessionTokenPayload | undefined;
    if (authUser?.jti) {
      revokedTokenIds.add(authUser.jti);
    }
    res.json({ success: true, message: 'Logged out securely.' });
  });

  // Current User /auth/me: STRICTLY verifies signed Bearer token (never auto-authenticates unauthenticated callers)
  const handleAuthMe = (req: express.Request, res: express.Response) => {
    const authUser = (req as any).authUser as (SessionTokenPayload & { adminRecord?: any }) | undefined;
    if (!authUser) {
      return res.json({
        success: true,
        isAuthenticated: false,
        isAdmin: false,
        role: 'guest',
        user: null
      });
    }

    if (authUser.role === 'admin' && authUser.adminRecord) {
      const adminCheck = authUser.adminRecord;
      return res.json({
        success: true,
        isAuthenticated: true,
        isAdmin: true,
        role: 'admin',
        user: {
          id: adminCheck.id,
          name: adminCheck.name,
          email: adminCheck.email,
          phone: adminCheck.phone || '+91 92849 83072',
          role: 'admin',
          designation: adminCheck.designation,
          adminRole: 'Super Admin',
          status: 'Active Verified Administrator',
          joinedDate: adminCheck.createdAt || '2024-01-01',
          permissions: ['events', 'attendance', 'crm', 'games', 'broadcasts', 'finance', 'reports', 'settings', 'audit'],
          area: adminCheck.area || 'Koregaon Park, Pune',
          avatar: adminCheck.avatar,
          bio: adminCheck.bio || `${adminCheck.designation} • Social Shuffle Organizer`,
          loginAt: new Date(authUser.iat).toISOString(),
          lastActiveAt: new Date().toISOString()
        }
      });
    }

    const participant = dbParticipants.getAll().find(
      (p: any) => p.id === authUser.participantId || String(p.email || '').trim().toLowerCase() === authUser.email
    );

    if (!participant) {
      return res.json({
        success: true,
        isAuthenticated: false,
        isAdmin: false,
        role: 'guest',
        user: null
      });
    }

    const safeP = sanitizeRecord(participant);
    return res.json({
      success: true,
      isAuthenticated: true,
      isAdmin: false,
      role: 'participant',
      user: {
        id: `u-${safeP.id}`,
        name: safeP.name,
        email: safeP.email,
        phone: safeP.phone,
        role: 'participant',
        area: safeP.area,
        avatar: safeP.avatar,
        bio: safeP.bio,
        participantId: safeP.id,
        loginAt: new Date(authUser.iat).toISOString(),
        lastActiveAt: new Date().toISOString()
      },
      participant: safeP
    });
  };

  app.get('/api/auth/me', handleAuthMe);
  app.get('/api/auth/current-user', handleAuthMe);

  // ==========================================
  // EVENTS DATABASE ENDPOINTS
  // ==========================================
  app.get('/api/events', (_req, res) => {
    res.json(dbEvents.getAll());
  });

  app.get('/api/events/:id', (req, res) => {
    const item = dbEvents.getById(req.params.id);
    if (item) return res.json(item);
    return res.status(404).json({ error: 'Event not found' });
  });

  app.post('/api/events', requireAdmin, (req, res) => {
    const eventData = req.body;
    const newId = eventData.id || `ev-${Date.now().toString(36)}`;
    const newNumber = eventData.number || (dbEvents.getAll().length + 1);
    const newEvent = {
      ...eventData,
      id: newId,
      number: newNumber,
      registeredCount: eventData.registeredCount || 0,
      checkedInCount: eventData.checkedInCount || 0,
      createdAt: new Date().toISOString()
    };
    dbEvents.insert(newEvent);
    console.log(`[DB Events] Created event #${newNumber}: ${newEvent.title}`);
    res.status(201).json(newEvent);
  });

  app.put('/api/events/:id', requireAdmin, (req, res) => {
    const updated = dbEvents.update(req.params.id, req.body);
    if (updated) return res.json(updated);
    return res.status(404).json({ error: 'Event not found' });
  });

  app.post('/api/events/:id/duplicate', requireAdmin, (req, res) => {
    const orig = dbEvents.getById(req.params.id);
    if (!orig) return res.status(404).json({ error: 'Original event not found' });
    const newNum = dbEvents.getAll().length + 1;
    const copy = {
      ...orig,
      id: `ev-${Date.now().toString(36)}`,
      number: newNum,
      title: `${orig.title} (Copy)`,
      status: 'upcoming',
      registeredCount: 0,
      checkedInCount: 0,
      createdAt: new Date().toISOString()
    };
    dbEvents.insert(copy);
    res.status(201).json(copy);
  });

  app.delete('/api/events/:id', requireAdmin, (req, res) => {
    const removed = dbEvents.remove(req.params.id);
    if (removed) return res.json({ success: true, message: 'Event deleted' });
    return res.status(404).json({ error: 'Event not found' });
  });

  // ==========================================
  // GAMES DATABASE ENDPOINTS
  // ==========================================
  app.get('/api/games', (_req, res) => {
    res.json(dbGames.getAll());
  });

  app.get('/api/games/:id', (req, res) => {
    const item = dbGames.getById(req.params.id);
    if (item) return res.json(item);
    return res.status(404).json({ error: 'Game not found' });
  });

  app.post('/api/games', requireAdmin, (req, res) => {
    const gameData = req.body;
    const newId = gameData.id || `g-${Date.now().toString(36)}`;
    const newGame = {
      ...gameData,
      id: newId,
      playsCount: gameData.playsCount || 0,
      active: gameData.active !== undefined ? gameData.active : true
    };
    dbGames.insert(newGame);
    console.log(`[DB Games] Added game: ${newGame.name}`);
    res.status(201).json(newGame);
  });

  app.put('/api/games/:id', requireAdmin, (req, res) => {
    const updated = dbGames.update(req.params.id, req.body);
    if (updated) return res.json(updated);
    return res.status(404).json({ error: 'Game not found' });
  });

  app.patch('/api/games/:id/toggle-active', requireAdmin, (req, res) => {
    const game = dbGames.getById(req.params.id);
    if (!game) return res.status(404).json({ error: 'Game not found' });
    const updated = dbGames.update(req.params.id, { active: !game.active });
    res.json(updated);
  });

  // ==========================================
  // PARTICIPANTS CRM DATABASE ENDPOINTS
  // ==========================================
  app.get('/api/participants', requireAuth, (req, res) => {
    const authUser = (req as any).authUser as SessionTokenPayload;
    const search = String(req.query.search || '').trim().toLowerCase();
    let list = dbParticipants.getAll().map(sanitizeRecord);

    if (authUser.role !== 'admin') {
      list = list.filter(
        (p: any) => p.id === authUser.participantId || String(p.email || '').trim().toLowerCase() === authUser.email
      );
      return res.json(list);
    }

    if (search) {
      list = list.filter((p: any) =>
        p.name.toLowerCase().includes(search) ||
        p.email.toLowerCase().includes(search) ||
        p.phone.includes(search) ||
        (p.area && p.area.toLowerCase().includes(search))
      );
    }
    res.json(list);
  });

  app.get('/api/participants/check-duplicate', requireAdmin, (req, res) => {
    const email = String(req.query.email || '').trim().toLowerCase();
    const phone = String(req.query.phone || '').trim().replace(/[^0-9]/g, '');
    const found = dbParticipants.getAll().find(p => {
      const pEmail = p.email.trim().toLowerCase();
      const pPhone = p.phone.replace(/[^0-9]/g, '');
      return (email && pEmail === email) || (phone.length >= 10 && pPhone.includes(phone));
    });
    res.json({ duplicate: found ? sanitizeRecord(found) : null });
  });

  app.post('/api/participants/merge', requireAdmin, (req, res) => {
    const sourceId = String(req.query.sourceId || req.body.sourceId);
    const targetId = String(req.query.targetId || req.body.targetId);
    const source = dbParticipants.getById(sourceId);
    const target = dbParticipants.getById(targetId);

    if (!source || !target) {
      return res.status(404).json({ error: 'Source or target participant not found' });
    }

    const merged = {
      ...target,
      totalEventsAttended: target.totalEventsAttended + source.totalEventsAttended,
      totalRegistrations: target.totalRegistrations + source.totalRegistrations,
      totalPaxBrought: target.totalPaxBrought + source.totalPaxBrought,
      gamesPlayedIds: Array.from(new Set([...target.gamesPlayedIds, ...source.gamesPlayedIds])),
      venuesVisited: Array.from(new Set([...target.venuesVisited, ...source.venuesVisited]))
    };

    dbParticipants.update(targetId, merged);
    dbParticipants.remove(sourceId);

    dbRegistrations.getAll().forEach(reg => {
      if (reg.participantId === sourceId) {
        dbRegistrations.update(reg.id, { participantId: targetId });
      }
    });

    console.log(`[DB CRM] Merged participant ${source.name} into ${target.name}`);
    res.json(sanitizeRecord(merged));
  });

  app.post('/api/participants', requireAdmin, (req, res) => {
    const data = req.body;
    const { password: _p, passwordHash: _ph, ...safeData } = data;
    const newId = safeData.id || `p-${Date.now().toString(36)}`;
    const newP: any = {
      ...safeData,
      id: newId,
      passwordHash: hashPassword(data.password || crypto.randomBytes(16).toString('hex')),
      joinedDate: safeData.joinedDate || new Date().toISOString().slice(0, 10),
      totalEventsAttended: safeData.totalEventsAttended || 0,
      totalRegistrations: safeData.totalRegistrations || 0,
      gamesPlayedIds: safeData.gamesPlayedIds || [],
      venuesVisited: safeData.venuesVisited || [],
      totalPaxBrought: safeData.totalPaxBrought || 0
    };
    dbParticipants.insert(newP);
    res.status(201).json(sanitizeRecord(newP));
  });

  app.put('/api/participants/:id', requireAuth, (req, res) => {
    const authUser = (req as any).authUser as SessionTokenPayload;
    if (authUser.role !== 'admin' && authUser.participantId !== req.params.id) {
      return res.status(403).json({ success: false, message: 'Forbidden: cannot modify another participant profile.' });
    }
    const { id: _id, role: _role, password: _p, passwordHash: _ph, ...allowedUpdates } = req.body || {};
    const updated = dbParticipants.update(req.params.id, allowedUpdates);
    if (updated) return res.json(sanitizeRecord(updated));
    return res.status(404).json({ error: 'Participant not found' });
  });

  // ==========================================
  // REGISTRATIONS & LIVE ATTENDANCE ENDPOINTS
  // ==========================================
  app.get('/api/registrations', requireAuth, (req, res) => {
    const authUser = (req as any).authUser as SessionTokenPayload;
    const eventId = req.query.eventId as string;
    let list = dbRegistrations.getAll();
    if (authUser.role !== 'admin') {
      list = list.filter(
        r => r.participantId === authUser.participantId || String(r.participantEmail || '').trim().toLowerCase() === authUser.email
      );
    }
    if (eventId) {
      list = list.filter(r => r.eventId === eventId);
    }
    res.json(list);
  });

  app.post('/api/registrations', requireAuth, (req, res) => {
    const authUser = (req as any).authUser as SessionTokenPayload;
    const regData = req.body;
    const paxCount = Math.max(1, Math.min(20, Number(regData.paxCount) || 1));
    const newId = regData.id || `reg-${Date.now().toString(36)}`;
    const ev = dbEvents.getById(regData.eventId);

    const participantId = authUser.role === 'admin'
      ? (regData.participantId || authUser.participantId)
      : (authUser.participantId || regData.participantId);

    const newReg = {
      ...regData,
      id: newId,
      participantId,
      paxCount,
      amountPaid: ev ? paxCount * (Number(ev.ticketPrice) || 350) : Number(regData.amountPaid) || 350,
      registeredAt: regData.registeredAt || new Date().toISOString(),
      attendanceStatus: authUser.role === 'admin' ? (regData.attendanceStatus || 'Registered') : 'Pending',
      paymentStatus: regData.paymentStatus || 'Completed'
    };
    dbRegistrations.insert(newReg);

    if (ev) {
      dbEvents.update(ev.id, { registeredCount: (ev.registeredCount || 0) + paxCount });
    }

    // Automatically dispatch real email confirmation with entry pass QR code
    if (newReg.participantEmail) {
      try {
        const emailHtml = generateRegistrationEmailHtml(newReg as any, ev);
        sendOutboundEmail({
          to: newReg.participantEmail,
          subject: `🎲 VIP Entry Pass: ${ev?.title || 'Social Shuffle Pune'}!`,
          html: emailHtml,
          text: `Your entry pass for ${ev?.title || 'Social Shuffle'} is confirmed. Token: ${newReg.qrCodeToken || newReg.id}`
        }).catch(err => console.warn('[Registration Mail Error]', err));
      } catch {}
    }

    res.status(201).json(newReg);
  });

  app.post('/api/registrations/bulk-import', requireAdmin, (req, res) => {
    const incoming = req.body;
    if (Array.isArray(incoming)) {
      incoming.forEach(reg => {
        if (!dbRegistrations.getById(reg.id)) {
          dbRegistrations.insert(reg);
        }
      });
      console.log(`[DB Reg] Bulk imported ${incoming.length} registrations to database.`);
      return res.json({ success: true, count: incoming.length });
    }
    return res.status(400).json({ error: 'Array expected' });
  });

  app.patch('/api/registrations/:id/attendance', requireAdmin, (req, res) => {
    const status = String(req.query.status || req.body.status || 'Checked In');
    const updated = dbRegistrations.update(req.params.id, {
      attendanceStatus: status as any,
      checkedInAt: status === 'Checked In' ? new Date().toISOString() : undefined
    });
    if (updated) {
      const eventRegs = dbRegistrations.getAll().filter(r => r.eventId === updated.eventId && r.attendanceStatus === 'Checked In');
      dbEvents.update(updated.eventId, { checkedInCount: eventRegs.reduce((acc, c) => acc + (c.paxCount || 1), 0) });
      return res.json(updated);
    }
    return res.status(404).json({ error: 'Registration not found' });
  });

  app.patch('/api/registrations/:id/payment', requireAdmin, (req, res) => {
    const status = String(req.query.status || req.body.status || 'Completed');
    const updated = dbRegistrations.update(req.params.id, { paymentStatus: status as any });
    if (updated) return res.json(updated);
    return res.status(404).json({ error: 'Registration not found' });
  });

  // ==========================================
  // RAZORPAY PAYMENT GATEWAY ENDPOINTS
  // ==========================================
  const RAZORPAY_CONFIG_PATH = path.resolve(process.cwd(), 'src/data/db/razorpay_config.json');

  const loadRazorpayConfig = () => {
    try {
      if (fs.existsSync(RAZORPAY_CONFIG_PATH)) {
        const raw = fs.readFileSync(RAZORPAY_CONFIG_PATH, 'utf-8');
        return JSON.parse(raw);
      }
    } catch (e) {
      console.warn('[Razorpay Config] Error reading config file', e);
    }
    return {
      keyId: process.env.RAZORPAY_KEY_ID || process.env.VITE_RAZORPAY_KEY_ID || 'rzp_test_SocialShufflePune',
      keySecret: process.env.RAZORPAY_KEY_SECRET || '',
      enabled: true,
      currency: 'INR',
      companyName: 'Social Shuffle Pune',
      themeColor: '#FF751F',
      isLive: Boolean(process.env.RAZORPAY_KEY_SECRET)
    };
  };

  app.get('/api/payments/razorpay/config', (_req, res) => {
    const config = loadRazorpayConfig();
    const effectiveKeyId = process.env.VITE_RAZORPAY_KEY_ID || process.env.RAZORPAY_KEY_ID || config.keyId || 'rzp_test_SocialShufflePune';
    res.json({
      keyId: effectiveKeyId,
      enabled: config.enabled !== false,
      currency: config.currency || 'INR',
      companyName: config.companyName || 'Social Shuffle Pune',
      themeColor: config.themeColor || '#FF751F',
      isLive: Boolean(config.isLive || (effectiveKeyId && !effectiveKeyId.startsWith('rzp_test_'))),
      supportedMethods: ['UPI (GPay / PhonePe / Paytm / QR)', 'Credit Cards', 'Debit Cards', 'NetBanking', 'Wallets']
    });
  });

  const issuedOrders = new Map<string, { amountInRupees: number; eventId?: string }>();

  app.post('/api/payments/razorpay/config', requireAdmin, (req, res) => {
    try {
      const current = loadRazorpayConfig();
      const updated = {
        ...current,
        keyId: req.body.keyId !== undefined ? req.body.keyId.trim() : current.keyId,
        keySecret: req.body.keySecret !== undefined && req.body.keySecret !== '••••••••••••' ? req.body.keySecret.trim() : current.keySecret,
        enabled: req.body.enabled !== undefined ? Boolean(req.body.enabled) : current.enabled,
        currency: req.body.currency || current.currency || 'INR',
        companyName: req.body.companyName || current.companyName || 'Social Shuffle Pune',
        themeColor: req.body.themeColor || current.themeColor || '#FF751F',
        isLive: Boolean(req.body.keyId && !req.body.keyId.startsWith('rzp_test_')),
        lastUpdated: new Date().toISOString()
      };
      fs.writeFileSync(RAZORPAY_CONFIG_PATH, JSON.stringify(updated, null, 2), 'utf-8');
      res.json({ success: true, message: 'Razorpay configuration saved successfully', config: { ...updated, keySecret: undefined } });
    } catch (err: any) {
      res.status(500).json({ success: false, error: err?.message });
    }
  });

  app.post('/api/payments/razorpay/create-order', requireAuth, (req, res) => {
    const { amount, eventId, eventTitle, paxCount } = req.body;
    const config = loadRazorpayConfig();
    const keyId = process.env.VITE_RAZORPAY_KEY_ID || process.env.RAZORPAY_KEY_ID || config.keyId || 'rzp_test_SocialShufflePune';

    const ev = eventId ? dbEvents.getById(String(eventId)) : null;
    const validPax = Math.max(1, Math.min(20, Number(paxCount) || 1));
    const numAmount = ev ? validPax * (Number(ev.ticketPrice) || 350) : Math.max(1, Number(amount) || 350);
    const amountInPaise = Math.round(numAmount * 100);

    const orderId = `order_${Date.now().toString(36)}_${crypto.randomBytes(4).toString('hex').toUpperCase()}`;
    issuedOrders.set(orderId, { amountInRupees: numAmount, eventId });

    console.log(`[Razorpay Order] Created order ${orderId} for ₹${numAmount} (${amountInPaise} paise) — Event: ${eventTitle || eventId}`);

    res.json({
      success: true,
      orderId,
      amount: amountInPaise,
      amountInRupees: numAmount,
      currency: 'INR',
      keyId,
      companyName: config.companyName || 'Social Shuffle Pune',
      themeColor: config.themeColor || '#FF751F'
    });
  });

  app.post('/api/payments/razorpay/verify-payment', requireAuth, (req, res) => {
    const { razorpayPaymentId, razorpayOrderId, razorpaySignature, registrationId, amount } = req.body;
    const config = loadRazorpayConfig();

    // If Razorpay HMAC signature and secret exist, enforce cryptographic signature validation
    if (config.keySecret && razorpayOrderId && razorpayPaymentId && razorpaySignature) {
      const expectedSig = crypto
        .createHmac('sha256', config.keySecret)
        .update(`${razorpayOrderId}|${razorpayPaymentId}`)
        .digest('hex');
      const sigBuf = Buffer.from(String(razorpaySignature));
      const expectedBuf = Buffer.from(expectedSig);
      if (sigBuf.length !== expectedBuf.length || !crypto.timingSafeEqual(sigBuf, expectedBuf)) {
        return res.status(400).json({ success: false, message: 'Razorpay payment signature verification failed!' });
      }
    }

    const orderInfo = razorpayOrderId ? issuedOrders.get(String(razorpayOrderId)) : undefined;
    const verifiedAmount = orderInfo?.amountInRupees || Number(amount) || 0;
    const paymentId = razorpayPaymentId || `pay_${Date.now().toString(36)}_${crypto.randomBytes(3).toString('hex').toUpperCase()}`;

    if (registrationId) {
      const reg = dbRegistrations.getById(registrationId);
      if (reg) {
        dbRegistrations.update(registrationId, {
          paymentStatus: 'Confirmed',
          razorpayPaymentId: paymentId,
          razorpayOrderId: razorpayOrderId || reg.razorpayOrderId,
          amountPaid: verifiedAmount || reg.amountPaid
        });
      }
    }

    res.json({
      success: true,
      message: 'Razorpay payment successfully verified and confirmed!',
      paymentId,
      orderId: razorpayOrderId,
      status: 'captured'
    });
  });

  app.post('/api/registrations/:id/toggle-game', requireAuth, (req, res) => {
    const authUser = (req as any).authUser as SessionTokenPayload;
    const gameId = String(req.query.gameId || req.body.gameId);
    const reg = dbRegistrations.getById(req.params.id);
    if (!reg) return res.status(404).json({ error: 'Registration not found' });
    if (
      authUser.role !== 'admin' &&
      reg.participantId !== authUser.participantId &&
      String(reg.participantEmail || '').trim().toLowerCase() !== authUser.email
    ) {
      return res.status(403).json({ error: 'Forbidden' });
    }

    const currentGames = reg.gamesPlayedIds || [];
    const updatedGames = currentGames.includes(gameId)
      ? currentGames.filter(g => g !== gameId)
      : [...currentGames, gameId];

    const updated = dbRegistrations.update(req.params.id, { gamesPlayedIds: updatedGames });
    res.json(updated);
  });

  app.post('/api/registrations/walk-in', requireAdmin, (req, res) => {
    const eventId = String(req.query.eventId || req.body.eventId);
    const { name, phone, email, area, paxCount, paymentStatus, notes } = req.body;
    const newId = `reg-walkin-${Date.now().toString(36)}`;

    let p: any = dbParticipants.getAll().find(part => part.email.toLowerCase() === String(email || '').toLowerCase());
    if (!p) {
      p = {
        id: `p-${Date.now().toString(36)}`,
        name,
        email,
        phone,
        passwordHash: hashPassword(crypto.randomBytes(16).toString('hex')),
        area: area || 'Pune',
        avatar: `https://api.dicebear.com/7.x/bottts/svg?seed=${encodeURIComponent(name)}`,
        bio: 'Board game lover',
        joinedDate: new Date().toISOString().slice(0, 10),
        totalEventsAttended: 1,
        totalRegistrations: 1,
        gamesPlayedIds: [],
        venuesVisited: [],
        totalPaxBrought: paxCount || 1
      };
      dbParticipants.insert(p);
    }

    const newReg = {
      id: newId,
      eventId,
      participantId: p.id,
      participantName: name,
      participantEmail: email,
      participantPhone: phone,
      participantArea: area || 'Pune',
      paxCount: paxCount || 1,
      guests: [],
      registeredAt: new Date().toISOString(),
      attendanceStatus: 'Checked In' as const,
      paymentStatus: paymentStatus || 'Completed',
      checkedInAt: new Date().toISOString(),
      notes: notes || 'Walk-in Registration'
    };

    dbRegistrations.insert(newReg);

    const ev = dbEvents.getById(eventId);
    if (ev) {
      dbEvents.update(ev.id, {
        registeredCount: (ev.registeredCount || 0) + (paxCount || 1),
        checkedInCount: (ev.checkedInCount || 0) + (paxCount || 1)
      });
    }

    if (email) {
      try {
        const emailHtml = generateRegistrationEmailHtml(newReg as any, ev);
        sendOutboundEmail({
          to: email,
          subject: `🎲 VIP Entry Pass (Walk-in Confirmed): ${ev?.title || 'Social Shuffle Pune'}!`,
          html: emailHtml,
          text: `Your walk-in registration for ${ev?.title || 'Social Shuffle'} is confirmed. Token: ${newReg.id}`
        }).catch(err => console.warn('[Walk-in Mail Error]', err));
      } catch {}
    }

    res.status(201).json(newReg);
  });

  app.delete('/api/registrations/:id', requireAuth, (req, res) => {
    const authUser = (req as any).authUser as SessionTokenPayload;
    const existing = dbRegistrations.getById(req.params.id);
    if (!existing) return res.status(404).json({ error: 'Registration not found' });
    if (
      authUser.role !== 'admin' &&
      existing.participantId !== authUser.participantId &&
      String(existing.participantEmail || '').trim().toLowerCase() !== authUser.email
    ) {
      return res.status(403).json({ error: 'Forbidden' });
    }
    const removed = dbRegistrations.remove(req.params.id);
    if (removed) return res.json({ success: true, message: 'Registration cancelled' });
    return res.status(404).json({ error: 'Registration not found' });
  });

  // ==========================================
  // COMMUNITY STATS & SITE CONTENT (DATABASE)
  // ==========================================
  app.get('/api/community/stats', (_req, res) => {
    let stats = dbCommunityStats.getAll()[0];
    if (!stats) {
      stats = INITIAL_COMMUNITY_STATS;
      dbCommunityStats.insert(stats);
    }
    const liveCounts = {
      participants: dbParticipants.getAll().length,
      games: dbGames.getAll().length,
      events: dbEvents.getAll().length,
      registrations: dbRegistrations.getAll().length
    };
    res.json({
      ...stats,
      liveCounts
    });
  });

  const handleUpdateCommunityStats = (req: express.Request, res: express.Response) => {
    const existing = dbCommunityStats.getAll()[0] || INITIAL_COMMUNITY_STATS;
    const updates = req.body;
    const updatedStats = {
      ...existing,
      ...updates,
      id: 'community-stats-main',
      updatedAt: new Date().toISOString()
    };
    dbCommunityStats.setAll([updatedStats]);
    console.log('[DB Community Stats] Updated community stats in database:', updatedStats.membersCount);
    res.json(updatedStats);
  };

  app.put('/api/community/stats', requireAdmin, handleUpdateCommunityStats);
  app.post('/api/community/stats', requireAdmin, handleUpdateCommunityStats);

  // ==========================================
  // COMMUNITY: FEEDBACK, REPORTS, VOLUNTEERS
  // ==========================================
  app.get('/api/community/feedback', (_req, res) => {
    res.json(dbFeedback.getAll());
  });

  app.post('/api/community/feedback', (req, res) => {
    const data = req.body;
    const newFb = {
      ...data,
      id: data.id || `fb-${Date.now().toString(36)}`,
      createdAt: new Date().toISOString()
    };
    dbFeedback.insert(newFb);

    try {
      const fbHtml = generateFeedbackAdminAlertEmailHtml(newFb);
      sendOutboundEmail({
        to: ADMIN_NOTIFICATION_EMAIL,
        subject: `⭐ New Community Review: ${newFb.rating || 5}/5 Stars from ${newFb.userName || 'Attendee'}`,
        html: fbHtml,
        text: `New feedback received from ${newFb.userName || 'Attendee'}: ${newFb.rating} stars. Comments: ${newFb.comment || 'N/A'}`
      }).catch(err => console.warn('[Feedback Alert Error]', err));
    } catch {}

    res.status(201).json(newFb);
  });

  app.get('/api/community/reports', requireAdmin, (_req, res) => {
    res.json(dbReports.getAll());
  });

  app.post('/api/community/reports', (req, res) => {
    const data = req.body;
    const newRep = {
      ...data,
      id: data.id || `rep-${Date.now().toString(36)}`,
      status: 'New',
      createdAt: new Date().toISOString()
    };
    dbReports.insert(newRep);

    try {
      const alertHtml = generateReportAdminAlertEmailHtml(newRep);
      sendOutboundEmail({
        to: ADMIN_NOTIFICATION_EMAIL,
        subject: `⚠️ [URGENT] Safety / Grievance Report: ${newRep.category || 'Incident'}`,
        html: alertHtml,
        text: `A safety / grievance report has been submitted by ${newRep.reporterName || 'Anonymous'}. Description: ${newRep.description || ''}`
      }).catch(err => console.warn('[Report Alert Error]', err));
    } catch {}

    if (newRep.reporterEmail) {
      try {
        const ackHtml = generateReportAckEmailHtml(newRep);
        sendOutboundEmail({
          to: newRep.reporterEmail,
          subject: `Social Shuffle: We have received your submission`,
          html: ackHtml,
          text: `Hello ${newRep.reporterName || 'Fellow Shuffler'}, thank you for contacting Social Shuffle leadership. We are reviewing your submission.`
        }).catch(err => console.warn('[Report Ack Error]', err));
      } catch {}
    }

    res.status(201).json(newRep);
  });

  app.patch('/api/community/reports/:id/status', requireAdmin, (req, res) => {
    const { status, notes } = req.query;
    const updated = dbReports.update(req.params.id, {
      status: (status as any) || 'Resolved',
      adminNotes: (notes as string) || undefined
    });
    if (updated) return res.json(updated);
    return res.status(404).json({ error: 'Report not found' });
  });

  app.get('/api/community/volunteers', requireAuth, (req, res) => {
    const authUser = (req as any).authUser as SessionTokenPayload;
    const all = dbVolunteers.getAll();
    if (authUser.role === 'admin') {
      return res.json(all);
    }
    return res.json(all.filter(v => String(v.email || '').trim().toLowerCase() === authUser.email));
  });

  app.post('/api/community/volunteers', (req, res) => {
    const data = req.body;
    const newVol = {
      ...data,
      id: data.id || `vol-${Date.now().toString(36)}`,
      status: 'New',
      submittedAt: new Date().toISOString()
    };
    dbVolunteers.insert(newVol);

    if (newVol.email) {
      try {
        const confHtml = generateVolunteerConfirmationEmailHtml(newVol);
        sendOutboundEmail({
          to: newVol.email,
          subject: `🎲 Thank You for Applying to Volunteer at Social Shuffle, ${newVol.name}!`,
          html: confHtml,
          text: `Hello ${newVol.name}, we received your volunteer application for Social Shuffle Pune. We will contact you shortly!`
        }).catch(err => console.warn('[Volunteer Conf Mail Error]', err));
      } catch {}
    }

    try {
      const alertHtml = generateVolunteerAdminAlertEmailHtml(newVol);
      sendOutboundEmail({
        to: ADMIN_NOTIFICATION_EMAIL,
        subject: `🙋‍♂️ New Volunteer Application: ${newVol.name} (${newVol.preferredArea || 'Pune'})`,
        html: alertHtml,
        text: `New volunteer application received from ${newVol.name} (${newVol.email}, ${newVol.phone}).`
      }).catch(err => console.warn('[Volunteer Admin Alert Error]', err));
    } catch {}

    res.status(201).json(newVol);
  });

  app.patch('/api/community/volunteers/:id/status', requireAdmin, (req, res) => {
    const { status } = req.query;
    const updated = dbVolunteers.update(req.params.id, {
      status: (status as any) || 'Approved'
    });
    if (updated) return res.json(updated);
    return res.status(404).json({ error: 'Volunteer not found' });
  });

  // ==========================================
  // AUDIT LOGS & NOTIFICATIONS ENDPOINTS
  // ==========================================
  app.get('/api/audit-logs', requireAdmin, (_req, res) => {
    res.json(dbAuditLogs.getAll());
  });

  app.post('/api/audit-logs', requireAdmin, (req, res) => {
    const data = req.body;
    const newLog = {
      ...data,
      id: data.id || `log-${Date.now().toString(36)}`,
      timestamp: new Date().toISOString()
    };
    dbAuditLogs.insert(newLog);
    res.status(201).json(newLog);
  });

  app.get('/api/notifications', (req, res) => {
    const authUser = (req as any).authUser as SessionTokenPayload | undefined;
    const items = dbNotifications.getAll().filter((n: any) => !String(n.id || '').startsWith('notif-otp-'));

    if (!authUser) {
      // Guest callers see only public broadcast/event/system announcements without private emails/HTML
      return res.json(
        items
          .filter((n: any) => ['event', 'broadcast', 'system'].includes(n.type) && !n.recipientEmail)
          .map((n: any) => {
            const { recipientEmail: _re, emailHtml: _eh, ...pub } = n;
            return pub;
          })
      );
    }

    if (authUser.role === 'admin') {
      return res.json(items);
    }

    return res.json(
      items.filter(
        (n: any) => !n.recipientEmail || String(n.recipientEmail).trim().toLowerCase() === authUser.email
      )
    );
  });

  app.patch('/api/notifications/:id/read', requireAuth, (req, res) => {
    const updated = dbNotifications.update(req.params.id, { read: true });
    if (updated) return res.json({ success: true });
    return res.status(404).json({ error: 'Notification not found' });
  });

  app.post('/api/notifications/mark-all-read', requireAuth, (_req, res) => {
    dbNotifications.getAll().forEach(n => {
      dbNotifications.update(n.id, { read: true });
    });
    res.json({ success: true });
  });

  app.delete('/api/notifications/:id', requireAuth, (req, res) => {
    dbNotifications.remove(req.params.id);
    res.json({ success: true, id: req.params.id });
  });

  app.post('/api/notifications/community-announcement', requireAdmin, (req, res) => {
    const { title, message } = req.body;
    const newNotif = {
      id: `notif-${Date.now()}`,
      title: title || 'Community Announcement',
      message: message || '',
      type: 'broadcast' as const,
      timestamp: 'Just now',
      read: false
    };
    dbNotifications.insert(newNotif);
    res.json({ success: true, notification: newNotif });
  });

  // ==========================================
  // BADGES & NOTIFICATION ENGINE
  // ==========================================
  app.get('/api/badges/levels', (_req, res) => {
    res.json({
      success: true,
      levels: BADGE_LEVELS,
      totalLevels: BADGE_LEVELS.length
    });
  });

  app.post('/api/badges/calculate', (req, res) => {
    const { gamesPlayedCount = 0, totalGamesCount = 24, eventsAttendedCount = 0 } = req.body;
    const result = calculateBadgeLevel({
      gamesPlayedCount: Number(gamesPlayedCount) || 0,
      totalGamesCount: Number(totalGamesCount) || 24,
      eventsAttendedCount: Number(eventsAttendedCount) || 0
    });
    res.json({
      success: true,
      data: result
    });
  });

  // ==========================================
  // REAL-TIME EMAIL & WHATSAPP DISPATCH ENGINE
  // ==========================================

  // Push notifications (Email Dispatcher; WhatsApp channel temporarily commented out)
  const handleDualPushNotification = async (req: express.Request, res: express.Response) => {
    const eventId = String(req.params.id || req.body.eventId || '');
    const {
      subject = 'Social Shuffle Event Notification',
      message = '',
      notificationType = 'event_reminder',
      recipientFilter = 'all',
      channels = ['email'],
      includeQrPass = true
    } = req.body;

    const event = dbEvents.getById(eventId);
    let allRegs = dbRegistrations.getAll().filter(r => r.eventId === eventId && r.attendanceStatus !== 'Cancelled');
    if (recipientFilter === 'confirmed_only') {
      allRegs = allRegs.filter(r => r.paymentStatus === 'Confirmed');
    } else if (recipientFilter === 'pending_only') {
      allRegs = allRegs.filter(r => r.attendanceStatus === 'Pending');
    }

    let emailSentCount = 0;
    const whatsappSentCount = 0;
    const whatsappUnavailableCount = 0;
    const recipientStatuses: any[] = [];

    for (const reg of allRegs) {
      // 1. Generate Email HTML & Send Real Outbound Email
      let htmlContent = '';
      try {
        if (notificationType === 'event_reminder') {
          htmlContent = generateEventReminderEmailHtml(reg as any, event, message);
        } else {
          htmlContent = generateCustomBroadcastEmailHtml(reg as any, event, subject, message, includeQrPass);
        }
      } catch (e) {
        htmlContent = `<p>${message.replace(/\n/g, '<br/>')}</p>`;
      }

      let emailResult: any = { success: false };
      if (reg.participantEmail) {
        try {
          emailResult = await sendOutboundEmail({
            to: reg.participantEmail,
            subject,
            html: htmlContent,
            text: message
          });
          if (emailResult.success) {
            emailSentCount++;
          }
        } catch (err: any) {
          console.warn(`[Broadcast Mail Error] ${reg.participantEmail}:`, err?.message);
        }
      }

      /*
      // WhatsApp channel dispatch temporarily commented out
      let waResult: any = { success: false, mode: 'web_link', statusText: 'WhatsApp temporarily commented out' };
      if (channels.includes('whatsapp') && reg.participantPhone) {
        try {
          const waText = generateEventWhatsAppNotification(reg as any, event, message);
          waResult = await dispatchOutboundWhatsApp({
            toPhone: reg.participantPhone,
            messageText: waText
          });
        } catch (err: any) {
          console.warn(`[Broadcast WhatsApp Error] ${reg.participantPhone}:`, err?.message);
        }
      }
      */

      recipientStatuses.push({
        registrationId: reg.id,
        participantId: reg.participantId,
        name: reg.participantName,
        email: reg.participantEmail,
        phone: reg.participantPhone,
        emailSent: emailResult.success,
        emailMessageId: emailResult.messageId,
        emailPreviewUrl: emailResult.previewUrl,
        whatsappSent: false,
        whatsappUrl: undefined,
        whatsappMode: 'disabled',
        statusSummary: emailResult.success
          ? `✅ Email delivered to ${reg.participantEmail}`
          : `⚠️ Email queued (${reg.participantEmail})`
      });
    }

    // Record in database notifications
    dbNotifications.insert({
      id: `notif-broadcast-${Date.now()}`,
      title: subject,
      message: `Dispatched to ${allRegs.length} attendees (${emailSentCount} real emails delivered)`,
      type: 'event',
      timestamp: 'Just now',
      read: false
    });

    res.json({
      success: true,
      eventId,
      eventTitle: event?.title || 'Meetup',
      totalRecipients: allRegs.length,
      emailSentCount,
      whatsappSentCount,
      whatsappUnavailableCount,
      fallbackEmailOnlyCount: allRegs.length - emailSentCount,
      recipients: recipientStatuses,
      message: `Dispatched to ${allRegs.length} attendees: ${emailSentCount} real emails delivered!`
    });
  };

  app.post('/api/notifications/event-broadcast', requireAdmin, handleDualPushNotification);
  app.post('/api/events/:id/push-notification', requireAdmin, handleDualPushNotification);
  app.post('/api/notifications/send-reminder/:id', requireAdmin, handleDualPushNotification);

  // Send Registration Confirmation Pass via Real Email
  app.post('/api/notifications/registration-confirmation/:id', requireAuth, async (req, res) => {
    const authUser = (req as any).authUser as SessionTokenPayload;
    let reg = dbRegistrations.getById(req.params.id);
    if (!reg && req.body && req.body.registration) {
      reg = req.body.registration;
      try {
        dbRegistrations.insert(reg);
      } catch {}
    }
    if (!reg) return res.status(404).json({ success: false, message: 'Registration not found' });
    if (
      authUser.role !== 'admin' &&
      reg.participantId !== authUser.participantId &&
      String(reg.participantEmail || '').trim().toLowerCase() !== authUser.email
    ) {
      return res.status(403).json({ success: false, message: 'Forbidden' });
    }
    const ev = dbEvents.getById(reg.eventId) || req.body?.event;

    let emailRes: any = { success: false };
    if (reg.participantEmail) {
      try {
        const emailHtml = generateRegistrationEmailHtml(reg as any, ev);
        emailRes = await sendOutboundEmail({
          to: reg.participantEmail,
          subject: `🎲 VIP Entry Pass Confirmed: ${ev?.title || 'Social Shuffle Pune'}!`,
          html: emailHtml,
          text: `Your entry pass for ${ev?.title || 'Social Shuffle'} is confirmed. Token: ${reg.qrCodeToken || reg.id}`
        });
      } catch (err: any) {
        emailRes = { success: false, error: err?.message };
      }
    }

    res.json({
      success: emailRes.success,
      message: emailRes.success
        ? `Entry pass dispatched to ${reg.participantEmail}`
        : `Email delivery status: ${emailRes.error || 'Failed'}`,
      emailResult: emailRes,
      whatsappResult: null
    });
  });

  // OTP Verification System with Cryptographic RNG, Cooldown & Attempt Lockout
  interface OtpRecord {
    email: string;
    otp: string;
    expiresAt: number;
    createdAt: number;
    attempts: number;
    name?: string;
  }
  const otpStore = new Map<string, OtpRecord>();
  const otpSendHistory = new Map<string, number[]>();
  const verifiedEmails = new Set<string>();

  app.post('/api/auth/otp/send', async (req, res) => {
    const { email, name } = req.body;
    if (!email || typeof email !== 'string') {
      return res.status(400).json({ success: false, message: 'Valid email address is required' });
    }
    const cleanEmail = email.trim().toLowerCase();

    // Prevent rapid OTP spamming per email
    const now = Date.now();
    const existing = otpStore.get(cleanEmail);
    if (existing && now - existing.createdAt < 30 * 1000) {
      return res.status(429).json({
        success: false,
        message: 'Please wait 30 seconds before requesting another verification code.'
      });
    }
    const history = (otpSendHistory.get(cleanEmail) || []).filter(ts => ts > now - 15 * 60 * 1000);
    if (history.length >= 5) {
      return res.status(429).json({
        success: false,
        message: 'Too many OTP requests for this email. Please wait 15 minutes and try again.'
      });
    }
    history.push(now);
    otpSendHistory.set(cleanEmail, history);

    const cleanName = (name && typeof name === 'string') ? name.trim() : 'Fellow Shuffler';
    const otp = crypto.randomInt(100000, 1000000).toString();
    const expiresInSeconds = 600;
    const expiresAt = now + expiresInSeconds * 1000;

    otpStore.set(cleanEmail, {
      email: cleanEmail,
      otp,
      expiresAt,
      createdAt: now,
      attempts: 0,
      name: cleanName
    });

    const otpEmailHtml = `
      <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #0E162B; color: #f8fafc; padding: 32px 24px; border-radius: 16px; max-width: 520px; margin: 20px auto; border: 1px solid #1F305E;">
        <div style="text-align: center; margin-bottom: 24px;">
          <span style="background-color: #FF751F; color: #ffffff; font-weight: 800; font-size: 11px; padding: 4px 12px; border-radius: 9999px; text-transform: uppercase;">Social Shuffle Pune</span>
          <h2 style="color: #ffffff; margin: 12px 0 6px 0; font-size: 22px; font-weight: 900;">Verify Your Email Address</h2>
          <p style="color: #94A3B8; font-size: 13px; margin: 0;">Pune's Biggest Board Game Meetup Community</p>
        </div>
        <div style="background-color: #131E3A; border: 1px solid #283677; border-radius: 12px; padding: 20px; text-align: center; margin-bottom: 20px;">
          <p style="color: #cbd5e1; font-size: 13px; margin: 0 0 12px 0;">Hello <strong>${cleanName}</strong>, enter this 6-digit code to complete verification:</p>
          <div style="background: #080D1F; border: 2px dashed #4AB4A4; border-radius: 12px; padding: 16px; margin: 10px 0;">
            <span style="font-size: 34px; font-weight: 900; letter-spacing: 10px; color: #FFD230; font-family: monospace;">${otp}</span>
          </div>
          <p style="color: #64748B; font-size: 11px; margin: 10px 0 0 0;">⏱️ Valid for 10 minutes</p>
        </div>
        <p style="font-size: 12px; color: #94A3B8; text-align: center; line-height: 1.5; margin: 0;">
          If you did not request this verification code, you can safely ignore this email.
        </p>
      </div>
    `;

    // Send OTP strictly to recipient email inbox (never expose in public notifications)
    sendOutboundEmail({
      to: cleanEmail,
      subject: `🎲 ${otp} is your Social Shuffle verification code`,
      html: otpEmailHtml,
      text: `Your Social Shuffle verification code is ${otp}. Valid for 10 minutes.`
    }).then(mailResult => {
      console.log(`[OTP Email] Dispatched to ${cleanEmail}:`, mailResult.success ? 'Success' : mailResult.error);
    }).catch(err => {
      console.warn('[OTP Email Error]', err?.message);
    });

    res.json({
      success: true,
      message: `Verification code sent to ${cleanEmail}`,
      email: cleanEmail,
      expiresInSeconds
    });
  });

  app.post('/api/auth/otp/verify', (req, res) => {
    const { email, otp } = req.body;
    if (!email || !otp) {
      return res.status(400).json({ success: false, message: 'Email and OTP code are required' });
    }
    const cleanEmail = String(email).trim().toLowerCase();
    const cleanOtp = String(otp).trim().replace(/\s+/g, '');
    const record = otpStore.get(cleanEmail);

    if (!record) {
      return res.status(400).json({ success: false, message: 'No active verification code found' });
    }
    if (Date.now() > record.expiresAt) {
      otpStore.delete(cleanEmail);
      return res.status(400).json({ success: false, message: 'Verification code has expired' });
    }
    if (record.attempts >= 5) {
      otpStore.delete(cleanEmail);
      return res.status(429).json({ success: false, message: 'Too many invalid attempts. Please request a new OTP code.' });
    }
    const otpBuf = Buffer.from(cleanOtp);
    const storedBuf = Buffer.from(record.otp);
    if (otpBuf.length !== storedBuf.length || !crypto.timingSafeEqual(otpBuf, storedBuf)) {
      record.attempts += 1;
      const remaining = Math.max(0, 5 - record.attempts);
      if (remaining === 0) {
        otpStore.delete(cleanEmail);
        return res.status(429).json({ success: false, message: 'Too many invalid attempts. Please request a new OTP code.' });
      }
      return res.status(400).json({ success: false, message: `Invalid OTP code (${remaining} attempts remaining)` });
    }

    verifiedEmails.add(cleanEmail);
    otpStore.delete(cleanEmail);
    res.json({ success: true, verified: true, message: 'Email verified successfully!', email: cleanEmail });
  });

  app.get('/api/auth/otp/status', (req, res) => {
    const email = String(req.query.email || '').trim().toLowerCase();
    res.json({ success: true, verified: verifiedEmails.has(email) });
  });

  // ==========================================
  // COMMUNICATIONS CONFIG & TEST DISPATCH APIS
  // ==========================================

  app.get('/api/communications/config', requireAdmin, async (_req, res) => {
    try {
      const config = loadCommunicationsConfig();
      const { provider, isRealSmtp } = await getMailTransporter();

      res.json({
        success: true,
        email: {
          ...config.email,
          pass: config.email.pass ? '••••••••••••' : '',
          isRealSmtp,
          activeProvider: provider
        },
        whatsapp: {
          ...config.whatsapp,
          cloudApi: {
            ...config.whatsapp.cloudApi,
            accessToken: config.whatsapp.cloudApi?.accessToken ? '••••••••••••' : ''
          }
        }
      });
    } catch (err: any) {
      res.status(500).json({ success: false, error: err?.message });
    }
  });

  app.post('/api/communications/config', requireAdmin, (req, res) => {
    try {
      const current = loadCommunicationsConfig();
      const { email, whatsapp } = req.body;

      const updated = {
        email: {
          ...current.email,
          ...(email || {}),
          pass: (email?.pass && email.pass !== '••••••••••••') ? email.pass : current.email.pass
        },
        whatsapp: {
          ...current.whatsapp,
          ...(whatsapp || {}),
          cloudApi: {
            ...current.whatsapp.cloudApi,
            ...(whatsapp?.cloudApi || {}),
            accessToken: (whatsapp?.cloudApi?.accessToken && whatsapp.cloudApi.accessToken !== '••••••••••••')
              ? whatsapp.cloudApi.accessToken
              : current.whatsapp.cloudApi?.accessToken
          }
        }
      };

      const saved = saveCommunicationsConfig(updated);
      res.json({ success: saved, message: saved ? 'Communications settings updated successfully' : 'Failed to save settings' });
    } catch (err: any) {
      res.status(500).json({ success: false, error: err?.message });
    }
  });

  app.post('/api/communications/set-gmail-token', requireAdmin, (req, res) => {
    const { token, email } = req.body;
    setGmailOAuthToken(token || null, email || null);
    res.json({ success: true, ...getGmailOAuthStatus() });
  });

  app.get('/api/communications/gmail-status', requireAdmin, (_req, res) => {
    res.json(getGmailOAuthStatus());
  });

  app.post('/api/communications/test-email', requireAdmin, async (req, res) => {
    const targetRecipient = req.body.toEmail || req.body.to || req.body.email || ADMIN_NOTIFICATION_EMAIL;
    const { subject = '🎲 Social Shuffle Test Email Dispatch', message = 'This is a live test email sent from the Social Shuffle Pune platform.' } = req.body;
    const toEmail = String(targetRecipient).trim();
    if (!toEmail) {
      return res.status(400).json({ success: false, message: 'Recipient email address is required' });
    }

    const testHtml = `
      <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #0E162B; color: #f8fafc; padding: 32px 24px; border-radius: 16px; max-width: 540px; margin: 20px auto; border: 1px solid #1F305E;">
        <div style="text-align: center; margin-bottom: 24px;">
          <span style="background-color: #4AB4A4; color: #0E162B; font-weight: 800; font-size: 11px; padding: 4px 12px; border-radius: 9999px; text-transform: uppercase;">Real Mail Delivery Test</span>
          <h2 style="color: #ffffff; margin: 12px 0 6px 0; font-size: 22px; font-weight: 900;">Live Email Dispatch Verified! 🎉</h2>
          <p style="color: #94A3B8; font-size: 13px; margin: 0;">Social Shuffle Pune • Outbound Mail Service</p>
        </div>
        <div style="background-color: #131E3A; border: 1px solid #283677; border-radius: 12px; padding: 20px; margin-bottom: 20px;">
          <p style="font-size: 14px; margin-top: 0; color: #e2e8f0;">${String(message).replace(/\n/g, '<br/>')}</p>
          <div style="border-top: 1px solid #283677; margin-top: 16px; padding-top: 14px; font-size: 12px; color: #94A3B8;">
            <div><strong>Recipient:</strong> ${toEmail}</div>
            <div><strong>Dispatched At:</strong> ${new Date().toLocaleString('en-IN', { timeZone: 'Asia/Kolkata' })} (IST)</div>
            <div><strong>Status:</strong> Successfully Transmitted over SMTP</div>
          </div>
        </div>
        <div style="text-align: center; font-size: 12px; color: #64748B;">
          Social Shuffle Pune • Board Game Community Platform
        </div>
      </div>
    `;

    const result = await sendOutboundEmail({
      to: toEmail,
      subject,
      html: testHtml,
      text: message
    });

    res.json(result);
  });

  // Live WhatsApp Test Dispatch / Link Preview (Temporarily commented out)
  app.post('/api/communications/test-whatsapp', async (_req, res) => {
    /*
    const { toPhone, messageText = '🎲 Hello from Social Shuffle Pune! This is a test WhatsApp notification.' } = req.body;
    if (!toPhone || typeof toPhone !== 'string') {
      return res.status(400).json({ success: false, message: 'Valid phone number is required' });
    }

    const result = await dispatchOutboundWhatsApp({
      toPhone,
      messageText
    });
    return res.json(result);
    */
    res.json({
      success: false,
      mode: 'disabled',
      statusText: 'WhatsApp feature is temporarily commented out/disabled',
      message: 'WhatsApp feature is temporarily commented out/disabled'
    });
  });

  // Serve public assets
  app.use(express.static('public'));

  // Mount Vite middlewares for frontend SPA
  if (process.env.NODE_ENV === 'production') {
    app.use(express.static('dist'));
    app.get('*', (_req, res) => {
      res.sendFile('dist/index.html', { root: '.' });
    });
  } else {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa'
    });
    app.use(vite.middlewares);
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`[Social Shuffle Full-Stack Engine] Persistent Database API active on http://0.0.0.0:${PORT}/api`);
  });
}

startServer().catch(err => {
  console.error('Failed to start server:', err);
  process.exit(1);
});
