package com.mathstrokes.analytics.dto;

import java.math.BigDecimal;

import com.mathstrokes.common.enums.Difficulty;
import com.mathstrokes.common.enums.ExamPattern;

/**
 * How a question has actually performed, as opposed to how it was labelled.
 *
 * accuracy = correct / attempted x 100, so a question nobody attempted reports null rather than
 * a misleading zero. A large gap between the tagged difficulty and the measured accuracy is the
 * signal a teacher is looking for: it usually means the tag is wrong or the question is unclear.
 */
public record QuestionQualityResponse(
        Long questionId,
        String chapterName,
        ExamPattern examPattern,
        Difficulty taggedDifficulty,
        String questionPreview,
        long timesShown,
        long timesAttempted,
        long correctCount,
        long partiallyCorrectCount,
        long incorrectCount,
        long unansweredCount,
        BigDecimal accuracy) {
}
