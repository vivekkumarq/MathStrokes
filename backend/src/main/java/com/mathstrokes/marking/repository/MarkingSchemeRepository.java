package com.mathstrokes.marking.repository;

import java.util.List;
import java.util.Optional;

import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.QuestionType;
import com.mathstrokes.marking.entity.MarkingScheme;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarkingSchemeRepository extends JpaRepository<MarkingScheme, Long> {

    Optional<MarkingScheme> findByExamPatternAndQuestionTypeAndActiveTrue(
            ExamPattern examPattern, QuestionType questionType);

    Optional<MarkingScheme> findByName(String name);

    List<MarkingScheme> findAllByOrderByExamPatternAscQuestionTypeAscNameAsc();
}
