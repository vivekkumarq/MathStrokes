package com.mathstrokes.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerStatusTest {

    @Test
    @DisplayName("an untouched question is NOT_VISITED")
    void untouched() {
        assertThat(AnswerStatus.of(false, false, false)).isEqualTo(AnswerStatus.NOT_VISITED);
    }

    @Test
    @DisplayName("seen but left blank is NOT_ANSWERED, which the palette shows differently")
    void visitedButBlank() {
        assertThat(AnswerStatus.of(true, false, false)).isEqualTo(AnswerStatus.NOT_ANSWERED);
    }

    @Test
    @DisplayName("answered and marked for review is its own palette state")
    void answeredAndMarked() {
        assertThat(AnswerStatus.of(true, true, true))
                .isEqualTo(AnswerStatus.ANSWERED_AND_MARKED_FOR_REVIEW);
    }

    @Test
    @DisplayName("marked for review without an answer is distinct from marked with one")
    void markedWithoutAnswer() {
        assertThat(AnswerStatus.of(true, false, true)).isEqualTo(AnswerStatus.MARKED_FOR_REVIEW);
    }

    @Test
    @DisplayName("both answered states count as answered for the submission summary")
    void answeredStates() {
        assertThat(AnswerStatus.ANSWERED.isAnswered()).isTrue();
        assertThat(AnswerStatus.ANSWERED_AND_MARKED_FOR_REVIEW.isAnswered()).isTrue();
        assertThat(AnswerStatus.MARKED_FOR_REVIEW.isAnswered()).isFalse();
        assertThat(AnswerStatus.NOT_ANSWERED.isAnswered()).isFalse();
        assertThat(AnswerStatus.NOT_VISITED.isAnswered()).isFalse();
    }
}
