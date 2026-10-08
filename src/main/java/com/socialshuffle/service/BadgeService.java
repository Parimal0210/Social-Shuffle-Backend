package com.socialshuffle.service;

import com.socialshuffle.dto.BadgeCalculationRequest;
import com.socialshuffle.dto.BadgeCalculationResult;
import com.socialshuffle.dto.BadgeLevelDefinition;
import com.socialshuffle.model.Participant;
import com.socialshuffle.repository.GameRepository;
import com.socialshuffle.repository.ParticipantRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class BadgeService {

    private final ParticipantRepository participantRepository;
    private final GameRepository gameRepository;

    private static final List<BadgeLevelDefinition> BADGE_LEVELS = new ArrayList<>();

    static {
        BADGE_LEVELS.add(new BadgeLevelDefinition(
                1,
                "Novice Roller",
                "Initiate of the Ivory Dice",
                "The First Move",
                "Broke the ice, rolled the initial dice, and attended their first Pune tabletop gathering.",
                "🎲",
                4, // 1 game of 24
                1,
                "Common",
                "from-slate-800 via-purple-950/40 to-slate-900",
                "border-slate-600/50",
                "text-slate-300",
                "bg-slate-700/50 text-slate-300 border-slate-600",
                "shadow-slate-500/10",
                Arrays.asList("Unlock Digital Shuffler Passport", "Table Rookie Profile Badge")
        ));

        BADGE_LEVELS.add(new BadgeLevelDefinition(
                2,
                "Apprentice Shuffler",
                "Acolyte of the Meeple Order",
                "Table Apprentice",
                "Expanded beyond the basics with multiple games tested and a verified event check-in.",
                "♟️",
                10,
                1,
                "Common",
                "from-amber-950/30 via-slate-900 to-purple-950/30",
                "border-amber-700/50",
                "text-amber-300",
                "bg-amber-900/30 text-amber-300 border-amber-700/50",
                "shadow-amber-700/15",
                Arrays.asList("Game Table Tagging Rights", "Access to Sunday Game Discussions")
        ));

        BADGE_LEVELS.add(new BadgeLevelDefinition(
                3,
                "Casual Tactician",
                "Bronze Vanguard of Koregaon",
                "Cafe Contender",
                "Frequents Pune board game cafes and demonstrates reliable tabletop instincts.",
                "⚔️",
                20,
                2,
                "Uncommon",
                "from-amber-800/30 via-slate-900 to-amber-950/40",
                "border-amber-600/60",
                "text-amber-200",
                "bg-amber-600/20 text-amber-200 border-amber-500/40",
                "shadow-amber-500/20",
                Arrays.asList("Early Seat Reservations", "Bronze Shuffler Tier Status")
        ));

        BADGE_LEVELS.add(new BadgeLevelDefinition(
                4,
                "Board Game Enthusiast",
                "Master of the Card & Tile",
                "Strategy Adept",
                "Explores nearly a third of the catalog with a strong grasp of tile placement and card play.",
                "🏰",
                30,
                2,
                "Uncommon",
                "from-blue-950/40 via-slate-900 to-indigo-950/40",
                "border-blue-500/60",
                "text-blue-300",
                "bg-blue-500/20 text-blue-300 border-blue-400/40",
                "shadow-blue-500/25",
                Arrays.asList("Custom Table Name Tag", "Host Co-Player Recommendation")
        ));

        BADGE_LEVELS.add(new BadgeLevelDefinition(
                5,
                "Tabletop Strategist",
                "Silver Viceroy of Baner Tables",
                "Tabletop Champion",
                "Comfortably maneuvers euro-style resource management, engine building, and deduction games.",
                "🛡️",
                40,
                3,
                "Rare",
                "from-slate-700/40 via-indigo-950/50 to-slate-900",
                "border-slate-300/60",
                "text-slate-100",
                "bg-slate-200/20 text-slate-100 border-slate-300/50",
                "shadow-slate-300/25",
                Arrays.asList("Silver Shuffler Tier Status", "Tournament Seeding Priority")
        ));

        BADGE_LEVELS.add(new BadgeLevelDefinition(
                6,
                "Seasoned Shuffler",
                "Commander of a Thousand Battles",
                "Halfway to Catalog Mastery (50%+)",
                "Conquered half the official game library and a loyal regular at weekend gatherings.",
                "🌟",
                50,
                4,
                "Rare",
                "from-cyan-950/50 via-teal-950/40 to-slate-900",
                "border-cyan-400/70",
                "text-cyan-200",
                "bg-cyan-500/20 text-cyan-200 border-cyan-400/50",
                "shadow-cyan-400/30",
                Arrays.asList("New Game Library Voting Voice", "Exclusive Shuffler Community Role")
        ));

        BADGE_LEVELS.add(new BadgeLevelDefinition(
                7,
                "Master of Meeples",
                "Gold Sovereign of Strategy",
                "High Tactician (60%+)",
                "Commanding table presence with extensive knowledge across strategic and party classics.",
                "👑",
                60,
                5,
                "Epic",
                "from-amber-600/30 via-yellow-600/20 to-purple-950/50",
                "border-amber-400/80",
                "text-amber-200",
                "bg-amber-400/20 text-amber-200 border-amber-400/50",
                "shadow-amber-400/35",
                Arrays.asList("Gold Strategist Tier Status", "Complimentary Table Host Drink")
        ));

        BADGE_LEVELS.add(new BadgeLevelDefinition(
                8,
                "Grandmaster Tactician",
                "Platinum Grand Marshal",
                "Tabletop Virtuoso (70%+)",
                "Demonstrates surgical precision in deep euro-games, complex negotiations, and heavy card systems.",
                "💎",
                70,
                6,
                "Epic",
                "from-indigo-600/30 via-purple-600/20 to-sky-950/50",
                "border-indigo-400/80",
                "text-indigo-200",
                "bg-indigo-400/20 text-indigo-200 border-indigo-400/50",
                "shadow-indigo-400/40",
                Arrays.asList("VIP Game Day Lounge Pass", "Rule Explainer Recognition")
        ));

        BADGE_LEVELS.add(new BadgeLevelDefinition(
                9,
                "Tabletop Sovereign",
                "Diamond Regent of Tabletop Realms",
                "Pune Board Game Legend (80%+)",
                "A legendary pillar of Pune gaming who has conquered 80%+ of all curated board games.",
                "🌌",
                80,
                7,
                "Legendary",
                "from-purple-600/40 via-pink-600/30 to-amber-900/40",
                "border-purple-300",
                "text-purple-100",
                "bg-purple-500/30 text-purple-100 border-purple-300/60",
                "shadow-purple-400/50",
                Arrays.asList("Diamond Grandmaster Tier Status", "Permanent Hall of Fame Listing", "Private Table Reservation")
        ));

        BADGE_LEVELS.add(new BadgeLevelDefinition(
                10,
                "Apex Shuffler",
                "Mythic Tabletop Archon",
                "90%+ Catalog Mastery & Elite Veteran",
                "Supreme mastery of 90%+ games in Pune meetups. An immortal legend of Social Shuffle.",
                "⚡",
                90,
                8,
                "Mythic",
                "from-amber-400/40 via-purple-600/40 to-cyan-400/40",
                "border-amber-300",
                "text-amber-100",
                "bg-gradient-to-r from-amber-400/30 to-purple-400/30 text-amber-200 border-amber-300/70",
                "shadow-amber-400/60",
                Arrays.asList(
                        "Mythic Golden Avatar Crown",
                        "Free VIP Entry to Annual Championship",
                        "Name Etched onto Social Shuffle Trophy",
                        "Lifetime Pune Shuffler Pioneer Status"
                )
        ));
    }

    public BadgeService(ParticipantRepository participantRepository, GameRepository gameRepository) {
        this.participantRepository = participantRepository;
        this.gameRepository = gameRepository;
    }

    public List<BadgeLevelDefinition> getAllLevels() {
        return Collections.unmodifiableList(BADGE_LEVELS);
    }

    public BadgeCalculationResult calculateBadgeLevel(BadgeCalculationRequest request) {
        int gamesPlayed = Math.max(0, request.getGamesPlayedCount());
        int totalGames = request.getTotalGamesCount() > 0 ? request.getTotalGamesCount() : 24;
        int eventsAttended = Math.max(0, request.getEventsAttendedCount());

        int gamePercentage = Math.min(100, Math.round(((float) gamesPlayed / totalGames) * 100));

        // Evaluate earned level from Level 10 down to 1
        int earnedLevel = 0;

        for (int i = BADGE_LEVELS.size() - 1; i >= 0; i--) {
            BadgeLevelDefinition def = BADGE_LEVELS.get(i);

            // Special rule: 90%+ games played awards Level 10
            if (def.getLevel() == 10 && gamePercentage >= 90) {
                earnedLevel = 10;
                break;
            }

            boolean satisfiesGames = gamePercentage >= def.getMinGamePercentage();
            boolean satisfiesEvents = eventsAttended >= def.getMinEventsAttended();

            if (satisfiesGames && satisfiesEvents) {
                earnedLevel = def.getLevel();
                break;
            } else if (gamePercentage >= def.getMinGamePercentage() && eventsAttended >= Math.max(1, def.getMinEventsAttended() - 1)) {
                earnedLevel = def.getLevel();
                break;
            }
        }

        // Fallback: If at least 1 game played or 1 event attended, minimum Level 1
        if (earnedLevel == 0 && (gamesPlayed >= 1 || eventsAttended >= 1)) {
            earnedLevel = 1;
        }

        int currentLevelIdx = Math.max(0, earnedLevel - 1);
        BadgeLevelDefinition currentBadge = earnedLevel > 0 ? BADGE_LEVELS.get(currentLevelIdx) : BADGE_LEVELS.get(0);
        BadgeLevelDefinition nextBadge = earnedLevel < 10 ? BADGE_LEVELS.get(earnedLevel) : null;

        // Progress toward next level
        int percentageToNext = 100;
        int gamesNeeded = 0;
        int eventsNeeded = 0;

        if (nextBadge != null) {
            int targetGames = (int) Math.ceil((nextBadge.getMinGamePercentage() / 100.0) * totalGames);
            gamesNeeded = Math.max(0, targetGames - gamesPlayed);
            eventsNeeded = Math.max(0, nextBadge.getMinEventsAttended() - eventsAttended);

            int prevThreshold = currentBadge.getMinGamePercentage();
            int range = Math.max(1, nextBadge.getMinGamePercentage() - prevThreshold);
            int progressInGame = Math.max(0, gamePercentage - prevThreshold);
            percentageToNext = Math.min(99, Math.round(((float) progressInGame / range) * 100));
        }

        List<BadgeCalculationResult.LevelStatus> levelsList = new ArrayList<>();
        for (BadgeLevelDefinition def : BADGE_LEVELS) {
            int targetGames = (int) Math.ceil((def.getMinGamePercentage() / 100.0) * totalGames);
            int gameProg = Math.min(100, Math.round(((float) gamesPlayed / Math.max(1, targetGames)) * 100));
            int eventProg = Math.min(100, Math.round(((float) eventsAttended / Math.max(1, def.getMinEventsAttended())) * 100));
            int combinedProg = Math.round((gameProg * 0.7f) + (eventProg * 0.3f));

            boolean isUnlocked = def.getLevel() <= earnedLevel;
            boolean isCurrent = def.getLevel() == earnedLevel;

            levelsList.add(new BadgeCalculationResult.LevelStatus(
                    def.getLevel(),
                    def,
                    isUnlocked,
                    isCurrent,
                    isUnlocked ? 100 : Math.min(99, combinedProg)
            ));
        }

        int overallScore = Math.round((gamePercentage * 0.65f) + (Math.min(100, eventsAttended * 12.5f) * 0.35f));

        BadgeCalculationResult result = new BadgeCalculationResult();
        result.setCurrentLevel(earnedLevel);
        result.setCurrentBadge(currentBadge);
        result.setNextBadge(nextBadge);
        result.setGamesPlayedCount(gamesPlayed);
        result.setTotalGamesCount(totalGames);
        result.setGamePercentage(gamePercentage);
        result.setEventsAttendedCount(eventsAttended);
        result.setOverallScore(overallScore);
        result.setLevelProgress(new BadgeCalculationResult.LevelProgress(percentageToNext, gamesNeeded, eventsNeeded));
        result.setLevels(levelsList);

        return result;
    }

    public BadgeCalculationResult calculateForParticipant(String participantId) {
        int totalCatalogGames = (int) gameRepository.count();
        if (totalCatalogGames <= 0) {
            totalCatalogGames = 24;
        }

        Participant participant = participantRepository.findById(participantId).orElse(null);
        if (participant == null) {
            BadgeCalculationRequest emptyReq = new BadgeCalculationRequest(0, totalCatalogGames, 0);
            return calculateBadgeLevel(emptyReq);
        }

        int gamesPlayed = participant.getGamesPlayedIds() != null ? participant.getGamesPlayedIds().size() : 0;
        int eventsAttended = participant.getTotalEventsAttended();

        BadgeCalculationRequest req = new BadgeCalculationRequest(gamesPlayed, totalCatalogGames, eventsAttended);
        req.setParticipantId(participantId);

        return calculateBadgeLevel(req);
    }
}
