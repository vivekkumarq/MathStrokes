package com.mathstrokes.question.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.mathstrokes.catalog.entity.Chapter;
import com.mathstrokes.catalog.entity.Subject;
import com.mathstrokes.common.domain.BaseEntity;
import com.mathstrokes.common.enums.Difficulty;
import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.QuestionStatus;
import com.mathstrokes.common.enums.QuestionType;
import com.mathstrokes.marking.entity.MarkingScheme;
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
 * A question in the bank.
 *
 * {@code questionContent}, {@code solutionContent} and every option store LaTeX/plain source,
 * never rendered HTML: KaTeX renders at display time, so the stored value stays the single
 * source of truth and cannot carry markup into the page.
 *
 * {@code version} doubles as the Hibernate optimistic lock (two admins editing the same question
 * cannot silently overwrite each other) and as the revision number recorded in attempt snapshots.
 */
@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
public class Question extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chapter_id", nullable = false)
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private QuestionStatus status = QuestionStatus.DRAFT;

    /** Optional override. When null the active scheme for (examPattern, questionType) is used. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marking_scheme_id")
    private MarkingScheme markingScheme;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("displayOrder asc, id asc")
    private List<QuestionOption> options = new ArrayList<>();

    public void replaceOptions(List<QuestionOption> newOptions) {
        this.options.clear();
        newOptions.forEach(this::addOption);
    }

    public void addOption(QuestionOption option) {
        option.setQuestion(this);
        this.options.add(option);
    }

    public List<QuestionOption> correctOptions() {
        return options.stream().filter(QuestionOption::isCorrect).toList();
    }
}
