package com.mathstrokes.attempt.repository;

import java.util.List;
import java.util.Optional;

import com.mathstrokes.attempt.entity.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Long> {

    @Query("""
            select distinct sa from StudentAnswer sa
            left join fetch sa.selectedOptions so
            left join fetch so.attemptQuestionOption
            where sa.attempt.id = :attemptId
            """)
    List<StudentAnswer> findByAttemptWithSelections(@Param("attemptId") Long attemptId);

    @Query("""
            select sa from StudentAnswer sa
            left join fetch sa.selectedOptions so
            left join fetch so.attemptQuestionOption
            where sa.attemptQuestion.id = :attemptQuestionId
            """)
    Optional<StudentAnswer> findByAttemptQuestion(@Param("attemptQuestionId") Long attemptQuestionId);
}
