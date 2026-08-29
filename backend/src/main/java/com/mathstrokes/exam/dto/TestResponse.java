package com.mathstrokes.exam.dto;

import java.time.Instant;

import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.TestGenerationMode;
import com.mathstrokes.common.enums.TestStatus;

/** Admin view of a test, including its blueprint and how many questions are actually attached. */
public record TestResponse(
        Long id,
        String title,
        String description,
        Long subjectId,
        String subjectName,
        Long chapterId,
        String chapterName,
        ExamPattern examPattern,
        int durationMinutes,
        int questionCount,
        TestGenerationMode generationMode,
        Integer easyCount,
        Integer mediumCount,
        Integer hardCount,
        TestStatus status,
        boolean rankingEnabled,
        int maxAttemptsPerStudent,
        long attachedQuestionCount,
        Instant publishedAt,
        Instant closedAt,
        Instant createdAt,
        Integer version) {
}
