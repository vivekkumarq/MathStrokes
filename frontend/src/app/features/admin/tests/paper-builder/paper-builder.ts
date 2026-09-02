import { LowerCasePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { toApiFailure } from '../../../../core/http/api-failure';
import {
  AdminTestResponse,
  ChapterResponse,
  DIFFICULTIES,
  Difficulty,
  EXAM_PATTERNS,
  ExamPattern,
  QUESTION_TYPES,
  QuestionQuery,
  QuestionRequest,
  QuestionResponse,
  QuestionSummaryResponse,
  paperScopeLabel,
} from '../../../../core/models';
import { MathContent } from '../../../../shared/math/math-content';
import { AdminCatalogService } from '../../data/admin-catalog.service';
import { QuestionService } from '../../data/question.service';
import { TestService } from '../../data/test.service';
import { AdminShell } from '../../layout/admin-shell';

const PAGE_SIZE = 10;

const OPTION_KEYS = ['A', 'B', 'C', 'D'] as const;

/**
 * Same rule the standalone question editor applies, and the same rule the server enforces:
 * one correct option for a single-correct question, at least one for a multiple-correct.
 * Checked here so the author is told before a round trip, not by a 400 afterwards.
 */
function correctAnswerRule(group: AbstractControl): ValidationErrors | null {
  const options = group.get('options') as FormArray | null;
  if (!options) {
    return null;
  }
  const correct = options.controls.filter((c) => c.get('isCorrect')?.value === true);
  if (correct.length === 0) {
    return { noCorrectOption: true };
  }
  if (group.get('questionType')?.value === 'SINGLE_CORRECT' && correct.length > 1) {
    return { tooManyCorrectOptions: true };
  }
  return null;
}

/**
 * Hand-picks the exact paper for a test.
 *
 * The bank on the left is searched and filtered; the tray on the right is the paper, in the
 * order students will sit it. Saving replaces the paper wholesale, and the array order IS the
 * question order, so there is no separate "reorder" call that could disagree with the list.
 *
 * Only a draft can be edited. A published paper may already have been sat, and rewriting the
 * questions underneath a submitted attempt would invalidate its marking.
 */
@Component({
  selector: 'app-paper-builder',
  imports: [AdminShell, FormsModule, LowerCasePipe, MathContent, ReactiveFormsModule, RouterLink],
  templateUrl: './paper-builder.html',
  styleUrl: './paper-builder.scss',
})
export class PaperBuilder {
  /** testKind first, chapter only for a practice paper. Shared so the screens agree. */
  protected readonly scopeLabel = paperScopeLabel;

  private readonly route = inject(ActivatedRoute);
  private readonly tests = inject(TestService);
  private readonly questions = inject(QuestionService);
  private readonly catalog = inject(AdminCatalogService);

  protected readonly patterns = EXAM_PATTERNS;
  protected readonly difficulties = DIFFICULTIES;

  private readonly testId = Number(this.route.snapshot.paramMap.get('id'));

  protected readonly test = signal<AdminTestResponse | null>(null);
  protected readonly chapters = signal<ChapterResponse[]>([]);

  /** The paper being built, in order. This is the single source of truth for the tray. */
  protected readonly chosen = signal<QuestionSummaryResponse[]>([]);

  /** Ids of the chosen questions, for an O(1) "is this already on the paper?" test per row. */
  protected readonly chosenIds = computed(() => new Set(this.chosen().map((q) => q.id)));

  protected readonly bank = signal<QuestionSummaryResponse[]>([]);
  protected readonly bankTotal = signal(0);
  protected readonly bankPages = signal(0);
  protected readonly bankPage = signal(0);

  protected readonly loading = signal(true);
  protected readonly bankLoading = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);

  /** Set once the tray differs from what was loaded, so the leave warning is honest. */
  protected readonly dirty = signal(false);

  protected search = '';
  protected chapterId: number | '' = '';
  protected difficulty: Difficulty | '' = '';
  protected examPattern: ExamPattern | '' = '';

  /** A published or archived paper is read-only; the tray renders but nothing can change it. */
  protected readonly editable = computed(() => this.test()?.status === 'DRAFT');

  /**
   * The paper length the test was created with. Saving REPLACES it with however many
   * questions are in the tray, so this is shown as the starting figure, never as a target
   * the teacher has to hit. It matters because a test whose stored count disagrees with its
   * attached questions fails at the moment a student clicks Start, with a message blaming
   * the teacher - deriving the count from the tray is what removes that failure entirely.
   */
  protected readonly createdWithCount = computed(() => this.test()?.questionCount ?? 0);

  /** Distinct chapters represented in the tray, by name. */
  protected readonly chosenChapters = computed(() =>
    [...new Set(this.chosen().map((q) => q.chapterName))].sort(),
  );

  /**
   * True when the tray no longer matches the chapter the test is filed under - either it
   * spans several chapters, or its one chapter is not the test's.
   *
   * The test would then advertise a chapter label that its questions contradict, and that
   * label is what students read on the test list. A mixed paper belongs to no single chapter
   * and has to be saved as full-syllabus instead.
   */
  protected readonly chapterConflict = computed(() => {
    const testChapter = this.test()?.chapterName;
    if (testChapter === undefined || this.chosen().length === 0) {
      return false;
    }
    const chapters = this.chosenChapters();
    return chapters.length > 1 || chapters[0] !== testChapter;
  });

  // --- Writing a question -------------------------------------------------------------
  // A teacher setting tomorrow's paper often needs a question that does not exist yet, and
  // sending them to the question bank to write it would lose the paper they are mid-way
  // through building. Composed here, published, and dropped straight onto the tray.

  private readonly fb = inject(FormBuilder);

  protected readonly optionKeys = OPTION_KEYS;
  protected readonly types = QUESTION_TYPES;

  protected readonly composerOpen = signal(false);
  protected readonly composing = signal(false);
  protected readonly composerError = signal<string | null>(null);

  protected readonly composer = this.fb.nonNullable.group(
    {
      // The pattern is NOT here: it is the test's, and a question of the other pattern is
      // refused when the paper is saved. Offering the choice would only offer the mistake.
      chapterId: [0, [Validators.required, Validators.min(1)]],
      difficulty: ['MEDIUM' as (typeof DIFFICULTIES)[number], [Validators.required]],
      questionType: ['SINGLE_CORRECT' as (typeof QUESTION_TYPES)[number], [Validators.required]],
      questionContent: ['', [Validators.required]],
      solutionContent: [''],
      options: this.fb.array(
        OPTION_KEYS.map((key, index) =>
          this.fb.nonNullable.group({
            optionKey: [key as string],
            content: ['', [Validators.required]],
            displayOrder: [index + 1],
            isCorrect: [false],
          }),
        ),
      ),
    },
    { validators: correctAnswerRule },
  );

  protected get composerOptions(): FormArray<FormGroup> {
    return this.composer.controls.options as FormArray<FormGroup>;
  }

  /** Live preview source, mirrored from the textareas as they are typed into. */
  protected readonly stemPreview = signal('');
  protected readonly solutionPreview = signal('');

  protected optionPreview(index: number): string {
    return (this.composerOptions.at(index).get('content')?.value as string) ?? '';
  }

  protected toggleComposer(): void {
    this.composerOpen.update((open) => !open);
    this.composerError.set(null);
  }

  /** Single-correct means exactly one, so picking another clears the previous. */
  protected onCorrectChange(index: number): void {
    if (this.composer.controls.questionType.value !== 'SINGLE_CORRECT') {
      return;
    }
    this.composerOptions.controls.forEach((control, i) => {
      if (i !== index) {
        control.get('isCorrect')?.setValue(false, { emitEvent: false });
      }
    });
  }

  /** Switching to single-correct collapses any extra selections down to the first. */
  protected onTypeChange(): void {
    if (this.composer.controls.questionType.value !== 'SINGLE_CORRECT') {
      return;
    }
    let seen = false;
    this.composerOptions.controls.forEach((control) => {
      const isCorrect = control.get('isCorrect');
      if (isCorrect?.value === true) {
        if (seen) {
          isCorrect.setValue(false, { emitEvent: false });
        }
        seen = true;
      }
    });
  }

  /**
   * Creates the question, publishes it, and puts it on the tray.
   *
   * Publishing is not optional: only a published question may be attached to a paper, so a
   * draft written here could never be saved onto one. The paper itself stays unsaved
   * afterwards - the teacher still sets the order, and nothing reaches students until the
   * paper is saved AND the test is flagged live.
   */
  protected addWrittenQuestion(): void {
    const test = this.test();
    if (test === null || this.composing()) {
      return;
    }
    this.composerError.set(null);

    if (this.composer.invalid) {
      this.composer.markAllAsTouched();
      if (this.composer.hasError('noCorrectOption')) {
        this.composerError.set('Mark at least one option as correct.');
      } else if (this.composer.hasError('tooManyCorrectOptions')) {
        this.composerError.set('A single-correct question can have only one correct option.');
      } else if (this.composer.controls.chapterId.invalid) {
        this.composerError.set('Choose the chapter this question belongs to.');
      } else {
        this.composerError.set('Fill in the question and all four options.');
      }
      return;
    }

    const raw = this.composer.getRawValue();
    const request: QuestionRequest = {
      chapterId: raw.chapterId,
      // Inherited from the paper, never chosen: the server refuses a question whose pattern
      // differs from the test's, and it would be refused at save time rather than here.
      examPattern: test.examPattern,
      difficulty: raw.difficulty,
      questionType: raw.questionType,
      questionContent: raw.questionContent.trim(),
      solutionContent: raw.solutionContent.trim() === '' ? undefined : raw.solutionContent.trim(),
      options: raw.options.map((option, index) => ({
        optionKey: option.optionKey,
        content: option.content.trim(),
        displayOrder: index + 1,
        isCorrect: option.isCorrect,
      })),
    };

    this.composing.set(true);
    this.questions.create(request).subscribe({
      next: (created) => {
        this.questions.publish(created.id).subscribe({
          next: () => {
            this.composing.set(false);
            this.chosen.update((list) => [...list, this.asSummary(created)]);
            this.dirty.set(true);
            this.notice.set('Question added to the paper. Save the paper to keep it.');
            this.resetComposer();
            // A written question belongs in the bank listing too, so writing a near-duplicate
            // next time shows it is already there.
            this.loadBank();
          },
          error: (err: unknown) => {
            this.composing.set(false);
            // The question exists but is still a draft, so it cannot go on the paper. Say
            // that plainly rather than leaving the teacher thinking nothing was saved.
            this.composerError.set(
              'Saved to the question bank as a draft, but publishing it failed, so it is not ' +
                'on the paper: ' +
                toApiFailure(err).message,
            );
          },
        });
      },
      error: (err: unknown) => {
        this.composing.set(false);
        this.composerError.set(toApiFailure(err).message);
      },
    });
  }

  /** The tray renders bank rows, so a freshly written question is shaped like one. */
  private asSummary(question: QuestionResponse): QuestionSummaryResponse {
    return {
      id: question.id,
      chapterName: question.chapterName,
      examPattern: question.examPattern,
      difficulty: question.difficulty,
      questionType: question.questionType,
      questionPreview: question.questionContent,
      status: 'PUBLISHED',
      optionCount: question.options.length,
      updatedAt: question.updatedAt,
      version: question.version,
    };
  }

  private resetComposer(): void {
    // Keep the chapter: the next question is usually for the same one.
    const chapterId = this.composer.controls.chapterId.value;
    this.composer.reset({
      chapterId,
      difficulty: 'MEDIUM',
      questionType: 'SINGLE_CORRECT',
      questionContent: '',
      solutionContent: '',
      options: OPTION_KEYS.map((key, index) => ({
        optionKey: key as string,
        content: '',
        displayOrder: index + 1,
        isCorrect: false,
      })),
    });
    this.stemPreview.set('');
    this.solutionPreview.set('');
  }

  constructor() {
    this.catalog.chapters().subscribe({
      next: (chapters) => this.chapters.set(chapters),
      error: () => undefined,
    });

    this.composer.controls.questionContent.valueChanges.subscribe((v) =>
      this.stemPreview.set(v ?? ''),
    );
    this.composer.controls.solutionContent.valueChanges.subscribe((v) =>
      this.solutionPreview.set(v ?? ''),
    );

    this.tests.get(this.testId).subscribe({
      next: (test) => {
        this.test.set(test);
        this.loading.set(false);
        if (test.chapterId !== undefined) {
          this.composer.controls.chapterId.setValue(test.chapterId);
        }
        // Default the bank filters to the paper's own scope, which is what a teacher
        // building a chapter test almost always wants to see first.
        if (test.chapterId !== undefined) {
          this.chapterId = test.chapterId;
        }
        this.examPattern = test.examPattern;
        this.loadBank();
      },
      error: (err: unknown) => {
        this.loading.set(false);
        this.error.set(toApiFailure(err).message);
      },
    });

    this.tests.questions(this.testId).subscribe({
      next: (attached) => this.chosen.set(attached),
      // An empty paper is the normal state for a new test, so a failure here must not read
      // as one. Only a real error surfaces.
      error: (err: unknown) => {
        const failure = toApiFailure(err);
        if (failure.status !== 404) {
          this.error.set(failure.message);
        }
      },
    });
  }

  protected applyFilters(): void {
    this.bankPage.set(0);
    this.loadBank();
  }

  protected clearFilters(): void {
    this.search = '';
    this.chapterId = '';
    this.difficulty = '';
    this.examPattern = '';
    this.applyFilters();
  }

  protected goToBankPage(page: number): void {
    if (page < 0 || page >= this.bankPages()) {
      return;
    }
    this.bankPage.set(page);
    this.loadBank();
  }

  protected isChosen(question: QuestionSummaryResponse): boolean {
    return this.chosenIds().has(question.id);
  }

  protected toggle(question: QuestionSummaryResponse): void {
    if (!this.editable()) {
      return;
    }
    this.dirty.set(true);
    this.notice.set(null);
    if (this.isChosen(question)) {
      this.chosen.update((list) => list.filter((q) => q.id !== question.id));
    } else {
      this.chosen.update((list) => [...list, question]);
    }
  }

  protected removeAt(index: number): void {
    if (!this.editable()) {
      return;
    }
    this.dirty.set(true);
    this.chosen.update((list) => list.filter((_, i) => i !== index));
  }

  /**
   * Moves a question one place along the paper. Buttons rather than drag-and-drop: the paper
   * is edited on a laptop but the same markup is used on a tablet, where a drag is easy to
   * start by accident and there is no undo.
   */
  protected move(index: number, delta: -1 | 1): void {
    const target = index + delta;
    const list = this.chosen();
    if (!this.editable() || target < 0 || target >= list.length) {
      return;
    }
    const next = [...list];
    [next[index], next[target]] = [next[target], next[index]];
    this.chosen.set(next);
    this.dirty.set(true);
  }

  protected clearPaper(): void {
    if (!this.editable() || this.chosen().length === 0) {
      return;
    }
    this.chosen.set([]);
    this.dirty.set(true);
  }

  protected save(): void {
    if (!this.editable() || this.saving()) {
      return;
    }
    this.error.set(null);
    this.notice.set(null);
    this.saving.set(true);

    this.tests
      .setQuestions(
        this.testId,
        this.chosen().map((q) => q.id),
      )
      .subscribe({
        next: (updated) => {
          this.saving.set(false);
          this.dirty.set(false);
          this.test.set(updated);
          const n = this.chosen().length;
          this.notice.set(
            `Paper saved — ${n} question${n === 1 ? '' : 's'}. The test is now ${n} questions long. Go live from the test list when you are ready.`,
          );
        },
        error: (err: unknown) => {
          this.saving.set(false);
          this.error.set(toApiFailure(err).message);
        },
      });
  }

  protected patternLabel(pattern: ExamPattern): string {
    return pattern === 'JEE_MAIN' ? 'JEE Main' : 'JEE Advanced';
  }

  private loadBank(): void {
    this.bankLoading.set(true);
    const query: QuestionQuery = {
      page: this.bankPage(),
      size: PAGE_SIZE,
      // Only a published question belongs on a paper a class will sit.
      status: 'PUBLISHED',
      search: this.search.trim() || undefined,
      chapterId: this.chapterId === '' ? undefined : this.chapterId,
      difficulty: this.difficulty === '' ? undefined : this.difficulty,
      examPattern: this.examPattern === '' ? undefined : this.examPattern,
    };

    this.questions.list(query).subscribe({
      next: (page) => {
        this.bank.set(page.content);
        this.bankTotal.set(page.totalElements);
        this.bankPages.set(page.totalPages);
        this.bankLoading.set(false);
      },
      error: (err: unknown) => {
        this.bankLoading.set(false);
        this.error.set(toApiFailure(err).message);
      },
    });
  }
}
