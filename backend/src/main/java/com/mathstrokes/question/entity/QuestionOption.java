package com.mathstrokes.question.entity;

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

@Entity
@Table(name = "question_options")
@Getter
@Setter
@NoArgsConstructor
public class QuestionOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    /** Stable label shown to the student: A, B, C, D, ... */
    @Column(name = "option_key", nullable = false, length = 5)
    private String optionKey;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;
}
