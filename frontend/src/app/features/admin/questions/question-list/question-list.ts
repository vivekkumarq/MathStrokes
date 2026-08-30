import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { toApiFailure } from '../../../../core/http/api-failure';
import {
  ChapterResponse,
  DIFFICULTIES,
  Difficulty,
  EXAM_PATTERNS,
  ExamPattern,
  QUESTION_STATUSES,
  QUESTION_TYPES,
  QuestionQuery,
  QuestionStatus,
  QuestionSummaryResponse,
  QuestionType,
} from '../../../../core/models';
import { MathContent } from '../../../../shared/math/math-content';
import { AdminCatalogService } from '../../data/admin-catalog.service';
import { QuestionService } from '../../data/question.service';
import { AdminShell } from '../../layout/admin-shell';

const PAGE_SIZE = 15;

@Component({
  selector: 'app-question-list',
  imports: [AdminShell, FormsModule, MathContent, RouterLink],
  templateUrl: './question-list.html',
  styleUrl: './question-list.scss',
})
export class QuestionList {
  private readonly questions = inject(QuestionService);
  private readonly catalog = inject(AdminCatalogService);
  private readonly router = inject(Router);

  protected readonly patterns = EXAM_PATTERNS;
  protected readonly difficulties = DIFFICULTIES;
  protected readonly types = QUESTION_TYPES;
  protected readonly statuses = QUESTION_STATUSES;

  protected readonly chapters = signal<ChapterResponse[]>([]);
  protected readonly rows = signal<QuestionSummaryResponse[]>([]);
  protected readonly total = signal(0);
  protected readonly page = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  // Filters. Bound with ngModel because they are plain scalars, not a validated form.
  protected chapterId: number | '' = '';
  protected examPattern: ExamPattern | '' = '';
  protected difficulty: Difficulty | '' = '';
  protected questionType: QuestionType | '' = '';
  protected status: QuestionStatus | '' = '';
  protected search = '';

  protected readonly rangeLabel = computed(() => {
    const count = this.rows().length;
    if (count === 0) {
      return 'No questions';
    }
    const from = this.page() * PAGE_SIZE + 1;
    return `${from}-${from + count - 1} of ${this.total()}`;
  });

  constructor() {
    this.catalog.chapters().subscribe({
      next: (chapters) => this.chapters.set(chapters),
      // A failed chapter lookup only costs the filter dropdown; the list still works.
      error: () => undefined,
    });
    this.load();
  }

  protected applyFilters(): void {
    this.page.set(0);
    this.load();
  }

  protected clearFilters(): void {
    this.chapterId = '';
    this.examPattern = '';
    this.difficulty = '';
    this.questionType = '';
    this.status = '';
    this.search = '';
    this.applyFilters();
  }

  protected goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) {
      return;
    }
    this.page.set(page);
    this.load();
  }

  protected edit(id: number): void {
    void this.router.navigate(['/admin/questions', id]);
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);

    const query: QuestionQuery = {
      page: this.page(),
      size: PAGE_SIZE,
      chapterId: this.chapterId === '' ? undefined : this.chapterId,
      examPattern: this.examPattern === '' ? undefined : this.examPattern,
      difficulty: this.difficulty === '' ? undefined : this.difficulty,
      questionType: this.questionType === '' ? undefined : this.questionType,
      status: this.status === '' ? undefined : this.status,
      search: this.search.trim() === '' ? undefined : this.search.trim(),
    };

    this.questions.list(query).subscribe({
      next: (result) => {
        this.rows.set(result.content);
        this.total.set(result.totalElements);
        this.totalPages.set(result.totalPages);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.loading.set(false);
        this.rows.set([]);
        this.error.set(toApiFailure(err).message);
      },
    });
  }
}
