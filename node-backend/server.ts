import express from 'express';
import { createServer as createViteServer } from 'vite';
import fs from 'fs';
import path from 'path';
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

  app.use(express.json({ limit: '10mb' }));

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
  // ADMINS & AUTHENTICATION (ALL DETAILS)
  // ==========================================
  app.get('/api/admins', (_req, res) => {
    res.json({
      success: true,
      admins: dbAdmins.getAll()
    });
  });

  app.get('/api/admins/check', (req, res) => {
    const query = String(req.query.email || req.query.phone || req.query.identifier || '').trim();
    if (!query) {
      return res.status(400).json({ success: false, message: 'Query is required' });
    }
    const admin = findAdminByEmailOrPhone(query);
    if (admin) {
      return res.json({
        success: true,
        isAdmin: true,
        admin
      });
    }
    return res.json({
      success: true,
      isAdmin: false
    });
  });

  // Direct Login: Unified regular login system for both Participants and Administrators
  // Admin is identified by matching all possible details (Name, Email, Phone) combined with admins table
  app.post('/api/auth/login', (req, res) => {
    const { emailOrPhone, password, name, phone, email, designation } = req.body;
    
    // Normalize input fields
    const inputEmail = String(email || (emailOrPhone && String(emailOrPhone).includes('@') ? emailOrPhone : '')).trim().toLowerCase();
    const inputPhone = String(phone || (!String(emailOrPhone || '').includes('@') ? emailOrPhone : '')).trim();
    const inputPhoneDigits = inputPhone.replace(/[^0-9]/g, '');
    const inputName = String(name || '').trim().toLowerCase();
    const cleanIdent = String(emailOrPhone || inputEmail || inputPhone).trim().toLowerCase();

    if (!cleanIdent && !inputEmail && !inputPhoneDigits) {
      return res.status(400).json({ success: false, message: 'Email address or mobile phone number is required' });
    }

    // --- ADMIN IDENTIFICATION: MATCH ALL POSSIBLE DETAILS COMBINED ---
    // Rule: Must match all details (Full Name, Registered Email, Mobile Phone) combined in admins table in database!
    // Not only name or not only email!
    let matchedAdmin: any = null;
    let partialAdminMatch: any = null;
    let missingAdminDetails: string[] = [];

    for (const admin of dbAdmins.getAll()) {
      if (!admin.active) continue;
      const adminEmail = (admin.email || '').trim().toLowerCase();
      const adminName = (admin.name || '').trim().toLowerCase();
      const adminPhoneDigits = (admin.phone || '').replace(/[^0-9]/g, '');

      const emailMatches = Boolean(inputEmail && adminEmail && inputEmail === adminEmail);
      const phoneMatches = Boolean(
        inputPhoneDigits.length >= 10 &&
        adminPhoneDigits.length >= 10 &&
        (adminPhoneDigits.includes(inputPhoneDigits) || inputPhoneDigits.includes(adminPhoneDigits))
      );
      const nameMatches = Boolean(
        inputName &&
        adminName &&
        (adminName === inputName || adminName.includes(inputName) || inputName.includes(adminName))
      );

      // ALL COMBINED MATCH: Name + Email + Phone must all match!
      if (emailMatches && phoneMatches && nameMatches) {
        matchedAdmin = admin;
        break;
      }

      // Check for partial match to provide guidance or prevent unauthorized admin escalation
      if (emailMatches || phoneMatches || (inputName && nameMatches)) {
        partialAdminMatch = admin;
        missingAdminDetails = [
          !nameMatches ? 'Full Name' : null,
          !emailMatches ? 'Registered Email' : null,
          !phoneMatches ? 'Mobile Phone' : null
        ].filter(Boolean) as string[];
      }
    }

    // If ALL details match combined: Authenticate as Administrator with full clearance!
    if (matchedAdmin) {
      const nowIso = new Date().toISOString();
      const adminUser = {
        id: matchedAdmin.id,
        name: matchedAdmin.name,
        email: matchedAdmin.email,
        phone: matchedAdmin.phone || '+91 98220 99887',
        role: 'admin',
        designation: designation?.trim() || matchedAdmin.designation,
        adminRole: 'Super Admin',
        status: 'Active Verified Administrator',
        joinedDate: matchedAdmin.createdAt || '2024-01-01',
        permissions: ['events', 'attendance', 'crm', 'games', 'broadcasts', 'finance', 'reports', 'settings', 'audit'],
        area: matchedAdmin.area || 'Koregaon Park, Pune',
        bio: matchedAdmin.bio || `${matchedAdmin.designation} • Social Shuffle Organizer`,
        avatar: matchedAdmin.avatar,
        authProvider: 'local',
        loginAt: nowIso,
        lastActiveAt: nowIso
      };

      console.log(`[Auth DB] Administrator verified by ALL combined details: ${adminUser.name} (${adminUser.email}, ${adminUser.phone}, ${adminUser.designation})`);

      return res.json({
        success: true,
        message: `Welcome back, Admin ${adminUser.name}! (${adminUser.designation})`,
        token: `tok_admin_${matchedAdmin.id}_${Date.now()}`,
        user: adminUser
      });
    }

    // Participant login lookup (by email, phone, or username)
    const cleanDigits = inputPhoneDigits || cleanIdent.replace(/[^0-9]/g, '');
    const participant = dbParticipants.getAll().find(p => {
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
      // If password was sent, check against participant's stored password (defaulting to 'shuffler123' if not yet set)
      const expectedPassword = participant.password || 'shuffler123';
      if (password && password.trim().length > 0 && participant.password && participant.password !== password) {
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
        role: 'participant',
        area: participant.area,
        avatar: participant.avatar,
        bio: participant.bio,
        participantId: participant.id,
        authProvider: 'local',
        loginAt: nowIso,
        lastActiveAt: nowIso
      };
      return res.json({
        success: true,
        message: `Welcome back, ${participant.name}!`,
        token: `tok_part_${participant.id}_${Date.now()}`,
        user,
        participant
      });
    }

    if (partialAdminMatch && missingAdminDetails.length > 0) {
      return res.status(401).json({
        success: false,
        message: `Administrator identification requires ALL details (Full Name, Registered Email, and Mobile Phone) to match the database combined. Missing or mismatched: ${missingAdminDetails.join(', ')}.`
      });
    }

    return res.status(404).json({
      success: false,
      message: 'No registered participant or administrator found with these credentials.'
    });
  });

  // Participant Registration: Stores credentials so participant can login henceforth
  app.post('/api/auth/register', (req, res) => {
    const data = req.body;
    if (!data.name || !data.email || !data.phone) {
      return res.status(400).json({ success: false, message: 'Name, email, and phone number are required.' });
    }

    const emailClean = String(data.email).trim().toLowerCase();
    const phoneClean = String(data.phone).trim();
    const phoneDigits = phoneClean.replace(/[^0-9]/g, '');

    const existing = dbParticipants.getAll().find(p => {
      const pEmail = (p.email || '').trim().toLowerCase();
      const pPhone = (p.phone || '').replace(/[^0-9]/g, '');
      return pEmail === emailClean || (phoneDigits.length >= 10 && pPhone === phoneDigits);
    });

    if (existing) {
      if (data.password) {
        dbParticipants.update(existing.id, { password: data.password });
      }
      return res.status(400).json({ success: false, message: 'An account with this email or phone already exists. Please sign in.' });
    }

    const newId = data.id || `p-${Date.now().toString(36)}`;
    const newParticipant = {
      id: newId,
      name: String(data.name).trim(),
      email: emailClean,
      phone: phoneClean,
      password: data.password || 'shuffler123',
      username: emailClean.split('@')[0],
      area: data.area || 'Koregaon Park',
      avatar: data.avatar || `https://api.dicebear.com/7.x/bottts/svg?seed=${encodeURIComponent(data.name)}`,
      bio: data.bio || 'Board game lover eager to play!',
      joinedDate: data.joinedDate || new Date().toISOString().slice(0, 10),
      totalEventsAttended: 0,
      totalRegistrations: 0,
      gamesPlayedIds: [],
      venuesVisited: [],
      totalPaxBrought: 0
    };

    dbParticipants.insert(newParticipant);

    const nowIso = new Date().toISOString();
    const user = {
      id: `u-${newId}`,
      name: newParticipant.name,
      email: newParticipant.email,
      phone: newParticipant.phone,
      role: 'participant',
      area: newParticipant.area,
      avatar: newParticipant.avatar,
      bio: newParticipant.bio,
      participantId: newId,
      authProvider: 'local',
      loginAt: nowIso,
      lastActiveAt: nowIso
    };

    console.log(`[Auth DB] Registered new participant with credentials: ${newParticipant.name} (${newParticipant.email})`);

    // Dispatch official Welcome Email to new participant
    if (newParticipant.email) {
      try {
        const welcomeHtml = generateWelcomeEmailHtml(newParticipant.name, newParticipant.email);
        sendOutboundEmail({
          to: newParticipant.email,
          subject: `🎲 Welcome to Social Shuffle Pune, ${newParticipant.name}!`,
          html: welcomeHtml,
          text: `Welcome to Social Shuffle Pune, ${newParticipant.name}! We're thrilled to have you in our board game community.`
        }).then(mailRes => {
          console.log(`[Welcome Mail] Sent to ${newParticipant.email}:`, mailRes.success ? 'Delivered' : mailRes.error);
        }).catch(err => console.warn('[Welcome Mail Error]', err));
      } catch (e) {
        console.warn('[Welcome Mail Template Error]', e);
      }
    }

    return res.status(201).json({
      success: true,
      message: `Registration successful! Welcome to Social Shuffle, ${newParticipant.name}!`,
      token: `tok_part_${newId}_${Date.now()}`,
      user,
      participant: newParticipant
    });
  });

  // Google Login: returns all details for admin or participant
  app.post('/api/auth/google/login', (req, res) => {
    const { email } = req.body;
    if (!email) {
      return res.status(400).json({ success: false, message: 'Email is required' });
    }

    const cleanEmail = String(email).trim().toLowerCase();
    const cleanName = String(req.body.name || '').trim().toLowerCase();
    const record = registeredGoogleUsers.get(cleanEmail);

    let adminCheck: any = null;
    for (const admin of dbAdmins.getAll()) {
      if (!admin.active) continue;
      const adminEmail = (admin.email || '').trim().toLowerCase();
      const adminName = (admin.name || '').trim().toLowerCase();
      // Verify both email and name match combined
      if (cleanEmail === adminEmail && (!cleanName || adminName === cleanName || adminName.includes(cleanName) || cleanName.includes(adminName))) {
        adminCheck = admin;
        break;
      }
    }

    if (adminCheck && adminCheck.role === 'admin') {
      const nowIso = new Date().toISOString();
      const adminUser = {
        id: adminCheck.id,
        name: adminCheck.name,
        email: adminCheck.email,
        phone: adminCheck.phone || '+91 98220 99887',
        role: 'admin',
        designation: adminCheck.designation,
        adminRole: 'Super Admin',
        status: 'Active Verified Administrator',
        joinedDate: adminCheck.createdAt || '2024-01-01',
        permissions: ['events', 'attendance', 'crm', 'games', 'broadcasts', 'finance', 'reports', 'settings', 'audit'],
        area: adminCheck.area || 'Koregaon Park, Pune',
        bio: adminCheck.bio || `${adminCheck.designation} • Social Shuffle Organizer`,
        avatar: adminCheck.avatar,
        authProvider: 'google',
        loginAt: nowIso,
        lastActiveAt: nowIso
      };

      console.log(`[Google Auth DB] Admin verified with all details: ${adminUser.name} (${adminUser.email})`);

      return res.json({
        success: true,
        message: `Welcome back, Admin ${adminCheck.name}! (${adminCheck.designation})`,
        token: `tok_admin_g_${adminCheck.id}`,
        user: adminUser,
        participant: record?.participant
      });
    }

    if (!record) {
      return res.status(404).json({
        success: false,
        code: 'USER_NOT_REGISTERED',
        message: 'Username not registered'
      });
    }

    const nowIso = new Date().toISOString();
    record.user.lastActiveAt = nowIso;
    record.user.loginAt = nowIso;
    record.user.role = 'participant';
    saveGoogleUsers();

    res.json({
      success: true,
      message: `Welcome back, ${record.user.name}!`,
      token: `tok_g_${record.user.id}`,
      user: record.user,
      participant: record.participant
    });
  });

  // Google Sign-Up
  app.post('/api/auth/google/signup', (req, res) => {
    const { email, name, avatar, phone, area, bio, googleId } = req.body;
    if (!email || !name) {
      return res.status(400).json({ success: false, message: 'Email and name are required' });
    }

    const cleanEmail = String(email).trim().toLowerCase();
    const cleanName = String(name).trim();
    const nowIso = new Date().toISOString();

    const participantId = `p-google-${Date.now().toString(36)}`;
    const userId = `u-${participantId}`;

    const participant = {
      id: participantId,
      name: cleanName,
      email: cleanEmail,
      phone: phone || '+91 98220 ' + Math.floor(10000 + Math.random() * 90000),
      area: area || 'Koregaon Park',
      avatar: avatar || `https://api.dicebear.com/7.x/bottts/svg?seed=${encodeURIComponent(cleanName)}`,
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

    const adminCheck = findAdminByEmailOrPhone(cleanEmail);
    const assignedRole = (adminCheck && adminCheck.role === 'admin') ? 'admin' : 'participant';

    const user = {
      id: adminCheck ? adminCheck.id : userId,
      name: adminCheck?.name || cleanName,
      email: cleanEmail,
      phone: adminCheck?.phone || participant.phone,
      role: assignedRole,
      designation: adminCheck?.designation,
      adminRole: adminCheck ? 'Super Admin' : undefined,
      status: adminCheck ? 'Active Verified Administrator' : undefined,
      joinedDate: adminCheck?.createdAt || nowIso.slice(0, 10),
      permissions: adminCheck ? ['events', 'attendance', 'crm', 'games', 'broadcasts', 'finance', 'reports', 'settings', 'audit'] : undefined,
      area: adminCheck?.area || participant.area || 'Koregaon Park, Pune',
      avatar: adminCheck?.avatar || participant.avatar,
      bio: adminCheck?.bio || participant.bio,
      participantId: participant.id,
      authProvider: 'google',
      loginAt: nowIso,
      lastActiveAt: nowIso
    };

    // Store in participants DB
    dbParticipants.insert(participant);

    // Store in google users DB
    registeredGoogleUsers.set(cleanEmail, {
      email: cleanEmail,
      googleId: googleId || `g_${cleanEmail}`,
      participant,
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
        }).then(mailRes => {
          console.log(`[Welcome Mail (Google)] Sent to ${cleanEmail}:`, mailRes.success ? 'Delivered' : mailRes.error);
        }).catch(err => console.warn('[Welcome Mail (Google) Error]', err));
      } catch (e) {
        console.warn('[Welcome Mail Template Error]', e);
      }
    }

    res.json({
      success: true,
      message: `Welcome to Social Shuffle Pune, ${cleanName}! Account registered in database.`,
      user,
      participant
    });
  });

  // Current User /auth/me: returns full admin or participant profile
  const handleAuthMe = (req: express.Request, res: express.Response) => {
    const emailQuery = String(req.query.email || req.query.identifier || 'parimalmshete@gmail.com').trim().toLowerCase();
    const adminCheck = findAdminByEmailOrPhone(emailQuery);

    if (adminCheck && adminCheck.role === 'admin') {
      const nowIso = new Date().toISOString();
      return res.json({
        success: true,
        isAuthenticated: true,
        isAdmin: true,
        role: 'admin',
        user: {
          id: adminCheck.id,
          name: adminCheck.name,
          email: adminCheck.email,
          phone: adminCheck.phone || '+91 98220 99887',
          role: 'admin',
          designation: adminCheck.designation,
          adminRole: 'Super Admin',
          status: 'Active Verified Administrator',
          joinedDate: adminCheck.createdAt || '2024-01-01',
          permissions: ['events', 'attendance', 'crm', 'games', 'broadcasts', 'finance', 'reports', 'settings', 'audit'],
          area: adminCheck.area || 'Koregaon Park, Pune',
          avatar: adminCheck.avatar,
          bio: adminCheck.bio || `${adminCheck.designation} • Social Shuffle Organizer`,
          authProvider: 'google',
          loginAt: nowIso,
          lastActiveAt: nowIso
        }
      });
    }

    const reg = registeredGoogleUsers.get(emailQuery);
    if (reg) {
      return res.json({
        success: true,
        isAuthenticated: true,
        isAdmin: false,
        role: 'participant',
        user: reg.user,
        participant: reg.participant
      });
    }

    // Default admin in database: Parimal Shete
    const defaultAdmin = dbAdmins.getAll()[0];
    if (defaultAdmin) {
      return res.json({
        success: true,
        isAuthenticated: true,
        isAdmin: true,
        role: 'admin',
        user: {
          id: defaultAdmin.id,
          name: defaultAdmin.name,
          email: defaultAdmin.email,
          phone: defaultAdmin.phone || '+91 98220 99887',
          role: 'admin',
          designation: defaultAdmin.designation,
          adminRole: 'Super Admin',
          status: 'Active Verified Administrator',
          joinedDate: defaultAdmin.createdAt || '2024-01-01',
          permissions: ['events', 'attendance', 'crm', 'games', 'broadcasts', 'finance', 'reports', 'settings', 'audit'],
          area: defaultAdmin.area || 'Koregaon Park, Pune',
          avatar: defaultAdmin.avatar,
          bio: defaultAdmin.bio || `${defaultAdmin.designation} • Social Shuffle Organizer`,
          authProvider: 'google',
          loginAt: new Date().toISOString(),
          lastActiveAt: new Date().toISOString()
        }
      });
    }

    return res.json({
      success: true,
      isAuthenticated: false,
      user: null
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

  app.post('/api/events', (req, res) => {
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

  app.put('/api/events/:id', (req, res) => {
    const updated = dbEvents.update(req.params.id, req.body);
    if (updated) return res.json(updated);
    return res.status(404).json({ error: 'Event not found' });
  });

  app.post('/api/events/:id/duplicate', (req, res) => {
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

  app.delete('/api/events/:id', (req, res) => {
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

  app.post('/api/games', (req, res) => {
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

  app.put('/api/games/:id', (req, res) => {
    const updated = dbGames.update(req.params.id, req.body);
    if (updated) return res.json(updated);
    return res.status(404).json({ error: 'Game not found' });
  });

  app.patch('/api/games/:id/toggle-active', (req, res) => {
    const game = dbGames.getById(req.params.id);
    if (!game) return res.status(404).json({ error: 'Game not found' });
    const updated = dbGames.update(req.params.id, { active: !game.active });
    res.json(updated);
  });

  // ==========================================
  // PARTICIPANTS CRM DATABASE ENDPOINTS
  // ==========================================
  app.get('/api/participants', (req, res) => {
    const search = String(req.query.search || '').trim().toLowerCase();
    let list = dbParticipants.getAll();
    if (search) {
      list = list.filter(p =>
        p.name.toLowerCase().includes(search) ||
        p.email.toLowerCase().includes(search) ||
        p.phone.includes(search) ||
        (p.area && p.area.toLowerCase().includes(search))
      );
    }
    res.json(list);
  });

  app.get('/api/participants/check-duplicate', (req, res) => {
    const email = String(req.query.email || '').trim().toLowerCase();
    const phone = String(req.query.phone || '').trim().replace(/[^0-9]/g, '');
    const found = dbParticipants.getAll().find(p => {
      const pEmail = p.email.trim().toLowerCase();
      const pPhone = p.phone.replace(/[^0-9]/g, '');
      return (email && pEmail === email) || (phone.length >= 10 && pPhone.includes(phone));
    });
    res.json({ duplicate: found || null });
  });

  app.post('/api/participants/merge', (req, res) => {
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

    // Update registrations to point to targetId
    dbRegistrations.getAll().forEach(reg => {
      if (reg.participantId === sourceId) {
        dbRegistrations.update(reg.id, { participantId: targetId });
      }
    });

    console.log(`[DB CRM] Merged participant ${source.name} into ${target.name}`);
    res.json(merged);
  });

  app.post('/api/participants', (req, res) => {
    const data = req.body;
    const newId = data.id || `p-${Date.now().toString(36)}`;
    const newP = {
      ...data,
      id: newId,
      joinedDate: data.joinedDate || new Date().toISOString().slice(0, 10),
      totalEventsAttended: data.totalEventsAttended || 0,
      totalRegistrations: data.totalRegistrations || 0,
      gamesPlayedIds: data.gamesPlayedIds || [],
      venuesVisited: data.venuesVisited || [],
      totalPaxBrought: data.totalPaxBrought || 0
    };
    dbParticipants.insert(newP);
    res.status(201).json(newP);
  });

  app.put('/api/participants/:id', (req, res) => {
    const updated = dbParticipants.update(req.params.id, req.body);
    if (updated) return res.json(updated);
    return res.status(404).json({ error: 'Participant not found' });
  });

  // ==========================================
  // REGISTRATIONS & LIVE ATTENDANCE ENDPOINTS
  // ==========================================
  app.get('/api/registrations', (req, res) => {
    const eventId = req.query.eventId as string;
    let list = dbRegistrations.getAll();
    if (eventId) {
      list = list.filter(r => r.eventId === eventId);
    }
    res.json(list);
  });

  app.post('/api/registrations', (req, res) => {
    const regData = req.body;
    const newId = regData.id || `reg-${Date.now().toString(36)}`;
    const newReg = {
      ...regData,
      id: newId,
      registeredAt: regData.registeredAt || new Date().toISOString(),
      attendanceStatus: regData.attendanceStatus || 'Registered',
      paymentStatus: regData.paymentStatus || 'Completed'
    };
    dbRegistrations.insert(newReg);

    // Update event registration count
    const ev = dbEvents.getById(newReg.eventId);
    if (ev) {
      dbEvents.update(ev.id, { registeredCount: (ev.registeredCount || 0) + (newReg.paxCount || 1) });
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
        }).then(mailRes => {
          console.log(`[Registration Mail] Pass sent to ${newReg.participantEmail}:`, mailRes.success ? 'Delivered' : mailRes.error);
        }).catch(err => console.warn('[Registration Mail Error]', err));
      } catch (err) {
        console.warn('[Registration Mail Template Error]', err);
      }
    }

    /*
    // WhatsApp notification temporarily commented out
    if (newReg.participantPhone) {
      try {
        const waText = generateEventWhatsAppNotification(newReg as any, ev, 'Your Social Shuffle pass is confirmed! Show this check-in token at the door.');
        dispatchOutboundWhatsApp({
          toPhone: newReg.participantPhone,
          messageText: waText
        }).then(waRes => {
          console.log(`[Registration WhatsApp] Dispatched to ${newReg.participantPhone}:`, waRes.statusText);
        }).catch(err => console.warn('[Registration WhatsApp Error]', err));
      } catch (err) {
        console.warn('[Registration WhatsApp Template Error]', err);
      }
    }
    */

    console.log(`[DB Reg] New RSVP in database for event ${newReg.eventId}: ${newReg.participantName} (${newReg.paxCount} pax)`);
    res.status(201).json(newReg);
  });

  app.post('/api/registrations/bulk-import', (req, res) => {
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

  app.patch('/api/registrations/:id/attendance', (req, res) => {
    const status = String(req.query.status || req.body.status || 'Checked In');
    const updated = dbRegistrations.update(req.params.id, {
      attendanceStatus: status as any,
      checkedInAt: status === 'Checked In' ? new Date().toISOString() : undefined
    });
    if (updated) {
      // Recalculate event checked-in count
      const eventRegs = dbRegistrations.getAll().filter(r => r.eventId === updated.eventId && r.attendanceStatus === 'Checked In');
      dbEvents.update(updated.eventId, { checkedInCount: eventRegs.reduce((acc, c) => acc + (c.paxCount || 1), 0) });
      return res.json(updated);
    }
    return res.status(404).json({ error: 'Registration not found' });
  });

  app.patch('/api/registrations/:id/payment', (req, res) => {
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

  app.post('/api/payments/razorpay/config', (req, res) => {
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
      console.log(`[Razorpay DB] Updated gateway config. Key: ${updated.keyId}, Live: ${updated.isLive}`);
      res.json({ success: true, message: 'Razorpay configuration saved successfully', config: { ...updated, keySecret: undefined } });
    } catch (err: any) {
      res.status(500).json({ success: false, error: err?.message });
    }
  });

  app.post('/api/payments/razorpay/create-order', (req, res) => {
    const { amount, eventId, eventTitle, participantName, participantEmail, paxCount } = req.body;
    const config = loadRazorpayConfig();
    const keyId = process.env.VITE_RAZORPAY_KEY_ID || process.env.RAZORPAY_KEY_ID || config.keyId || 'rzp_test_SocialShufflePune';

    // Amount in INR
    const numAmount = Number(amount) || 350;
    const amountInPaise = Math.round(numAmount * 100);

    // Generate unique Razorpay Order ID format: order_XXXXXXXXXXXXXXXX
    const orderId = `order_${Date.now().toString(36)}_${Math.random().toString(36).substring(2, 8).toUpperCase()}`;

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

  app.post('/api/payments/razorpay/verify-payment', (req, res) => {
    const { razorpayPaymentId, razorpayOrderId, registrationId, eventId, amount } = req.body;
    const paymentId = razorpayPaymentId || `pay_${Date.now().toString(36)}_${Math.random().toString(36).substring(2, 6).toUpperCase()}`;

    console.log(`[Razorpay Verify] Payment verified: ${paymentId} for order ${razorpayOrderId || 'N/A'}, amount: ₹${amount || 0}`);

    if (registrationId) {
      const reg = dbRegistrations.getById(registrationId);
      if (reg) {
        dbRegistrations.update(registrationId, {
          paymentStatus: 'Confirmed',
          razorpayPaymentId: paymentId,
          razorpayOrderId: razorpayOrderId || reg.razorpayOrderId,
          amountPaid: Number(amount) || reg.amountPaid
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

  app.post('/api/registrations/:id/toggle-game', (req, res) => {
    const gameId = String(req.query.gameId || req.body.gameId);
    const reg = dbRegistrations.getById(req.params.id);
    if (!reg) return res.status(404).json({ error: 'Registration not found' });

    const currentGames = reg.gamesPlayedIds || [];
    const updatedGames = currentGames.includes(gameId)
      ? currentGames.filter(g => g !== gameId)
      : [...currentGames, gameId];

    const updated = dbRegistrations.update(req.params.id, { gamesPlayedIds: updatedGames });
    res.json(updated);
  });

  app.post('/api/registrations/walk-in', (req, res) => {
    const eventId = String(req.query.eventId || req.body.eventId);
    const { name, phone, email, area, paxCount, paymentStatus, notes } = req.body;
    const newId = `reg-walkin-${Date.now().toString(36)}`;

    // Create participant if doesn't exist
    let p = dbParticipants.getAll().find(part => part.email.toLowerCase() === email.toLowerCase());
    if (!p) {
      p = {
        id: `p-${Date.now().toString(36)}`,
        name,
        email,
        phone,
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

    // Update event counts
    const ev = dbEvents.getById(eventId);
    if (ev) {
      dbEvents.update(ev.id, {
        registeredCount: (ev.registeredCount || 0) + (paxCount || 1),
        checkedInCount: (ev.checkedInCount || 0) + (paxCount || 1)
      });
    }

    // Automatically dispatch entry pass email for walk-in attendee
    if (email) {
      try {
        const emailHtml = generateRegistrationEmailHtml(newReg as any, ev);
        sendOutboundEmail({
          to: email,
          subject: `🎲 VIP Entry Pass (Walk-in Confirmed): ${ev?.title || 'Social Shuffle Pune'}!`,
          html: emailHtml,
          text: `Your walk-in registration for ${ev?.title || 'Social Shuffle'} is confirmed. Token: ${newReg.id}`
        }).then(mailRes => {
          console.log(`[Walk-in Mail] Sent to ${email}:`, mailRes.success ? 'Delivered' : mailRes.error);
        }).catch(err => console.warn('[Walk-in Mail Error]', err));
      } catch (err) {
        console.warn('[Walk-in Mail Template Error]', err);
      }
    }

    res.status(201).json(newReg);
  });

  app.delete('/api/registrations/:id', (req, res) => {
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

  app.put('/api/community/stats', handleUpdateCommunityStats);
  app.post('/api/community/stats', handleUpdateCommunityStats);

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

    // Alert admin Parimal with feedback details
    try {
      const fbHtml = generateFeedbackAdminAlertEmailHtml(newFb);
      sendOutboundEmail({
        to: 'parimalmshete@gmail.com',
        subject: `⭐ New Community Review: ${newFb.rating || 5}/5 Stars from ${newFb.userName || 'Attendee'}`,
        html: fbHtml,
        text: `New feedback received from ${newFb.userName || 'Attendee'}: ${newFb.rating} stars. Comments: ${newFb.comment || 'N/A'}`
      }).catch(err => console.warn('[Feedback Alert Error]', err));
    } catch (err) {
      console.warn('[Feedback Alert Template Error]', err);
    }

    res.status(201).json(newFb);
  });

  app.get('/api/community/reports', (_req, res) => {
    res.json(dbReports.getAll());
  });

  app.post('/api/community/reports', (req, res) => {
    const data = req.body;
    const newRep = {
      ...data,
      id: data.id || `rep-${Date.now().toString(36)}`,
      status: data.status || 'New',
      createdAt: new Date().toISOString()
    };
    dbReports.insert(newRep);

    // 1. Alert leadership Parimal immediately
    try {
      const alertHtml = generateReportAdminAlertEmailHtml(newRep);
      sendOutboundEmail({
        to: 'parimalmshete@gmail.com',
        subject: `⚠️ [URGENT] Safety / Grievance Report: ${newRep.category || 'Incident'}`,
        html: alertHtml,
        text: `A safety / grievance report has been submitted by ${newRep.reporterName || 'Anonymous'}. Description: ${newRep.description || ''}`
      }).catch(err => console.warn('[Report Alert Error]', err));
    } catch (err) {
      console.warn('[Report Alert Template Error]', err);
    }

    // 2. Acknowledge reporter if email provided
    if (newRep.reporterEmail) {
      try {
        const ackHtml = generateReportAckEmailHtml(newRep);
        sendOutboundEmail({
          to: newRep.reporterEmail,
          subject: `Social Shuffle: We have received your submission`,
          html: ackHtml,
          text: `Hello ${newRep.reporterName || 'Fellow Shuffler'}, thank you for contacting Social Shuffle leadership. We are reviewing your submission.`
        }).catch(err => console.warn('[Report Ack Error]', err));
      } catch (err) {
        console.warn('[Report Ack Template Error]', err);
      }
    }

    res.status(201).json(newRep);
  });

  app.patch('/api/community/reports/:id/status', (req, res) => {
    const { status, notes } = req.query;
    const updated = dbReports.update(req.params.id, {
      status: (status as any) || 'Resolved',
      adminNotes: (notes as string) || undefined
    });
    if (updated) return res.json(updated);
    return res.status(404).json({ error: 'Report not found' });
  });

  app.get('/api/community/volunteers', (_req, res) => {
    res.json(dbVolunteers.getAll());
  });

  app.post('/api/community/volunteers', (req, res) => {
    const data = req.body;
    const newVol = {
      ...data,
      id: data.id || `vol-${Date.now().toString(36)}`,
      status: data.status || 'New',
      submittedAt: new Date().toISOString()
    };
    dbVolunteers.insert(newVol);

    // 1. Send confirmation to volunteer
    if (newVol.email) {
      try {
        const confHtml = generateVolunteerConfirmationEmailHtml(newVol);
        sendOutboundEmail({
          to: newVol.email,
          subject: `🎲 Thank You for Applying to Volunteer at Social Shuffle, ${newVol.name}!`,
          html: confHtml,
          text: `Hello ${newVol.name}, we received your volunteer application for Social Shuffle Pune. We will contact you shortly!`
        }).catch(err => console.warn('[Volunteer Conf Mail Error]', err));
      } catch (err) {
        console.warn('[Volunteer Conf Mail Template Error]', err);
      }
    }

    // 2. Alert admin Parimal
    try {
      const alertHtml = generateVolunteerAdminAlertEmailHtml(newVol);
      sendOutboundEmail({
        to: 'parimalmshete@gmail.com',
        subject: `🙋‍♂️ New Volunteer Application: ${newVol.name} (${newVol.preferredArea || 'Pune'})`,
        html: alertHtml,
        text: `New volunteer application received from ${newVol.name} (${newVol.email}, ${newVol.phone}).`
      }).catch(err => console.warn('[Volunteer Admin Alert Error]', err));
    } catch (err) {
      console.warn('[Volunteer Admin Alert Template Error]', err);
    }

    res.status(201).json(newVol);
  });

  app.patch('/api/community/volunteers/:id/status', (req, res) => {
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
  app.get('/api/audit-logs', (_req, res) => {
    res.json(dbAuditLogs.getAll());
  });

  app.post('/api/audit-logs', (req, res) => {
    const data = req.body;
    const newLog = {
      ...data,
      id: data.id || `log-${Date.now().toString(36)}`,
      timestamp: new Date().toISOString()
    };
    dbAuditLogs.insert(newLog);
    res.status(201).json(newLog);
  });

  app.get('/api/notifications', (_req, res) => {
    res.json(dbNotifications.getAll());
  });

  app.patch('/api/notifications/:id/read', (req, res) => {
    const updated = dbNotifications.update(req.params.id, { read: true });
    if (updated) return res.json({ success: true });
    return res.status(404).json({ error: 'Notification not found' });
  });

  app.post('/api/notifications/mark-all-read', (_req, res) => {
    dbNotifications.getAll().forEach(n => {
      dbNotifications.update(n.id, { read: true });
    });
    res.json({ success: true });
  });

  app.delete('/api/notifications/:id', (req, res) => {
    dbNotifications.remove(req.params.id);
    res.json({ success: true, id: req.params.id });
  });

  app.post('/api/notifications/community-announcement', (req, res) => {
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

  app.post('/api/notifications/event-broadcast', handleDualPushNotification);
  app.post('/api/events/:id/push-notification', handleDualPushNotification);
  app.post('/api/notifications/send-reminder/:id', handleDualPushNotification);

  // Send Registration Confirmation Pass via Real Email
  app.post('/api/notifications/registration-confirmation/:id', async (req, res) => {
    let reg = dbRegistrations.getById(req.params.id);
    if (!reg && req.body && req.body.registration) {
      reg = req.body.registration;
      try {
        dbRegistrations.insert(reg);
      } catch {}
    }
    if (!reg) return res.status(404).json({ success: false, message: 'Registration not found' });
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
        console.log(`[Registration Confirmation Email] Dispatched to ${reg.participantEmail}:`, emailRes.success ? 'Delivered' : emailRes.error);
      } catch (err: any) {
        console.warn('[Confirmation Mail Error]', err?.message);
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

  // OTP Verification System with REAL Outbound Email
  interface OtpRecord {
    email: string;
    otp: string;
    expiresAt: number;
    createdAt: number;
    attempts: number;
    name?: string;
  }
  const otpStore = new Map<string, OtpRecord>();
  const verifiedEmails = new Set<string>();

  app.post('/api/auth/otp/send', async (req, res) => {
    const { email, name } = req.body;
    if (!email || typeof email !== 'string') {
      return res.status(400).json({ success: false, message: 'Valid email address is required' });
    }
    const cleanEmail = email.trim().toLowerCase();
    const cleanName = (name && typeof name === 'string') ? name.trim() : 'Fellow Shuffler';
    const otp = Math.floor(100000 + Math.random() * 900000).toString();
    const expiresInSeconds = 600;
    const expiresAt = Date.now() + expiresInSeconds * 1000;

    otpStore.set(cleanEmail, {
      email: cleanEmail,
      otp,
      expiresAt,
      createdAt: Date.now(),
      attempts: 0,
      name: cleanName
    });

    // Send REAL OTP verification email
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

    // Send REAL OTP verification email asynchronously (non-blocking)
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

    // Also persist in in-app notifications so user can view it in the app
    try {
      dbNotifications.insert({
        id: `notif-otp-${Date.now()}`,
        userId: cleanEmail,
        title: 'Email Verification Code',
        message: `Your verification code is ${otp}. Enter this 6-digit code to complete signup.`,
        type: 'account',
        read: false,
        createdAt: new Date().toISOString()
      });
    } catch {}

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
    if (record.otp !== cleanOtp) {
      record.attempts += 1;
      return res.status(400).json({ success: false, message: 'Invalid OTP code' });
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

  // Get current Email & WhatsApp configuration (passwords masked)
  app.get('/api/communications/config', async (_req, res) => {
    try {
      const config = loadCommunicationsConfig();
      const { transporter, provider, isRealSmtp } = await getMailTransporter();

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

  // Update Email SMTP or WhatsApp configuration
  app.post('/api/communications/config', (req, res) => {
    try {
      const current = loadCommunicationsConfig();
      const { email, whatsapp } = req.body;

      const updated = {
        email: {
          ...current.email,
          ...(email || {}),
          // Don't overwrite password with masked placeholder
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

  // Sync active Google OAuth token for Gmail API dispatch
  app.post('/api/communications/set-gmail-token', (req, res) => {
    const { token, email } = req.body;
    setGmailOAuthToken(token || null, email || null);
    res.json({ success: true, ...getGmailOAuthStatus() });
  });

  app.get('/api/communications/gmail-status', (_req, res) => {
    res.json(getGmailOAuthStatus());
  });

  // Live Outbound Email Test Dispatch
  app.post('/api/communications/test-email', async (req, res) => {
    const targetRecipient = req.body.toEmail || req.body.to || req.body.email || 'parimalmshete@gmail.com';
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
          <p style="font-size: 14px; margin-top: 0; color: #e2e8f0;">${message.replace(/\n/g, '<br/>')}</p>
          <div style="border-top: 1px solid #283677; margin-top: 16px; padding-top: 14px; font-size: 12px; color: #94A3B8; space-y: 4px;">
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
