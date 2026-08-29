package com.mathstrokes.question.dto;

import java.time.Instant;
import java.util.List;

import com.mathstrokes.common.enums.Difficulty;
import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.QuestionStatus;
import com.mathstrokes.common.enums.QuestionType;

/** Full admin view of a question, answer key included. */
public record QuestionResponse(
        Long id,
        Long subjectId,
        String subjectName,
        Long chapterId,
        String chapterName,
        ExamPattern examPattern,
        Difficulty difficulty,
        QuestionType questionType,
        String questionContent,
        String solutionContent,
        QuestionStatus status,
        Long markingSchemeId,
        String markingSchemeName,
        String createdByName,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        Integer version,
        List<QuestionOptionResponse> options) {
}
