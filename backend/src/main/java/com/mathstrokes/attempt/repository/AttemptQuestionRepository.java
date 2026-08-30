package com.mathstrokes.attempt.repository;

import java.util.List;
import java.util.Optional;

import com.mathstrokes.attempt.entity.AttemptQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttemptQuestionRepository extends JpaRepository<AttemptQuestion, Long> {

    @Query("""
            select distinct aq from AttemptQuestion aq
            left join fetch aq.options
            where aq.attempt.id = :attemptId
            order by aq.questionOrder asc
            """)
    List<AttemptQuestion> findByAttemptWithOptions(@Param("attemptId") Long attemptId);

    @Query("""
            select aq from AttemptQuestion aq
            left join fetch aq.options
            where aq.id = :id
            """)
    Optional<AttemptQuestion> findByIdWithOptions(@Param("id") Long id);
}
