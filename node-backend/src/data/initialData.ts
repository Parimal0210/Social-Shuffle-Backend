import {
  Game,
  ShuffleEvent,
  Participant,
  Registration,
  EventFeedback,
  SafetyReport,
  VolunteerApplication,
  AuditLog,
  NotificationItem,
  AdminRecord,
  CommunityStats
} from '../types';

export const INITIAL_GAMES: Game[] = [
  // Party
  {
    id: 'g-codenames',
    name: 'Codenames',
    category: 'Party',
    difficulty: 'Beginner',
    players: '4-8+ Players',
    duration: '15-20 mins',
    description: 'Two rival spymasters know the secret identities of 25 agents. Teammates must decode clues without uncovering the assassin.',
    active: true,
    playsCount: 142,
    rating: 4.8
  },
  {
    id: 'g-justone',
    name: 'Just One',
    category: 'Party',
    difficulty: 'Beginner',
    players: '3-7 Players',
    duration: '20 mins',
    description: 'Cooperative party game where players write a one-word clue to help the guesser, but duplicate clues get eliminated!',
    active: true,
    playsCount: 96,
    rating: 4.9
  },
  {
    id: 'g-decrypto',
    name: 'Decrypto',
    category: 'Party',
    difficulty: 'Medium',
    players: '4-8 Players',
    duration: '30 mins',
    description: 'Relay secret codes to teammates without letting the opposing team intercept and decipher your frequency.',
    active: true,
    playsCount: 68,
    rating: 4.7
  },
  {
    id: 'g-wavelength',
    name: 'Wavelength',
    category: 'Party',
    difficulty: 'Beginner',
    players: '4-12 Players',
    duration: '30-45 mins',
    description: 'Telepathic party game of reading each other minds on a spectrum between Hot and Cold, Sad and Happy.',
    active: true,
    playsCount: 88,
    rating: 4.8
  },
  {
    id: 'g-dixit',
    name: 'Dixit',
    category: 'Party',
    difficulty: 'Beginner',
    players: '3-8 Players',
    duration: '30 mins',
    description: 'Enchanting illustration card game of poetic clues, subtlety, and visual imagination.',
    active: true,
    playsCount: 110,
    rating: 4.6
  },
  {
    id: 'g-tacocat',
    name: 'Taco Cat Goat Cheese Pizza',
    category: 'Party',
    difficulty: 'Beginner',
    players: '3-8 Players',
    duration: '10-15 mins',
    description: 'Fast-paced, hand-slapping card game requiring lightning reflexes and pure laughter.',
    active: true,
    playsCount: 130,
    rating: 4.5
  },

  // Strategy
  {
    id: 'g-catan',
    name: 'Catan',
    category: 'Strategy',
    difficulty: 'Medium',
    players: '3-4 Players',
    duration: '60-90 mins',
    description: 'Trade, build, and settle the island of Catan. Manage wheat, sheep, ore, and brick to reach 10 victory points.',
    active: true,
    playsCount: 175,
    rating: 4.7
  },
  {
    id: 'g-ticket',
    name: 'Ticket to Ride: Europe',
    category: 'Strategy',
    difficulty: 'Beginner',
    players: '2-5 Players',
    duration: '45-60 mins',
    description: 'Cross-country train adventure connecting iconic European cities through clever route drafting.',
    active: true,
    playsCount: 124,
    rating: 4.8
  },
  {
    id: 'g-splendor',
    name: 'Splendor',
    category: 'Strategy',
    difficulty: 'Beginner',
    players: '2-4 Players',
    duration: '30 mins',
    description: 'Renaissance gemstone merchants acquiring mines, transport ships, and artisan shops to attract noble patronage.',
    active: true,
    playsCount: 160,
    rating: 4.9
  },
  {
    id: 'g-wingspan',
    name: 'Wingspan',
    category: 'Strategy',
    difficulty: 'Medium',
    players: '1-5 Players',
    duration: '40-70 mins',
    description: 'Award-winning engine-building board game discovering and attracting the best birds to your wildlife preserve.',
    active: true,
    playsCount: 78,
    rating: 4.9
  },
  {
    id: 'g-scythe',
    name: 'Scythe',
    category: 'Strategy',
    difficulty: 'Advanced',
    players: '1-5 Players',
    duration: '90-120 mins',
    description: 'Alternate-history 1920s dieselpunk epic of farming, mechs, engine-building, and faction glory.',
    active: true,
    playsCount: 42,
    rating: 4.8
  },
  {
    id: 'g-dune',
    name: 'Dune: Imperium',
    category: 'Strategy',
    difficulty: 'Advanced',
    players: '1-4 Players',
    duration: '90-120 mins',
    description: 'Worker placement and deck-building set in Frank Herbert’s desert world of Arrakis and spice control.',
    active: true,
    playsCount: 54,
    rating: 4.9
  },

  // Bluffing
  {
    id: 'g-coup',
    name: 'Coup',
    category: 'Bluffing',
    difficulty: 'Beginner',
    players: '2-6 Players',
    duration: '15 mins',
    description: 'Dystopian sci-fi city where deception, bribery, and bluffing are the only paths to political survival.',
    active: true,
    playsCount: 220,
    rating: 4.9
  },
  {
    id: 'g-skull',
    name: 'Skull',
    category: 'Bluffing',
    difficulty: 'Beginner',
    players: '3-6 Players',
    duration: '15-20 mins',
    description: 'The ancient art of bluffing and poker-faces boiled down to coasters, roses, and deadly skulls.',
    active: true,
    playsCount: 95,
    rating: 4.8
  },
  {
    id: 'g-sheriff',
    name: 'Sheriff of Nottingham',
    category: 'Bluffing',
    difficulty: 'Medium',
    players: '3-5 Players',
    duration: '60 mins',
    description: 'Smuggle contraband past the watchful Sheriff of Nottingham using cunning, bribes, and honest deception.',
    active: true,
    playsCount: 84,
    rating: 4.7
  },
  {
    id: 'g-cockroach',
    name: 'Cockroach Poker',
    category: 'Bluffing',
    difficulty: 'Beginner',
    players: '2-6 Players',
    duration: '20 mins',
    description: 'Can you convince your friend that this card is a royal stink bug or a vicious scorpion?',
    active: true,
    playsCount: 112,
    rating: 4.6
  },

  // Social Deduction
  {
    id: 'g-avalon',
    name: 'The Resistance: Avalon',
    category: 'Social Deduction',
    difficulty: 'Medium',
    players: '5-10 Players',
    duration: '30-45 mins',
    description: 'Arthurian knights battle the sinister minions of Mordred. Who among you is loyal to the round table?',
    active: true,
    playsCount: 190,
    rating: 4.9
  },
  {
    id: 'g-secrethitler',
    name: 'Secret Hitler',
    category: 'Social Deduction',
    difficulty: 'Medium',
    players: '5-10 Players',
    duration: '45 mins',
    description: 'Dramatic political deduction game of trust, betrayal, and election of chancellors in 1930s Berlin.',
    active: true,
    playsCount: 165,
    rating: 4.8
  },
  {
    id: 'g-bloodontheclocktower',
    name: 'Blood on the Clocktower',
    category: 'Social Deduction',
    difficulty: 'Advanced',
    players: '7-20 Players',
    duration: '60-90 mins',
    description: 'A masterpiece of murder mystery and storytelling where even dead players continue to participate and vote.',
    active: true,
    playsCount: 45,
    rating: 5.0
  },
  {
    id: 'g-deception',
    name: 'Deception: Murder in Hong Kong',
    category: 'Social Deduction',
    difficulty: 'Medium',
    players: '4-12 Players',
    duration: '20-30 mins',
    description: 'Forensic scientist provides silent crime scene tiles while detectives deduce which player is the murderer.',
    active: true,
    playsCount: 92,
    rating: 4.8
  },

  // Card Games
  {
    id: 'g-flip7',
    name: 'Flip 7',
    category: 'Card Games',
    difficulty: 'Beginner',
    players: '2-8 Players',
    duration: '15 mins',
    description: 'The addictive press-your-luck card craze. Collect numbered cards without busting on duplicates!',
    active: true,
    playsCount: 210,
    rating: 4.9
  },
  {
    id: 'g-exploding',
    name: 'Exploding Kittens',
    category: 'Card Games',
    difficulty: 'Beginner',
    players: '2-5 Players',
    duration: '15 mins',
    description: 'Russian roulette card game with laser pointers, catnip sandwiches, and defuse kits.',
    active: true,
    playsCount: 155,
    rating: 4.6
  },
  {
    id: 'g-sushigo',
    name: 'Sushi Go Party!',
    category: 'Card Games',
    difficulty: 'Beginner',
    players: '2-8 Players',
    duration: '20-30 mins',
    description: 'Fast-paced conveyor belt drafting game collecting sashimi, wasabi dipped nigiri, and maki rolls.',
    active: true,
    playsCount: 140,
    rating: 4.7
  },
  {
    id: 'g-scout',
    name: 'Scout',
    category: 'Card Games',
    difficulty: 'Medium',
    players: '3-5 Players',
    duration: '20 mins',
    description: 'Circus-themed Japanese ladder-climbing sensation where your hand order cannot be rearranged.',
    active: true,
    playsCount: 82,
    rating: 4.9
  },
  {
    id: 'g-seasalt',
    name: 'Sea Salt & Paper',
    category: 'Card Games',
    difficulty: 'Beginner',
    players: '2-4 Players',
    duration: '30 mins',
    description: 'Origami art card game with clever set collection and thrilling end-round bets.',
    active: true,
    playsCount: 75,
    rating: 4.8
  },
  {
    id: 'g-thecrew',
    name: 'The Crew: Mission Deep Sea',
    category: 'Card Games',
    difficulty: 'Medium',
    players: '2-5 Players',
    duration: '20 mins',
    description: 'Cooperative trick-taking deep-sea voyage solving increasingly brain-bending oceanic missions.',
    active: true,
    playsCount: 64,
    rating: 4.8
  },

  // Family
  {
    id: 'g-azul',
    name: 'Azul',
    category: 'Family',
    difficulty: 'Beginner',
    players: '2-4 Players',
    duration: '30-45 mins',
    description: 'Draft gorgeous Moorish ceramic tiles to embellish the royal palace walls of Evora.',
    active: true,
    playsCount: 148,
    rating: 4.9
  },
  {
    id: 'g-carcassonne',
    name: 'Carcassonne',
    category: 'Family',
    difficulty: 'Beginner',
    players: '2-5 Players',
    duration: '35-45 mins',
    description: 'Classic tile placement forming French countryside landscapes, walled medieval cities, and monasteries.',
    active: true,
    playsCount: 135,
    rating: 4.7
  },
  {
    id: 'g-cascadia',
    name: 'Cascadia',
    category: 'Family',
    difficulty: 'Beginner',
    players: '1-4 Players',
    duration: '30-45 mins',
    description: 'Harmonious spatial puzzle building Pacific Northwest ecosystems with grizzly bears, elk, and salmon.',
    active: true,
    playsCount: 89,
    rating: 4.9
  },
  {
    id: 'g-camelup',
    name: 'Camel Up',
    category: 'Family',
    difficulty: 'Beginner',
    players: '3-8 Players',
    duration: '30-45 mins',
    description: 'Wild and chaotic camel race around the pyramid with camels stacking atop one another!',
    active: true,
    playsCount: 98,
    rating: 4.7
  },

  // Team Games
  {
    id: 'g-captainsonar',
    name: 'Captain Sonar',
    category: 'Team Games',
    difficulty: 'Advanced',
    players: '6-8 Players',
    duration: '45 mins',
    description: 'Real-time submarine duel with Captain, First Mate, Radio Operator, and Engineer working simultaneously.',
    active: true,
    playsCount: 38,
    rating: 4.9
  },
  {
    id: 'g-soclover',
    name: 'So Clover!',
    category: 'Team Games',
    difficulty: 'Beginner',
    players: '3-6 Players',
    duration: '30 mins',
    description: 'Clever cooperative word association puzzle using clover boards and shared brainstorms.',
    active: true,
    playsCount: 62,
    rating: 4.7
  },

  // Long Format
  {
    id: 'g-root',
    name: 'Root',
    category: 'Long Format',
    difficulty: 'Advanced',
    players: '2-4 Players',
    duration: '90-120 mins',
    description: 'Asymmetric forest kingdom warfare featuring Marquise de Cat, Eyrie Dynasties, and Woodland Alliance.',
    active: true,
    playsCount: 31,
    rating: 4.8
  },
  {
    id: 'g-arknova',
    name: 'Ark Nova',
    category: 'Long Format',
    difficulty: 'Advanced',
    players: '1-4 Players',
    duration: '120-150 mins',
    description: 'Plan and build a modern, scientifically managed zoological garden and conservation project.',
    active: true,
    playsCount: 27,
    rating: 4.9
  },

  // Tournament
  {
    id: 'g-klask',
    name: 'Klask',
    category: 'Tournament',
    difficulty: 'Beginner',
    players: '2 Players',
    duration: '10 mins',
    description: 'Magnetic tabletop air-hockey skill game with obstacle magnets and quick competitive rounds.',
    active: true,
    playsCount: 115,
    rating: 4.8
  },
  {
    id: 'g-crokinole',
    name: 'Crokinole',
    category: 'Tournament',
    difficulty: 'Beginner',
    players: '2-4 Players',
    duration: '15-20 mins',
    description: 'Traditional Canadian wooden flicking board game requiring precision, strategy, and finger dexterity.',
    active: true,
    playsCount: 89,
    rating: 5.0
  }
];

export const INITIAL_EVENTS: ShuffleEvent[] = [
  {
    id: 'ev-35',
    number: 35,
    title: 'Social Shuffle #35 — Weekend Board Game Bash',
    date: '2026-09-27',
    time: 'Sunday, 4:00 PM – 7:30 PM',
    venue: 'The Daily All Day',
    address: 'Lane 7, Koregaon Park, Pune',
    area: 'Koregaon Park',
    category: 'Regular Community Meetup',
    capacity: 65,
    ticketPrice: 350,
    description: 'Our 35th milestone edition! Join 60+ shufflers for Sunday afternoon board games, drinks, great bites, and curated game tables with dedicated hosts.',
    notes: 'Free welcome drink included. Host explanations at 4:15 PM and 5:30 PM. Beginners warmly welcomed!',
    plannedGames: ['g-codenames', 'g-catan', 'g-coup', 'g-flip7', 'g-avalon', 'g-justone', 'g-azul', 'g-klask'],
    status: 'upcoming'
  },
  {
    id: 'ev-36',
    number: 36,
    title: 'Social Shuffle #36 — Strategy & Social Deduction Night',
    date: '2026-10-04',
    time: 'Sunday, 5:00 PM – 9:00 PM',
    venue: 'FC Road Social',
    address: 'FC Road, Near Fergusson College, Pune',
    area: 'FC Road',
    category: 'Themed Game Night',
    capacity: 75,
    ticketPrice: 400,
    description: 'Deep deception and cunning strategy! Featuring full tables for Secret Hitler, Blood on the Clocktower, Dune Imperium, and Flip 7 tournament.',
    notes: 'Please arrive by 4:45 PM for seat assignment. High-energy event!',
    plannedGames: ['g-secrethitler', 'g-bloodontheclocktower', 'g-dune', 'g-flip7', 'g-scout', 'g-splendor'],
    status: 'upcoming'
  },
  {
    id: 'ev-34',
    number: 34,
    title: 'Social Shuffle #34 — Craft Brews & Meeples',
    date: '2026-09-20',
    time: 'Sunday, 4:00 PM – 8:00 PM',
    venue: 'Toit Brewpub',
    address: 'Kalyani Nagar, Near Joggers Park, Pune',
    area: 'Kalyani Nagar',
    category: 'Regular Community Meetup',
    capacity: 70,
    ticketPrice: 350,
    description: 'Packed house in Kalyani Nagar! Over 62 shufflers battled over Catan, Coup, Ticket to Ride, and Just One.',
    notes: 'Huge turnout; congratulations to the Klask mini-tournament winners!',
    plannedGames: ['g-catan', 'g-ticket', 'g-coup', 'g-justone', 'g-splendor', 'g-klask'],
    status: 'completed'
  },
  {
    id: 'ev-33',
    number: 33,
    title: 'Social Shuffle #33 — West Pune Cozy Cafe Edition',
    date: '2026-09-13',
    time: 'Sunday, 3:30 PM – 7:00 PM',
    venue: 'Pagdandi Books Chai Cafe',
    address: 'Baner-Pashan Link Road, Baner, Pune',
    area: 'Baner',
    category: 'Chill & Cozy Meetup',
    capacity: 45,
    ticketPrice: 300,
    description: 'Cozy vibes with aromatic chai, indie books, and casual card and family board games.',
    notes: 'Very popular with Baner/Aundh crowd. Sold out in 48 hours.',
    plannedGames: ['g-azul', 'g-sushigo', 'g-dixit', 'g-flip7', 'g-seasalt'],
    status: 'completed'
  }
];

export const INITIAL_PARTICIPANTS: Participant[] = [
  {
    id: 'p-priya',
    name: 'Priya Nair',
    email: 'priya.nair@example.com',
    phone: '+91 97632 99011',
    area: 'Wakad',
    avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=160&q=80',
    bio: 'Weekly regular shuffler. Always looking for teammates for Just One & Wingspan!',
    joinedDate: '2024-01-15',
    totalEventsAttended: 6,
    totalRegistrations: 7,
    gamesPlayedIds: ['g-codenames', 'g-justone', 'g-azul', 'g-wingspan', 'g-dixit', 'g-cascadia'],
    venuesVisited: ['Toit Brewery', 'FC Road Social', 'Waari Book Cafe'],
    totalPaxBrought: 3,
    lastAttendedEventId: 'ev-ss3',
    lastAttendedEventTitle: 'Secret Social & Mystery Games'
  },
  {
    id: 'p-rohan',
    name: 'Rohan Deshmukh',
    email: 'rohan.deshmukh@example.com',
    phone: '+91 98223 44551',
    area: 'Koregaon Park',
    avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=160&q=80',
    bio: 'Heavy strategy enthusiast. Terraforming Mars, Dune Imperium & Scythe fan.',
    joinedDate: '2024-01-05',
    totalEventsAttended: 11,
    totalRegistrations: 12,
    gamesPlayedIds: ['g-dune', 'g-terraformingmars', 'g-scythe', 'g-brass', 'g-root'],
    venuesVisited: ['Toit Brewery', 'FC Road Social', 'Effingut KP', 'Waari Book Cafe'],
    totalPaxBrought: 8,
    lastAttendedEventId: 'ev-ss3',
    lastAttendedEventTitle: 'Secret Social & Mystery Games'
  },
  {
    id: 'p-ananya',
    name: 'Ananya Kulkarni',
    email: 'ananya.k@example.com',
    phone: '+91 98901 22334',
    area: 'Baner',
    avatar: 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?auto=format&fit=crop&w=160&q=80',
    bio: 'Designer by day, social deduction master by night. Secret Hitler champ.',
    joinedDate: '2024-01-20',
    totalEventsAttended: 8,
    totalRegistrations: 8,
    gamesPlayedIds: ['g-secrethitler', 'g-avalon', 'g-bloodontheclocktower', 'g-decrypto', 'g-wavelength'],
    venuesVisited: ['Toit Brewery', 'Waari Book Cafe'],
    totalPaxBrought: 5,
    lastAttendedEventId: 'ev-ss3',
    lastAttendedEventTitle: 'Secret Social & Mystery Games'
  },
  {
    id: 'p-tanmay',
    name: 'Tanmay Joshi',
    email: 'tanmay.j@example.com',
    phone: '+91 98221 88990',
    area: 'Viman Nagar',
    avatar: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&w=160&q=80',
    bio: 'Love learning new modern European games and meeting new people over coffee.',
    joinedDate: '2024-02-01',
    totalEventsAttended: 5,
    totalRegistrations: 6,
    gamesPlayedIds: ['g-catan', 'g-ticketoride', 'g-carcassonne', 'g-splendor', 'g-cascadia'],
    venuesVisited: ['FC Road Social', 'Toit Brewery'],
    totalPaxBrought: 2,
    lastAttendedEventId: 'ev-ss2',
    lastAttendedEventTitle: 'Strategy & Heavyweight Battles'
  },
  {
    id: 'p-sneha',
    name: 'Sneha Patil',
    email: 'sneha.patil@example.com',
    phone: '+91 97665 11223',
    area: 'Aundh',
    avatar: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=160&q=80',
    bio: 'Board game collector and casual teacher. Let us play Cascadia and Sushi Go!',
    joinedDate: '2024-02-14',
    totalEventsAttended: 4,
    totalRegistrations: 4,
    gamesPlayedIds: ['g-cascadia', 'g-sushigo', 'g-azul', 'g-flip7', 'g-dixit'],
    venuesVisited: ['Waari Book Cafe', 'FC Road Social'],
    totalPaxBrought: 1,
    lastAttendedEventId: 'ev-ss3',
    lastAttendedEventTitle: 'Secret Social & Mystery Games'
  },
  {
    id: 'p-aditya',
    name: 'Aditya Shinde',
    email: 'aditya.shinde@example.com',
    phone: '+91 98220 33445',
    area: 'Kothrud',
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=160&q=80',
    bio: 'Tournament competitor. Competitive Spirit Island and Blood on the Clocktower storyteller.',
    joinedDate: '2024-01-10',
    totalEventsAttended: 9,
    totalRegistrations: 10,
    gamesPlayedIds: ['g-spiritisland', 'g-bloodontheclocktower', 'g-dune', 'g-root', 'g-clank'],
    venuesVisited: ['Toit Brewery', 'FC Road Social', 'Effingut KP'],
    totalPaxBrought: 6,
    lastAttendedEventId: 'ev-ss3',
    lastAttendedEventTitle: 'Secret Social & Mystery Games'
  }
];

export const INITIAL_REGISTRATIONS: Registration[] = [
  {
    id: 'reg-ss4-priya',
    eventId: 'ev-ss4',
    participantId: 'p-priya',
    participantName: 'Priya Nair',
    participantEmail: 'priya.nair@example.com',
    participantPhone: '+91 97632 99011',
    participantArea: 'Wakad',
    paxCount: 2,
    guests: [{ id: 'g-1', name: 'Aarav Nair', phone: '+91 97632 99012' }],
    registeredAt: '2024-11-01T10:30:00Z',
    attendanceStatus: 'Pending',
    paymentStatus: 'Confirmed',
    gamesPlayed: [],
    notes: 'Please assign to beginner/party game table'
  },
  {
    id: 'reg-ss4-rohan',
    eventId: 'ev-ss4',
    participantId: 'p-rohan',
    participantName: 'Rohan Deshmukh',
    participantEmail: 'rohan.deshmukh@example.com',
    participantPhone: '+91 98223 44551',
    participantArea: 'Koregaon Park',
    paxCount: 1,
    guests: [],
    registeredAt: '2024-11-01T11:15:00Z',
    attendanceStatus: 'Pending',
    paymentStatus: 'Confirmed',
    gamesPlayed: [],
    notes: 'Excited for Terraforming Mars session'
  },
  {
    id: 'reg-ss4-ananya',
    eventId: 'ev-ss4',
    participantId: 'p-ananya',
    participantName: 'Ananya Kulkarni',
    participantEmail: 'ananya.k@example.com',
    participantPhone: '+91 98901 22334',
    participantArea: 'Baner',
    paxCount: 1,
    guests: [],
    registeredAt: '2024-11-01T12:00:00Z',
    attendanceStatus: 'Pending',
    paymentStatus: 'Confirmed',
    gamesPlayed: []
  },
  {
    id: 'reg-ss4-aditya',
    eventId: 'ev-ss4',
    participantId: 'p-aditya',
    participantName: 'Aditya Shinde',
    participantEmail: 'aditya.shinde@example.com',
    participantPhone: '+91 98220 33445',
    participantArea: 'Kothrud',
    paxCount: 2,
    guests: [{ id: 'g-2', name: 'Kavita Shinde' }],
    registeredAt: '2024-11-01T14:20:00Z',
    attendanceStatus: 'Pending',
    paymentStatus: 'Confirmed',
    gamesPlayed: []
  },
  {
    id: 'reg-ss3-priya',
    eventId: 'ev-ss3',
    participantId: 'p-priya',
    participantName: 'Priya Nair',
    participantEmail: 'priya.nair@example.com',
    participantPhone: '+91 97632 99011',
    participantArea: 'Wakad',
    paxCount: 1,
    guests: [],
    registeredAt: '2024-10-15T09:00:00Z',
    attendanceStatus: 'Checked In',
    paymentStatus: 'Confirmed',
    checkInTime: '2024-10-20T16:15:00Z',
    gamesPlayed: ['g-codenames', 'g-justone']
  },
  {
    id: 'reg-ss3-rohan',
    eventId: 'ev-ss3',
    participantId: 'p-rohan',
    participantName: 'Rohan Deshmukh',
    participantEmail: 'rohan.deshmukh@example.com',
    participantPhone: '+91 98223 44551',
    participantArea: 'Koregaon Park',
    paxCount: 2,
    guests: [{ id: 'g-3', name: 'Manish Verma' }],
    registeredAt: '2024-10-15T10:00:00Z',
    attendanceStatus: 'Checked In',
    paymentStatus: 'Confirmed',
    checkInTime: '2024-10-20T16:10:00Z',
    gamesPlayed: ['g-bloodontheclocktower', 'g-secrethitler']
  }
];

export const INITIAL_FEEDBACK: EventFeedback[] = [
  {
    id: 'fb-1',
    eventId: 'ev-ss3',
    eventTitle: 'Secret Social & Mystery Games',
    participantName: 'Priya Nair',
    isAnonymous: false,
    overallRating: 5,
    venueRating: 5,
    gameRating: 5,
    hostRating: 5,
    suggestions: 'Super well organized by Aman and Parimal. The explanations were crisp and people were so friendly!',
    createdAt: '2024-10-21T10:00:00Z'
  },
  {
    id: 'fb-2',
    eventId: 'ev-ss3',
    eventTitle: 'Secret Social & Mystery Games',
    participantName: 'Rohan Deshmukh',
    isAnonymous: false,
    overallRating: 5,
    venueRating: 4,
    gameRating: 5,
    hostRating: 5,
    suggestions: 'Best board gaming community in Pune hands down. Great craft beers at Toit too.',
    createdAt: '2024-10-21T12:30:00Z'
  }
];

export const INITIAL_REPORTS: SafetyReport[] = [
  {
    id: 'rep-1',
    eventId: 'ev-ss2',
    eventTitle: 'Strategy & Heavyweight Battles',
    reporterName: 'Ananya Kulkarni',
    type: 'Venue issue',
    description: 'Minor tea spill near Table 4. Cloth clean-up was immediate and sleeves saved the cards.',
    isAnonymous: false,
    status: 'Resolved',
    resolutionNotes: 'Cleaned and double-sleeved Terraforming Mars deck.',
    createdAt: '2024-09-18T18:00:00Z'
  }
];

export const INITIAL_VOLUNTEERS: VolunteerApplication[] = [
  {
    id: 'vol-1',
    name: 'Sneha Patil',
    email: 'sneha.patil@example.com',
    phone: '+91 97665 11223',
    area: 'Aundh',
    reason: 'Love fostering board game communities in Pune.',
    experience: 'Played over 80+ tabletop games. Happy to teach Azul, Cascadia, and Wingspan.',
    skills: 'Quick game teacher, welcoming host',
    availability: 'Weekend evenings',
    preferredResponsibilities: ['Game Explainer / Rules Teacher', 'Welcome Desk'],
    status: 'Accepted',
    submittedAt: '2024-10-01T14:00:00Z'
  }
];

export const INITIAL_AUDIT_LOGS: AuditLog[] = [
  {
    id: 'log-1',
    timestamp: '2024-11-01T09:00:00Z',
    adminName: 'Parimal Shete',
    action: 'Created Event',
    target: 'Social Shuffle #4 - The Grand Tabletop Gala',
    details: 'Published event with 65 capacity and Rs. 400 ticket price.'
  },
  {
    id: 'log-2',
    timestamp: '2024-11-01T09:30:00Z',
    adminName: 'Parimal Shete',
    action: 'Added Game to Catalog',
    target: 'Harmonies',
    details: 'Added Libellud 2024 release Harmonies with Medium difficulty.'
  }
];

export const INITIAL_NOTIFICATIONS: NotificationItem[] = [
  {
    id: 'notif-1',
    title: 'Social Shuffle #4 Meetup This Weekend 🎲',
    message: 'Registrations are filling fast at FC Road Social. 35+ spots confirmed!',
    type: 'event',
    timestamp: '2 hours ago',
    read: false
  },
  {
    id: 'notif-2',
    title: 'New Game Added: Harmonies',
    message: 'Libellud gorgeous pattern builder Harmonies is now available in the Pune game library.',
    type: 'system',
    timestamp: 'Yesterday',
    read: true
  }
];

export const INITIAL_ADMINS: AdminRecord[] = [
  {
    id: 'u-admin-parimal',
    name: 'Parimal Shete',
    email: 'parimalmshete@gmail.com',
    phone: '+91 98220 99887',
    role: 'admin',
    designation: 'Co-Founder & Community Architect',
    avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=200',
    bio: 'Social Shuffle Co-Founder & Community Architect',
    area: 'Koregaon Park, Pune',
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
    area: 'Baner, Pune',
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
    area: 'Kalyani Nagar, Pune',
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
    area: 'Viman Nagar, Pune',
    active: true,
    createdAt: '2024-01-01T00:00:00Z'
  }
];

export const INITIAL_COMMUNITY_STATS: CommunityStats = {
  id: 'community-stats-main',
  membersCount: '2,400+',
  membersCountNumeric: 2400,
  gamesTaught: '120+',
  gamesTaughtNumeric: 120,
  soloPercentage: '60%',
  soloPercentageNumeric: 60,
  eventsHosted: '34',
  eventsHostedLabel: 'Sold-out Editions',
  eventsHostedNumeric: 34,
  neighborhoods: ['Koregaon Park', 'Baner', 'Kalyani Nagar'],
  heroHeadline: 'Unplug, Roll Dice & Make Real Friends in Pune',
  heroSchedule: 'Every Sunday 3 PM – 8 PM',
  heroDescription: 'Social Shuffle brings together over 2,400+ board gamers across Koregaon Park, Baner, and Kalyani Nagar. Never played modern tabletop games before? Friendly community hosts explain every rule step-by-step!',
  communityTagline: 'Connect with 2,400+ friendly Pune shufflers. Get instant table RSVP drops, spot alerts, rule walk-throughs, and weekend cafe photos.',
  whyJoinText: 'Registering as a participant is 100% free and takes less than 30 seconds. Over 2,400+ Pune shufflers are waiting to welcome you to the table!',
  curatedMatchmakingText: 'Curated matchmaking (60% come solo & make friends)',
  metricCards: [
    {
      id: 'members',
      value: '2,400+',
      label: 'Pune Shufflers',
      color: '#FFFFFF',
      subtext: 'Active community'
    },
    {
      id: 'games',
      value: '120+',
      label: 'Games Taught',
      color: '#4AB4A4',
      subtext: 'In our library'
    },
    {
      id: 'solo',
      value: '60%',
      label: 'Come Solo',
      color: '#FFD230',
      subtext: 'Instant table seating'
    },
    {
      id: 'events',
      value: '34',
      label: 'Sold-out Editions',
      color: '#FF751F',
      subtext: 'Hosted meetups'
    }
  ],
  updatedAt: '2026-11-04T12:00:00.000Z',
  updatedBy: 'Parimal Shete (Co-Founder)'
};


