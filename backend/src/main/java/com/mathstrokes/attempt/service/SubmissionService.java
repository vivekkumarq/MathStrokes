package com.mathstrokes.attempt.service;

import java.time.Instant;

import com.mathstrokes.attempt.dto.AttemptResultResponse;
import com.mathstrokes.attempt.entity.TestAttempt;
import com.mathstrokes.attempt.repository.TestAttemptRepository;
import com.mathstrokes.common.enums.AttemptStatus;
import com.mathstrokes.ranking.service.RankingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finalises an attempt and drives evaluation and ranking.
 *
 * Submission is idempotent by contract. Submitting an attempt that has already been finalised -
 * because the student double-clicked, because a retry arrived, or because the expiry sweep got
 * there first - returns the result rather than an error. That last case is the important one: a
 * student whose time runs out while their submit is in flight sees their marks, not a conflict.
 */
@Service
public class SubmissionService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionService.class);

    private final TestAttemptRepository attemptRepository;
    private final AttemptService attemptService;
    private final EvaluationService evaluationService;
    private final ResultService resultService;
    private final RankingService rankingService;

    public SubmissionService(TestAttemptRepository attemptRepository,
                             AttemptService attemptService,
                             EvaluationService evaluationService,
                             ResultService resultService,
                             RankingService rankingService) {
        this.attemptRepository = attemptRepository;
        this.attemptService = attemptService;
        this.evaluationService = evaluationService;
        this.resultService = resultService;
        this.rankingService = rankingService;
    }

    /** Student-initiated submission. */
    @Transactional
    public AttemptResultResponse submit(Long attemptId, Long studentId) {
        TestAttempt attempt = attemptService.requireOwnedAttempt(attemptId, studentId);
        finalise(attempt, Instant.now());
        return resultService.buildResult(attempt);
    }

    /**
     * Closes and scores the attempt.
     *
     * Whether the deadline had already passed decides only how the submission is recorded -
     * SUBMITTED or AUTO_SUBMITTED - never whether it is accepted. Either way the marks come from
     * answers already saved on the server, so nothing a student typed and had saved is lost.
     */
    @Transactional
    public TestAttempt finalise(TestAttempt attempt, Instant now) {
        if (attempt.getStatus() == AttemptStatus.EVALUATED) {
            return attempt;
        }

        if (attempt.getStatus() == AttemptStatus.ACTIVE) {
            boolean expired = attempt.hasExpired(now);
            attempt.setStatus(expired ? AttemptStatus.AUTO_SUBMITTED : AttemptStatus.SUBMITTED);
            // Clamp to the deadline so an attempt finalised by the sweep a few seconds late does
            // not report a time taken beyond the paper's own duration.
            attempt.setSubmittedAt(expired ? attempt.getExpiresAt() : now);
        }

        evaluationService.evaluate(attempt);
        attemptRepository.saveAndFlush(attempt);

        // Runs in its own transaction: a ranking failure must not undo a committed evaluation.
        try {
            rankingService.recomputeForTest(attempt.getTest().getId());
        } catch (RuntimeException ex) {
            log.error("Ranking failed for test {} after attempt {} was evaluated. "
                            + "The result stands; the leaderboard will correct on the next submission.",
                    attempt.getTest().getId(), attempt.getId(), ex);
        }
        return attempt;
    }
}
