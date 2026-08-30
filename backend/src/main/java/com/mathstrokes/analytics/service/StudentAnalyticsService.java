package com.mathstrokes.analytics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

import com.mathstrokes.analytics.dto.ChapterPerformanceResponse;
import com.mathstrokes.analytics.dto.StudentPerformanceResponse;
import com.mathstrokes.analytics.repository.AnalyticsRepository;
import com.mathstrokes.attempt.entity.TestAttempt;
import com.mathstrokes.attempt.repository.TestAttemptRepository;
import com.mathstrokes.common.enums.AttemptStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The student's own performance summary.
 *
 * Every query here is scoped to the caller's id, taken from the security context. There is no
 * code path that lets a student pass an id and read somebody else's figures.
 */
@Service
@Transactional(readOnly = true)
public class StudentAnalyticsService {

    private static final int RECENT_SCORE_POINTS = 10;

    private final TestAttemptRepository attemptRepository;
    private final AnalyticsRepository analyticsRepository;

    public StudentAnalyticsService(TestAttemptRepository attemptRepository,
                                   AnalyticsRepository analyticsRepository) {
        this.attemptRepository = attemptRepository;
        this.analyticsRepository = analyticsRepository;
    }

    public StudentPerformanceResponse summaryFor(Long studentId) {
        List<TestAttempt> recent = attemptRepository.findByStudent(studentId,
                        PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "startedAt")))
                .getContent();

        List<TestAttempt> evaluated = recent.stream()
                .filter(attempt -> attempt.getStatus() == AttemptStatus.EVALUATED)
                .filter(attempt -> attempt.getMaxScore() != null
                        && attempt.getMaxScore().signum() > 0)
                .toList();

        long inProgress = recent.stream()
                .filter(attempt -> attempt.getStatus() == AttemptStatus.ACTIVE)
                .count();

        List<BigDecimal> percentages = evaluated.stream()
                .map(this::scorePercentage)
                .toList();

        List<StudentPerformanceResponse.ScorePoint> points = evaluated.stream()
                .sorted(Comparator.comparing(TestAttempt::getStartedAt))
                .skip(Math.max(0, evaluated.size() - RECENT_SCORE_POINTS))
                .map(attempt -> new StudentPerformanceResponse.ScorePoint(
                        attempt.getId(),
                        attempt.getTest().getTitle(),
                        attempt.getSubmittedAt() == null
                                ? attempt.getStartedAt() : attempt.getSubmittedAt(),
                        attempt.getScore(),
                        attempt.getMaxScore(),
                        scorePercentage(attempt)))
                .toList();

        List<ChapterPerformanceResponse> chapters =
                analyticsRepository.chapterPerformance(studentId).stream()
                        .map(row -> new ChapterPerformanceResponse(
                                AdminAnalyticsService.toLong(row[0]),
                                (String) row[1],
                                AdminAnalyticsService.toLong(row[2]),
                                AdminAnalyticsService.toDecimal(row[3]),
                                AdminAnalyticsService.toDecimal(row[4])))
                        .toList();

        return new StudentPerformanceResponse(
                evaluated.size(),
                inProgress,
                average(percentages),
                percentages.stream().max(Comparator.naturalOrder()).orElse(null),
                average(evaluated.stream().map(TestAttempt::getAccuracy)
                        .filter(java.util.Objects::nonNull).toList()),
                evaluated.stream()
                        .map(TestAttempt::getRankPosition)
                        .filter(java.util.Objects::nonNull)
                        .min(Comparator.naturalOrder())
                        .orElse(null),
                points,
                chapters);
    }

    /** Score as a percentage of what the paper was worth, so papers of different sizes compare. */
    private BigDecimal scorePercentage(TestAttempt attempt) {
        if (attempt.getScore() == null || attempt.getMaxScore() == null
                || attempt.getMaxScore().signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        // Negative marking can push a total below zero; a negative percentage would read oddly
        // on a progress chart, so the floor is zero.
        BigDecimal raw = attempt.getScore()
                .multiply(BigDecimal.valueOf(100))
                .divide(attempt.getMaxScore(), 2, RoundingMode.HALF_UP);
        return raw.signum() < 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : raw;
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }
}
