package com.mathstrokes.attempt.service;

import java.time.Instant;

import com.mathstrokes.attempt.entity.TestAttempt;
import com.mathstrokes.attempt.repository.TestAttemptRepository;
import com.mathstrokes.common.enums.AttemptStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Closes an attempt and scores it, in one transaction.
 *
 * Deliberately does NOT touch rankings. Ranking rewrites rows in test_attempts, including the row
 * this transaction has just locked, so running it here would have the nested transaction wait on
 * a lock the outer one only releases at commit - a self-deadlock that blocks until the pool times
 * out. Ranking therefore runs after this commits; see SubmissionService.
 *
 * @return the id of the test the attempt belongs to, so the caller can rank that cohort
 */
@Service
public class AttemptFinalisationService {

    private final TestAttemptRepository attemptRepository;
    private final EvaluationService evaluationService;

    public AttemptFinalisationService(TestAttemptRepository attemptRepository,
                                      EvaluationService evaluationService) {
        this.attemptRepository = attemptRepository;
        this.evaluationService = evaluationService;
    }

    @Transactional
    public Long finalise(Long attemptId, Instant now) {
        TestAttempt attempt = attemptRepository.findByIdWithTest(attemptId).orElse(null);
        if (attempt == null) {
            return null;
        }
        if (attempt.getStatus() == AttemptStatus.ACTIVE) {
            boolean expired = attempt.hasExpired(now);
            AttemptStatus target = expired ? AttemptStatus.AUTO_SUBMITTED : AttemptStatus.SUBMITTED;
            if (!attempt.getStatus().canTransitionTo(target)) {
                throw new IllegalStateException("Cannot move attempt " + attemptId + " from "
                        + attempt.getStatus() + " to " + target);
            }
            attempt.setStatus(target);
            // Clamp to the deadline, so an attempt the sweep finalises a few seconds late does
            // not report a time taken beyond the paper's own duration.
            attempt.setSubmittedAt(expired ? attempt.getExpiresAt() : now);
        }
        evaluationService.evaluate(attempt);
        attemptRepository.save(attempt);
        return attempt.getTest().getId();
    }
}
