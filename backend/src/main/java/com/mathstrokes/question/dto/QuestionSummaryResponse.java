package com.mathstrokes.question.dto;

import java.time.Instant;

import com.mathstrokes.common.enums.Difficulty;
import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.QuestionStatus;
import com.mathstrokes.common.enums.QuestionType;

/** Row in the admin question grid. Deliberately omits option content to keep listings light. */
public record QuestionSummaryResponse(
        Long id,
        String chapterName,
        ExamPattern examPattern,
        Difficulty difficulty,
        QuestionType questionType,
        String questionPreview,
        QuestionStatus status,
        int optionCount,
        Instant updatedAt,
        Integer version) {
}
