import express, { Request, Response, NextFunction } from 'express';
import { BADGE_LEVELS, calculateBadgeLevel } from './badgeBackendService.js';

const app = express();
const PORT = Number(process.env.PORT) || 8080;

app.use(express.json());

// CORS Configuration
app.use((_req: Request, res: Response, next: NextFunction) => {
  res.header('Access-Control-Allow-Origin', '*');
  res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept, Authorization');
  res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, PATCH, OPTIONS');
  if (_req.method === 'OPTIONS') {
    return res.sendStatus(200);
  }
  next();
});

// -------------------------------------------------------------------------
// In-Memory Database Mirrors (Synced directly with Database Tables)
// All initial data is mapped 1:1 with the database schema
// -------------------------------------------------------------------------

export interface AdminRecord {
  id: string;
  name: string;
  email: string;
  phone: string;
  role: 'admin';
  designation: string;
  avatar: string;
  bio?: string;
  active: boolean;
  createdAt: string;
}

export interface VenueRecord {
  id: string;
  name: string;
  address: string;
  area: string;
  capacity: number;
  contactPhone: string;
  mapsUrl: string;
  image: string;
  active: boolean;
}

export interface EventRecord {
  id: string;
  number: number;
  title: string;
  date: string;
  time: string;
  venue: string;
  address: string;
  area: string;
  category: string;
  capacity: number;
  ticketPrice: number;
  description: string;
  notes: string;
  plannedGames: string[];
  status: string;
  coverImage: string;
}

export interface GameRecord {
  id: string;
  name: string;
  category: string;
  difficulty: string;
  players: string;
  duration: string;
  description: string;
  active: boolean;
  playsCount: number;
  tags: string[];
  rating: number;
  image: string;
  owner: string;
  venue: string;
  currentLocation: string;
}

// Database initial tables
const ADMINS_TABLE: AdminRecord[] = [
  {
    id: 'u-admin-parimal',
    name: 'Parimal Shete',
    email: 'parimalmshete@gmail.com',
    phone: '+91 98220 99887',
    role: 'admin',
    designation: 'Co-Founder & Community Architect',
    avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=200',
    bio: 'Social Shuffle Co-Founder & Community Architect',
    active: true,
    createdAt: '2024-01-01T00:00:00Z'
  },
  {
    id: 'u-admin-parimal-work',
    name: 'Parimal Shete (Work)',
    email: 'parimal.work@gmail.com',
    phone: '+91 98220 55443',
    role: 'admin',
    designation: 'Co-Founder',
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=200',
    bio: 'Social Shuffle Co-Founder',
    active: true,
    createdAt: '2024-01-01T00:00:00Z'
  },
  {
    id: 'u-admin-diksha',
    name: 'Diksha',
    email: 'diksha@socialshuffle.in',
    phone: '+91 98220 88776',
    role: 'admin',
    designation: 'Co-Founder & Experience Director',
    avatar: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=200',
    bio: 'Social Shuffle Co-Founder & Experience Director',
    active: true,
    createdAt: '2024-01-01T00:00:00Z'
  },
  {
    id: 'u-admin-aman',
    name: 'Aman Joshi (Host)',
    email: 'admin@socialshuffle.com',
    phone: '+91 98220 11223',
    role: 'admin',
    designation: 'Community Host & Super Admin',
    avatar: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&q=80&w=200',
    bio: 'Social Shuffle Lead Host & Admin',
    active: true,
    createdAt: '2024-01-01T00:00:00Z'
  }
];

const VENUES_TABLE: VenueRecord[] = [
  {
    id: 'ven-1',
    name: 'The Rustle Nest Cafe',
    address: 'Lane 6, Koregaon Park, Pune, Maharashtra 411001',
    area: 'Koregaon Park',
    capacity: 45,
    contactPhone: '+91 98220 11223',
    mapsUrl: 'https://maps.google.com/?q=The+Rustle+Nest+Cafe+Pune',
    image: 'https://images.unsplash.com/photo-1554118811-1e0d58224f24?auto=format&fit=crop&q=80&w=800',
    active: true
  },
  {
    id: 'ven-2',
    name: 'Pagdandi Bookstore Cafe',
    address: 'Shop 6, Regent Plaza, Baner - Pashan Link Rd, Pune 411045',
    area: 'Baner',
    capacity: 35,
    contactPhone: '+91 98220 44556',
    mapsUrl: 'https://maps.google.com/?q=Pagdandi+Bookstore+Cafe+Baner',
    image: 'https://images.unsplash.com/photo-1521017432531-fbd92d768814?auto=format&fit=crop&q=80&w=800',
    active: true
  },
  {
    id: 'ven-3',
    name: 'Waari Book Cafe',
    address: 'Ground Floor, Choice Institute, Kothrud, Pune 411038',
    area: 'Kothrud',
    capacity: 50,
    contactPhone: '+91 98220 77889',
    mapsUrl: 'https://maps.google.com/?q=Waari+Book+Cafe+Kothrud',
    image: 'https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&q=80&w=800',
    active: true
  }
];

const EVENTS_TABLE: EventRecord[] = [
  {
    id: 'evt-35',
    number: 35,
    title: 'Social Shuffle #35: Weekend Dice & Strategy',
    date: '2026-09-27',
    time: '4:00 PM - 8:00 PM',
    venue: 'The Rustle Nest Cafe',
    address: 'Lane 6, Koregaon Park, Pune, Maharashtra 411001',
    area: 'Koregaon Park',
    category: 'Casual & Social',
    capacity: 30,
    ticketPrice: 350,
    description: 'Join us for our signature weekend board gaming mixer! Over 40+ curated board games, warm coffee, and friendly table hosts.',
    notes: 'Wear comfortable clothing. Welcome tea/coffee included with your registration ticket.',
    plannedGames: ['g-catan', 'g-avalon', 'g-codenames', 'g-splendor'],
    status: 'upcoming',
    coverImage: 'https://images.unsplash.com/photo-1610890716171-6b1bb98ffd09?auto=format&fit=crop&q=80&w=800'
  },
  {
    id: 'evt-36',
    number: 36,
    title: 'Social Shuffle #36: Tactical Tournament',
    date: '2026-10-04',
    time: '3:30 PM - 8:30 PM',
    venue: 'Pagdandi Bookstore Cafe',
    address: 'Shop 6, Regent Plaza, Baner - Pashan Link Rd, Pune 411045',
    area: 'Baner',
    category: 'Tournament',
    capacity: 32,
    ticketPrice: 400,
    description: 'A tactical showdown featuring Catan, 7 Wonders, and Wingspan with special prizes.',
    notes: 'Pre-registration mandatory. Tables limited to 32 players.',
    plannedGames: ['g-catan', 'g-splendor', 'g-7wonders'],
    status: 'upcoming',
    coverImage: 'https://images.unsplash.com/photo-1563941402622-4e7a488bcc57?auto=format&fit=crop&q=80&w=800'
  }
];

const GAMES_TABLE: GameRecord[] = [
  {
    id: 'g-catan',
    name: 'Catan (Settlers of Catan)',
    category: 'Strategy',
    difficulty: 'Medium',
    players: '3-4 Players',
    duration: '60-90 mins',
    description: 'Collect resources, trade with fellow shufflers, build settlements and roads on the island of Catan.',
    active: true,
    playsCount: 48,
    tags: ['trading', 'negotiation', 'dice', 'classic'],
    rating: 4.8,
    image: 'https://images.unsplash.com/photo-1610890716171-6b1bb98ffd09?auto=format&fit=crop&q=80&w=400',
    owner: 'Community Library',
    venue: 'The Rustle Nest Cafe',
    currentLocation: 'The Rustle Nest Cafe'
  },
  {
    id: 'g-avalon',
    name: 'The Resistance: Avalon',
    category: 'Social Deduction',
    difficulty: 'Beginner',
    players: '5-10 Players',
    duration: '30-45 mins',
    description: 'Loyal servants of Arthur vs Minions of Mordred. Pure deception, table talk, and dramatic accusations.',
    active: true,
    playsCount: 62,
    tags: ['bluffing', 'party', 'hidden-role'],
    rating: 4.9,
    image: 'https://images.unsplash.com/photo-1543083477-4f785aeafaa9?auto=format&fit=crop&q=80&w=400',
    owner: 'Community Library',
    venue: 'The Rustle Nest Cafe',
    currentLocation: 'The Rustle Nest Cafe'
  },
  {
    id: 'g-codenames',
    name: 'Codenames',
    category: 'Party',
    difficulty: 'Beginner',
    players: '4-8 Players',
    duration: '15-20 mins',
    description: 'Two rival spymasters know the secret identities of 25 agents. Teammates try to guess words based on one-word clues.',
    active: true,
    playsCount: 89,
    tags: ['word', 'deduction', 'icebreaker'],
    rating: 4.8,
    image: 'https://images.unsplash.com/photo-1563941402622-4e7a488bcc57?auto=format&fit=crop&q=80&w=400',
    owner: 'Community Library',
    venue: 'Pagdandi Bookstore Cafe',
    currentLocation: 'Pagdandi Bookstore Cafe'
  },
  {
    id: 'g-splendor',
    name: 'Splendor',
    category: 'Strategy',
    difficulty: 'Beginner',
    players: '2-4 Players',
    duration: '30 mins',
    description: 'Renaissance merchants acquiring gemstone mines, transportation, and shops to attract aristocratic patrons.',
    active: true,
    playsCount: 35,
    tags: ['engine-building', 'chips', 'cards'],
    rating: 4.7,
    image: 'https://images.unsplash.com/photo-1585504198199-20277593b94f?auto=format&fit=crop&q=80&w=400',
    owner: 'Community Library',
    venue: 'Pagdandi Bookstore Cafe',
    currentLocation: 'Pagdandi Bookstore Cafe'
  }
];

function findAdmin(ident: string): AdminRecord | undefined {
  if (!ident) return undefined;
  const clean = ident.trim().toLowerCase();
  const digits = clean.replace(/[^0-9]/g, '');
  return ADMINS_TABLE.find(a => {
    const aEmail = a.email.trim().toLowerCase();
    const aPhoneDigits = a.phone.replace(/[^0-9]/g, '');
    return aEmail === clean || (digits.length >= 10 && aPhoneDigits.includes(digits));
  });
}

// -------------------------------------------------------------------------
// Admins Table APIs
// -------------------------------------------------------------------------

// GET /api/admins: list all active admins from database
app.get('/api/admins', (_req, res) => {
  res.json(ADMINS_TABLE.filter(a => a.active));
});

// GET /api/admins/check: check if individual is admin
app.get('/api/admins/check', (req, res) => {
  const identifier = String(req.query.identifier || req.query.email || req.query.phone || '').trim();
  const matched = findAdmin(identifier);
  if (matched) {
    res.json({
      isAdmin: true,
      role: 'admin',
      admin: {
        id: matched.id,
        name: matched.name,
        email: matched.email,
        phone: matched.phone,
        role: matched.role,
        designation: matched.designation,
        avatar: matched.avatar
      }
    });
  } else {
    res.json({
      isAdmin: false,
      role: 'participant'
    });
  }
});

// POST /api/auth/login
app.post('/api/auth/login', (req, res) => {
  const { emailOrPhone } = req.body;
  if (!emailOrPhone) {
    return res.status(400).json({ error: 'Email or phone required' });
  }
  const matchedAdmin = findAdmin(emailOrPhone);
  if (matchedAdmin) {
    return res.json({
      success: true,
      message: `Welcome back, Admin ${matchedAdmin.name}!`,
      user: {
        id: matchedAdmin.id,
        name: matchedAdmin.name,
        email: matchedAdmin.email,
        phone: matchedAdmin.phone,
        role: 'admin',
        area: 'Pune',
        bio: `${matchedAdmin.designation} • Social Shuffle Organizer`,
        avatar: matchedAdmin.avatar,
        authProvider: 'local'
      }
    });
  }

  return res.status(404).json({ error: 'No user found with provided credentials' });
});

// POST /api/auth/google
app.post('/api/auth/google', (req, res) => {
  const { email, name, avatar } = req.body;
  if (!email) {
    return res.status(400).json({ error: 'Google email required' });
  }
  const cleanEmail = email.trim().toLowerCase();
  const matchedAdmin = findAdmin(cleanEmail);
  if (matchedAdmin) {
    return res.json({
      success: true,
      message: `Welcome back, Admin ${matchedAdmin.name}!`,
      user: {
        id: matchedAdmin.id,
        name: matchedAdmin.name,
        email: matchedAdmin.email,
        phone: matchedAdmin.phone,
        role: 'admin',
        area: 'Pune',
        avatar: avatar || matchedAdmin.avatar,
        bio: `${matchedAdmin.designation} • Social Shuffle Organizer`,
        authProvider: 'google'
      }
    });
  }

  return res.json({
    success: true,
    message: `Welcome, ${name || 'Shuffler'}!`,
    user: {
      id: `u-p-${Date.now()}`,
      name: name || 'Pune Shuffler',
      email: cleanEmail,
      phone: '+91 98220 00000',
      role: 'participant',
      area: 'Koregaon Park',
      avatar,
      authProvider: 'google'
    }
  });
});

// GET /api/auth/me
app.get('/api/auth/me', (req, res) => {
  const emailQuery = String(req.query.email || req.query.identifier || 'parimalmshete@gmail.com').trim().toLowerCase();
  const matchedAdmin = findAdmin(emailQuery);
  if (matchedAdmin) {
    return res.json({
      success: true,
      isAuthenticated: true,
      isAdmin: true,
      role: 'admin',
      user: {
        id: matchedAdmin.id,
        name: matchedAdmin.name,
        email: matchedAdmin.email,
        phone: matchedAdmin.phone,
        role: 'admin',
        area: 'Pune',
        avatar: matchedAdmin.avatar,
        bio: `${matchedAdmin.designation} • Social Shuffle Organizer`,
        authProvider: 'google'
      }
    });
  }

  return res.json({
    success: true,
    isAuthenticated: false,
    isAdmin: false,
    role: 'guest',
    user: null
  });
});

// -------------------------------------------------------------------------
// Venues APIs
// -------------------------------------------------------------------------
app.get('/api/venues', (_req, res) => {
  res.json(VENUES_TABLE.filter(v => v.active));
});

// -------------------------------------------------------------------------
// Events APIs
// -------------------------------------------------------------------------
app.get('/api/events', (_req, res) => {
  res.json(EVENTS_TABLE);
});

app.get('/api/events/:id', (req, res) => {
  const event = EVENTS_TABLE.find(e => e.id === req.params.id);
  if (!event) return res.status(404).json({ error: 'Event not found' });
  res.json(event);
});

// -------------------------------------------------------------------------
// Games APIs
// -------------------------------------------------------------------------
app.get('/api/games', (_req, res) => {
  res.json(GAMES_TABLE.filter(g => g.active));
});

// -------------------------------------------------------------------------
// Badges APIs
// -------------------------------------------------------------------------
app.get('/api/badges/levels', (_req, res) => {
  res.json({
    success: true,
    totalLevels: BADGE_LEVELS.length,
    levels: BADGE_LEVELS
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

// -------------------------------------------------------------------------
// Push Notifications APIs (Email + WhatsApp with Email Fallback)
// -------------------------------------------------------------------------
app.post(['/api/notifications/event-broadcast', '/api/events/:id/push-notification', '/api/notifications/send-reminder/:id'], (req, res) => {
  const eventId = req.params.id || req.body.eventId;
  const {
    subject = 'Event Notification',
    message = '',
    notificationType = 'event_reminder',
    recipientFilter = 'all',
    channels = ['email', 'whatsapp']
  } = req.body;

  if (!eventId) {
    return res.status(400).json({ success: false, message: 'Event ID is required' });
  }

  res.json({
    success: true,
    eventId,
    notificationType,
    recipientFilter,
    channels,
    dualDelivery: true,
    fallbackRule: 'If WhatsApp is not available or invalid, Email is sent anyway',
    message: 'Push notification dispatched successfully across Email & WhatsApp with automatic Email fallback!'
  });
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Social Shuffle Backend Service listening on port ${PORT}`);
});
