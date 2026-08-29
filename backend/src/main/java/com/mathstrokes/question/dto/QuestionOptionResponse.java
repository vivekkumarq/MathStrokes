package com.mathstrokes.question.dto;

/** Admin-facing option view. Includes the answer key, so it is never used on a student route. */
public record QuestionOptionResponse(Long id, String optionKey, String content,
                                     int displayOrder, boolean isCorrect) {
}
