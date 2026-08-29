package com.mathstrokes.exam.repository;

import java.util.List;

import com.mathstrokes.exam.entity.TestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestQuestionRepository extends JpaRepository<TestQuestion, Long> {

    @Query("""
            select tq from TestQuestion tq
            join fetch tq.question q
            left join fetch q.options
            where tq.test.id = :testId
            order by tq.questionOrder asc
            """)
    List<TestQuestion> findByTestWithQuestions(@Param("testId") Long testId);

    long countByTestId(Long testId);
}
