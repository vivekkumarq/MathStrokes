package com.mathstrokes.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AttemptStatusTest {

    @Test
    @DisplayName("an attempt may only move forward through the lifecycle")
    void allowedTransitions() {
        assertThat(AttemptStatus.NOT_STARTED.canTransitionTo(AttemptStatus.ACTIVE)).isTrue();
        assertThat(AttemptStatus.ACTIVE.canTransitionTo(AttemptStatus.SUBMITTED)).isTrue();
        assertThat(AttemptStatus.ACTIVE.canTransitionTo(AttemptStatus.AUTO_SUBMITTED)).isTrue();
        assertThat(AttemptStatus.SUBMITTED.canTransitionTo(AttemptStatus.EVALUATED)).isTrue();
        assertThat(AttemptStatus.AUTO_SUBMITTED.canTransitionTo(AttemptStatus.EVALUATED)).isTrue();
    }

    @Test
    @DisplayName("an evaluated attempt is terminal, so a result can never be reopened")
    void evaluatedIsTerminal() {
        for (AttemptStatus target : AttemptStatus.values()) {
            assertThat(AttemptStatus.EVALUATED.canTransitionTo(target)).isFalse();
        }
    }

    @Test
    @DisplayName("an attempt cannot go back to being active")
    void cannotReopenAFinishedAttempt() {
        assertThat(AttemptStatus.SUBMITTED.canTransitionTo(AttemptStatus.ACTIVE)).isFalse();
        assertThat(AttemptStatus.AUTO_SUBMITTED.canTransitionTo(AttemptStatus.ACTIVE)).isFalse();
        assertThat(AttemptStatus.EVALUATED.canTransitionTo(AttemptStatus.ACTIVE)).isFalse();
    }

    @Test
    @DisplayName("finalised covers every state in which answers must be frozen")
    void finalisedStates() {
        assertThat(AttemptStatus.SUBMITTED.isFinalised()).isTrue();
        assertThat(AttemptStatus.AUTO_SUBMITTED.isFinalised()).isTrue();
        assertThat(AttemptStatus.EVALUATED.isFinalised()).isTrue();
        assertThat(AttemptStatus.ACTIVE.isFinalised()).isFalse();
        assertThat(AttemptStatus.NOT_STARTED.isFinalised()).isFalse();
    }
}
