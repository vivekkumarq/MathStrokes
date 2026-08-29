package com.mathstrokes.exam.entity;

import com.mathstrokes.common.domain.BaseEntity;
import com.mathstrokes.question.entity.Question;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Membership of a question in a FIXED_SET test, with its position on the paper. */
@Entity
@Table(name = "test_questions")
@Getter
@Setter
@NoArgsConstructor
public class TestQuestion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_id", nullable = false)
    private ExamTest test;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "question_order", nullable = false)
    private int questionOrder;
}
