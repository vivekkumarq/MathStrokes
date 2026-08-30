import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { ApiFailure, QuestionResultStatus, ReviewQuestion, toApiFailure } from '../../core';
import { MathContent } from '../../shared/math/math-content';
import { ExamService } from '../exam/exam.service';

/**
 * Question-by-question review.
 *
 * This renders the paper AS IT WAS SAT. The stem, options, answer key and marks all come from
 * the snapshot taken when the attempt started, so a question the teacher has since rewritten,
 * re-keyed or archived still shows here exactly as the student saw it.
 *
 * It is also the only screen where the answer key exists at all — the live attempt payload has
 * no correctness field on it.
 */
@Component({
  selector: 'app-review-page',
  imports: [RouterLink, MathContent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './review-page.html',
  styleUrl: './review-page.scss',
})
export class ReviewPage {
  private readonly exam = inject(ExamService);
  private readonly route = inject(ActivatedRoute);

  protected readonly attemptId = Number(this.route.snapshot.paramMap.get('attemptId'));
  protected readonly questions = signal<ReviewQuestion[]>([]);
  protected readonly loading = signal(true);
  protected readonly failure = signal<ApiFailure | null>(null);
  protected readonly filter = signal<QuestionResultStatus | 'ALL'>('ALL');

  protected readonly visible = computed(() => {
    const mode = this.filter();
    const all = this.questions();
    return mode === 'ALL' ? all : all.filter((q) => q.resultStatus === mode);
  });

  protected readonly counts = computed(() => {
    const all = this.questions();
    return {
      ALL: all.length,
      CORRECT: all.filter((q) => q.resultStatus === 'CORRECT').length,
      PARTIALLY_CORRECT: all.filter((q) => q.resultStatus === 'PARTIALLY_CORRECT').length,
      INCORRECT: all.filter((q) => q.resultStatus === 'INCORRECT').length,
      UNANSWERED: all.filter((q) => q.resultStatus === 'UNANSWERED').length,
    };
  });

  constructor() {
    this.exam.review(this.attemptId).subscribe({
      next: (questions) => {
        this.questions.set(questions);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.failure.set(toApiFailure(error));
        this.loading.set(false);
      },
    });
  }

  protected setFilter(mode: QuestionResultStatus | 'ALL'): void {
    this.filter.set(mode);
  }

  protected statusLabel(status: QuestionResultStatus): string {
    switch (status) {
      case 'CORRECT':
        return 'Correct';
      case 'PARTIALLY_CORRECT':
        return 'Partially correct';
      case 'INCORRECT':
        return 'Incorrect';
      default:
        return 'Not attempted';
    }
  }

  protected statusClass(status: QuestionResultStatus): string {
    switch (status) {
      case 'CORRECT':
        return 'is-correct';
      case 'PARTIALLY_CORRECT':
        return 'is-partial';
      case 'INCORRECT':
        return 'is-incorrect';
      default:
        return 'is-blank';
    }
  }

  /** Marks read as +4 / -1 rather than 4 / -1, so a penalty is unmistakable. */
  protected signed(marks: number): string {
    return marks > 0 ? `+${marks}` : `${marks}`;
  }
}
