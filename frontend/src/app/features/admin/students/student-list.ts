import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { toApiFailure } from '../../../core/http/api-failure';
import {
  AttemptHistoryItem,
  StudentPerformanceResponse,
  StudentSummaryResponse,
  paperScopeLabel,
} from '../../../core/models';
import { StudentAdminService } from '../data/student-admin.service';
import { AdminShell } from '../layout/admin-shell';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-admin-student-list',
  imports: [AdminShell, DatePipe, DecimalPipe, FormsModule],
  templateUrl: './student-list.html',
  styleUrl: './student-list.scss',
})
export class AdminStudentList {
  /** testKind first, chapter only for a practice paper. Shared so the screens agree. */
  protected readonly scopeLabel = paperScopeLabel;

  private readonly students = inject(StudentAdminService);

  protected readonly rows = signal<StudentSummaryResponse[]>([]);
  protected readonly total = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly page = signal(0);
  protected readonly loading = signal(true);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  /** Which student's detail panel is open, if any. */
  protected readonly expandedId = signal<number | null>(null);

  // Detail for the open student. Fetched on expand rather than up front: loading
  // performance and attempts for every row would be dozens of calls to show one panel.
  protected readonly detailLoading = signal(false);
  protected readonly detailError = signal<string | null>(null);
  protected readonly performance = signal<StudentPerformanceResponse | null>(null);
  protected readonly attempts = signal<AttemptHistoryItem[]>([]);

  protected search = '';

  protected readonly rangeLabel = computed(() => {
    const count = this.rows().length;
    if (count === 0) {
      return 'No students';
    }
    const from = this.page() * PAGE_SIZE + 1;
    return `${from}-${from + count - 1} of ${this.total()}`;
  });

  /** Signed in at some point, as opposed to registered and never returned. */
  protected readonly activeCount = computed(
    () => this.rows().filter((s) => s.lastLoginAt !== undefined).length,
  );

  constructor() {
    this.load();
  }

  protected applySearch(): void {
    this.page.set(0);
    this.load();
  }

  protected clearSearch(): void {
    this.search = '';
    this.applySearch();
  }

  protected goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) {
      return;
    }
    this.page.set(page);
    this.expandedId.set(null);
    this.load();
  }

  protected toggle(student: StudentSummaryResponse): void {
    const alreadyOpen = this.expandedId() === student.id;
    this.expandedId.set(alreadyOpen ? null : student.id);
    if (!alreadyOpen) {
      this.loadDetail(student.id);
    }
  }

  /** True once an attempt has a result worth showing. */
  protected isEvaluated(attempt: AttemptHistoryItem): boolean {
    return attempt.status === 'EVALUATED';
  }

  private loadDetail(id: number): void {
    this.detailLoading.set(true);
    this.detailError.set(null);
    this.performance.set(null);
    this.attempts.set([]);

    this.students.performance(id).subscribe({
      next: (p) => this.performance.set(p),
      // The attempts list is the more useful half, so a failed summary must not hide it.
      error: () => undefined,
    });

    this.students.attempts(id).subscribe({
      next: (page) => {
        // Discard a response for a student the admin has since collapsed or moved past.
        if (this.expandedId() === id) {
          this.attempts.set(page.content);
        }
        this.detailLoading.set(false);
      },
      error: (err: unknown) => {
        this.detailLoading.set(false);
        this.detailError.set(toApiFailure(err).message);
      },
    });
  }

  protected setEnabled(student: StudentSummaryResponse, enabled: boolean): void {
    this.busy.set(true);
    this.error.set(null);
    this.students.setEnabled(student.id, enabled).subscribe({
      next: () => {
        this.busy.set(false);
        // Patch in place rather than refetching, so the row does not jump under the cursor.
        this.rows.update((list) => list.map((s) => (s.id === student.id ? { ...s, enabled } : s)));
      },
      error: (err: unknown) => {
        this.busy.set(false);
        this.error.set(toApiFailure(err).message);
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);

    this.students
      .list({ page: this.page(), size: PAGE_SIZE, search: this.search.trim() || undefined })
      .subscribe({
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
