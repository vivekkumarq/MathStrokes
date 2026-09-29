package com.mathstrokes.attempt.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.mathstrokes.attempt.repository.TestAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The sweep must stay out of the database when no exam can be running - that is what lets a
 * metered serverless database sleep - without ever missing an attempt that is still in progress.
 */
class AttemptExpirySchedulerTest {

    private TestAttemptRepository repository;
    private AttemptDeadlineTracker deadlines;
    private AttemptExpiryScheduler scheduler;

    @BeforeEach
    void setUp() {
        repository = mock(TestAttemptRepository.class);
        deadlines = new AttemptDeadlineTracker();
        scheduler = new AttemptExpiryScheduler(repository, mock(ExpiredAttemptFinaliser.class), deadlines);
        when(repository.findExpiredActiveAttemptIds(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());
    }

    @Test
    @DisplayName("with no exam in progress, the sweep asks once at boot and then stays silent")
    void idleNeverSweeps() {
        when(repository.findLatestActiveDeadline()).thenReturn(null);

        scheduler.finaliseExpiredAttempts();
        scheduler.finaliseExpiredAttempts();
        scheduler.finaliseExpiredAttempts();

        verify(repository, times(1)).findLatestActiveDeadline();
        verify(repository, never()).findExpiredActiveAttemptIds(any(), any());
    }

    @Test
    @DisplayName("a newly started attempt wakes the sweep")
    void newAttemptWakesSweep() {
        when(repository.findLatestActiveDeadline()).thenReturn(null);
        scheduler.finaliseExpiredAttempts();

        deadlines.cover(Instant.now().plus(Duration.ofMinutes(30)));
        scheduler.finaliseExpiredAttempts();

        verify(repository, times(1)).findExpiredActiveAttemptIds(any(), any());
    }

    @Test
    @DisplayName("an attempt started before a restart is still swept after it")
    void attemptFromBeforeRestartIsCovered() {
        when(repository.findLatestActiveDeadline()).thenReturn(Instant.now().plus(Duration.ofMinutes(20)));

        scheduler.finaliseExpiredAttempts();

        verify(repository, times(1)).findExpiredActiveAttemptIds(any(), any());
    }

    @Test
    @DisplayName("sweeping continues through the grace period, then stops")
    void stopsOnlyAfterGrace() {
        when(repository.findLatestActiveDeadline()).thenReturn(null);
        scheduler.finaliseExpiredAttempts();

        // Deadline just passed: still inside the grace period, so the attempt gets finalised.
        deadlines.cover(Instant.now().minus(Duration.ofSeconds(30)));
        scheduler.finaliseExpiredAttempts();
        verify(repository, times(1)).findExpiredActiveAttemptIds(any(), any());

        // A fresh tracker whose only deadline is well past the grace period: silent.
        AttemptDeadlineTracker stale = new AttemptDeadlineTracker();
        stale.cover(Instant.now().minus(AttemptExpiryScheduler.GRACE).minus(Duration.ofMinutes(1)));
        TestAttemptRepository quiet = mock(TestAttemptRepository.class);
        AttemptExpiryScheduler done = new AttemptExpiryScheduler(quiet, mock(ExpiredAttemptFinaliser.class), stale);
        done.finaliseExpiredAttempts();
        verify(quiet, never()).findExpiredActiveAttemptIds(any(), any());
    }

    @Test
    @DisplayName("the tracked deadline only moves forward, so an earlier report cannot shorten sweeping")
    void trackerNeverMovesBackwards() {
        Instant later = Instant.now().plus(Duration.ofHours(2));
        deadlines.cover(later);
        deadlines.cover(Instant.now().plus(Duration.ofMinutes(5)));
        deadlines.cover(null);

        assertThat(deadlines.latest()).isEqualTo(later);
    }
}
