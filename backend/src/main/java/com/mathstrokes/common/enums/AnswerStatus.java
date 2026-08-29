package com.mathstrokes.common.enums;

/** Palette state for a single question inside an attempt. Persisted so it survives a refresh. */
public enum AnswerStatus {
    NOT_VISITED,
    NOT_ANSWERED,
    ANSWERED,
    MARKED_FOR_REVIEW,
    ANSWERED_AND_MARKED_FOR_REVIEW;

    public static AnswerStatus of(boolean visited, boolean answered, boolean markedForReview) {
        if (!visited && !answered && !markedForReview) {
            return NOT_VISITED;
        }
        if (answered) {
            return markedForReview ? ANSWERED_AND_MARKED_FOR_REVIEW : ANSWERED;
        }
        return markedForReview ? MARKED_FOR_REVIEW : NOT_ANSWERED;
    }

    public boolean isAnswered() {
        return this == ANSWERED || this == ANSWERED_AND_MARKED_FOR_REVIEW;
    }
}
