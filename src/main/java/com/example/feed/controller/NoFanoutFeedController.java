package com.example.feed.controller;

import com.example.feed.dto.FeedItemDTO;
import com.example.feed.service.NoFanoutFeedService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pull-feed")
@Validated
public class NoFanoutFeedController {

    private final NoFanoutFeedService noFanoutFeedService;

    public NoFanoutFeedController(NoFanoutFeedService noFanoutFeedService) {
        this.noFanoutFeedService = noFanoutFeedService;
    }

    @GetMapping("/timeline/{userId}")
    public ResponseEntity<Page<FeedItemDTO>> getUserFeedPullModel(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        long startTime = System.currentTimeMillis();

        var stats = noFanoutFeedService.getComplexityStats(userId);

        Page<FeedItemDTO> feed = noFanoutFeedService.getUserFeedPullModel(userId, page, size);

        long totalTime = System.currentTimeMillis() - startTime;

        return ResponseEntity.ok().body(feed);
    }

    @GetMapping("/stats/{userId}")
    public ResponseEntity<Map<String, Object>> getFeedComplexityStats(@PathVariable Long userId) {
        long startTime = System.currentTimeMillis();

        var stats = noFanoutFeedService.getComplexityStats(userId);
        long analysisTime = System.currentTimeMillis() - startTime;

        Map<String, Object> response = Map.of(
                "userId", userId,
                "usersFollowing", stats.usersFollowing(),
                "estimatedPostsToScan", stats.estimatedPostsToScan(),
                "timeComplexity", stats.timeComplexity(),
                "analysisTimeMs", analysisTime,
                "recommendations", getPerformanceRecommendations(stats));

        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/comparison")
    public ResponseEntity<Map<String, Object>> getModelComparison() {
        Map<String, Object> comparison = Map.of(
                "pushModel", Map.of(
                        "endpoint", "/api/feed/timeline",
                        "description", "Pre-calculated feed with fan-out",
                        "timeComplexity", "O(1)",
                        "performance", "Fast (~1-5ms)",
                        "scalability", "Excellent",
                        "storage", "Higher (duplicated data)",
                        "consistency", "Eventual"),
                "pullModel", Map.of(
                        "endpoint", "/api/pull-feed/timeline/{userId}",
                        "description", "Real-time feed with JOIN queries",
                        "timeComplexity", "O(n*m)",
                        "performance", "Slow (~50-500ms+)",
                        "scalability", "Limited",
                        "storage", "Lower (no duplication)",
                        "consistency", "Immediate"),
                "recommendation", "Use push model (fan-out) for production social media applications");

        return ResponseEntity.ok(comparison);
    }

    private Map<String, Object> getPerformanceRecommendations(NoFanoutFeedService.FeedComplexityStats stats) {
        String recommendation;
        String reason;

        if (stats.usersFollowing() < 50) {
            recommendation = "PULL model acceptable";
            reason = "Low number of follows, performance impact minimal";
        } else if (stats.usersFollowing() < 200) {
            recommendation = "PUSH model recommended";
            reason = "Moderate follows count, pull model will be noticeably slower";
        } else {
            recommendation = "PUSH model strongly recommended";
            reason = "High follows count, pull model will be very slow";
        }

        return Map.of(
                "recommendation", recommendation,
                "reason", reason,
                "estimatedResponseTime",
                stats.usersFollowing() < 50 ? "< 50ms" : stats.usersFollowing() < 200 ? "50-200ms" : "> 200ms");
    }
}