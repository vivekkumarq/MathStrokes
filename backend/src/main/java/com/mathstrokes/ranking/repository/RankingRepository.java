package com.mathstrokes.ranking.repository;

import com.mathstrokes.attempt.entity.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Ranking is computed in the database with window functions, in one pass over the cohort.
 * Doing it in Java would mean loading every attempt of a test into memory to place one student.
 */
public interface RankingRepository extends JpaRepository<TestAttempt, Long> {

    /**
     * Clears the ranking columns for a test before they are recomputed, so an attempt that is no
     * longer a student's best does not keep a stale rank.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE test_attempts
            SET rank_position = NULL, total_candidates = NULL, percentile = NULL
            WHERE test_id = :testId
            """, nativeQuery = true)
    int clearRankings(@Param("testId") Long testId);

    /**
     * Recomputes the whole cohort for one test. Called after each evaluation, so a leaderboard is
     * never stale, and cheap enough to do that: one indexed pass over the test's attempts.
     *
     * best   - one row per student, their highest-placing evaluated attempt. Only that row is
     *          ranked, so sitting a test twice cannot occupy two leaderboard positions.
     * ranked - final_rank uses RANK() over the full tie-break chain, so genuinely tied students
     *          share a position and the next rank skips accordingly: 1, 2, 2, 4.
     *          score_rank is a separate score-only RANK, because the percentile is defined on raw
     *          score and must not be split by a tie-break on time.
     *
     * percentile = 100 x (candidates scoring at or below you) / total candidates,
     *              which places the topper at 100.00.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            WITH best AS (
                SELECT a.id,
                       a.student_id,
                       a.score,
                       a.correct_count,
                       a.incorrect_count,
                       COALESCE(a.time_taken_seconds, 2147483647) AS time_taken,
                       a.submitted_at,
                       ROW_NUMBER() OVER (
                           PARTITION BY a.student_id
                           ORDER BY a.score DESC,
                                    a.correct_count DESC,
                                    a.incorrect_count ASC,
                                    COALESCE(a.time_taken_seconds, 2147483647) ASC,
                                    a.submitted_at ASC,
                                    a.id ASC
                       ) AS student_attempt_rank
                FROM test_attempts a
                WHERE a.test_id = :testId
                  AND a.status = 'EVALUATED'
                  AND a.score IS NOT NULL
            ),
            ranked AS (
                SELECT b.id,
                       RANK() OVER (ORDER BY b.score DESC) AS score_rank,
                       RANK() OVER (
                           ORDER BY b.score DESC,
                                    b.correct_count DESC,
                                    b.incorrect_count ASC,
                                    b.time_taken ASC,
                                    b.submitted_at ASC
                       ) AS final_rank,
                       COUNT(*) OVER () AS cohort_size
                FROM best b
                WHERE b.student_attempt_rank = 1
            )
            UPDATE test_attempts t
            SET rank_position    = r.final_rank,
                total_candidates = r.cohort_size,
                percentile       = ROUND(
                    (100.0 * (r.cohort_size - r.score_rank + 1)) / r.cohort_size, 2)
            FROM ranked r
            WHERE t.id = r.id
            """, nativeQuery = true)
    int recomputeRankings(@Param("testId") Long testId);
}
