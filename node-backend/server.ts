import express from 'express';
import { BADGE_LEVELS, calculateBadgeLevel } from './badgeBackendService';

const app = express();
const PORT = Number(process.env.PORT) || 8080;

app.use(express.json());

// CORS
app.use((req, res, next) => {
  res.header('Access-Control-Allow-Origin', '*');
  res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept, Authorization');
  res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, PATCH, OPTIONS');
  if (req.method === 'OPTIONS') {
    return res.sendStatus(200);
  }
  next();
});

// Backend Badge API: 10 Badge Levels
app.get('/api/badges/levels', (_req, res) => {
  res.json({
    success: true,
    totalLevels: BADGE_LEVELS.length,
    levels: BADGE_LEVELS
  });
});

// Backend Badge API: Calculate 10-level badge from games played & events attended
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

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Social Shuffle Badge Backend Service listening on port ${PORT}`);
});
