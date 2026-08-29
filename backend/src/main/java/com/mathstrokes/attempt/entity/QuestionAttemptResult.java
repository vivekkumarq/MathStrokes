package com.mathstrokes.attempt.entity;

import java.math.BigDecimal;

import com.mathstrokes.common.domain.BaseEntity;
import com.mathstrokes.common.enums.QuestionResultStatus;
import com.mathstrokes.question.entity.Question;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The scored outcome of one question in one attempt, written once at evaluation.
 *
 * A unique constraint on attempt_question_id makes evaluation idempotent at the database level:
 * a replayed submit cannot produce a second set of results.
 *
 * {@code question} is denormalised from the snapshot so question-quality analytics can aggregate
 * across attempts without joining through attempt_questions.
 */
@Entity
@Table(name = "question_attempt_results")
@Getter
@Setter
@NoArgsConstructor
public class QuestionAttemptResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private TestAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_question_id", nullable = false)
    private AttemptQuestion attemptQuestion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 30)
    private QuestionResultStatus resultStatus;

    @Column(name = "marks_awarded", nullable = false, precision = 8, scale = 2)
    private BigDecimal marksAwarded;

    @Column(name = "max_marks", nullable = false, precision = 8, scale = 2)
    private BigDecimal maxMarks;

    @Column(name = "selected_option_count", nullable = false)
    private int selectedOptionCount;

    @Column(name = "correct_option_count", nullable = false)
    private int correctOptionCount;
}
