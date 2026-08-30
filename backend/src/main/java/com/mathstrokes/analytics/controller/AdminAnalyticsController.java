package com.mathstrokes.analytics.controller;

import java.util.List;

import com.mathstrokes.analytics.dto.AdminDashboardResponse;
import com.mathstrokes.analytics.dto.ChapterPerformanceResponse;
import com.mathstrokes.analytics.dto.QuestionQualityResponse;
import com.mathstrokes.analytics.service.AdminAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/analytics")
@Tag(name = "Admin - Analytics", description = "Platform and question-quality metrics")
public class AdminAnalyticsController {

    private final AdminAnalyticsService analyticsService;

    public AdminAnalyticsController(AdminAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Headline platform counters")
    public AdminDashboardResponse dashboard() {
        return analyticsService.dashboard();
    }

    @GetMapping("/questions/hardest")
    @Operation(summary = "Questions with the lowest measured accuracy",
            description = "Ignores questions with too few attempts to be meaningful. A large gap "
                    + "between the tagged difficulty and the measured accuracy usually means the "
                    + "tag is wrong or the question is unclear.")
    public List<QuestionQualityResponse> hardestQuestions(
            @RequestParam(required = false) Long chapterId,
            @RequestParam(defaultValue = "20") int limit) {
        return analyticsService.hardestQuestions(chapterId, Math.min(Math.max(limit, 1), 100));
    }

    @GetMapping("/chapters")
    @Operation(summary = "Average score and accuracy by chapter")
    public List<ChapterPerformanceResponse> chapterPerformance() {
        return analyticsService.chapterPerformance();
    }
}
