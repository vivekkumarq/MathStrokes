package com.mathstrokes.attempt.service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

/**
 * The latest deadline of any attempt that may still be in progress.
 *
 * Exists so the expiry sweep can tell whether an exam is running without asking the database.
 * The database is serverless and metered by the hour it spends awake; a sweep that queries it
 * every thirty seconds keeps it awake permanently and exhausted the free allowance in under a
 * month. With this, the sweep only queries while some attempt's deadline has not yet passed.
 *
 * The value only ever moves forward. A deadline is written once and never extended, so the
 * latest one known is a safe upper bound on when sweeping can stop, and raising it can never
 * cause an attempt to be missed - including when a start and a sweep race each other.
 *
 * Deliberately standalone: AttemptService must report new deadlines here, and it cannot
 * depend on the scheduler, which reaches back to AttemptService through the finaliser.
 */
@Component
public class AttemptDeadlineTracker {

    private final AtomicReference<Instant> latest = new AtomicReference<>(Instant.EPOCH);

    /** Records a deadline; only ever raises the value, never lowers it. */
    public void cover(Instant deadline) {
        if (deadline != null) {
            latest.accumulateAndGet(deadline, (a, b) -> a.isAfter(b) ? a : b);
        }
    }

    public Instant latest() {
        return latest.get();
    }
}
