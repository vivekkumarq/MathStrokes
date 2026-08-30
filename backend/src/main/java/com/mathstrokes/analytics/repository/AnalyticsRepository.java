package com.mathstrokes.analytics.repository;

import java.util.List;

import com.mathstrokes.attempt.entity.QuestionAttemptResult;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Aggregate reads for the analytics screens.
 *
 * These are grouped aggregations, not row fetches: a dashboard costs a handful of indexed
 * GROUP BY queries rather than pulling attempts into memory and counting them in Java.
 */
public interface AnalyticsRepository extends JpaRepository<QuestionAttemptResult, Long> {

    /**
     * Per-question outcome tallies across every attempt that has used the question.
     *
     * Columns: questionId, timesShown, correct, partiallyCorrect, incorrect, unanswered.
     * Ordered by measured accuracy ascending, so the hardest questions surface first. Questions
     * with too few attempts to say anything meaningful are filtered out rather than ranked on noise.
     */
    @Query(value = """
            SELECT r.question_id                                                 AS question_id,
                   COUNT(*)                                                      AS times_shown,
                   COUNT(*) FILTER (WHERE r.result_status = 'CORRECT')           AS correct_count,
                   COUNT(*) FILTER (WHERE r.result_status = 'PARTIALLY_CORRECT') AS partial_count,
                   COUNT(*) FILTER (WHERE r.result_status = 'INCORRECT')         AS incorrect_count,
                   COUNT(*) FILTER (WHERE r.result_status = 'UNANSWERED')        AS unanswered_count
            FROM question_attempt_results r
            JOIN questions q ON q.id = r.question_id
            WHERE (:chapterId IS NULL OR q.chapter_id = :chapterId)
            GROUP BY r.question_id
            HAVING COUNT(*) FILTER (WHERE r.result_status <> 'UNANSWERED') >= :minimumAttempts
            ORDER BY (COUNT(*) FILTER (WHERE r.result_status = 'CORRECT'))::numeric
                     / NULLIF(COUNT(*) FILTER (WHERE r.result_status <> 'UNANSWERED'), 0) ASC
            """, nativeQuery = true)
    List<Object[]> questionOutcomeTallies(@Param("chapterId") Long chapterId,
                                          @Param("minimumAttempts") int minimumAttempts,
                                          Pageable pageable);

    /**
     * Average performance per chapter over evaluated attempts.
     * Columns: chapterId, chapterName, attemptCount, averageScore, averageAccuracy.
     * Passing a studentId narrows it to that student; null gives the platform-wide picture.
     */
    @Query(value = """
            SELECT c.id                      AS chapter_id,
                   c.name                    AS chapter_name,
                   COUNT(a.id)               AS attempt_count,
                   ROUND(AVG(a.score), 2)    AS average_score,
                   ROUND(AVG(a.accuracy), 2) AS average_accuracy
            FROM test_attempts a
            JOIN tests t    ON t.id = a.test_id
            JOIN chapters c ON c.id = t.chapter_id
            WHERE a.status = 'EVALUATED'
              AND (:studentId IS NULL OR a.student_id = :studentId)
            GROUP BY c.id, c.name
            ORDER BY c.name ASC
            """, nativeQuery = true)
    List<Object[]> chapterPerformance(@Param("studentId") Long studentId);
}
