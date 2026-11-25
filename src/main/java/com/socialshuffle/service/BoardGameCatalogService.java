package com.socialshuffle.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialshuffle.model.BoardGameCatalogItem;
import com.socialshuffle.repository.BoardGameCatalogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BoardGameCatalogService {

    private static final Logger log = LoggerFactory.getLogger(BoardGameCatalogService.class);
    private static final long CATALOG_TTL_MS = 24L * 60 * 60 * 1000;
    private static final Pattern NUMERIC_ENTITY_PATTERN = Pattern.compile("&#(\\d+);");

    private final ObjectMapper objectMapper;
    private final BoardGameCatalogRepository catalogRepository;
    private final JdbcTemplate jdbcTemplate;
    private final HttpClient httpClient;

    private volatile List<CatalogEntry> catalogCache = Collections.emptyList();
    private volatile long catalogCachedAt = 0L;
    private volatile String catalogUpdatedDate = "";
    private final Object refreshLock = new Object();

    private final Map<String, CachedDetails> detailsCache = new ConcurrentHashMap<>();

    public record CatalogEntry(
            String name,
            String genre,
            String players,
            String duration,
            String externalId,
            int rank
    ) {}

    public record GameDetails(
            String genre,
            String players,
            String duration
    ) {}

    private record CachedDetails(
            GameDetails details,
            long cachedAt
    ) {}

    public BoardGameCatalogService(ObjectMapper objectMapper,
                                   BoardGameCatalogRepository catalogRepository,
                                   JdbcTemplate jdbcTemplate) {
        this.objectMapper = objectMapper;
        this.catalogRepository = catalogRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Async("mailTaskExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void warmOnStartup() {
        try {
            loadFromDatabaseIfEmpty();
            refreshCatalogIfNeeded(true);
        } catch (Exception e) {
            log.warn("Initial daily board game catalog warm-up deferred: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = CATALOG_TTL_MS, initialDelay = CATALOG_TTL_MS)
    public void scheduledDailyRefresh() {
        try {
            refreshCatalogIfNeeded(true);
        } catch (Exception e) {
            log.warn("Scheduled daily board game catalog refresh failed: {}", e.getMessage());
        }
    }

    public Map<String, Object> searchCatalog(String rawQuery) {
        refreshCatalogIfNeeded(false);
        List<CatalogEntry> snapshot = catalogCache;
        String q = rawQuery != null ? rawQuery.trim().toLowerCase(Locale.ROOT) : "";

        List<CatalogEntry> results;
        if (q.isEmpty()) {
            results = snapshot.subList(0, Math.min(120, snapshot.size()));
        } else {
            List<CatalogEntry> prefixMatches = new ArrayList<>(25);
            List<CatalogEntry> wordMatches = new ArrayList<>(20);
            List<CatalogEntry> substringMatches = new ArrayList<>(20);

            for (CatalogEntry entry : snapshot) {
                String lower = entry.name().toLowerCase(Locale.ROOT);
                if (lower.startsWith(q)) {
                    prefixMatches.add(entry);
                    if (prefixMatches.size() >= 25) break;
                } else if (lower.contains(" " + q)) {
                    if (wordMatches.size() < 20) wordMatches.add(entry);
                } else if (lower.contains(q)) {
                    if (substringMatches.size() < 20) substringMatches.add(entry);
                }
            }

            List<CatalogEntry> combined = new ArrayList<>(20);
            combined.addAll(prefixMatches);
            combined.addAll(wordMatches);
            combined.addAll(substringMatches);
            results = combined.subList(0, Math.min(20, combined.size()));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("games", results);
        payload.put("totalCached", snapshot.size());
        payload.put("lastUpdated", catalogUpdatedDate);
        return payload;
    }

    public GameDetails fetchGameDetails(String rawExternalId) {
        String externalId = rawExternalId != null ? rawExternalId.replaceAll("[^0-9]", "") : "";
        GameDetails fallback = new GameDetails("Strategy", "2-4 Players", "45-60 mins");
        if (externalId.isEmpty()) {
            return fallback;
        }

        CachedDetails cached = detailsCache.get(externalId);
        if (cached != null && (System.currentTimeMillis() - cached.cachedAt()) < CATALOG_TTL_MS) {
            return cached.details();
        }

        try {
            String url = "https://api.geekdo.com/api/geekitems?objectid="
                    + URLEncoder.encode(externalId, StandardCharsets.UTF_8)
                    + "&objecttype=thing";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(4))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body() == null) {
                return fallback;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode item = root.path("item");
            if (item.isMissingNode() || item.isNull()) {
                return fallback;
            }

            int minP = parsePositiveInt(item.path("minplayers").asText(""), 2);
            int maxP = parsePositiveInt(item.path("maxplayers").asText(""), minP);
            String players = (minP == maxP) ? (minP + " Players") : (minP + "-" + maxP + " Players");

            int minT = parsePositiveInt(item.path("minplaytime").asText(""), 45);
            int maxT = parsePositiveInt(item.path("maxplaytime").asText(""), minT);
            String duration = (minT == maxT) ? (minT + " mins") : (minT + "-" + maxT + " mins");

            List<String> categories = new ArrayList<>();
            JsonNode catNodes = item.path("links").path("boardgamecategory");
            if (catNodes.isArray()) {
                for (JsonNode c : catNodes) {
                    String name = c.path("name").asText("").trim();
                    if (!name.isEmpty()) categories.add(name);
                }
            }

            List<String> subdomains = new ArrayList<>();
            JsonNode subNodes = item.path("links").path("boardgamesubdomain");
            if (subNodes.isArray()) {
                for (JsonNode s : subNodes) {
                    String name = s.path("name").asText("").trim();
                    if (!name.isEmpty()) subdomains.add(name);
                }
            }

            String genre = mapExternalGenre(categories, subdomains);
            GameDetails resolved = new GameDetails(genre, players, duration);
            detailsCache.put(externalId, new CachedDetails(resolved, System.currentTimeMillis()));
            persistVerifiedSpecsToDb(externalId, resolved);
            return resolved;
        } catch (Exception e) {
            return fallback;
        }
    }

    private void loadFromDatabaseIfEmpty() {
        if (!catalogCache.isEmpty()) return;
        try {
            List<BoardGameCatalogItem> dbRows = catalogRepository.findAllByOrderByBggRankAsc();
            if (!dbRows.isEmpty()) {
                List<CatalogEntry> loaded = new ArrayList<>(dbRows.size());
                String latestDate = "";
                for (BoardGameCatalogItem row : dbRows) {
                    if (row.getName() == null || row.getName().isBlank()) continue;
                    loaded.add(new CatalogEntry(
                            row.getName(),
                            row.getGenre() != null ? row.getGenre() : "Strategy",
                            row.getPlayers() != null ? row.getPlayers() : "2-4 Players",
                            row.getDuration() != null ? row.getDuration() : "45-60 mins",
                            row.getExternalId(),
                            row.getBggRank()
                    ));
                    if (latestDate.isEmpty() && row.getSnapshotDate() != null) {
                        latestDate = row.getSnapshotDate();
                    }
                    if (row.isHasVerifiedSpecs() && row.getExternalId() != null && !row.getExternalId().isBlank()) {
                        detailsCache.put(
                                row.getExternalId(),
                                new CachedDetails(
                                        new GameDetails(
                                                row.getGenre() != null ? row.getGenre() : "Strategy",
                                                row.getPlayers() != null ? row.getPlayers() : "2-4 Players",
                                                row.getDuration() != null ? row.getDuration() : "45-60 mins"
                                        ),
                                        System.currentTimeMillis()
                                )
                        );
                    }
                }
                if (!loaded.isEmpty()) {
                    catalogCache = Collections.unmodifiableList(loaded);
                    if (catalogUpdatedDate.isEmpty()) catalogUpdatedDate = latestDate;
                    log.info("Loaded {} board games from Supabase board_game_catalog", loaded.size());
                }
            }
        } catch (Exception e) {
            log.warn("Could not hydrate board_game_catalog from database: {}", e.getMessage());
        }
    }

    private void refreshCatalogIfNeeded(boolean force) {
        if (!force && catalogCache.isEmpty()) {
            loadFromDatabaseIfEmpty();
        }
        long now = System.currentTimeMillis();
        if (!force && !catalogCache.isEmpty() && (now - catalogCachedAt) < CATALOG_TTL_MS) {
            return;
        }
        synchronized (refreshLock) {
            if (!force && !catalogCache.isEmpty() && (System.currentTimeMillis() - catalogCachedAt) < CATALOG_TTL_MS) {
                return;
            }

            List<CatalogEntry> merged = new ArrayList<>(26100);
            Set<String> seen = new HashSet<>(35000);
            String resolvedDate = "";

            SnapshotResult snapshot = fetchDailyRankedSnapshot();
            if (!snapshot.games().isEmpty()) {
                resolvedDate = snapshot.snapshotDate();
                for (CatalogEntry entry : snapshot.games()) {
                    String key = entry.name().toLowerCase(Locale.ROOT);
                    if (seen.add(key)) {
                        merged.add(entry);
                    }
                }
            }

            List<CatalogEntry> hotness = fetchHotnessEntries();
            for (int i = hotness.size() - 1; i >= 0; i--) {
                CatalogEntry entry = hotness.get(i);
                String key = entry.name().toLowerCase(Locale.ROOT);
                if (seen.add(key)) {
                    merged.add(0, entry);
                }
            }

            if (!merged.isEmpty()) {
                catalogCache = Collections.unmodifiableList(merged);
                catalogUpdatedDate = resolvedDate.isEmpty() ? LocalDate.now(ZoneOffset.UTC).toString() : resolvedDate;
                catalogCachedAt = System.currentTimeMillis();
                persistCatalogToDatabase(merged, catalogUpdatedDate);
                log.info("Synced {} daily ranked board games (snapshot: {})",
                        merged.size(), catalogUpdatedDate);
            }
        }
    }

    private void persistCatalogToDatabase(List<CatalogEntry> entries, String snapshotDate) {
        try {
            String now = Instant.now().toString();
            String sql = """
                    INSERT INTO board_game_catalog
                        (id, external_id, name, normalized_name, genre, players, duration, bgg_rank, has_verified_specs, snapshot_date, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, FALSE, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET
                        external_id = COALESCE(EXCLUDED.external_id, board_game_catalog.external_id),
                        name = EXCLUDED.name,
                        normalized_name = EXCLUDED.normalized_name,
                        bgg_rank = EXCLUDED.bgg_rank,
                        snapshot_date = EXCLUDED.snapshot_date,
                        updated_at = EXCLUDED.updated_at,
                        genre = CASE WHEN board_game_catalog.has_verified_specs THEN board_game_catalog.genre ELSE EXCLUDED.genre END,
                        players = CASE WHEN board_game_catalog.has_verified_specs THEN board_game_catalog.players ELSE EXCLUDED.players END,
                        duration = CASE WHEN board_game_catalog.has_verified_specs THEN board_game_catalog.duration ELSE EXCLUDED.duration END
                    """;

            final int batchSize = 1000;
            List<CatalogEntry> batch = new ArrayList<>(batchSize);
            for (CatalogEntry entry : entries) {
                batch.add(entry);
                if (batch.size() >= batchSize) {
                    executeCatalogBatch(sql, batch, snapshotDate, now);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                executeCatalogBatch(sql, batch, snapshotDate, now);
            }
        } catch (Exception e) {
            log.warn("Skipped persisting board_game_catalog batch: {}", e.getMessage());
        }
    }

    private void executeCatalogBatch(String sql, List<CatalogEntry> batch, String snapshotDate, String now) {
        jdbcTemplate.batchUpdate(sql, batch, batch.size(), (PreparedStatement ps, CatalogEntry entry) -> {
            String norm = entry.name().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
            String rowId = (entry.externalId() != null && !entry.externalId().isBlank())
                    ? "bgg-" + entry.externalId()
                    : "slug-" + Integer.toHexString(norm.hashCode());
            ps.setString(1, truncate(rowId, 64));
            ps.setString(2, entry.externalId());
            ps.setString(3, truncate(entry.name(), 255));
            ps.setString(4, truncate(norm, 255));
            ps.setString(5, truncate(entry.genre(), 100));
            ps.setString(6, truncate(entry.players(), 64));
            ps.setString(7, truncate(entry.duration(), 64));
            ps.setInt(8, entry.rank());
            ps.setString(9, truncate(snapshotDate, 32));
            ps.setString(10, now);
        });
    }

    private void persistVerifiedSpecsToDb(String externalId, GameDetails details) {
        try {
            catalogRepository.findFirstByExternalId(externalId).ifPresent(item -> {
                item.setGenre(truncate(details.genre(), 100));
                item.setPlayers(truncate(details.players(), 64));
                item.setDuration(truncate(details.duration(), 64));
                item.setHasVerifiedSpecs(true);
                item.setUpdatedAt(Instant.now().toString());
                catalogRepository.save(item);
            });
        } catch (Exception ignored) {
        }
    }

    private String truncate(String val, int maxLen) {
        if (val == null) return null;
        return val.length() <= maxLen ? val : val.substring(0, maxLen);
    }

    private record SnapshotResult(List<CatalogEntry> games, String snapshotDate) {}

    private SnapshotResult fetchDailyRankedSnapshot() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate yesterday = today.minusDays(1);
        List<String> dates = Arrays.asList(today.toString(), yesterday.toString());

        for (String dateStr : dates) {
            String url = "https://raw.githubusercontent.com/beefsack/bgg-ranking-historicals/master/" + dateStr + ".csv";
            List<CatalogEntry> parsed = downloadAndParseCsv(url);
            if (!parsed.isEmpty()) {
                return new SnapshotResult(parsed, dateStr);
            }
        }

        // Fallback: latest commit CSV on master branch
        try {
            HttpRequest commitReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/beefsack/bgg-ranking-historicals/commits/master"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Accept", "application/vnd.github+json")
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(commitReq, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200 && resp.body() != null) {
                JsonNode root = objectMapper.readTree(resp.body());
                JsonNode files = root.path("files");
                if (files.isArray()) {
                    for (JsonNode f : files) {
                        String filename = f.path("filename").asText("");
                        if (filename.endsWith(".csv")) {
                            List<CatalogEntry> parsed = downloadAndParseCsv(
                                    "https://raw.githubusercontent.com/beefsack/bgg-ranking-historicals/master/" + filename
                            );
                            if (!parsed.isEmpty()) {
                                return new SnapshotResult(parsed, filename.replace(".csv", ""));
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return new SnapshotResult(Collections.emptyList(), "");
    }

    private List<CatalogEntry> downloadAndParseCsv(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200 || resp.body() == null) {
                return Collections.emptyList();
            }

            String[] lines = resp.body().split("\\r?\\n");
            List<CatalogEntry> parsed = new ArrayList<>(Math.min(lines.length, 26000));
            Set<String> seen = new HashSet<>(30000);

            for (int i = 1; i < lines.length && parsed.size() < 26000; i++) {
                String line = lines[i];
                if (line == null || line.isEmpty()) continue;
                List<String> cols = parseCsvRow(line);
                if (cols.size() < 2) continue;

                String id = cols.get(0).trim();
                String rawName = decodeHtmlEntities(cols.get(1));
                int rankNum = cols.size() > 3 ? parsePositiveInt(cols.get(3), 99999) : 99999;

                if (rawName.length() < 2) continue;
                String key = rawName.toLowerCase(Locale.ROOT);
                if (seen.add(key)) {
                    parsed.add(new CatalogEntry(
                            rawName,
                            "Strategy",
                            "2-4 Players",
                            "45-60 mins",
                            id.isEmpty() ? null : id,
                            rankNum
                    ));
                }
            }
            return parsed;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<CatalogEntry> fetchHotnessEntries() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.geekdo.com/api/hotness?geeksite=boardgame&objecttype=thing&showcount=50"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200 || resp.body() == null) {
                return Collections.emptyList();
            }
            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode items = root.path("items");
            if (!items.isArray()) {
                return Collections.emptyList();
            }
            List<CatalogEntry> out = new ArrayList<>();
            for (JsonNode it : items) {
                String id = it.path("objectid").asText("");
                if (id.isEmpty()) id = it.path("id").asText("");
                String name = decodeHtmlEntities(it.path("name").asText(""));
                if (name.length() >= 2) {
                    out.add(new CatalogEntry(
                            name,
                            "Strategy",
                            "2-4 Players",
                            "45-60 mins",
                            id.isEmpty() ? null : id,
                            0
                    ));
                }
            }
            return out;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<String> parseCsvRow(String line) {
        List<String> cols = new ArrayList<>(8);
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                cols.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        cols.add(current.toString());
        return cols;
    }

    private String decodeHtmlEntities(String text) {
        if (text == null) return "";
        String decoded = text
                .replace("&amp;", "&")
                .replace("&#039;", "'")
                .replace("&apos;", "'")
                .replace("&quot;", "\"")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
        Matcher matcher = NUMERIC_ENTITY_PATTERN.matcher(decoded);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            try {
                int code = Integer.parseInt(matcher.group(1));
                matcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf((char) code)));
            } catch (Exception ignored) {
            }
        }
        matcher.appendTail(sb);
        return sb.toString().trim();
    }

    private int parsePositiveInt(String val, int fallback) {
        if (val == null || val.trim().isEmpty()) return fallback;
        try {
            int parsed = Integer.parseInt(val.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String mapExternalGenre(List<String> categories, List<String> subdomains) {
        StringBuilder sb = new StringBuilder();
        for (String c : categories) sb.append(c).append(' ');
        for (String s : subdomains) sb.append(s).append(' ');
        String joined = sb.toString().toLowerCase(Locale.ROOT);

        if (joined.contains("social deduction") || joined.contains("hidden identity")
                || joined.contains("mafia") || joined.contains("werewolf")) return "Social Deduction";
        if (joined.contains("bluff") || joined.contains("negotiation")) return "Bluffing";
        if (joined.contains("party")) return "Party";
        if (joined.contains("abstract")) return "Abstract";
        if (joined.contains("cooperative") || joined.contains("co-op")) return "Co-op";
        if (joined.contains("dexterity") || joined.contains("action / dexterity")) return "Dexterity";
        if (joined.contains("card game")) return "Card Games";
        if (joined.contains("family")) return "Family";
        if (joined.contains("thematic")) return "Thematic";
        if (joined.contains("eurogame")) return "Euro";
        if (joined.contains("strategy")) return "Strategy";
        if (!categories.isEmpty()) return categories.get(0);
        return "Strategy";
    }
}
