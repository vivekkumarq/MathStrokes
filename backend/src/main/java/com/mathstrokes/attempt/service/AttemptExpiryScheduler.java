package com.mathstrokes.attempt.service;

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
 */
@Component
public class AttemptExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(AttemptExpiryScheduler.class);

    /** Bounded so one sweep cannot monopolise a connection if a large backlog builds up. */
    private static final int BATCH_SIZE = 50;

    private final TestAttemptRepository attemptRepository;
    private final ExpiredAttemptFinaliser finaliser;

    public AttemptExpiryScheduler(TestAttemptRepository attemptRepository,
                                  ExpiredAttemptFinaliser finaliser) {
        this.attemptRepository = attemptRepository;
        this.finaliser = finaliser;
    }

    @Scheduled(fixedDelayString = "${mathstrokes.app.exam.expiry-sweep-interval-ms:30000}")
    public void finaliseExpiredAttempts() {
        Instant now = Instant.now();
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
