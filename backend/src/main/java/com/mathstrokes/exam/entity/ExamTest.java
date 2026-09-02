package com.mathstrokes.exam.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.mathstrokes.catalog.entity.Chapter;
import com.mathstrokes.catalog.entity.Subject;
import com.mathstrokes.common.domain.BaseEntity;
import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.TestGenerationMode;
import com.mathstrokes.common.enums.TestKind;
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

    /**
     * The chapter this test draws from, or null for a full-syllabus paper. Absence is the signal;
     * there is deliberately no separate scope flag that could disagree with this field.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
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

    /**
     * What this paper is for. PRACTICE is the browsable bank; CLASS_TEST is one a teacher built
     * by hand for a class. Explicit rather than inferred from the schedule below, because a
     * teacher may open a class test immediately with no window at all - and because a derived
     * kind would change once the window passed.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "test_kind", nullable = false, length = 20)
    private TestKind testKind = TestKind.PRACTICE;

    /**
     * The window in which a student may START. Either bound may be absent, meaning unbounded on
     * that side.
     *
     * This does not publish anything and nothing watches the clock: the window is evaluated when
     * a student asks to start, so there is no scheduled job that could fail to fire. Publishing
     * remains a deliberate act.
     */
    @Column(name = "scheduled_start_at")
    private Instant scheduledStartAt;

    @Column(name = "scheduled_end_at")
    private Instant scheduledEndAt;

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

    public boolean isClassTest() {
        return testKind == TestKind.CLASS_TEST;
    }

    /** True once the paper's window has opened, and always true when no start was set. */
    public boolean hasOpenedBy(Instant now) {
        return scheduledStartAt == null || !now.isBefore(scheduledStartAt);
    }

    /** True once the window has passed, and never when no end was set. */
    public boolean hasWindowClosedBy(Instant now) {
        return scheduledEndAt != null && now.isAfter(scheduledEndAt);
    }

    /**
     * Whether a NEW attempt may begin. Says nothing about one already running: an attempt in
     * flight finishes on its own clock, exactly as it does when a test is closed.
     */
    public boolean isWithinSchedule(Instant now) {
        return hasOpenedBy(now) && !hasWindowClosedBy(now);
    }

    /** A paper with no chapter draws from every chapter of its subject. */
    public boolean isFullSyllabus() {
        return chapter == null;
    }

    public Long chapterId() {
        return chapter == null ? null : chapter.getId();
    }

    public String chapterName() {
        return chapter == null ? null : chapter.getName();
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
