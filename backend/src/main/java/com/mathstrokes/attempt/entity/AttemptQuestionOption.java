package com.mathstrokes.attempt.entity;

import com.mathstrokes.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A snapshotted option, including whether it was correct.
 *
 * {@code correct} is never mapped into any DTO a student can reach while the attempt is live;
 * the student-facing option record has no such field at all.
 */
@Entity
@Table(name = "attempt_question_options")
@Getter
@Setter
@NoArgsConstructor
public class AttemptQuestionOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_question_id", nullable = false)
    private AttemptQuestion attemptQuestion;

    /** The question_options row this was copied from, kept for traceability only. */
    @Column(name = "source_option_id")
    private Long sourceOptionId;

    @Column(name = "option_key", nullable = false, length = 5)
    private String optionKey;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;
}
