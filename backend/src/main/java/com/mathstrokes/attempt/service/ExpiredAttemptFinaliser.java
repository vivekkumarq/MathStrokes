package com.mathstrokes.attempt.service;

import java.time.Instant;

import com.mathstrokes.attempt.entity.TestAttempt;
import com.mathstrokes.attempt.repository.TestAttemptRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finalises a single expired attempt in its own transaction.
 *
 * A separate bean on purpose: calling this from inside the scheduler would bypass the proxy and
 * silently join the caller's transaction, which would mean one failing attempt rolled back the
 * whole sweep.
 */
@Component
public class ExpiredAttemptFinaliser {

    private final TestAttemptRepository attemptRepository;
    private final SubmissionService submissionService;

    public ExpiredAttemptFinaliser(TestAttemptRepository attemptRepository,
                                   SubmissionService submissionService) {
        this.attemptRepository = attemptRepository;
        this.submissionService = submissionService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finalise(Long attemptId, Instant now) {
        TestAttempt attempt = attemptRepository.findByIdWithTest(attemptId).orElse(null);
        if (attempt == null) {
            return;
        }
        submissionService.finalise(attempt, now);
    }
}
