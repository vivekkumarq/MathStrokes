package com.mathstrokes.attempt.service;

import java.time.Instant;

import org.springframework.stereotype.Component;

/**
 * Finalises a single expired attempt.
 *
 * A separate bean so the scheduler's loop calls it through the proxy: each attempt gets its own
 * transaction inside SubmissionService, and one student's failure cannot roll back the sweep for
 * everybody else.
 */
@Component
public class ExpiredAttemptFinaliser {

    private final SubmissionService submissionService;

    public ExpiredAttemptFinaliser(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    public void finalise(Long attemptId, Instant now) {
        submissionService.finaliseExpired(attemptId, now);
    }
}
