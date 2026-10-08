/**
 * Badge Backend Service & 10-Level Calculation Engine
 * 
 * Provides server-authoritative 10-Level badge hierarchy based on:
 * 1. Total games played in our events & catalog exploration percentage (e.g. 90%+ = Level 10)
 * 2. Total meetups / events attended
 */

export interface BadgeLevelDefinition {
  level: number;
  fancyName: string;
  shortTitle: string;
  subtitle: string;
  description: string;
  icon: string;
  minGamePercentage: number; // e.g. 90 for Level 10
  minEventsAttended: number;
  rarity: 'Common' | 'Uncommon' | 'Rare' | 'Epic' | 'Legendary' | 'Mythic';
  gradient: string;
  borderColor: string;
  textColor: string;
  badgeTagColor: string;
  glowEffect: string;
  perks: string[];
}

export const BADGE_LEVELS: BadgeLevelDefinition[] = [
  {
    level: 1,
    shortTitle: 'Novice Roller',
    fancyName: 'Initiate of the Ivory Dice',
    subtitle: 'The First Move',
    description: 'Broke the ice, rolled the initial dice, and attended their first Pune tabletop gathering.',
    icon: '🎲',
    minGamePercentage: 4, // 1 game out of 24
    minEventsAttended: 1,
    rarity: 'Common',
    gradient: 'from-slate-800 via-purple-950/40 to-slate-900',
    borderColor: 'border-slate-600/50',
    textColor: 'text-slate-300',
    badgeTagColor: 'bg-slate-700/50 text-slate-300 border-slate-600',
    glowEffect: 'shadow-slate-500/10',
    perks: ['Unlock Digital Shuffler Passport', 'Table Rookie Profile Badge']
  },
  {
    level: 2,
    shortTitle: 'Apprentice Shuffler',
    fancyName: 'Acolyte of the Meeple Order',
    subtitle: 'Table Apprentice',
    description: 'Expanded beyond the basics with multiple games tested and a verified event check-in.',
    icon: '♟️',
    minGamePercentage: 10, // ~2-3 games
    minEventsAttended: 1,
    rarity: 'Common',
    gradient: 'from-amber-950/30 via-slate-900 to-purple-950/30',
    borderColor: 'border-amber-700/50',
    textColor: 'text-amber-300',
    badgeTagColor: 'bg-amber-900/30 text-amber-300 border-amber-700/50',
    glowEffect: 'shadow-amber-700/15',
    perks: ['Game Table Tagging Rights', 'Access to Sunday Game Discussions']
  },
  {
    level: 3,
    shortTitle: 'Casual Tactician',
    fancyName: 'Bronze Vanguard of Koregaon',
    subtitle: 'Cafe Contender',
    description: 'Frequents Pune board game cafes and demonstrates reliable tabletop instincts.',
    icon: '⚔️',
    minGamePercentage: 20, // ~5 games
    minEventsAttended: 2,
    rarity: 'Uncommon',
    gradient: 'from-amber-800/30 via-slate-900 to-amber-950/40',
    borderColor: 'border-amber-600/60',
    textColor: 'text-amber-200',
    badgeTagColor: 'bg-amber-600/20 text-amber-200 border-amber-500/40',
    glowEffect: 'shadow-amber-500/20',
    perks: ['Early Seat Reservations', 'Bronze Shuffler Tier Status']
  },
  {
    level: 4,
    shortTitle: 'Board Game Enthusiast',
    fancyName: 'Master of the Card & Tile',
    subtitle: 'Strategy Adept',
    description: 'Explores nearly a third of the catalog with a strong grasp of tile placement and card play.',
    icon: '🏰',
    minGamePercentage: 30, // ~7-8 games
    minEventsAttended: 2,
    rarity: 'Uncommon',
    gradient: 'from-blue-950/40 via-slate-900 to-indigo-950/40',
    borderColor: 'border-blue-500/60',
    textColor: 'text-blue-300',
    badgeTagColor: 'bg-blue-500/20 text-blue-300 border-blue-400/40',
    glowEffect: 'shadow-blue-500/25',
    perks: ['Custom Table Name Tag', 'Host Co-Player Recommendation']
  },
  {
    level: 5,
    shortTitle: 'Tabletop Strategist',
    fancyName: 'Silver Viceroy of Baner Tables',
    subtitle: 'Tabletop Champion',
    description: 'Comfortably maneuvers euro-style resource management, engine building, and deduction games.',
    icon: '🛡️',
    minGamePercentage: 40, // ~10 games
    minEventsAttended: 3,
    rarity: 'Rare',
    gradient: 'from-slate-700/40 via-indigo-950/50 to-slate-900',
    borderColor: 'border-slate-300/60',
    textColor: 'text-slate-100',
    badgeTagColor: 'bg-slate-200/20 text-slate-100 border-slate-300/50',
    glowEffect: 'shadow-slate-300/25',
    perks: ['Silver Shuffler Tier Status', 'Tournament Seeding Priority']
  },
  {
    level: 6,
    shortTitle: 'Seasoned Shuffler',
    fancyName: 'Commander of a Thousand Battles',
    subtitle: 'Halfway to Catalog Mastery (50%+)',
    description: 'Conquered half the official game library and a loyal regular at weekend gatherings.',
    icon: '🌟',
    minGamePercentage: 50, // 12+ games
    minEventsAttended: 4,
    rarity: 'Rare',
    gradient: 'from-cyan-950/50 via-teal-950/40 to-slate-900',
    borderColor: 'border-cyan-400/70',
    textColor: 'text-cyan-200',
    badgeTagColor: 'bg-cyan-500/20 text-cyan-200 border-cyan-400/50',
    glowEffect: 'shadow-cyan-400/30',
    perks: ['New Game Library Voting Voice', 'Exclusive Shuffler Community Role']
  },
  {
    level: 7,
    shortTitle: 'Master of Meeples',
    fancyName: 'Gold Sovereign of Strategy',
    subtitle: 'High Tactician (60%+)',
    description: 'Commanding table presence with extensive knowledge across strategic and party classics.',
    icon: '👑',
    minGamePercentage: 60, // 14+ games
    minEventsAttended: 5,
    rarity: 'Epic',
    gradient: 'from-amber-600/30 via-yellow-600/20 to-purple-950/50',
    borderColor: 'border-amber-400/80',
    textColor: 'text-amber-200',
    badgeTagColor: 'bg-amber-400/20 text-amber-200 border-amber-400/50',
    glowEffect: 'shadow-amber-400/35',
    perks: ['Gold Strategist Tier Status', 'Complimentary Table Host Drink']
  },
  {
    level: 8,
    shortTitle: 'Grandmaster Tactician',
    fancyName: 'Platinum Grand Marshal',
    subtitle: 'Tabletop Virtuoso (70%+)',
    description: 'Demonstrates surgical precision in deep euro-games, complex negotiations, and heavy card systems.',
    icon: '💎',
    minGamePercentage: 70, // 17+ games
    minEventsAttended: 6,
    rarity: 'Epic',
    gradient: 'from-indigo-600/30 via-purple-600/20 to-sky-950/50',
    borderColor: 'border-indigo-400/80',
    textColor: 'text-indigo-200',
    badgeTagColor: 'bg-indigo-400/20 text-indigo-200 border-indigo-400/50',
    glowEffect: 'shadow-indigo-400/40',
    perks: ['VIP Game Day Lounge Pass', 'Rule Explainer Recognition']
  },
  {
    level: 9,
    shortTitle: 'Tabletop Sovereign',
    fancyName: 'Diamond Regent of Tabletop Realms',
    subtitle: 'Pune Board Game Legend (80%+)',
    description: 'A legendary pillar of Pune gaming who has conquered 80%+ of all curated board games.',
    icon: '🌌',
    minGamePercentage: 80, // 19+ games
    minEventsAttended: 7,
    rarity: 'Legendary',
    gradient: 'from-purple-600/40 via-pink-600/30 to-amber-900/40',
    borderColor: 'border-purple-300',
    textColor: 'text-purple-100',
    badgeTagColor: 'bg-purple-500/30 text-purple-100 border-purple-300/60',
    glowEffect: 'shadow-purple-400/50',
    perks: ['Diamond Grandmaster Tier Status', 'Permanent Hall of Fame Listing', 'Private Table Reservation']
  },
  {
    level: 10,
    shortTitle: 'Apex Shuffler',
    fancyName: 'Mythic Tabletop Archon',
    subtitle: '90%+ Catalog Mastery & Elite Veteran',
    description: 'Supreme mastery of 90%+ games in Pune meetups. An immortal legend of Social Shuffle.',
    icon: '⚡',
    minGamePercentage: 90, // 21+ games
    minEventsAttended: 8,
    rarity: 'Mythic',
    gradient: 'from-amber-400/40 via-purple-600/40 to-cyan-400/40',
    borderColor: 'border-amber-300',
    textColor: 'text-amber-100',
    badgeTagColor: 'bg-gradient-to-r from-amber-400/30 to-purple-400/30 text-amber-200 border-amber-300/70',
    glowEffect: 'shadow-amber-400/60',
    perks: [
      'Mythic Golden Avatar Crown',
      'Free VIP Entry to Annual Championship',
      'Name Etched onto Social Shuffle Trophy',
      'Lifetime Pune Shuffler Pioneer Status'
    ]
  }
];

export interface BadgeCalculationResult {
  currentLevel: number;
  currentBadge: BadgeLevelDefinition;
  nextBadge?: BadgeLevelDefinition;
  gamesPlayedCount: number;
  totalGamesCount: number;
  gamePercentage: number;
  eventsAttendedCount: number;
  overallScore: number;
  levelProgress: {
    percentageToNextLevel: number;
    gamesNeededForNextLevel: number;
    eventsNeededForNextLevel: number;
  };
  levels: Array<{
    level: number;
    definition: BadgeLevelDefinition;
    isUnlocked: boolean;
    isCurrent: boolean;
    progressPercentage: number;
  }>;
}

/**
 * Calculates a participant's official 10-level badge status based on
 * total games played in our events and number of events attended.
 */
export function calculateBadgeLevel(params: {
  gamesPlayedCount: number;
  totalGamesCount?: number;
  eventsAttendedCount: number;
}): BadgeCalculationResult {
  const { gamesPlayedCount, totalGamesCount = 24, eventsAttendedCount } = params;

  const validTotal = Math.max(1, totalGamesCount);
  const gamePercentage = Math.min(100, Math.round((gamesPlayedCount / validTotal) * 100));

  // Determine current level (checks from Level 10 down to 1)
  let earnedLevel = 0;

  for (let i = BADGE_LEVELS.length - 1; i >= 0; i--) {
    const def = BADGE_LEVELS[i];
    // Special rule as requested: 90%+ games played awards Level 10!
    if (def.level === 10 && gamePercentage >= 90) {
      earnedLevel = 10;
      break;
    }

    const satisfiesGames = gamePercentage >= def.minGamePercentage;
    const satisfiesEvents = eventsAttendedCount >= def.minEventsAttended;

    // A participant qualifies if they meet game percentage AND events,
    // OR if their game percentage is substantially ahead (at least 2 levels higher)
    if (satisfiesGames && satisfiesEvents) {
      earnedLevel = def.level;
      break;
    } else if (gamePercentage >= def.minGamePercentage && eventsAttendedCount >= Math.max(1, def.minEventsAttended - 1)) {
      earnedLevel = def.level;
      break;
    }
  }

  // Fallback: If at least 1 game played or 1 event attended, minimum Level 1
  if (earnedLevel === 0 && (gamesPlayedCount >= 1 || eventsAttendedCount >= 1)) {
    earnedLevel = 1;
  }

  const currentLevelIndex = Math.max(0, earnedLevel - 1);
  const currentBadge = earnedLevel > 0 ? BADGE_LEVELS[currentLevelIndex] : BADGE_LEVELS[0];
  const nextBadge = earnedLevel < 10 ? BADGE_LEVELS[earnedLevel] : undefined;

  // Calculate progress toward next level
  let percentageToNextLevel = 100;
  let gamesNeededForNextLevel = 0;
  let eventsNeededForNextLevel = 0;

  if (nextBadge) {
    const targetGames = Math.ceil((nextBadge.minGamePercentage / 100) * validTotal);
    gamesNeededForNextLevel = Math.max(0, targetGames - gamesPlayedCount);
    eventsNeededForNextLevel = Math.max(0, nextBadge.minEventsAttended - eventsAttendedCount);

    const prevThreshold = currentBadge.minGamePercentage;
    const range = Math.max(1, nextBadge.minGamePercentage - prevThreshold);
    const progressInGame = Math.max(0, gamePercentage - prevThreshold);
    percentageToNextLevel = Math.min(99, Math.round((progressInGame / range) * 100));
  }

  // Build full level status list
  const levels = BADGE_LEVELS.map(def => {
    const targetGames = Math.ceil((def.minGamePercentage / 100) * validTotal);
    const gameProgress = Math.min(100, Math.round((gamesPlayedCount / (targetGames || 1)) * 100));
    const eventProgress = Math.min(100, Math.round((eventsAttendedCount / (def.minEventsAttended || 1)) * 100));
    const combinedProgress = Math.round((gameProgress * 0.7) + (eventProgress * 0.3));

    const isUnlocked = def.level <= earnedLevel;
    const isCurrent = def.level === earnedLevel;

    return {
      level: def.level,
      definition: def,
      isUnlocked,
      isCurrent,
      progressPercentage: isUnlocked ? 100 : Math.min(99, combinedProgress)
    };
  });

  const overallScore = Math.round((gamePercentage * 0.65) + (Math.min(100, eventsAttendedCount * 12.5) * 0.35));

  return {
    currentLevel: earnedLevel,
    currentBadge,
    nextBadge,
    gamesPlayedCount,
    totalGamesCount: validTotal,
    gamePercentage,
    eventsAttendedCount,
    overallScore,
    levelProgress: {
      percentageToNextLevel,
      gamesNeededForNextLevel,
      eventsNeededForNextLevel
    },
    levels
  };
}
