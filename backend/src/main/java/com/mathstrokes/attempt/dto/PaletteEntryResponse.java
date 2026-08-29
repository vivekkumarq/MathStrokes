package com.mathstrokes.attempt.dto;

import com.mathstrokes.common.enums.AnswerStatus;

/** One square of the question palette. Kept tiny: the palette refreshes on every autosave. */
public record PaletteEntryResponse(Long attemptQuestionId, int questionOrder,
                                   AnswerStatus answerStatus) {
}
