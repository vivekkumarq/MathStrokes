package com.mathstrokes.attempt.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.mathstrokes.attempt.entity.TestAttempt;
import com.mathstrokes.common.enums.AttemptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {

    // The chapter fetches below are LEFT joins because a full-syllabus test has no chapter. An
    // inner join here does not error - it silently returns no row, which surfaces much later as
    // "attempt not found" on a perfectly valid attempt. The subject joins stay inner: subject is
    // still mandatory.


    @Query("""
            select a from TestAttempt a
            join fetch a.test t
            left join fetch t.chapter c
            join fetch t.subject s
            where a.id = :id
            """)
    Optional<TestAttempt> findByIdWithTest(@Param("id") Long id);

    @Query("""
            select a from TestAttempt a
            join fetch a.test t
            left join fetch t.chapter
            join fetch t.subject
            where a.student.id = :studentId
              and a.status = com.mathstrokes.common.enums.AttemptStatus.ACTIVE
            order by a.startedAt desc
            """)
    List<TestAttempt> findActiveByStudent(@Param("studentId") Long studentId);

    @Query("""
            select a from TestAttempt a
            where a.student.id = :studentId and a.test.id = :testId
              and a.status = com.mathstrokes.common.enums.AttemptStatus.ACTIVE
            """)
    Optional<TestAttempt> findActiveByStudentAndTest(@Param("studentId") Long studentId,
                                                     @Param("testId") Long testId);

    @Query("""
            select a from TestAttempt a
            join fetch a.test t
            left join fetch t.chapter
            join fetch t.subject
            where a.student.id = :studentId
            """)
    Page<TestAttempt> findByStudent(@Param("studentId") Long studentId, Pageable pageable);

    long countByStudentIdAndTestId(Long studentId, Long testId);

    /**
     * How many attempts this student has made against each test they have ever touched.
     * Columns: testId, attemptCount.
     *
     * Exists so the student catalogue can ask once instead of once per test. Listing 62 tests
     * through countByStudentIdAndTestId is 62 round trips to answer a question one GROUP BY
     * answers, on the first screen every student opens.
     *
     * Tests the student has never attempted are absent rather than zero, so callers must treat
     * a missing key as none - which is what getOrDefault is for.
     */
    @Query("""
            select a.test.id, count(a)
            from TestAttempt a
            where a.student.id = :studentId
            group by a.test.id
            """)
    List<Object[]> attemptCountsByTest(@Param("studentId") Long studentId);

    /**
     * Attempts whose clock has run out but which are still open. Drives the finalisation sweep.
     * Ordered oldest first so a backlog is cleared in the order it built up.
     */
    @Query("""
            select a.id from TestAttempt a
            where a.status = com.mathstrokes.common.enums.AttemptStatus.ACTIVE
              and a.expiresAt <= :now
            order by a.expiresAt asc
            """)
    List<Long> findExpiredActiveAttemptIds(@Param("now") Instant now, Pageable pageable);

    long countByStatus(AttemptStatus status);

    @Query("select count(a) from TestAttempt a where a.startedAt >= :since")
    long countStartedSince(@Param("since") Instant since);

    @Query("""
            select a from TestAttempt a
            join fetch a.student
            join fetch a.test t
            where t.id = :testId
              and a.status = com.mathstrokes.common.enums.AttemptStatus.EVALUATED
            order by a.rankPosition asc nulls last, a.score desc
            """)
    List<TestAttempt> findEvaluatedByTest(@Param("testId") Long testId);
}
