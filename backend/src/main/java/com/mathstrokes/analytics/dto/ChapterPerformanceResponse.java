package com.mathstrokes.analytics.dto;

import java.math.BigDecimal;

public record ChapterPerformanceResponse(
        Long chapterId,
        String chapterName,
        long attemptCount,
        BigDecimal averageScore,
        BigDecimal averageAccuracy) {
}
