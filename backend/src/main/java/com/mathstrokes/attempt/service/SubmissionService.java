package com.mathstrokes.attempt.service;

import java.time.Instant;

import com.mathstrokes.attempt.dto.AttemptResultResponse;
import com.mathstrokes.ranking.service.RankingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates submission: close and score, then rank, then read the result back.
 *
 * Deliberately NOT transactional. The three steps have to commit in sequence, because ranking
 * updates the very row the evaluation locks - running them in one nested arrangement deadlocks
 * against ourselves. Splitting them also means a ranking failure leaves a committed, correct
 * result behind rather than rolling a student's marks back.
 *
 * Submission is idempotent by contract. Submitting an attempt that has already been finalised -
 * because the student double-clicked, because a retry arrived, or because the expiry sweep got
 * there first - returns the result rather than an error. That last case is the important one: a
 * student whose time runs out while their submit is in flight sees their marks, not a conflict.
 */
@Service
public class SubmissionService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionService.class);

    private final AttemptService attemptService;
    private final AttemptFinalisationService finalisationService;
    private final ResultService resultService;
    private final RankingService rankingService;

    public SubmissionService(AttemptService attemptService,
                             AttemptFinalisationService finalisationService,
                             ResultService resultService,
                             RankingService rankingService) {
        this.attemptService = attemptService;
        this.finalisationService = finalisationService;
        this.resultService = resultService;
        this.rankingService = rankingService;
    }

    public AttemptResultResponse submit(Long attemptId, Long studentId) {
        // Ownership is proved before anything is written; a foreign attempt never reaches here.
        attemptService.requireOwnedAttempt(attemptId, studentId);

        Long testId = finalisationService.finalise(attemptId, Instant.now());
        if (testId != null) {
            rankCohortQuietly(testId, attemptId);
        }
        return resultService.getResult(attemptId, studentId);
    }

    /**
     * Called by the expiry sweep, which has already resolved the attempt and has no student in
     * context.
     */
    public void finaliseExpired(Long attemptId, Instant now) {
        Long testId = finalisationService.finalise(attemptId, now);
        if (testId != null) {
            rankCohortQuietly(testId, attemptId);
        }
    }

    /**
     * A student's marks are worth more than their position on a leaderboard, so a ranking failure
     * is logged and swallowed. The next submission on the same test recomputes the whole cohort
     * and repairs it.
     */
    private void rankCohortQuietly(Long testId, Long attemptId) {
        try {
            rankingService.recomputeForTest(testId);
        } catch (RuntimeException ex) {
            log.error("Ranking failed for test {} after attempt {} was evaluated. The result "
                            + "stands; the leaderboard will correct on the next submission.",
                    testId, attemptId, ex);
        }
    }
}
