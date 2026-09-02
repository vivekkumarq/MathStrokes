import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnDestroy,
  computed,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, concatMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import {
  ApiFailure,
  AnswerStatus,
  AttemptQuestion,
  AttemptResponse,
  SaveAnswerRequest,
  isAttemptClosedFailure,
  toApiFailure,
} from '../../core';
import { Logo } from '../../shared/brand/logo';
import { MathContent } from '../../shared/math/math-content';
import { ExamService } from './exam.service';
import { paperScopeLabel } from '../../core/models';

/** Local working state for one question, kept in step with the server's acks. */
interface QuestionState {
  selectedOptionIds: number[];
  markedForReview: boolean;
  visited: boolean;
  answerStatus: AnswerStatus;
}

/**
 * The examination screen.
 *
 * Three things here are load-bearing and easy to get wrong:
 *
 * 1. The countdown is the SERVER's. We measure the offset between the server clock and this
 *    browser's at load, and re-measure it on every autosave ack, then tick locally against
 *    that offset. A student whose machine clock is wrong, or whose tab was suspended, still
 *    sees the real remaining time.
 *
 * 2. Autosave always sends the COMPLETE selection with a monotonic sequence number. The ack
 *    reports what the server actually stored, which is not always what we sent: a late write
 *    that lost a race comes back accepted:false carrying the newer state, and we adopt that
 *    rather than our own optimistic value.
 *
 * 3. Radio versus checkbox follows each question's own questionType, never the paper's exam
 *    pattern. A JEE Advanced paper may contain single-correct questions.
 */
@Component({
  selector: 'app-exam-runner',
  imports: [MathContent, Logo],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './exam-runner.html',
  styleUrl: './exam-runner.scss',
})
export class ExamRunner implements OnDestroy {
  /** testKind first, chapter only for a practice paper. Shared so the screens agree. */
  protected readonly scopeLabel = paperScopeLabel;

  private readonly exam = inject(ExamService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly attempt = signal<AttemptResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly failure = signal<ApiFailure | null>(null);
  protected readonly index = signal(0);
  protected readonly submitting = signal(false);
  protected readonly confirmOpen = signal(false);
  protected readonly saving = signal(false);
  protected readonly paletteOpen = signal(false);

  /** attemptQuestionId -> working state. */
  private readonly states = signal<Record<number, QuestionState>>({});

  /** Server clock minus browser clock, in milliseconds. */
  private serverOffsetMs = 0;
  private expiresAtMs = 0;
  private sequence = 0;
  private ticker?: ReturnType<typeof setInterval>;
  private readonly debounceTimers = new Map<number, ReturnType<typeof setTimeout>>();
  private readonly saveQueue = new Subject<SaveAnswerRequest>();
  private autoSubmitted = false;

  /** Drives the countdown; re-read every second so the computed values refresh. */
  private readonly tick = signal(Date.now());

  protected readonly questions = computed(() => this.attempt()?.questions ?? []);
  protected readonly current = computed<AttemptQuestion | null>(
    () => this.questions()[this.index()] ?? null,
  );

  protected readonly remainingSeconds = computed(() => {
    this.tick();
    if (!this.expiresAtMs) {
      return 0;
    }
    const serverNow = Date.now() + this.serverOffsetMs;
    return Math.max(0, Math.round((this.expiresAtMs - serverNow) / 1000));
  });

  protected readonly clock = computed(() => {
    const total = this.remainingSeconds();
    const hours = Math.floor(total / 3600);
    const minutes = Math.floor((total % 3600) / 60);
    const seconds = total % 60;
    const pad = (value: number) => String(value).padStart(2, '0');
    return hours > 0
      ? `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
      : `${pad(minutes)}:${pad(seconds)}`;
  });

  /** Under five minutes the timer turns urgent; under one, critical. */
  protected readonly clockLevel = computed(() => {
    const left = this.remainingSeconds();
    if (left <= 60) {
      return 'critical';
    }
    return left <= 300 ? 'urgent' : 'normal';
  });

  protected readonly answeredCount = computed(
    () => Object.values(this.states()).filter((s) => s.selectedOptionIds.length > 0).length,
  );
  protected readonly markedCount = computed(
    () => Object.values(this.states()).filter((s) => s.markedForReview).length,
  );
  protected readonly unansweredCount = computed(
    () => this.questions().length - this.answeredCount(),
  );

  constructor() {
    this.saveQueue
      .pipe(
        // concatMap, not switchMap: every question's save must reach the server. Cancelling an
        // in-flight save because a different question changed would silently drop an answer.
        concatMap((request) => this.sendSave(request)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    const attemptId = Number(this.route.snapshot.paramMap.get('attemptId'));
    this.load(attemptId);
  }

  ngOnDestroy(): void {
    this.stopTicker();
    this.debounceTimers.forEach((timer) => clearTimeout(timer));
    this.debounceTimers.clear();
  }

  private load(attemptId: number): void {
    this.loading.set(true);
    this.exam.attempt(attemptId).subscribe({
      next: (attempt) => this.adopt(attempt),
      error: (error: unknown) => {
        const failure = toApiFailure(error);
        this.loading.set(false);
        // Already submitted, or the sweep finalised it while the tab was closed: the student
        // wants their result, not an error page.
        if (isAttemptClosedFailure(failure) || failure.code === 'BUSINESS_RULE_VIOLATION') {
          void this.router.navigate(['/results', attemptId]);
          return;
        }
        this.failure.set(failure);
      },
    });
  }

  private adopt(attempt: AttemptResponse): void {
    this.attempt.set(attempt);
    this.sequence = attempt.clientSequence;
    this.syncClock(attempt.timing.serverTime, attempt.timing.expiresAt);

    const states: Record<number, QuestionState> = {};
    for (const question of attempt.questions) {
      states[question.attemptQuestionId] = {
        selectedOptionIds: [...question.selectedOptionIds],
        markedForReview: question.markedForReview,
        visited: question.visited,
        answerStatus: question.answerStatus,
      };
    }
    this.states.set(states);
    this.loading.set(false);

    // Resume where they left off: the first question they have not yet visited.
    const firstUnseen = attempt.questions.findIndex((q) => !q.visited);
    this.index.set(firstUnseen === -1 ? 0 : firstUnseen);
    this.markVisited();
    this.startTicker();
  }

  /**
   * Re-anchors the local clock to the server's. Called on load and on every ack, so drift
   * cannot accumulate over a 60-minute paper.
   */
  private syncClock(serverTime: string, expiresAt: string): void {
    this.serverOffsetMs = Date.parse(serverTime) - Date.now();
    this.expiresAtMs = Date.parse(expiresAt);
  }

  private startTicker(): void {
    this.stopTicker();
    this.ticker = setInterval(() => {
      this.tick.set(Date.now());
      if (this.remainingSeconds() <= 0) {
        this.autoSubmit();
      }
    }, 1000);
  }

  private stopTicker(): void {
    if (this.ticker) {
      clearInterval(this.ticker);
      this.ticker = undefined;
    }
  }

  // ----------------------------------------------------------------- navigation

  protected goTo(position: number): void {
    if (position < 0 || position >= this.questions().length) {
      return;
    }
    this.index.set(position);
    this.paletteOpen.set(false);
    this.markVisited();
  }

  protected next(): void {
    this.goTo(this.index() + 1);
  }

  protected previous(): void {
    this.goTo(this.index() - 1);
  }

  protected stateOf(questionId: number): QuestionState | undefined {
    return this.states()[questionId];
  }

  protected isSelected(questionId: number, optionId: number): boolean {
    return this.stateOf(questionId)?.selectedOptionIds.includes(optionId) ?? false;
  }

  protected statusOf(questionId: number): AnswerStatus {
    return this.stateOf(questionId)?.answerStatus ?? 'NOT_VISITED';
  }

  /** Maps a palette state onto the CSS custom properties defined in styles.scss. */
  protected paletteClass(status: AnswerStatus): string {
    switch (status) {
      case 'ANSWERED':
        return 'is-answered';
      case 'NOT_ANSWERED':
        return 'is-not-answered';
      case 'MARKED_FOR_REVIEW':
        return 'is-review';
      case 'ANSWERED_AND_MARKED_FOR_REVIEW':
        return 'is-answered-review';
      default:
        return 'is-not-visited';
    }
  }

  // ----------------------------------------------------------------- answering

  /**
   * Radio behaviour: replaces the selection. Checkbox behaviour: toggles within it.
   * Which one applies is decided by the question's own type.
   */
  protected select(question: AttemptQuestion, optionId: number): void {
    const state = this.stateOf(question.attemptQuestionId);
    if (!state || this.isClosed()) {
      return;
    }

    let selected: number[];
    if (question.questionType === 'SINGLE_CORRECT') {
      selected = state.selectedOptionIds[0] === optionId ? [optionId] : [optionId];
    } else {
      selected = state.selectedOptionIds.includes(optionId)
        ? state.selectedOptionIds.filter((id) => id !== optionId)
        : [...state.selectedOptionIds, optionId];
    }
    this.patch(question.attemptQuestionId, { selectedOptionIds: selected });
  }

  protected clearAnswer(): void {
    const question = this.current();
    if (!question || this.isClosed()) {
      return;
    }
    this.patch(question.attemptQuestionId, { selectedOptionIds: [] });
  }

  protected toggleReview(): void {
    const question = this.current();
    const state = question && this.stateOf(question.attemptQuestionId);
    if (!question || !state || this.isClosed()) {
      return;
    }
    this.patch(question.attemptQuestionId, { markedForReview: !state.markedForReview });
  }

  private markVisited(): void {
    const question = this.current();
    const state = question && this.stateOf(question.attemptQuestionId);
    if (!question || !state || state.visited || this.isClosed()) {
      return;
    }
    this.patch(question.attemptQuestionId, { visited: true });
  }

  /**
   * Applies the change locally so the UI responds immediately, then schedules a save.
   *
   * Per-question debounce rather than one global one: debouncing globally would drop a save
   * for question 3 when the student moved on and changed question 4 within the window.
   */
  private patch(questionId: number, change: Partial<QuestionState>): void {
    const states = { ...this.states() };
    const existing = states[questionId];
    if (!existing) {
      return;
    }
    const updated: QuestionState = { ...existing, ...change };
    updated.answerStatus = this.deriveStatus(updated);
    states[questionId] = updated;
    this.states.set(states);

    const pending = this.debounceTimers.get(questionId);
    if (pending) {
      clearTimeout(pending);
    }
    this.debounceTimers.set(
      questionId,
      setTimeout(() => {
        this.debounceTimers.delete(questionId);
        this.enqueueSave(questionId);
      }, 350),
    );
  }

  /** Mirrors the server's AnswerStatus.of, so the palette is right before the ack lands. */
  private deriveStatus(state: QuestionState): AnswerStatus {
    const answered = state.selectedOptionIds.length > 0;
    if (!state.visited && !answered && !state.markedForReview) {
      return 'NOT_VISITED';
    }
    if (answered) {
      return state.markedForReview ? 'ANSWERED_AND_MARKED_FOR_REVIEW' : 'ANSWERED';
    }
    return state.markedForReview ? 'MARKED_FOR_REVIEW' : 'NOT_ANSWERED';
  }

  private enqueueSave(questionId: number): void {
    const state = this.stateOf(questionId);
    if (!state) {
      return;
    }
    this.sequence += 1;
    this.saveQueue.next({
      attemptQuestionId: questionId,
      selectedOptionIds: state.selectedOptionIds,
      markedForReview: state.markedForReview,
      visited: state.visited,
      clientSequence: this.sequence,
    });
  }

  private sendSave(request: SaveAnswerRequest) {
    const attemptId = this.attempt()?.attemptId;
    this.saving.set(true);
    return new Promise<void>((resolve) => {
      if (!attemptId) {
        this.saving.set(false);
        resolve();
        return;
      }
      this.exam.saveAnswer(attemptId, request).subscribe({
        next: (ack) => {
          this.saving.set(false);
          this.syncClock(ack.timing.serverTime, ack.timing.expiresAt);
          this.sequence = Math.max(this.sequence, ack.clientSequence);
          // The server reports what it actually holds. When our write lost a race this is
          // the NEWER state, not ours, so adopt it rather than keeping the optimistic value.
          const states = { ...this.states() };
          const existing = states[ack.attemptQuestionId];
          if (existing) {
            states[ack.attemptQuestionId] = {
              ...existing,
              selectedOptionIds: [...ack.selectedOptionIds],
              markedForReview: ack.markedForReview,
              answerStatus: ack.answerStatus,
            };
            this.states.set(states);
          }
          resolve();
        },
        error: (error: unknown) => {
          this.saving.set(false);
          const failure = toApiFailure(error);
          if (isAttemptClosedFailure(failure)) {
            // Time ran out, or the sweep finalised it. Stop accepting input and collect the
            // result rather than leaving the student typing into a closed paper.
            this.autoSubmit();
          } else if (!failure.offline) {
            this.failure.set(failure);
          }
          resolve();
        },
      });
    });
  }

  // ----------------------------------------------------------------- submission

  protected isClosed(): boolean {
    return this.submitting() || this.autoSubmitted;
  }

  protected openConfirm(): void {
    this.confirmOpen.set(true);
  }

  protected closeConfirm(): void {
    this.confirmOpen.set(false);
  }

  /** Manual submission, from the confirmation dialog. */
  protected submit(): void {
    this.confirmOpen.set(false);
    this.finish();
  }

  /**
   * Time expired. No confirmation: the paper is over whether or not anyone clicks. Answers
   * already saved on the server are what get marked, so nothing acknowledged is lost.
   */
  private autoSubmit(): void {
    if (this.autoSubmitted) {
      return;
    }
    this.autoSubmitted = true;
    this.stopTicker();
    this.finish();
  }

  private finish(): void {
    const attemptId = this.attempt()?.attemptId;
    if (!attemptId || this.submitting()) {
      return;
    }
    this.submitting.set(true);
    this.stopTicker();

    // Flush anything still sitting in a debounce window before closing the paper.
    this.debounceTimers.forEach((timer, questionId) => {
      clearTimeout(timer);
      this.enqueueSave(questionId);
    });
    this.debounceTimers.clear();

    this.exam.submit(attemptId).subscribe({
      next: () => {
        this.submitting.set(false);
        void this.router.navigate(['/results', attemptId]);
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        const failure = toApiFailure(error);
        // Submission is idempotent server-side, so a conflict here means it is already
        // finalised - which is a success from the student's point of view.
        if (isAttemptClosedFailure(failure)) {
          void this.router.navigate(['/results', attemptId]);
          return;
        }
        this.failure.set(failure);
      },
    });
  }

  protected togglePalette(): void {
    this.paletteOpen.update((open) => !open);
  }

  protected dismissError(): void {
    this.failure.set(null);
  }
}
