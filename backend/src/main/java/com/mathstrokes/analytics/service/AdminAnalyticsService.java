package com.mathstrokes.analytics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mathstrokes.analytics.dto.AdminDashboardResponse;
import com.mathstrokes.analytics.dto.ChapterPerformanceResponse;
import com.mathstrokes.analytics.dto.QuestionQualityResponse;
import com.mathstrokes.analytics.repository.AnalyticsRepository;
import com.mathstrokes.attempt.repository.TestAttemptRepository;
import com.mathstrokes.common.enums.AttemptStatus;
import com.mathstrokes.common.enums.QuestionStatus;
import com.mathstrokes.common.enums.RoleName;
import com.mathstrokes.common.enums.TestStatus;
import com.mathstrokes.exam.repository.ExamTestRepository;
import com.mathstrokes.question.entity.Question;
import com.mathstrokes.question.repository.QuestionRepository;
import com.mathstrokes.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminAnalyticsService {

    /** Below this many real attempts an accuracy figure is noise, not a signal. */
    private static final int MINIMUM_ATTEMPTS_FOR_QUALITY = 3;
    private static final int PREVIEW_LENGTH = 120;

    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final ExamTestRepository testRepository;
    private final TestAttemptRepository attemptRepository;
    private final AnalyticsRepository analyticsRepository;

    public AdminAnalyticsService(UserRepository userRepository,
                                 QuestionRepository questionRepository,
                                 ExamTestRepository testRepository,
                                 TestAttemptRepository attemptRepository,
                                 AnalyticsRepository analyticsRepository) {
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.testRepository = testRepository;
        this.attemptRepository = attemptRepository;
        this.analyticsRepository = analyticsRepository;
    }

    public AdminDashboardResponse dashboard() {
        Instant thirtyDaysAgo = Instant.now().minus(Duration.ofDays(30));
        Instant sevenDaysAgo = Instant.now().minus(Duration.ofDays(7));
        return new AdminDashboardResponse(
                userRepository.countByRole(RoleName.ROLE_STUDENT),
                userRepository.countActiveSince(RoleName.ROLE_STUDENT, thirtyDaysAgo),
                questionRepository.count(),
                questionRepository.countByStatus(QuestionStatus.PUBLISHED),
                questionRepository.countByStatus(QuestionStatus.DRAFT),
                questionRepository.countByStatus(QuestionStatus.ARCHIVED),
                testRepository.count(),
                testRepository.countByStatus(TestStatus.PUBLISHED),
                attemptRepository.count(),
                attemptRepository.countByStatus(AttemptStatus.ACTIVE),
                attemptRepository.countStartedSince(sevenDaysAgo));
    }

    /**
     * The questions students find hardest, worst accuracy first.
     *
     * Aggregated from stored per-question results rather than recomputed from answers, so a
     * teacher editing a question afterwards does not distort the history it has already built up.
     */
    public List<QuestionQualityResponse> hardestQuestions(Long chapterId, int limit) {
        List<Object[]> rows = analyticsRepository.questionOutcomeTallies(
                chapterId, MINIMUM_ATTEMPTS_FOR_QUALITY, PageRequest.of(0, limit));
        return toQualityResponses(rows);
    }

    private List<QuestionQualityResponse> toQualityResponses(List<Object[]> rows) {
        List<Long> questionIds = rows.stream().map(row -> toLong(row[0])).toList();
        if (questionIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Question> questions = new HashMap<>();
        questionRepository.findAllByIdWithOptions(questionIds)
                .forEach(question -> questions.put(question.getId(), question));

        return rows.stream().map(row -> {
            Long questionId = toLong(row[0]);
            long timesShown = toLong(row[1]);
            long correct = toLong(row[2]);
            long partial = toLong(row[3]);
            long incorrect = toLong(row[4]);
            long unanswered = toLong(row[5]);
            long attempted = correct + partial + incorrect;

            Question question = questions.get(questionId);
            return new QuestionQualityResponse(
                    questionId,
                    question == null ? null : question.getChapter().getName(),
                    question == null ? null : question.getExamPattern(),
                    question == null ? null : question.getDifficulty(),
                    question == null ? "" : preview(question.getQuestionContent()),
                    timesShown,
                    attempted,
                    correct,
                    partial,
                    incorrect,
                    unanswered,
                    attempted == 0 ? null : percentage(correct, attempted));
        }).toList();
    }

    public List<ChapterPerformanceResponse> chapterPerformance() {
        return analyticsRepository.chapterPerformance(null).stream()
                .map(row -> new ChapterPerformanceResponse(
                        toLong(row[0]),
                        (String) row[1],
                        toLong(row[2]),
                        toDecimal(row[3]),
                        toDecimal(row[4])))
                .toList();
    }

    static long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    static BigDecimal toDecimal(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof BigDecimal decimal
                ? decimal
                : BigDecimal.valueOf(((Number) value).doubleValue()).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private String preview(String content) {
        if (content == null) {
            return "";
        }
        String collapsed = content.trim();
        return collapsed.length() <= PREVIEW_LENGTH
                ? collapsed
                : collapsed.substring(0, PREVIEW_LENGTH) + "...";
    }
}
