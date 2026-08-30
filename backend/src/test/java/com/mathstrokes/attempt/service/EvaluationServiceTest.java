package com.mathstrokes.attempt.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.mathstrokes.attempt.entity.AttemptQuestion;
import com.mathstrokes.attempt.entity.AttemptQuestionOption;
import com.mathstrokes.attempt.entity.StudentAnswer;
import com.mathstrokes.attempt.entity.StudentAnswerOption;
import com.mathstrokes.attempt.entity.TestAttempt;
import com.mathstrokes.attempt.repository.AttemptQuestionRepository;
import com.mathstrokes.attempt.repository.QuestionAttemptResultRepository;
import com.mathstrokes.common.enums.AttemptStatus;
import com.mathstrokes.common.enums.QuestionType;
import com.mathstrokes.marking.entity.MarkingConfig;
import com.mathstrokes.marking.entity.PartialCreditMode;
import com.mathstrokes.marking.strategy.EvaluationStrategyRegistry;
import com.mathstrokes.marking.strategy.MultipleCorrectEvaluationStrategy;
import com.mathstrokes.marking.strategy.SingleCorrectEvaluationStrategy;
import com.mathstrokes.question.entity.Question;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises the aggregation the result page depends on: totals, counts, negative marks and the
 * two guarded percentages. Scoring an individual question is covered by the strategy tests; what
 * is under test here is how those outcomes add up into an attempt.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EvaluationServiceTest {

    private static final MarkingConfig JEE_MAIN = new MarkingConfig(
            new BigDecimal("4.00"), new BigDecimal("-1.00"), BigDecimal.ZERO,
            PartialCreditMode.NONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    @Mock
    private AttemptQuestionRepository attemptQuestionRepository;
    @Mock
    private QuestionAttemptResultRepository resultRepository;
    @Mock
    private AttemptService attemptService;

    private EvaluationService evaluationService;
    private final AtomicLong ids = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        EvaluationStrategyRegistry registry = new EvaluationStrategyRegistry(List.of(
                new SingleCorrectEvaluationStrategy(), new MultipleCorrectEvaluationStrategy()));
        evaluationService = new EvaluationService(attemptQuestionRepository, resultRepository,
                registry, attemptService);
    }

    private TestAttempt attempt(int totalQuestions) {
        TestAttempt attempt = new TestAttempt();
        ReflectionTestUtils.setField(attempt, "id", 1L);
        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setTotalQuestions(totalQuestions);
        attempt.setDurationMinutes(60);
        attempt.setStartedAt(Instant.now().minus(Duration.ofMinutes(20)));
        attempt.setSubmittedAt(Instant.now());
        return attempt;
    }

    /** A four-option single-correct snapshot whose key is option A. */
    private AttemptQuestion question(TestAttempt attempt, int order) {
        AttemptQuestion question = new AttemptQuestion();
        ReflectionTestUtils.setField(question, "id", ids.getAndIncrement());
        question.setAttempt(attempt);
        question.setQuestionOrder(order);
        question.setQuestionType(QuestionType.SINGLE_CORRECT);
        question.setMarkingConfig(JEE_MAIN);
        question.setMaxMarks(JEE_MAIN.maxMarks());
        Question source = new Question();
        ReflectionTestUtils.setField(source, "id", 1000L + order);
        question.setQuestion(source);

        for (String key : List.of("A", "B", "C", "D")) {
            AttemptQuestionOption option = new AttemptQuestionOption();
            ReflectionTestUtils.setField(option, "id", ids.getAndIncrement());
            option.setOptionKey(key);
            option.setContent("$" + key + "$");
            option.setCorrect("A".equals(key));
            question.addOption(option);
        }
        return question;
    }

    private StudentAnswer answer(TestAttempt attempt, AttemptQuestion question, String optionKey) {
        StudentAnswer studentAnswer = new StudentAnswer();
        studentAnswer.setAttempt(attempt);
        studentAnswer.setAttemptQuestion(question);
        studentAnswer.setSelectedOptions(new HashSet<>());
        question.getOptions().stream()
                .filter(option -> option.getOptionKey().equals(optionKey))
                .findFirst()
                .ifPresent(option -> studentAnswer.getSelectedOptions()
                        .add(new StudentAnswerOption(studentAnswer, option)));
        studentAnswer.refreshStatus();
        return studentAnswer;
    }

    @Test
    @DisplayName("a mixed paper aggregates into the right score, counts and percentages")
    void aggregatesMixedPaper() {
        TestAttempt attempt = attempt(10);
        List<AttemptQuestion> questions = new ArrayList<>();
        Map<Long, StudentAnswer> answers = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            AttemptQuestion question = question(attempt, i);
            questions.add(question);
            // 6 answered correctly, 2 answered wrongly, 2 left blank
            String selected = i <= 6 ? "A" : (i <= 8 ? "B" : null);
            if (selected != null) {
                answers.put(question.getId(), answer(attempt, question, selected));
            }
        }
        when(attemptQuestionRepository.findByAttemptWithOptions(1L)).thenReturn(questions);
        when(attemptService.answersByQuestionId(1L)).thenReturn(answers);

        evaluationService.evaluate(attempt);

        assertThat(attempt.getScore()).isEqualByComparingTo("22.00");    // 6*4 - 2*1
        assertThat(attempt.getMaxScore()).isEqualByComparingTo("40.00"); // 10*4
        assertThat(attempt.getCorrectCount()).isEqualTo(6);
        assertThat(attempt.getIncorrectCount()).isEqualTo(2);
        assertThat(attempt.getUnansweredCount()).isEqualTo(2);
        assertThat(attempt.getAttemptedCount()).isEqualTo(8);
        assertThat(attempt.getNegativeMarks()).isEqualByComparingTo("2.00");
        assertThat(attempt.getAccuracy()).isEqualByComparingTo("75.00");    // 6 of 8 attempted
        assertThat(attempt.getAttemptRate()).isEqualByComparingTo("80.00"); // 8 of 10
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.EVALUATED);
    }

    @Test
    @DisplayName("a paper with nothing attempted reports zero accuracy, not a division by zero")
    void handlesNothingAttempted() {
        TestAttempt attempt = attempt(5);
        List<AttemptQuestion> questions = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            questions.add(question(attempt, i));
        }
        when(attemptQuestionRepository.findByAttemptWithOptions(1L)).thenReturn(questions);
        when(attemptService.answersByQuestionId(1L)).thenReturn(Map.of());

        evaluationService.evaluate(attempt);

        assertThat(attempt.getScore()).isEqualByComparingTo("0.00");
        assertThat(attempt.getUnansweredCount()).isEqualTo(5);
        assertThat(attempt.getAttemptedCount()).isZero();
        assertThat(attempt.getAccuracy()).isEqualByComparingTo("0.00");
        assertThat(attempt.getAttemptRate()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("a paper answered entirely wrongly can score below zero")
    void scoreCanGoNegative() {
        TestAttempt attempt = attempt(3);
        List<AttemptQuestion> questions = new ArrayList<>();
        Map<Long, StudentAnswer> answers = new HashMap<>();
        for (int i = 1; i <= 3; i++) {
            AttemptQuestion question = question(attempt, i);
            questions.add(question);
            answers.put(question.getId(), answer(attempt, question, "C"));
        }
        when(attemptQuestionRepository.findByAttemptWithOptions(1L)).thenReturn(questions);
        when(attemptService.answersByQuestionId(1L)).thenReturn(answers);

        evaluationService.evaluate(attempt);

        assertThat(attempt.getScore()).isEqualByComparingTo("-3.00");
        assertThat(attempt.getNegativeMarks()).isEqualByComparingTo("3.00");
        assertThat(attempt.getAccuracy()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("evaluating an evaluated attempt is a no-op, so a retry cannot double-score")
    void evaluationIsIdempotent() {
        TestAttempt attempt = attempt(5);
        attempt.setStatus(AttemptStatus.EVALUATED);
        attempt.setScore(new BigDecimal("20.00"));

        evaluationService.evaluate(attempt);

        assertThat(attempt.getScore()).isEqualByComparingTo("20.00");
        verify(resultRepository, never()).saveAll(any());
        verify(attemptQuestionRepository, never()).findByAttemptWithOptions(anyLong());
    }

    @Test
    @DisplayName("an attempt that already has stored results is closed without rescoring")
    void existingResultsAreNotRewritten() {
        TestAttempt attempt = attempt(5);
        when(resultRepository.existsByAttemptId(1L)).thenReturn(true);

        evaluationService.evaluate(attempt);

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.EVALUATED);
        verify(resultRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("time taken never exceeds the paper duration, however late the sweep runs")
    void timeTakenIsCappedAtTheDuration() {
        TestAttempt attempt = attempt(1);
        attempt.setStartedAt(Instant.now().minus(Duration.ofHours(3)));
        attempt.setSubmittedAt(Instant.now());
        when(attemptQuestionRepository.findByAttemptWithOptions(1L))
                .thenReturn(List.of(question(attempt, 1)));
        when(attemptService.answersByQuestionId(1L)).thenReturn(Map.of());

        evaluationService.evaluate(attempt);

        assertThat(attempt.getTimeTakenSeconds()).isEqualTo(3600);
    }
}
