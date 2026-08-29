package com.mathstrokes.attempt.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.mathstrokes.common.domain.BaseEntity;
import com.mathstrokes.common.enums.AttemptStatus;
import com.mathstrokes.exam.entity.ExamTest;
import com.mathstrokes.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One student's sitting of one test.
 *
 * {@code expiresAt} is written once when the attempt starts and is the single authority on when
 * the paper closes. Nothing in the system extends it, and every answer write is checked against
 * it, so a manipulated client clock changes nothing.
 */
@Entity
@Table(name = "test_attempts")
@Getter
@Setter
@NoArgsConstructor
public class TestAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_id", nullable = false)
    private ExamTest test;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttemptStatus status = AttemptStatus.ACTIVE;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "evaluated_at")
    private Instant evaluatedAt;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(name = "score", precision = 8, scale = 2)
    private BigDecimal score;

    @Column(name = "max_score", precision = 8, scale = 2)
    private BigDecimal maxScore;

    @Column(name = "negative_marks", precision = 8, scale = 2)
    private BigDecimal negativeMarks;

    @Column(name = "correct_count")
    private Integer correctCount;

    @Column(name = "partially_correct_count")
    private Integer partiallyCorrectCount;

    @Column(name = "incorrect_count")
    private Integer incorrectCount;

    @Column(name = "unanswered_count")
    private Integer unansweredCount;

    @Column(name = "attempted_count")
    private Integer attemptedCount;

    @Column(name = "accuracy", precision = 5, scale = 2)
    private BigDecimal accuracy;

    @Column(name = "attempt_rate", precision = 5, scale = 2)
    private BigDecimal attemptRate;

    @Column(name = "time_taken_seconds")
    private Integer timeTakenSeconds;

    @Column(name = "rank_position")
    private Integer rankPosition;

    @Column(name = "total_candidates")
    private Integer totalCandidates;

    @Column(name = "percentile", precision = 5, scale = 2)
    private BigDecimal percentile;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("questionOrder asc")
    private List<AttemptQuestion> questions = new ArrayList<>();

    public boolean hasExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public long remainingSeconds(Instant now) {
        long remaining = java.time.Duration.between(now, expiresAt).toSeconds();
        return Math.max(remaining, 0L);
    }

    public void addQuestion(AttemptQuestion question) {
        question.setAttempt(this);
        this.questions.add(question);
    }
}
