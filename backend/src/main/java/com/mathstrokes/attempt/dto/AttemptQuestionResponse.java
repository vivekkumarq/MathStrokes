package com.mathstrokes.attempt.dto;

import java.util.List;

import com.mathstrokes.common.enums.AnswerStatus;
import com.mathstrokes.common.enums.Difficulty;
import com.mathstrokes.common.enums.QuestionType;

/**
 * One question of a live attempt, with the student's current working state.
 *
 * {@code questionType} is what the client uses to choose radio buttons or checkboxes - never the
 * exam pattern.
 */
public record AttemptQuestionResponse(
        Long attemptQuestionId,
        int questionOrder,
        QuestionType questionType,
        Difficulty difficulty,
        String questionContent,
        List<AttemptOptionResponse> options,
        List<Long> selectedOptionIds,
        AnswerStatus answerStatus,
        boolean markedForReview,
        boolean visited) {
}
