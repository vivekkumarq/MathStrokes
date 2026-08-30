import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import {
  ApiFailure,
  AuthService,
  AuthStore,
  TestSummaryResponse,
  toApiFailure,
} from '../../../core';
import { ExamService } from '../../exam/exam.service';
import { Logo } from '../../../shared/brand/logo';

/**
 * The tests a student can sit.
 *
 * Whether a test is startable is the server's decision, not arithmetic done here: it sends
 * `canStart`, and `unavailableReason` as student-facing prose when it is false. Rendering that
 * verbatim keeps one wording in one place instead of two that can drift.
 */
@Component({
  selector: 'app-test-list',
  imports: [NgTemplateOutlet, Logo],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './test-list.html',
  styleUrl: './test-list.scss',
})
export class TestList {
  private readonly exam = inject(ExamService);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  protected readonly auth = inject(AuthStore);

  protected readonly tests = signal<TestSummaryResponse[]>([]);

  /**
   * A full-syllabus paper arrives without a chapter, because it draws across all of
   * them. This single predicate is the only place that decides, so if the backend ever
   * signals it differently only this line changes.
   */
  protected isFullSyllabus(test: TestSummaryResponse): boolean {
    return test.chapterId === undefined;
  }

  /** Full-syllabus papers first: they are the headline, not one row among 56. */
  protected readonly fullSyllabusTests = computed(() =>
    this.tests().filter((t) => this.isFullSyllabus(t)),
  );

  protected readonly chapterTests = computed(() =>
    this.tests().filter((t) => !this.isFullSyllabus(t)),
  );
  protected readonly loading = signal(true);
  protected readonly failure = signal<ApiFailure | null>(null);
  /** The test whose Start button was pressed, so only that card shows a spinner. */
  protected readonly starting = signal<number | null>(null);

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.failure.set(null);
    this.exam.availableTests().subscribe({
      next: (tests) => {
        this.tests.set(tests);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.failure.set(toApiFailure(error));
        this.loading.set(false);
      },
    });
  }

  /**
   * Start and resume are the same call. If an attempt is already in flight the server hands
   * back that attempt rather than creating a second one, so the button can say "Resume"
   * without needing a different code path behind it.
   */
  protected start(test: TestSummaryResponse): void {
    if (!test.canStart || this.starting() !== null) {
      return;
    }
    this.starting.set(test.id);
    this.failure.set(null);
    this.exam.startOrResume(test.id).subscribe({
      next: (attempt) => {
        this.starting.set(null);
        void this.router.navigate(['/exam', attempt.attemptId]);
      },
      error: (error: unknown) => {
        this.starting.set(null);
        this.failure.set(toApiFailure(error));
      },
    });
  }

  protected patternLabel(pattern: string): string {
    return pattern === 'JEE_MAIN' ? 'JEE Main' : 'JEE Advanced';
  }

  /**
   * Revoking the refresh token server-side is best effort; tearing down the local session is
   * not. A failed logout call must never leave a student stuck in a session they asked to end,
   * so the session is cleared on both outcomes.
   */
  protected signOut(): void {
    this.authService.signOut();
  }

}
