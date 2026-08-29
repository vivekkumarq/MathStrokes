package com.mathstrokes.attempt.repository;

import java.util.List;

import com.mathstrokes.attempt.entity.QuestionAttemptResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionAttemptResultRepository extends JpaRepository<QuestionAttemptResult, Long> {

    @Query("""
            select r from QuestionAttemptResult r
            join fetch r.attemptQuestion aq
            where r.attempt.id = :attemptId
            order by aq.questionOrder asc
            """)
    List<QuestionAttemptResult> findByAttempt(@Param("attemptId") Long attemptId);

    boolean existsByAttemptId(Long attemptId);

    long countByAttemptId(Long attemptId);
}
