package com.mathstrokes.exam.service;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestScheduleTest {

    @Test
    @DisplayName("a UTC instant is rendered in the classroom's local time, not UTC")
    void rendersInIndianTime() {
        // 10:30 UTC is 4:00 PM in Kolkata. Rendering the stored value verbatim would tell a class
        // their paper opens at half past ten when the teacher announced four o'clock.
        assertThat(TestSchedule.humanise(Instant.parse("2026-09-04T10:30:00Z")))
                .isEqualTo("4 September at 4:00 PM");
    }

    @Test
    @DisplayName("an instant that crosses midnight locally is reported on the local date")
    void crossesTheDateLine() {
        // 20:00 UTC on the 3rd is 1:30 AM on the 4th in Kolkata. The date has to move with it.
        assertThat(TestSchedule.humanise(Instant.parse("2026-09-03T20:00:00Z")))
                .isEqualTo("4 September at 1:30 AM");
    }

    @Test
    @DisplayName("an absent bound has no sentence, because an unbounded window is not an error")
    void absentBoundIsNull() {
        assertThat(TestSchedule.humanise(null)).isNull();
    }

    @Test
    @DisplayName("noon and midnight are not confused with one another")
    void noonAndMidnight() {
        assertThat(TestSchedule.humanise(Instant.parse("2026-09-04T06:30:00Z")))
                .isEqualTo("4 September at 12:00 PM");
        assertThat(TestSchedule.humanise(Instant.parse("2026-09-03T18:30:00Z")))
                .isEqualTo("4 September at 12:00 AM");
    }
}
