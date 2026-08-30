package com.mathstrokes.analytics.dto;

/** Headline counters for the admin landing page. One query per figure, all indexed. */
public record AdminDashboardResponse(
        long totalStudents,
        long activeStudentsLast30Days,
        long totalQuestions,
        long publishedQuestions,
        long draftQuestions,
        long archivedQuestions,
        long totalTests,
        long publishedTests,
        long totalAttempts,
        long attemptsInProgress,
        long attemptsLast7Days) {
}
