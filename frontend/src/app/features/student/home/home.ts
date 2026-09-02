import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../../core/auth/auth.service';
import { AuthStore } from '../../../core/auth/auth.store';
import {
  AttemptHistoryItem,
  StudentPerformanceResponse,
  paperScopeLabel,
} from '../../../core/models';
import { Logo } from '../../../shared/brand/logo';
import { StudentService } from '../data/student.service';

@Component({
  selector: 'app-student-home',
  imports: [DatePipe, DecimalPipe, Logo, RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class StudentHome {
  /** testKind first, chapter only for a practice paper. Shared so the screens agree. */
  protected readonly scopeLabel = paperScopeLabel;

  protected readonly store = inject(AuthStore);
  private readonly auth = inject(AuthService);
  private readonly students = inject(StudentService);

  protected readonly performance = signal<StudentPerformanceResponse | null>(null);
  protected readonly recent = signal<AttemptHistoryItem[]>([]);
  protected readonly loading = signal(true);

  constructor() {
    this.students.performance().subscribe({
      next: (performance) => this.performance.set(performance),
      // The dashboard degrades to its call-to-action rather than showing an error page:
      // a student who has never sat a test still needs the route to the test list.
      error: () => undefined,
    });

    this.students.history(0, 5).subscribe({
      next: (page) => {
        this.recent.set(page.content);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  /** An attempt only has a result worth opening once it has been evaluated. */
  protected isEvaluated(attempt: AttemptHistoryItem): boolean {
    return attempt.status === 'EVALUATED';
  }

  protected isResumable(attempt: AttemptHistoryItem): boolean {
    return attempt.status === 'ACTIVE' || attempt.status === 'NOT_STARTED';
  }

  protected signOut(): void {
    this.auth.signOut();
  }
}
