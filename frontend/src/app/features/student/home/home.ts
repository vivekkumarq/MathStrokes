import { Component, inject } from '@angular/core';

import { AuthService } from '../../../core/auth/auth.service';
import { AuthStore } from '../../../core/auth/auth.store';

@Component({
  selector: 'app-student-home',
  imports: [],
  template: `
    <main class="page">
      <header class="bar">
        <p class="brand">MathStrokes</p>
        <div class="who">
          <span>{{ store.displayName() }}</span>
          <button class="ms-btn ms-btn--ghost" type="button" (click)="signOut()">Sign out</button>
        </div>
      </header>
      <section class="ms-card panel">
        <h1>Welcome back, {{ store.displayName() }}</h1>
        <p class="muted">
          Choose a chapter and an exam pattern to begin a 25-question, 60-minute test.
        </p>
        <p class="ms-alert ms-alert--info">
          Test selection is not wired up yet — the backend exam endpoints are still being built.
        </p>
      </section>
    </main>
  `,
  styles: `
    .page {
      max-width: 960px;
      margin: 0 auto;
      padding: 24px;
    }
    .bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 24px;
    }
    .brand {
      font-size: 12px;
      font-weight: 700;
      letter-spacing: 0.09em;
      text-transform: uppercase;
      color: var(--ms-primary);
    }
    .who {
      display: flex;
      gap: 14px;
      align-items: center;
      font-size: 14px;
      color: var(--ms-ink-muted);
    }
    .panel {
      padding: 28px;
    }
    .panel h1 {
      font-size: 22px;
    }
    .muted {
      margin-top: 8px;
      color: var(--ms-ink-muted);
    }
    .ms-alert {
      margin-top: 20px;
    }
  `,
})
export class StudentHome {
  protected readonly store = inject(AuthStore);
  private readonly auth = inject(AuthService);

  protected signOut(): void {
    // Clear locally regardless of the server's answer: a failed logout call must never
    // leave the student stuck in a session they asked to end.
    this.auth.logout().subscribe({
      next: () => this.store.clearSession(),
      error: () => this.store.clearSession(),
    });
  }
}
