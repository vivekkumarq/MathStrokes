package com.mathstrokes.attempt.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mathstrokes.attempt.entity.AttemptQuestion;
import com.mathstrokes.attempt.entity.QuestionAttemptResult;
import com.mathstrokes.attempt.entity.StudentAnswer;
import com.mathstrokes.attempt.entity.TestAttempt;
import com.mathstrokes.attempt.repository.AttemptQuestionRepository;
import com.mathstrokes.attempt.repository.QuestionAttemptResultRepository;
import com.mathstrokes.common.enums.AttemptStatus;
import com.mathstrokes.marking.strategy.AnswerEvaluation;
import com.mathstrokes.marking.strategy.EvaluationStrategy;
import com.mathstrokes.marking.strategy.EvaluationStrategyRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scores a finalised attempt.
 *
 * Everything it reads comes from the attempt snapshot: the answer key from
 * attempt_question_options, the rules from the marking_config copied onto each question. The
 * live question bank is never consulted, so a result computed today would come out identically
 * if it were recomputed years later, whatever has happened to the questions in the meantime.
 */
@Service
public class EvaluationService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationService.class);

    private final AttemptQuestionRepository attemptQuestionRepository;
    private final QuestionAttemptResultRepository resultRepository;
    private final EvaluationStrategyRegistry strategyRegistry;
    private final AttemptService attemptService;

    public EvaluationService(AttemptQuestionRepository attemptQuestionRepository,
                             QuestionAttemptResultRepository resultRepository,
                             EvaluationStrategyRegistry strategyRegistry,
                             AttemptService attemptService) {
        this.attemptQuestionRepository = attemptQuestionRepository;
        this.resultRepository = resultRepository;
        this.strategyRegistry = strategyRegistry;
        this.attemptService = attemptService;
    }

    /**
     * Evaluates and closes the attempt.
     *
     * Idempotent: an attempt already EVALUATED returns untouched. Together with the unique
     * constraint on question_attempt_results.attempt_question_id, a double-click, a retried
     * request and the expiry sweep racing a manual submit all converge on one set of marks.
     */
    @Transactional
    public TestAttempt evaluate(TestAttempt attempt) {
        if (attempt.getStatus() == AttemptStatus.EVALUATED) {
            return attempt;
        }
        if (resultRepository.existsByAttemptId(attempt.getId())) {
            log.debug("Attempt {} already has stored results; marking evaluated", attempt.getId());
            attempt.setStatus(AttemptStatus.EVALUATED);
            attempt.setEvaluatedAt(Instant.now());
            return attempt;
        }

        List<AttemptQuestion> questions =
                attemptQuestionRepository.findByAttemptWithOptions(attempt.getId());
        Map<Long, StudentAnswer> answers = attemptService.answersByQuestionId(attempt.getId());

        Totals totals = new Totals();
        List<QuestionAttemptResult> results = new ArrayList<>(questions.size());

        for (AttemptQuestion question : questions) {
            StudentAnswer answer = answers.get(question.getId());
            Set<Long> selected = answer == null ? Set.of() : answer.selectedOptionIds();

            EvaluationStrategy strategy = strategyRegistry.forType(question.getQuestionType());
            AnswerEvaluation evaluation = strategy.evaluate(
                    question.correctOptionIds(), selected, question.getMarkingConfig());

            results.add(toResult(attempt, question, evaluation));
            totals.accumulate(evaluation);
        }

        resultRepository.saveAll(results);
        applyTotals(attempt, totals);

        attempt.setStatus(AttemptStatus.EVALUATED);
        attempt.setEvaluatedAt(Instant.now());
        return attempt;
    }

    private QuestionAttemptResult toResult(TestAttempt attempt, AttemptQuestion question,
                                           AnswerEvaluation evaluation) {
        QuestionAttemptResult result = new QuestionAttemptResult();
        result.setAttempt(attempt);
        result.setAttemptQuestion(question);
        result.setQuestion(question.getQuestion());
        result.setResultStatus(evaluation.status());
        result.setMarksAwarded(evaluation.marksAwarded());
        result.setMaxMarks(evaluation.maxMarks());
        result.setSelectedOptionCount(evaluation.selectedOptionCount());
        result.setCorrectOptionCount(evaluation.correctOptionCount());
        return result;
    }

    private void applyTotals(TestAttempt attempt, Totals totals) {
        attempt.setScore(totals.score.setScale(2, RoundingMode.HALF_UP));
        attempt.setMaxScore(totals.maxScore.setScale(2, RoundingMode.HALF_UP));
        attempt.setNegativeMarks(totals.negativeMarks.abs().setScale(2, RoundingMode.HALF_UP));
        attempt.setCorrectCount(totals.correct);
        attempt.setPartiallyCorrectCount(totals.partiallyCorrect);
        attempt.setIncorrectCount(totals.incorrect);
        attempt.setUnansweredCount(totals.unanswered);

        int attempted = totals.correct + totals.partiallyCorrect + totals.incorrect;
        attempt.setAttemptedCount(attempted);
        // Guarded division: a student who answered nothing scores 0 accuracy, not a crash.
        attempt.setAccuracy(percentage(totals.correct, attempted));
        attempt.setAttemptRate(percentage(attempted, attempt.getTotalQuestions()));

        Instant finishedAt =
                attempt.getSubmittedAt() == null ? Instant.now() : attempt.getSubmittedAt();
        long seconds = Duration.between(attempt.getStartedAt(), finishedAt).toSeconds();
        // The sweep can finalise slightly after expiry; never report more than the allotted time.
        long capped = Math.min(Math.max(seconds, 0L), attempt.getDurationMinutes() * 60L);
        attempt.setTimeTakenSeconds((int) capped);
    }

    private BigDecimal percentage(int numerator, int denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    /** Running tallies for one attempt. */
    private static final class Totals {

        private BigDecimal score = BigDecimal.ZERO;
        private BigDecimal maxScore = BigDecimal.ZERO;
        private BigDecimal negativeMarks = BigDecimal.ZERO;
        private int correct;
        private int partiallyCorrect;
        private int incorrect;
        private int unanswered;

        void accumulate(AnswerEvaluation evaluation) {
            score = score.add(evaluation.marksAwarded());
            maxScore = maxScore.add(evaluation.maxMarks());
            if (evaluation.isNegative()) {
                negativeMarks = negativeMarks.add(evaluation.marksAwarded());
            }
            switch (evaluation.status()) {
                case CORRECT -> correct++;
                case PARTIALLY_CORRECT -> partiallyCorrect++;
                case INCORRECT -> incorrect++;
                case UNANSWERED -> unanswered++;
            }
        }
    }
}
