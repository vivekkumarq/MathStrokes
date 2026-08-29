package com.mathstrokes.exam.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.mathstrokes.catalog.entity.Chapter;
import com.mathstrokes.catalog.entity.Subject;
import com.mathstrokes.common.domain.BaseEntity;
import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.TestGenerationMode;
import com.mathstrokes.common.enums.TestStatus;
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
 * A publishable examination.
 *
 * Named ExamTest rather than Test so the class is not confused with a JUnit test class.
 * The table is still {@code tests}.
 *
 * A FIXED_SET test materialises its 25 questions once, at publish time, into
 * {@code test_questions}. Every student then sits an identical paper, which is the only
 * arrangement under which a ranking is meaningful - see {@code rankingEnabled}.
 */
@Entity
@Table(name = "tests")
@Getter
@Setter
@NoArgsConstructor
public class ExamTest extends BaseEntity {

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_pattern", nullable = false, length = 30)
    private ExamPattern examPattern;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 60;

    @Column(name = "question_count", nullable = false)
    private int questionCount = 25;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_mode", nullable = false, length = 30)
    private TestGenerationMode generationMode = TestGenerationMode.FIXED_SET;

    /** Difficulty blueprint. A null band means "no constraint"; the remainder is drawn at random. */
    @Column(name = "easy_count")
    private Integer easyCount;

    @Column(name = "medium_count")
    private Integer mediumCount;

    @Column(name = "hard_count")
    private Integer hardCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TestStatus status = TestStatus.DRAFT;

    /**
     * False for RANDOM_PER_ATTEMPT tests: two students who answered different questions cannot
     * be fairly placed on the same leaderboard.
     */
    @Column(name = "ranking_enabled", nullable = false)
    private boolean rankingEnabled = true;

    @Column(name = "max_attempts_per_student", nullable = false)
    private int maxAttemptsPerStudent = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("questionOrder asc")
    private List<TestQuestion> questions = new ArrayList<>();

    public boolean isOpenForAttempts() {
        return status == TestStatus.PUBLISHED;
    }

    public boolean hasFixedQuestionSet() {
        return generationMode == TestGenerationMode.FIXED_SET;
    }

    public void replaceQuestions(List<TestQuestion> newQuestions) {
        this.questions.clear();
        newQuestions.forEach(this::addQuestion);
    }

    public void addQuestion(TestQuestion testQuestion) {
        testQuestion.setTest(this);
        this.questions.add(testQuestion);
    }
}
