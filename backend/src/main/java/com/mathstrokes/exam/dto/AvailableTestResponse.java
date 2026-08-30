package com.mathstrokes.exam.dto;

import com.mathstrokes.common.enums.ExamPattern;

/**
 * What a student sees when browsing tests. Carries no question data at all - the paper is only
 * revealed once an attempt has been created.
 */
public record AvailableTestResponse(
        Long id,
        String title,
        String description,
        String subjectName,
        /** Absent for a full-syllabus test. Absence is the signal, not a sentinel value. */
        Long chapterId,
        String chapterName,
        ExamPattern examPattern,
        int durationMinutes,
        int questionCount,
        boolean rankingEnabled,
        int maxAttemptsPerStudent,
        int attemptsUsed,
        boolean canStart,
        Long activeAttemptId,
        String unavailableReason) {
}
