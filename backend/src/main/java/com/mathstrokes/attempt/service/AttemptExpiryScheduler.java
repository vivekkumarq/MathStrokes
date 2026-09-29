package com.mathstrokes.attempt.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.mathstrokes.attempt.repository.TestAttemptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Finalises attempts whose time has run out.
 *
 * The browser is not trusted to submit on expiry: a student who closes the tab, loses power or
 * simply walks away must still be scored on what they had saved. This sweep is what guarantees
 * that, and it is also what stops abandoned attempts blocking a retake through the one-active-
 * attempt rule.
 *
 * Expiry itself is enforced synchronously on every answer write, so the sweep is a safety net
 * for cleanup rather than the mechanism that stops late answers.
 *
 * The sweep only touches the database while an exam can still be in progress. It used to query
 * every thirty seconds unconditionally, which kept the serverless database awake around the
 * clock and spent its whole free monthly compute allowance - taking the site down on 29 Sep.
 * Now it stays silent once every known deadline, plus a grace period, has passed, and wakes
 * again the moment a new attempt starts.
 */
@Component
public class AttemptExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(AttemptExpiryScheduler.class);

    /** Bounded so one sweep cannot monopolise a connection if a large backlog builds up. */
    private static final int BATCH_SIZE = 50;

    /**
     * How long to keep sweeping after the last known deadline. Several sweep intervals, so at
     * least one sweep runs after every deadline even if one fails, and a class of a few hundred
     * finishing together still clears in BATCH_SIZE chunks before sweeping stops.
     */
    static final Duration GRACE = Duration.ofMinutes(5);

    private final TestAttemptRepository attemptRepository;
    private final ExpiredAttemptFinaliser finaliser;
    private final AttemptDeadlineTracker deadlines;

    // ponytail: per-instance state. Correct while iota runs as one instance; a multi-instance
    // deployment would need the latest deadline in a shared store, or each node to load it.
    private volatile boolean loaded;

    public AttemptExpiryScheduler(TestAttemptRepository attemptRepository,
                                  ExpiredAttemptFinaliser finaliser,
                                  AttemptDeadlineTracker deadlines) {
        this.attemptRepository = attemptRepository;
        this.finaliser = finaliser;
        this.deadlines = deadlines;
    }

    @Scheduled(fixedDelayString = "${mathstrokes.app.exam.expiry-sweep-interval-ms:30000}")
    public void finaliseExpiredAttempts() {
        Instant now = Instant.now();

        // Once per boot: attempts started before a restart are not in memory, so ask the
        // database for the latest deadline among them. A student mid-exam during a redeploy is
        // still finalised on time. If this throws, loaded stays false and the next tick retries.
        if (!loaded) {
            deadlines.cover(attemptRepository.findLatestActiveDeadline());
            loaded = true;
        }

        if (now.isAfter(deadlines.latest().plus(GRACE))) {
            return;
        }

        List<Long> expired = attemptRepository.findExpiredActiveAttemptIds(
                now, PageRequest.of(0, BATCH_SIZE));
        if (expired.isEmpty()) {
            return;
        }

        log.info("Finalising {} attempt(s) whose time has expired", expired.size());
        for (Long attemptId : expired) {
            try {
                finaliser.finalise(attemptId, now);
            } catch (RuntimeException ex) {
                // One bad attempt must not stall the queue for everyone else.
                log.error("Could not finalise expired attempt {}", attemptId, ex);
            }
        }
    }
}
