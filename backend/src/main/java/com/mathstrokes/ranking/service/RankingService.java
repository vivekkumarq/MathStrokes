package com.mathstrokes.ranking.service;

import java.util.List;

import com.mathstrokes.attempt.entity.TestAttempt;
import com.mathstrokes.attempt.repository.TestAttemptRepository;
import com.mathstrokes.exam.entity.ExamTest;
import com.mathstrokes.exam.service.TestService;
import com.mathstrokes.ranking.dto.LeaderboardEntryResponse;
import com.mathstrokes.ranking.dto.LeaderboardResponse;
import com.mathstrokes.ranking.repository.RankingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps every test's leaderboard current.
 *
 * Policy: rankings are recomputed for the whole cohort each time an attempt is evaluated, rather
 * than frozen when the test closes. A student who finishes first therefore sees a real position
 * immediately, and that position may move as later candidates finish - which is honest, and is
 * what "rank among everyone who sat this paper" has to mean while the paper is still open.
 * Once the test is CLOSED no new attempts can start, so the board settles on its own.
 *
 * Only FIXED_SET tests are ranked. A randomly drawn paper differs per student, so placing two
 * such attempts on one board would compare different examinations.
 */
@Service
public class RankingService {

    private static final Logger log = LoggerFactory.getLogger(RankingService.class);

    private final RankingRepository rankingRepository;
    private final TestAttemptRepository attemptRepository;
    private final TestService testService;

    public RankingService(RankingRepository rankingRepository,
                          TestAttemptRepository attemptRepository,
                          TestService testService) {
        this.rankingRepository = rankingRepository;
        this.attemptRepository = attemptRepository;
        this.testService = testService;
    }

    /**
     * Runs in its own transaction so a ranking hiccup can never roll back an evaluation that has
     * already been committed. A student's marks are worth more than their position.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recomputeForTest(Long testId) {
        ExamTest test = testService.requireTest(testId);
        if (!test.isRankingEnabled()) {
            log.debug("Test {} draws a different paper per attempt; skipping ranking", testId);
            return;
        }
        rankingRepository.clearRankings(testId);
        int ranked = rankingRepository.recomputeRankings(testId);
        log.debug("Recomputed rankings for test {}: {} candidate(s) placed", testId, ranked);
    }

    @Transactional(readOnly = true)
    public LeaderboardResponse leaderboard(Long testId, Long viewingStudentId) {
        ExamTest test = testService.requireTest(testId);
        List<TestAttempt> attempts = attemptRepository.findEvaluatedByTest(testId);
        List<LeaderboardEntryResponse> entries = attempts.stream()
                .filter(attempt -> attempt.getRankPosition() != null)
                .map(attempt -> new LeaderboardEntryResponse(
                        attempt.getRankPosition(),
                        attempt.getStudent().getFullName(),
                        attempt.getScore(),
                        attempt.getMaxScore(),
                        attempt.getCorrectCount(),
                        attempt.getIncorrectCount(),
                        attempt.getTimeTakenSeconds(),
                        attempt.getPercentile(),
                        viewingStudentId != null
                                && attempt.getStudent().getId().equals(viewingStudentId)))
                .toList();
        return new LeaderboardResponse(test.getId(), test.getTitle(), test.isRankingEnabled(),
                entries.size(), entries);
    }
}
