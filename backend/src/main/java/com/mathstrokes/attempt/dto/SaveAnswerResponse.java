package com.mathstrokes.attempt.dto;

import java.util.List;

import com.mathstrokes.common.enums.AnswerStatus;

/**
 * The acknowledgement for an autosave.
 *
 * It echoes the state the server actually holds rather than the state that was requested. When a
 * write is rejected as stale, {@code accepted} is false and the fields describe the newer answer
 * already stored, so the client can reconcile instead of guessing.
 *
 * The palette is returned alongside so the exam screen updates its navigator without a second
 * round trip, and the timing block keeps the countdown honest on every save.
 */
public record SaveAnswerResponse(
        boolean accepted,
        Long attemptQuestionId,
        List<Long> selectedOptionIds,
        AnswerStatus answerStatus,
        boolean markedForReview,
        long clientSequence,
        AttemptTimingResponse timing,
        List<PaletteEntryResponse> palette) {
}
