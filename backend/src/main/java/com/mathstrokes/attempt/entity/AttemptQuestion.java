package com.mathstrokes.attempt.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.mathstrokes.catalog.entity.Chapter;
import com.mathstrokes.common.domain.BaseEntity;
import com.mathstrokes.common.enums.Difficulty;
import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.QuestionType;
import com.mathstrokes.marking.entity.MarkingConfig;
import com.mathstrokes.marking.entity.MarkingScheme;
import com.mathstrokes.question.entity.Question;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * An immutable snapshot of one question as it stood when the attempt began.
 *
 * This is the single most important table for correctness. Evaluation reads the content, the
 * options, the answer key and the marking configuration from HERE, never from the live question
 * rows. A teacher who later rewrites the question, flips which option is correct, archives it or
 * changes the marking scheme cannot move a mark on any attempt that already exists.
 *
 * {@code question} is kept only so analytics can group attempts by source question.
 */
@Entity
@Table(name = "attempt_questions")
@Getter
@Setter
@NoArgsConstructor
public class AttemptQuestion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private TestAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "question_order", nullable = false)
    private int questionOrder;

    /** The revision of the source question this snapshot was taken from. */
    @Column(name = "question_version", nullable = false)
    private int questionVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_pattern", nullable = false, length = 30)
    private ExamPattern examPattern;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 40)
    private QuestionType questionType;

    @Column(name = "question_content", nullable = false, columnDefinition = "text")
    private String questionContent;

    @Column(name = "solution_content", columnDefinition = "text")
    private String solutionContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marking_scheme_id")
    private MarkingScheme markingScheme;

    @Column(name = "marking_scheme_name", length = 150)
    private String markingSchemeName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "marking_config", nullable = false, columnDefinition = "jsonb")
    private MarkingConfig markingConfig;

    @Column(name = "max_marks", nullable = false, precision = 8, scale = 2)
    private BigDecimal maxMarks;

    @OneToMany(mappedBy = "attemptQuestion", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("displayOrder asc, id asc")
    private List<AttemptQuestionOption> options = new ArrayList<>();

    public void addOption(AttemptQuestionOption option) {
        option.setAttemptQuestion(this);
        this.options.add(option);
    }

    /** The answer key, as snapshotted. */
    public Set<Long> correctOptionIds() {
        return options.stream()
                .filter(AttemptQuestionOption::isCorrect)
                .map(AttemptQuestionOption::getId)
                .collect(Collectors.toSet());
    }
}
