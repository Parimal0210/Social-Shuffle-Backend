package com.socialshuffle.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * Admin-only analytics controller backed by Supabase PostgreSQL views
 * (v_top_board_game_conquerors and v_community_analytics_summary).
 * Protected automatically by AuthInterceptor for all /api/admin/** routes.
 */
@RestController
@RequestMapping("/api/admin/analytics")
@CrossOrigin(origins = "*")
public class AdminAnalyticsController {

    private final JdbcTemplate jdbcTemplate;

    public AdminAnalyticsController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/top-conquerors")
    public ResponseEntity<Map<String, Object>> getTopConquerors(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        int safeLimit = Math.max(1, Math.min(50, limit));
        List<Map<String, Object>> topConquerors = queryTopConquerors(safeLimit);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("limit", safeLimit);
        response.put("count", topConquerors.size());
        response.put("topConquerors", topConquerors);
        response.put("generatedAt", Instant.now().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getAnalyticsSummary() {
        Map<String, Object> summaryRow;
        try {
            summaryRow = jdbcTemplate.queryForMap("SELECT * FROM v_community_analytics_summary");
        } catch (Exception ex) {
            summaryRow = new LinkedHashMap<>();
        }

        List<Map<String, Object>> topConquerors = queryTopConquerors(10);

        List<Map<String, Object>> topOutsideGames;
        try {
            topOutsideGames = jdbcTemplate.queryForList("""
                SELECT
                  MIN(name) AS name,
                  COALESCE(NULLIF(TRIM(MIN(genre)), ''), 'Strategy') AS genre,
                  COUNT(DISTINCT participant_id)::int AS players_count
                FROM participant_conquered_games
                WHERE (library_game_id IS NULL OR TRIM(library_game_id) = '')
                  AND COALESCE(TRIM(normalized_name), '') <> ''
                GROUP BY normalized_name
                ORDER BY players_count DESC, MIN(name) ASC
                LIMIT 5
                """);
        } catch (Exception ex) {
            topOutsideGames = Collections.emptyList();
        }

        Map<String, Object> feedbackBreakdown;
        try {
            feedbackBreakdown = jdbcTemplate.queryForMap("""
                SELECT
                  COUNT(*)::int AS feedback_count,
                  COALESCE(ROUND(AVG(overall_rating)::numeric, 1), 0) AS avg_overall_rating,
                  COALESCE(ROUND(AVG(venue_rating)::numeric, 1), 0) AS avg_venue_rating,
                  COALESCE(ROUND(AVG(game_rating)::numeric, 1), 0) AS avg_game_rating,
                  COALESCE(ROUND(AVG(host_rating)::numeric, 1), 0) AS avg_host_rating
                FROM event_feedback
                """);
        } catch (Exception ex) {
            feedbackBreakdown = Collections.emptyMap();
        }

        List<Map<String, Object>> areaBreakdown;
        try {
            areaBreakdown = jdbcTemplate.queryForList("""
                SELECT
                  COALESCE(NULLIF(TRIM(area), ''), 'Pune') AS area,
                  COUNT(*)::int AS participants_count,
                  COALESCE(SUM(total_events_attended), 0)::int AS total_attendance
                FROM participants
                GROUP BY COALESCE(NULLIF(TRIM(area), ''), 'Pune')
                ORDER BY participants_count DESC, area ASC
                LIMIT 10
                """);
        } catch (Exception ex) {
            areaBreakdown = Collections.emptyList();
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("summary", summaryRow);
        response.put("topConquerors", topConquerors);
        response.put("topOutsideGames", topOutsideGames);
        response.put("feedbackBreakdown", feedbackBreakdown);
        response.put("areaBreakdown", areaBreakdown);
        response.put("generatedAt", Instant.now().toString());
        return ResponseEntity.ok(response);
    }

    private List<Map<String, Object>> queryTopConquerors(int limit) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    """
                    SELECT
                      participant_id,
                      name,
                      area,
                      avatar,
                      total_events_attended,
                      library_conquered_count,
                      outside_conquered_count,
                      total_conquered_count
                    FROM v_top_board_game_conquerors
                    LIMIT ?
                    """,
                    limit
            );
            List<Map<String, Object>> formatted = new ArrayList<>(rows.size());
            int rank = 1;
            for (Map<String, Object> r : rows) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("rank", rank++);
                item.put("participantId", r.get("participant_id"));
                item.put("name", r.get("name"));
                item.put("area", r.get("area"));
                item.put("avatar", r.get("avatar"));
                item.put("eventsAttended", r.get("total_events_attended"));
                item.put("libraryCount", r.get("library_conquered_count"));
                item.put("outsideCount", r.get("outside_conquered_count"));
                item.put("totalConquered", r.get("total_conquered_count"));
                formatted.add(item);
            }
            return formatted;
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }
}
