package com.mathstrokes.question.repository;

import com.mathstrokes.common.enums.Difficulty;
import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.QuestionStatus;
import com.mathstrokes.common.enums.QuestionType;
import com.mathstrokes.question.entity.Question;
import org.springframework.data.jpa.domain.Specification;

/**
 * Composable filters for the admin question grid. Kept as specifications rather than a growing
 * pile of derived query methods so any combination of filters costs one indexed query.
 */
public final class QuestionSpecifications {

    private QuestionSpecifications() {
    }

    public static Specification<Question> subjectIs(Long subjectId) {
        return subjectId == null ? null
                : (root, query, cb) -> cb.equal(root.get("subject").get("id"), subjectId);
    }

    public static Specification<Question> chapterIs(Long chapterId) {
        return chapterId == null ? null
                : (root, query, cb) -> cb.equal(root.get("chapter").get("id"), chapterId);
    }

    public static Specification<Question> examPatternIs(ExamPattern examPattern) {
        return examPattern == null ? null
                : (root, query, cb) -> cb.equal(root.get("examPattern"), examPattern);
    }

    public static Specification<Question> difficultyIs(Difficulty difficulty) {
        return difficulty == null ? null
                : (root, query, cb) -> cb.equal(root.get("difficulty"), difficulty);
    }

    public static Specification<Question> questionTypeIs(QuestionType questionType) {
        return questionType == null ? null
                : (root, query, cb) -> cb.equal(root.get("questionType"), questionType);
    }

    public static Specification<Question> statusIs(QuestionStatus status) {
        return status == null ? null
                : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /** Case-insensitive contains over the LaTeX source of the question and its solution. */
    public static Specification<Question> textContains(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("questionContent")), pattern),
                cb.like(cb.lower(root.get("solutionContent")), pattern));
    }

    /** Combines the non-null specifications with AND. */
    @SafeVarargs
    public static Specification<Question> allOf(Specification<Question>... specifications) {
        Specification<Question> combined = null;
        for (Specification<Question> specification : specifications) {
            if (specification == null) {
                continue;
            }
            combined = combined == null ? specification : combined.and(specification);
        }
        return combined;
    }
}
