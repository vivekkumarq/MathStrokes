package com.mathstrokes.exam.entity;

import java.time.Instant;

import com.mathstrokes.common.enums.TestKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The scheduling window on a class test.
 *
 * These are boundary conditions on a real classroom: a paper that opens a minute late, or refuses
 * a student a minute early, is a room full of people waiting. Every case here is the exact instant
 * of a bound rather than a comfortable hour either side of it.
 */
class ExamTestScheduleTest {

    private static final Instant OPENS = Instant.parse("2026-09-04T10:30:00Z");
    private static final Instant CLOSES = Instant.parse("2026-09-04T11:30:00Z");

    private ExamTest scheduled(Instant start, Instant end) {
        ExamTest test = new ExamTest();
        test.setTestKind(TestKind.CLASS_TEST);
        test.setScheduledStartAt(start);
        test.setScheduledEndAt(end);
        return test;
    }

    @Test
    @DisplayName("a paper with no window at all is always startable")
    void unscheduledIsAlwaysOpen() {
        ExamTest test = scheduled(null, null);
        assertThat(test.isWithinSchedule(Instant.EPOCH)).isTrue();
        assertThat(test.isWithinSchedule(Instant.parse("2099-01-01T00:00:00Z"))).isTrue();
    }

    @Test
    @DisplayName("the opening instant itself is inside the window, not before it")
    void opensOnTheBoundary() {
        ExamTest test = scheduled(OPENS, CLOSES);
        assertThat(test.hasOpenedBy(OPENS.minusMillis(1))).isFalse();
        assertThat(test.hasOpenedBy(OPENS)).isTrue();
        assertThat(test.isWithinSchedule(OPENS)).isTrue();
    }

    @Test
    @DisplayName("the closing instant itself is still inside the window")
    void closesOnTheBoundary() {
        ExamTest test = scheduled(OPENS, CLOSES);
        assertThat(test.hasWindowClosedBy(CLOSES)).isFalse();
        assertThat(test.isWithinSchedule(CLOSES)).isTrue();
        assertThat(test.hasWindowClosedBy(CLOSES.plusMillis(1))).isTrue();
        assertThat(test.isWithinSchedule(CLOSES.plusMillis(1))).isFalse();
    }

    @Test
    @DisplayName("either bound may stand alone")
    void eitherBoundMayStandAlone() {
        ExamTest opensOnly = scheduled(OPENS, null);
        assertThat(opensOnly.isWithinSchedule(OPENS.minusSeconds(1))).isFalse();
        assertThat(opensOnly.isWithinSchedule(OPENS.plusSeconds(86_400))).isTrue();

        ExamTest closesOnly = scheduled(null, CLOSES);
        assertThat(closesOnly.isWithinSchedule(Instant.EPOCH)).isTrue();
        assertThat(closesOnly.isWithinSchedule(CLOSES.plusSeconds(1))).isFalse();
    }

    @Test
    @DisplayName("a window shorter than the paper is legal - it gates starting, not finishing")
    void aShortWindowIsAStartSlot() {
        // "Begin any time in the next ten minutes, then you get your full hour" is a real way to
        // run a class test, so this must not be rejected as a misconfiguration.
        ExamTest test = scheduled(OPENS, OPENS.plusSeconds(600));
        test.setDurationMinutes(60);
        assertThat(test.isWithinSchedule(OPENS.plusSeconds(599))).isTrue();
    }

    @Test
    @DisplayName("a practice test defaults to no window and is not a class test")
    void practiceIsTheDefault() {
        ExamTest test = new ExamTest();
        assertThat(test.isClassTest()).isFalse();
        assertThat(test.getScheduledStartAt()).isNull();
        assertThat(test.getScheduledEndAt()).isNull();
        assertThat(test.isWithinSchedule(Instant.now())).isTrue();
    }
}
