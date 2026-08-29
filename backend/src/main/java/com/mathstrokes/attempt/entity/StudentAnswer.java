package com.mathstrokes.attempt.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.mathstrokes.common.domain.BaseEntity;
import com.mathstrokes.common.enums.AnswerStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A student's working state for one question of one attempt.
 *
 * Selections are held in a child table rather than a delimited string, so the answer key can be
 * compared with a set operation and a selection can be traced back to the exact snapshotted
 * option it refers to.
 *
 * {@code clientSequence} is the autosave guard. The client increments it once per attempt, and a
 * write arriving with a lower value than the one already stored is a late packet from a flaky
 * connection and is discarded rather than allowed to overwrite newer work.
 */
@Entity
@Table(name = "student_answers")
@Getter
@Setter
@NoArgsConstructor
public class StudentAnswer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private TestAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_question_id", nullable = false)
    private AttemptQuestion attemptQuestion;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_status", nullable = false, length = 40)
    private AnswerStatus answerStatus = AnswerStatus.NOT_VISITED;

    @Column(name = "visited", nullable = false)
    private boolean visited;

    @Column(name = "marked_for_review", nullable = false)
    private boolean markedForReview;

    @Column(name = "client_sequence", nullable = false)
    private long clientSequence;

    @Column(name = "answered_at")
    private Instant answeredAt;

    @OneToMany(mappedBy = "studentAnswer", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    private Set<StudentAnswerOption> selectedOptions = new HashSet<>();

    public Set<Long> selectedOptionIds() {
        return selectedOptions.stream()
                .map(selection -> selection.getAttemptQuestionOption().getId())
                .collect(Collectors.toSet());
    }

    public boolean isAnswered() {
        return !selectedOptions.isEmpty();
    }

    /** Keeps the persisted palette status in step with the underlying flags. */
    public void refreshStatus() {
        this.answerStatus = AnswerStatus.of(visited, isAnswered(), markedForReview);
    }
}
