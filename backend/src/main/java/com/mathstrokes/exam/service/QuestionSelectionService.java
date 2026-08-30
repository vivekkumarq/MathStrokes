package com.mathstrokes.exam.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mathstrokes.common.enums.Difficulty;
import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.exception.BusinessRuleException;
import com.mathstrokes.common.exception.ErrorCode;
import com.mathstrokes.question.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Draws a question set from the published bank against a test blueprint.
 *
 * Used in two places: once at publish time for a FIXED_SET test, and once per attempt for a
 * RANDOM_PER_ATTEMPT test. Either way the result is written down immediately, so a refresh can
 * never redraw the paper.
 */
@Service
public class QuestionSelectionService {

    /** PostgreSQL rejects an empty IN list, so the exclusion set always carries this sentinel. */
    private static final List<Long> NO_EXCLUSIONS = List.of(-1L);

    private final QuestionRepository questionRepository;

    public QuestionSelectionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    /**
     * @param blueprint what the test asks for
     * @return exactly {@code blueprint.totalQuestions()} question ids, in draw order
     * @throws BusinessRuleException with NOT_ENOUGH_QUESTIONS when the bank cannot satisfy the
     *         blueprint. Failing loudly is deliberate: silently handing a student a short paper
     *         would corrupt both their score and the ranking cohort.
     */
    @Transactional(readOnly = true)
    public List<Long> selectQuestionIds(Blueprint blueprint) {
        Set<Long> chosen = new LinkedHashSet<>();

        // Satisfy the explicit difficulty bands first, then top up with anything published.
        for (Map.Entry<Difficulty, Integer> band : blueprint.difficultyBands().entrySet()) {
            int wanted = band.getValue();
            if (wanted <= 0) {
                continue;
            }
            List<Long> drawn = draw(blueprint, band.getKey(), wanted, chosen);
            if (drawn.size() < wanted) {
                throw shortfall(blueprint, band.getKey(), wanted, drawn.size());
            }
            chosen.addAll(drawn);
        }

        int remaining = blueprint.totalQuestions() - chosen.size();
        if (remaining > 0) {
            List<Long> drawn = draw(blueprint, null, remaining, chosen);
            if (drawn.size() < remaining) {
                throw shortfall(blueprint, null, blueprint.totalQuestions(),
                        chosen.size() + drawn.size());
            }
            chosen.addAll(drawn);
        }

        return List.copyOf(chosen);
    }

    /** How many published questions back a blueprint, so the admin UI can warn before publishing. */
    @Transactional(readOnly = true)
    public long countAvailable(Long chapterId, ExamPattern examPattern, Difficulty difficulty) {
        return questionRepository.countPublished(chapterId, examPattern, difficulty);
    }

    private List<Long> draw(Blueprint blueprint, Difficulty difficulty, int wanted,
                            Set<Long> alreadyChosen) {
        List<Long> exclusions = alreadyChosen.isEmpty() ? NO_EXCLUSIONS : new ArrayList<>(alreadyChosen);
        return questionRepository.pickRandomPublishedIds(
                blueprint.chapterId(),
                blueprint.examPattern().name(),
                difficulty == null ? null : difficulty.name(),
                exclusions,
                wanted);
    }

    private BusinessRuleException shortfall(Blueprint blueprint, Difficulty difficulty,
                                            int wanted, int found) {
        String band = difficulty == null ? "" : " at " + difficulty + " difficulty";
        String scope = blueprint.chapterId() == null ? " across the syllabus" : " in this chapter";
        return new BusinessRuleException(ErrorCode.NOT_ENOUGH_QUESTIONS,
                "The question bank holds only " + found + " published "
                        + blueprint.examPattern() + " question(s)" + band + scope
                        + ", but " + wanted + " are needed. "
                        + "Publish more questions before using this test.");
    }

    /**
     * @param chapterId       the chapter to draw from, or null for the whole syllabus
     * @param difficultyBands how many questions to take from each band; bands summing to less
     *                        than the total are topped up from the whole published pool
     */
    public record Blueprint(Long chapterId,
                            ExamPattern examPattern,
                            int totalQuestions,
                            Map<Difficulty, Integer> difficultyBands) {

        public Blueprint {
            difficultyBands = difficultyBands == null ? Map.of() : Map.copyOf(difficultyBands);
            int banded = difficultyBands.values().stream().mapToInt(Integer::intValue).sum();
            if (banded > totalQuestions) {
                throw new BusinessRuleException(
                        "The difficulty split adds up to " + banded + " questions, which is more "
                                + "than the " + totalQuestions + " this test is meant to have.");
            }
        }
    }
}
