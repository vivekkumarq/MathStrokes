package com.mathstrokes.attempt.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * An autosave write.
 *
 * {@code selectedOptionIds} is the complete selection for the question, not a delta - sending an
 * empty list clears the answer. That makes the write idempotent and means a retry after a
 * dropped connection cannot double-toggle a checkbox.
 *
 * {@code clientSequence} increments once per attempt on the client. A write whose sequence is
 * older than the stored one is discarded, so a delayed request cannot overwrite newer work.
 */
public record SaveAnswerRequest(
        @NotNull(message = "Question reference is required")
        Long attemptQuestionId,

        @Size(max = 10, message = "Too many options selected")
        List<Long> selectedOptionIds,

        Boolean markedForReview,

        Boolean visited,

        @PositiveOrZero(message = "Client sequence cannot be negative")
        Long clientSequence) {
}
